/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camp.bigoldataset;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point2d;
import javax.vecmath.Tuple2d;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import straightskeleton.ui.PointEditor;
import utils.LContext;
import utils.Line;
import utils.Loop;
import utils.PanMouseAdaptor;
import utils.PanMouseAdaptor.RangeListener;
import utils.ui.Colour;
import static camp.bigoldataset.AtlantisPointEditor.Mode.*;

/**
 *
 * @author twak
 */
public class AtlantisPointEditor extends PointEditor
{
    private int range;
    private Atlantis at;
    private Map <Bar, Plot> barToPlot = new HashMap();
//    Plot plot;
    public AtlantisEditor ae;
    Bar selected = null;

    @Override
    public void addBetween( LContext<Bar> ctx, Point l )
    {
        super.addBetween( ctx, l );
        Bar created = dragged.get();
        Plot plot = barToPlot.get( ctx.get() );
        barToPlot.put (created, plot);
        plot.profiles.put (created, plot.profiles.get ( ctx.get()) );
    }

    BarSelected bs = new BarSelected() {
        public void barSelected(LContext<Bar> ctx) {
            selected = ctx.get();
            Plot p = barToPlot.get(selected);
            ae.plotSelected (p);
            ae.barSelected (ctx, p);
        }
    };

    Point2d offsetSoFar = null; @Override
    public void moveLoop( Loop<Bar> draggedLoop, Tuple2d offset )
    {
        if (offsetSoFar == null)
            offsetSoFar = new Point2d(0,0);

        if (offset != null)
            offsetSoFar.add( offset ); // still dragging
        else
        {
            for ( Plot p : ae.selected )
                for ( Bar b : p.points.eIterator() )
                    b.start.add( offsetSoFar );

                at.updateAll();
                
            offsetSoFar = null;
        }
    }


    RangeListener rangeListener = new RangeListener() {
        @Override
        public void changed(PanMouseAdaptor ma) {
            updateLoops();
        }
    };

    public AtlantisPointEditor(AtlantisEditor ae)
    {
        this.ae = ae;
        setup();
        ma.addListener(rangeListener);
        barSelected = bs;
        ma.center( new Point2d( 1068, 1905 ) );
    }

    @Override
    protected void createInitial() {
        updateLoops();
    }

    Point2d nearestRoadPoint( Point2d loc )
    {
        double tol = ma.fromX( 5 );
        List<Point2d> pts = at.getRoads( new Rectangle( (int) (loc.x - tol), (int) (loc.y - tol), (int) (tol * 2), (int) (tol * 2) ) );

            if (pts.size() != 0)
            {
                Point2d nearest = null;//pts(0);
                double dist = Double.MAX_VALUE;

                for (Point2d pt :pts)
                {
                    double ptDist = pt.distance(loc);
                    if (ptDist < dist)
                    {
                        nearest = pt;
                        dist = ptDist;
                    }
                }

                return nearest;

            }
            return null;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
        repaint();
    }

    Mode mode = PLOTS;

    public enum Mode
    {
        ADD_ROAD_POINTS, REMOVE_ROAD_POINTS, ADD_ROADS, PLOTS;
    }

    Point2d tl, br, start;
    @Override
    public void painting( Point2d location, Point2d offset, MouseEvent evt )
    {
         // released
        if (location == null)
        {
            if ( mode == PLOTS )
            {
                if ( tl != null && br != null )
                {
                    Set<Plot> plots = at.lookupPlot( tl.x, tl.y, br.x, br.y ) ;
                    if (!plots.isEmpty())
                        ae.plotsSelected( plots );
                }
            }
            else if ( mode == ADD_ROADS && br != null )
            {
                if (tl != null);
                at.addRoad (tl, nearestRoadPoint(br) );
            }
            else if ( mode == ADD_ROAD_POINTS && br != null )
            {
                at.addRoadPoints( new Point2d( br ) );
            }
            else if ( mode == REMOVE_ROAD_POINTS && br != null )
            {
                Point2d nearestRoadPt = nearestRoadPoint( br );
                if ( nearestRoadPt != null )
                {
                    at.removeRoadPoint( nearestRoadPt );
                    repaint();
                }
            }

            tl = br = start = null;
            repaint();
            return;
        }

         // starting
        if (start == null)
        {
            if ( evt.getButton() != 1 )
                 return;

            if (mode == PLOTS)
            {
                start = location;
            }
            else if (mode == ADD_ROADS)
            {
                start = nearestRoadPoint(location);
            }
            else
            {
                start = location;
            }

        }

         // bail if not the right mousebutton.
        if ( start == null )
            return;

        if ( mode == PLOTS )
        {
            // rectangle
            tl = new Point2d( Math.min( start.x, location.x ), Math.min( start.y, location.y ) );
            br = new Point2d( Math.max( start.x, location.x ), Math.max( start.y, location.y ) );
        }
        else
        {
            //line
            tl = start;
            br = new Point2d( location );
        }
        repaint();
    }

    @Override
    public void paintPointEditor(Graphics2D g2) {

        for (Plot p : ae.selected)
            for (Bar b : p.points.eIterator())
        {
            g2.setStroke(new BasicStroke(6f));
            g2.setColor(Color.orange);
            drawLine( g2, new Line(b.start, b.end) );

            for (Marker m : b.mould.markersOn( b ))
            {
                int size = 5;
                g2.setColor(m.feature.color);
                g2.fillOval(ma.toX(m.x)-size, ma.toY(m.y)-size, size*2, size*2);
            }
        }

        g2.setStroke(new BasicStroke(2f));
        for (Point2d pt : at.getRoads( new Rectangle ( (int) ma.fromX( 0), (int)ma.fromY( 0), (int)ma.fromZoom(getWidth()), (int)ma.fromZoom(getHeight()))))
        {
            g2.setColor( Color.cyan );
            drawPixel( g2, pt );
            g2.setColor( Color.blue );
            List<Point2d> dests = at.roads.get( pt ) ;
            if (dests != null)
            for ( Point2d dest : dests)
            {
                drawLine( g2, new Line( pt, dest ) );
            }
        }

        for (Plot plot : ae.selected)
        {
            Point2d rdPt = ae.selected.get( 0 ).nearestRoad;
            if ( rdPt != null )
            {
                int size = 5;
                g2.setColor( Color.magenta );
                g2.fillOval(ma.toX(rdPt.x)-size, ma.toY(rdPt.y)-size, size*2, size*2);
            }

            g2.setStroke( new BasicStroke( 3f ) );
            g2.setColor( Color.black );
            for ( Loop<Bar> loop : plot.points )
                for ( Bar bar : loop )
                    g2.drawLine(
                            ma.toX( bar.start.x ),
                            ma.toY( bar.start.y ),
                            ma.toX( bar.end.x ),
                            ma.toY( bar.end.y ) );
        }


         // override me!

        g2.setStroke(new BasicStroke(1f));
//        g2.setColor(Color.red);
        for (Loop<Bar> loop : edges) {
            for (Bar bar : loop) {
                if (barToPlot.get( bar ) != null)
                g2.setColor (barToPlot.get( bar ).profiles.get(bar).color);
                g2.drawLine(
                        ma.toX(bar.start.x),
                        ma.toY(bar.start.y),
                        ma.toX(bar.end.x),
                        ma.toY(bar.end.y));
            }
        }

        g2.setColor(Color.orange);
        for (Bar bar : edges.eIterator())
            drawPixel( g2, bar.start );

        if (tl != null)
        {
                g2.setColor(Colour.transparent(Color.green, 100));
            if (mode == PLOTS)
            {
               g2.fillRect(ma.toX (tl.x), ma.toY(tl.y), ma.toZoom(br.x - tl.x),  ma.toZoom(br.y - tl.y));
            }
            else if (mode == ADD_ROADS )
            {
                drawLine (g2,new Line (tl, br));
            }
        }
    }



    public void setData(Atlantis at)
    {
        this.at = at;
        updateLoops();
        repaint();
    }

    public void updateLoops()
    {
        Point2d pt = ma.getCenter();
        double range = ma.getMaxRange();

        if (at != null)
        barToPlot = at.lookup (new Rectangle( (int)(pt.x - range/2), (int)(pt.y - range/2), (int)range, (int)range ) );
        edges.clear();
        for (Plot p : barToPlot.values())
            for (Loop<Bar> b : p.points)
                edges.add(b);

    }
}
