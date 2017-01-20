package org.twak.siteplan.anchors;

import com.thoughtworks.xstream.XStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.vecmath.Point2d;
import javax.vecmath.Tuple2d;

import org.twak.siteplan.campskeleton.CampSkeleton;
import org.twak.siteplan.campskeleton.PlanUI;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.straightskeleton.Corner;
import org.twak.straightskeleton.offset.Offset;
import org.twak.straightskeleton.offset.PerEdgeOffsetSkeleton;
import org.twak.straightskeleton.ui.Bar;
import org.twak.straightskeleton.ui.Marker;
import org.twak.straightskeleton.ui.PointEditor;
import org.twak.utils.BackgroundUpdate;
import org.twak.utils.LContext;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;

/**
 * @author twak
 */
public class NaturalStepUI extends PlanUI
{
    NaturalStepShip step;


    public NaturalStepUI( NaturalStepShip step, PointEditor.BarSelected es )
    {
        super (CampSkeleton.instance.plan, step.shape , es);

        this.step = step;
//        edges = new LoopL();
//        createInitial();
    }

    @Override
    public void movePoint(LContext<Bar> ctx, Point2d pt, Point2d location, MouseEvent evt)
    {   
        if (pt instanceof Marker)
        {
            super.movePoint(ctx, pt, location, evt);
        }
        else if (!isFixed(pt))
        {
            pt.x = location.x;
            pt.y = location.y;

            for (Bar b : ctx.loop)
                refreshMarkersOn(b);


            skeletonCalculator.changed();
        }
    }

    @Override
    public void addBetween(LContext<Bar> ctx, Point l)
    {
        if (isFixed(ctx.get().start) && isFixed(ctx.get().end))
            return;
        
        super.addBetween(ctx, l);
    }

    @Override
    protected void addVertex(LContext<Bar> ctx, Point l)
    {
        super.addVertex(ctx, l);
// step.speeds.count()
        LContext addAfter = ctx.tranlsate(edges, step.speeds);
        addAfter.loop.addAfter(addAfter.loopable, addAfter.loopable.get());
    }


    @Override
    public void showMenu(LContext<Bar> dragged, MouseEvent evt)
    {
        super.showMenu(dragged, evt);



        if (dragged != null && dragged.hook == null) // this isn't a point, must be the entire bar
        {
            JPopupMenu popup = new JPopupMenu();
             
            popup.add(getSliderFor(dragged.get()));

            popup.show(this, evt.getX(), evt.getY()+50);

            
        }
    }

    protected JComponent getSliderFor(Bar b)
    {
        if (isFixed(b.start) && isFixed(b.end))
            return null;

        LContext<Double> toEdit = currentBar.tranlsate(edges, step.speeds);

        return new PlanSpeedWidget(this, toEdit.loopable);
    }

    @Override
    protected boolean allowRemove(LContext<Bar> ctx, Point2d corner)
    {
        if (isFixed(corner))
            return false;
        
        if ( ctx.hook == null ) // it's an edge!
            return false;

        if (!(corner instanceof Marker))
        {
            LContext<Double> beingRemoved = ctx.tranlsate(edges, step.speeds);
            beingRemoved.loop.remove(beingRemoved.loopable);
        }

        skeletonCalculator.changed();

        return super.allowRemove(ctx, corner);
    }

    private boolean isFixed(Point2d pt)
    {
        // location of first and last pair of points to be locked
        Loop<Bar> loop = shape.get(0);

        Bar s = loop.start.get(), e = loop.start.getPrev().get();
        return  pt == s.start ||
                pt == s.end   ||
                pt == e.start ||
                pt == e.end;
    }

    @Override
    public void paintPointEditor(Graphics2D g2)
    {
        drawGrid(g2);
        // draw the preview if it's been calculated yet...
        LoopL<Point2d> result = skeletonCalculator.get();

        g2.setColor (Color.black);
        g2.drawLine(ma.toX(150), 0, ma.toX(150), getHeight());

        if (result != null)
        {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor( new Color (255,196,152) );
            for (Loop<Point2d> loop : result)
            {
                Polygon poly = new Polygon();
                for (Loopable<Point2d> loopable : loop.loopableIterator())
                {
//                    drawLine(g2, loopable.get(), loopable.getNext().get());
                    poly.addPoint(ma.toX(loopable.get().x), ma.toY(loopable.get().y));
                }
                g2.fillPolygon(poly);
            }
        }

        for (Bar b : edges.eIterator())
            drawTags( g2, b, plan );

        for ( Loop<Bar> e2 : edges )
            for ( Bar e : e2 )
            {
                g2.setStroke( new BasicStroke( currentBar == null ? 2f : currentBar.get() == e ? 4f : 2f ) );
                g2.setColor( plan.profiles.get( e ).color );
                g2.drawLine( ma.toX( e.start.x ), ma.toY( e.start.y ), ma.toX( e.end.x ), ma.toY( e.end.y ) );
            }

        g2.setColor( grass.darker().darker() );

        for (Loop<Bar> e2 : edges)
            for (Bar e : e2)
                if (!isFixed(e.start))
                    drawPixel(g2, e.start);

        drawMarkers( g2 );
        somethingChanged(g2);
    }


    public BackgroundUpdate<LoopL<Point2d>> skeletonCalculator = new BackgroundUpdate<LoopL<Point2d>>()
    {
        @Override
        public LoopL<Point2d> update()
        {
            // create a tiny clone of the data structure that is a complete loop; add extra edges at the end of the loop
            LoopL<Bar> previewShape = Bar.clone(edges, NaturalStepShip.getScaleTransform());

            // move the end points out to give the ideal of an infinitely long wall
            previewShape.get(0).start.get().start.x -=300;
            previewShape.get(0).start.getPrev().get().end.x +=300;

            Loop<Bar> toInsert = previewShape.get(0);
            Point2d start = toInsert.start.getPrev().get().end,
             end = toInsert.start.get().start,
             br = new Point2d(end.x, 400),
             bl = new Point2d(start.x, 400);

            toInsert.append(new Bar (start, bl));
            toInsert.append(new Bar (bl, br));
            toInsert.append(new Bar (br, end));

            // similarly for the set of speeds (add in 3 vertical walls)
            LoopL<Double> previewSpeeds = step.speeds.new Map<Double>() {
                @Override
                public Double map(Loopable<Double> input)
                {
                    return new Double ( input.get() * 10 );
                }
            }.run();

            Loop<Double> toInsertS = previewSpeeds.get(0 );
            toInsertS.append(0.);
            toInsertS.append(0.);
            toInsertS.append(0.);

            Offset result = new PerEdgeOffsetSkeleton(Corner.fromBar( previewShape ), previewSpeeds).getResult();
            LoopL<Point2d> out = Corner.toPoint2d( result.shape );
            // re-allign with the rest of the ui
            for (Point2d pt : out.eIterator())
                pt.x +=150;

            repaint();

            return out;
        }
    };

    @Override
    public void moveLoop(Loop<Bar> draggedLoop, Tuple2d offset)
    {
        // do nothing
    }

    public boolean canExport() { return true; }
    public boolean canImport() { return true; }

    public void exportt(File f)
    {
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( f );
            new XStream().toXML( step, fos );
        }
        catch ( Exception ex )
        {
            JOptionPane.showMessageDialog( this, "error saving file :(" );
            ex.printStackTrace();
        } finally
        {
            try
            {
                if ( fos != null )
                    fos.close();
            }
            catch ( IOException ex )
            {
                ex.printStackTrace();
            }
        }
    }
    
    public void importt(File f)
    {
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(f);
            NaturalStepShip imported = (NaturalStepShip) new XStream().fromXML(fis);

            Profile p = plan.profiles.get ( shape.get(0).getFirst() );
            this.shape = step.shape = imported.shape;
            step.speeds = imported.speeds;

            for (Bar b : shape.eIterator())
            {
                plan.profiles.put(b, p);
                refreshMarkersOn(b);
            }

            createInitial();
            skeletonCalculator.changed();

            repaint();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally
        {
            try
            {
                if (fis != null)
                    fis.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public void createSection( Point loc , boolean inside)
    {
        return; // can't do this.
    }

}
