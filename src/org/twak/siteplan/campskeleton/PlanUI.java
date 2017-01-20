
package org.twak.siteplan.campskeleton;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.vecmath.Point2d;

import org.twak.straightskeleton.ui.*;
import org.twak.utils.LContext;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.PolygonArea;
import org.twak.utils.ReverseList;
import org.twak.utils.ui.Colour;

/**
 * uses the point editor to create a plan
 * @author twak
 */
public class PlanUI extends MarkerUI
{
    protected Plan plan;
    protected int fileIndex = 0;
    public static Color grass = new Color( 150, 255, 150 );
    protected LoopL<Bar> shape;


    public BufferedImage bgImage = null;

    @Override
    protected void createInitial()
    {
        edges = shape;
        for ( Bar bar : edges.eIterator() )
            refreshMarkersOn( bar );
    }

    public PlanUI( Plan plan, PointEditor.BarSelected es )
    {
        this( plan, plan.points, es );
    }

    protected PlanUI( Plan plan, LoopL<Bar> shape, PointEditor.BarSelected es )
    {
        super (es);
        setBackground( grass );
        this.shape = shape;
        this.plan = plan;
        setup();
    }

    @Override
    protected boolean allowRemove( LContext<Bar> ctx, Point2d corner )
    {
        if (super.allowRemove( ctx, corner ))
            return true;

        if (ctx.hook == null) // it's an edge!
            return false;

        // otherwise tis a loop
        if ( ctx.loop.count() <= 3 )
        {
            if (edges.size () > 1)
                edges.remove( ctx.loop );
            else
                return false;
        }
        
        return true;
    }


    @Override
    public void releasePoint( Point2d pt, LContext<Bar> ctx, MouseEvent evt)
    {
//        if (pressed == null)
//            return; // if movePoint doesn't set pressed, we don't want the menu

        // show a menu if they didn't really move the marker
        if ( pt instanceof Marker )
//            if ( evt.getPoint().distanceSq( pressed ) < 25 ) // radius of 5
            {
                CampSkeleton.instance.setAnchorPlan ((Marker) pt);
                repaint();
            }
//        pressed = null;
    }

    @Override
    protected void edgeAdded(LContext<Bar> ctx)
    {
        // use a natural huristic based on a non-euclian-distance metric to select the correct profile
        Profile profile = plan.profiles.get ( ctx.loopable.getPrev().get() );
        if (profile == null)
            profile = plan.profiles.get( plan.points.get( 0 ).getFirst() );
        plan.profiles.put( ctx.get(), profile );
    }

    @Override
    public void createSection( Point loc , boolean inside)
    {
        createCircularPoints( 3, loc.x, loc.y, 50, inside );
        repaint();
    }

    protected JComponent getWidgetFor ( Bar b )
    {
        return null;
    }

    JComponent currentWidget;

    @Override
    protected void barSelected(LContext<Bar> selected)
    {
        super.barSelected(selected);
        
        if (currentWidget != null)
            remove (currentWidget);
        if (selected != null)
        {
            currentWidget = getWidgetFor(selected.get());
            if (currentWidget != null)
                add(currentWidget);
            repaint();
        }
        else
            currentWidget = null;
    }

//    @Override
//    public void painting(Point2d location, Point2d offset, MouseEvent evt)
//    {
//        barSelected (null);
//        super.painting(location, offset, evt);
//    }

    @Override
    public void paint(Graphics g) {

        // calls paintPointEditor below.
        super.paint(g);

        // update the bar-widget's positions
        if (currentWidget != null)
        {
            Point2d mid = new Point2d ( currentBar.get().start );
            mid.add(currentBar.get().end);
            mid.scale(0.5);
         
            Point p = ma.to(mid);
            currentWidget.setSize(currentWidget.getPreferredSize());
            p.x -= currentWidget.getWidth()/2;
            p.y -= currentWidget.getHeight()/2 + 30;
            currentWidget.setLocation(p);
        }

        paintChildren(g);
    }
    
    int dumpCount = 0;

    @Override
    public void paintPointEditor( Graphics2D g2 )
    {
        if (bgImage != null)
            ma.drawImage(g2, bgImage, 0, 0);

        for ( Loop<Bar> e2 : edges )
        {
            Polygon pg = new Polygon();
            PolygonArea pa = new PolygonArea();
            
            for ( Bar e : e2 )
            {
                pg.addPoint( ma.toX( e.start.x ), ma.toY( e.start.y ) );
                pa.add( e.start );
            }

            g2.setColor(Color.white);
            // needs a proper union approach! - or just figure out which polygons contain what & sort list in order
//            g2.setColor( Colour.transparent( pa.area() > 0 ? Color.lightGray : grass, 100 ) );

            g2.fillPolygon( pg );
        }

        drawGrid(g2);

//        for (Bar b : edges.eIterator())
//            drawTags( g2, b, plan );

        for ( Loop<Bar> e2 : edges )
            for ( Bar e : e2 )
            {
                g2.setStroke( new BasicStroke( currentBar == null ? 2f : currentBar.get() == e ? 4f : 2f ) );
                if (plan.profiles.get( e )!= null)
                    g2.setColor( plan.profiles.get( e ).color );
                g2.drawLine( ma.toX( e.start.x ), ma.toY( e.start.y ), ma.toX( e.end.x ), ma.toY( e.end.y ) );
            }



        g2.setColor( grass.darker().darker() );

        for ( Loop<Bar> e2 : edges )
            for ( Bar e : e2 )
                drawPixel( g2, e.start );

        drawMarkers( g2 );

        if (this.getClass() == PlanUI.class) // no - _you_ suck    ....   output should be 3d anywhoo
        {
            g2.setStroke (new BasicStroke(1));
            g2.setColor( new Color (200,200,200));

            somethingChanged( g2 );
        }
    }

    protected void somethingChanged( Graphics2D g2 )
    {
        CampSkeleton.instance.somethingChanged();
//        try
//        {painting
//            Skeleton s = new PlanSkeleton ( plan );
//            s.skeleton();
//
//            if ( s.output.faces != null )
//            {
//                Output output = s.output;
//
//                if ( g2 != null )
//                {
//                    g2.setStroke( new BasicStroke( 1 ) );
//                    for ( Face face : output.faces.values() )
//                        {
//                            LoopL<Point3d> loopl = face.getLoopL();
//                            int count = 0;
//
//                            for (Loop<Point3d> loop : loopl )
//                            {
//                                Polygon pg = new Polygon();
//                                for ( Point3d p : loop ) //loop.count()
//                                    pg.addPoint( ma.toX( p.x ), ma.toY( p.y ) );
//
//                                if ( pg.npoints > 2 )
//                                {
//                                    g2.setColor( Color.black );
//                                    g2.drawPolygon( pg );
//
//                                    if ( count == 0 )
//                                        g2.setColor( new Color( (int) ( Math.random() * 100 ) + 100, (int) ( Math.random() * 100 ) + 100, (int) ( Math.random() * 100 ) + 100, 100 ) );
//                                    else
//                                        g2.setColor( Color.black ); // hole..?
//
//                                    g2.fillPolygon( pg );
//                                }
//                                count++;
//                            }
//
//                        }
//                }
//                CampSkeleton.instance.show( s.output, s );
//            }
//        }
//        catch ( Throwable t )
//        {
//            t.printStackTrace();
//        }
    }
}
