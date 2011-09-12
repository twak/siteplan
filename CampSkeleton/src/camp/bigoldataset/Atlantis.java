/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camp.bigoldataset;

import camp.junk.wiggle.MeshFeature;
import camp.junk.wiggle.WindowFeature;
import campskeleton.Profile;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point2d;
import straightskeleton.Tag;
import straightskeleton.ui.Bar;
import utils.GraphMap;
import utils.Line;
import utils.Loop;
import utils.MUtils;
import utils.PolygonArea;

/**
 * @author twak
 */
public class Atlantis
{
    public List<Plot> plots = new ArrayList();
    public Set<Profile> profiles = new HashSet();

    Set<Point2d> roadPoints = new HashSet();
    GraphMap<Point2d> roads = new GraphMap();
    
    transient KDTree plotKD = null;
    transient KDTree roadKD = null;

    Tag windowFeature  = new Tag("window");
    Tag groundWindowFeature  = new Tag("window");
    Tag doorFeature = new Tag("door");
    Tag heightFeature = new Tag("height");

    public Atlantis()
    {
        doorFeature   = new MeshFeature("door.md5");
        windowFeature = new WindowFeature("second floor windows");//MeshFeature("window.md5");
        groundWindowFeature = new WindowFeature("ground floor windows");//new MeshFeature("window.md5");
//        plan.features.add(door);
    }

    public void updateAll() {
        plotKD = new KDTree(2);
        int i = 0;
        for (Plot p : plots) {
            System.out.println( (i++) + " from "+plots.size());
            update(p);
        }
    }

    public void update( Plot p )
    {
        for (Loop<Bar> loop : p.points) {
                for (Bar b : loop) {
                    try {
                        plotKD.insert(new double[]{b.start.x, b.start.y}, p);
                    } catch (Throwable ex) {
//                        ex.printStackTrace();
                    }
                }
            }
    }

    public class CachedLine extends Line
    {
        public CachedLine (Point2d start, Point2d end)
        {
            super(start, end);
        }

        public void setStart (Point2d start)
        {

        }

        public void setEnd( Point2d end )
        {
            
        }
    }

    public Map<Bar, Plot> lookup(Rectangle rectangle) {
        if (plotKD == null)
            updateAll();

        Map<Bar, Plot> res = new HashMap();

        try {
            Object[] out = plotKD.range(new double[]{rectangle.x, rectangle.y}, new double[]{rectangle.x + rectangle.width, rectangle.y + rectangle.height});

            for (Object o : out) {
                Plot p = (Plot) o;
                for (Bar b : p.points.eIterator())
                    res.put(b, p);
            }
        } catch (KeySizeException ex) {
            Logger.getLogger(Atlantis.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("detail: "+ res.size());

        return res;
    }

    Set<Plot> lookupPlot(double x1, double y1, double x2, double y2) {
        Set<Plot> plots = new HashSet();
        try {
            Object[] out = plotKD.range(new double[]{x1, y1}, new double[]{x2, y2});

            for (Object o : out) {
                plots.add((Plot) o);
            }
        } catch (KeySizeException ex) {
            Logger.getLogger(Atlantis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return plots;
    }




    public List<Point2d> getRoads(Rectangle rectangle)
    {
        List<Point2d> out = new ArrayList();
        try
        {

            if ( roadKD == null )
            {
                roadKD = new KDTree( 2 );
                for ( Point2d pt : roadPoints )
                    roadKD.insert( new double[]
                            {
                                pt.x, pt.y
                            }, pt );
            }


            for (Object o : roadKD.range( new double[]
                    {
                        rectangle.x, rectangle.y
                    }, new double[]
                    {
                        rectangle.x + rectangle.width, rectangle.y + rectangle.height
                    } ))
            {
                out.add((Point2d)o);
            }
            return out;
        }
        catch ( Throwable ex )
        {
            ex.printStackTrace();
        }

        return out;
    }

    public void addRoad( Point2d tl, Point2d br )
    {
        if (tl != null && br != null && !tl.equals(br))
            roads.add( tl, br );
    }
    
    void addRoadPoints( Point2d point2d )
    {
        roadPoints.add( point2d );
        try
        {
            roadKD.insert( new double[]{point2d.x, point2d.y}, point2d );
        }
        catch ( Throwable ex )
        {
            ex.printStackTrace();
        }
    }

    void removeRoadPoint( Point2d point2d )
    {
        roadPoints.remove( point2d );

        for ( Point2d pair : new HashSet<Point2d>( roads.get( point2d ) ) )
            roads.remove( point2d, pair );

        // rebuild tree...?!
        roadKD = null;
    }



    public Point2d findNearestPointOnRoad ( Point2d pt )
    {
        int n = 5;
        if (n > roadPoints.size())
            n = roadPoints.size();
        try
        {

            Point2d nearest = new Point2d(Math.random()*1000,Math.random()*1000);
            double dist = Double.MAX_VALUE;

//            for ( Point2d pt : points )
//            {
                Object[] res = roadKD.nearestKeys( new double[]
                        {
                            pt.x, pt.y
                        }, n );
                if (res != null)
                for ( Object o : res )
                {
                    double [] start_ = (double[])o;
                    Point2d start = new Point2d( start_[0], start_[1] );
                    for ( Point2d dest : roads.get( start ) )
                    {
                        Point2d onLine = new Line( start, dest ).project( pt, true );
                        if ( onLine != null )
                        {
                            double onLineDist = onLine.distanceSquared( pt );
                            if ( onLineDist < dist )
                            {
                                dist = onLineDist;
                                nearest = onLine;
                            }
                        }
                    }
//                }
            }
            
            return nearest;
        }
        catch ( Throwable ex )
        {
            ex.printStackTrace();
        }
        return null;
    }

//    public void findPlotsWithSelfIntersection()
//    {
//        Intersector is = new Intersector();
//        for (Plot p : plots)
//        {
//
//        }
//    }

    public void findRoadsForPlots(List<Plot> plots)
    {
        int i = 0;
        for (Plot plot : plots)
        {
            System.out.println("processing plot "+(i++) );

            Point2d point = new Point2d(); int c = 0;
            for (Bar b : plot.points.eIterator())
            {
                point.add (b.start);
                c++;
            }
            
            point.scale (1./c);

            plot.nearestRoad = findNearestPointOnRoad( point );
            System.out.println ("that would be " + plot.nearestRoad);

            plot.area = 0;
            // also calculate area
            for (Loop<Bar> loop : plot.points)
            {
                PolygonArea pa = new PolygonArea();
                for (Bar b : loop)
                    pa.add(b.start);
                plot.area += pa.area();
            }

            // make up some heights
            plot.height = MUtils.clamp (1.3 *plot.area / 100 + Math.random()*1, 1.5, 10);
        }
    }

}