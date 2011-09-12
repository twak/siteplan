package camp.junk.wiggle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import no.geosoft.cc.geometry.spline.SplineFactory;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Machine;
import straightskeleton.offset.OffsetSkeleton;
import straightskeleton.Output;
import straightskeleton.Output.Face;
import straightskeleton.Output.SharedEdge;
import straightskeleton.Skeleton;
import straightskeleton.ui.Bar;
import straightskeleton.ui.PointEditor;
import utils.Cache;
import utils.ConsecutivePairs;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;
import utils.Pair;
import utils.UnionWalker;

/**
 *
 * @author twak
 */
public class WiggleUI extends PointEditor
{

    HouseListChanged houseListChanged = null;
    public WiggleUI()
    {
        
    }

    @Override
    protected void createInitial()
    {
        Loop<Bar> loop = new Loop();
        edges.add( loop );

        Point2d p  = new Point2d (300,0);
        loop.append( new Bar (new Point2d (0,0), p));
        loop.append( new Bar ( p, new Point2d (400,50)) );
    }

    @Override
    public void paintPointEditor( Graphics2D g2 )
    {
        paintPointEditor2( g2 );
//        Graphics2D g = FrameDumper.dumpFrame( 800,800);
//        paintPointEditor2( g );
    }
    public void paintPointEditor2( Graphics2D g2 )
    {
//              g2.setColor( Color.red );
//        for (Loop<Bar> loop : edges)
//        {
//            for (Bar bar : loop)
//                g2.drawLine(
//                        ma.toX( bar.start.x ),
//                        ma.toY( bar.start.y ),
//                        ma.toX( bar.end.x ),
//                        ma.toY( bar.end.y ) );
//            g2.setColor( Color.orange );
//        }
//
//
//        for (Bar bar : edges.eIterator())
//        {
//            drawPixel( g2, bar.start );
//            drawPixel( g2, bar.end );
//        }
//
//        double[] points = new double[(edges.count()+1)*3];
//
//        double lineLength = 0;
//
//        int i = 0;
//        for (Bar bar : edges.eIterator())
//        {
//            points[i++] = bar.start.x;
//            points[i++] = bar.start.y;
//            points[i++] = 0;
//            lineLength += bar.start.distance(bar.end);
//        }
//
//        Point2d last = edges.get( 0 ).start.getPrev().get().end;
//        points[i++] = last.x;
//        points[i++] = last.y;
//        points[i++] = 0;
//
////        double[] spline = SplineFactory.createCubic( points, (int)Math.ceil( length * 0.02 ));
//        double[] spline = SplineFactory.createCatmullRom( points, (int)Math.ceil( lineLength * 0.02 ));
//
//        // smooth for the ui
//        g2.setColor( Color.blue );
//        for (i = 3; i < spline.length; i+=3)
//        {
//            drawLine( g2, spline[i-3], spline[i-2], spline[i], spline[i+1] );
//        }
//
//        // find a the boundary of skinny line down the middle of the spline for the centre point of the houses.
//        List<Point2d> ribbon = new ArrayList();
//        spline = SplineFactory.createCatmullRom( points, (int) Math.ceil( lineLength * 0.01 ));//06 ) );
//        for ( i = 3; i < spline.length-3; i += 3 )
//        {
//            double dx = spline[i+3] - spline[i - 3];
//            double dy = spline[i+4] - spline [i-2];
//            double l = Math.sqrt( dx*dx+dy*dy);
//            dx /= l;//*10;
//            dy /= l;//*10;
//
//            Point2d above = new Point2d(spline[i]-dy, spline[i+1]+dx);
//            Point2d below = new Point2d(spline[i]+dy, spline[i+1]-dx);
//
//            ribbon.add( 0, above );
//            ribbon.add( below );
//
//            drawPixel( g2, above );
//            drawPixel( g2, below );
//        }
//
//        double splineLength = 0;
//        for (Pair <Point2d, Point2d> pair: new ConsecutivePairs<Point2d>( ribbon, false ))
//            splineLength += pair.first().distance(pair.second());
//        splineLength /=2;
//
//        LoopL <Edge> out = new LoopL( Edge.fromPoints(ribbon) );
//
//        Machine topMachine = new Machine( -Math.PI/4 );
//        Machine bottomMachine = new Machine( -Math.PI/4 );
//        Machine endMachine = new Machine( 0 );
//
//        int edgeCount = 0;
//        for (Edge e : out.eIterator())
//        {
//            if ( edgeCount ==  ((spline.length-6)/3)-1 || edgeCount == (((spline.length-6)/3) * 2)-1 )
//                e.machine = endMachine;
//            else
//            if ( edgeCount < spline.length / 3 -2 )
//                e.machine = bottomMachine;
//            else
//                e.machine = topMachine;
//            edgeCount++;
//        }
//
//        // create the offset from the skinny centrelines to the boundary of all the hosues
//        OffsetSkeleton offsetSkel = new OffsetSkeleton( out, 50, new OffsetSkeleton.OffsetEdgeReport() {
//
//            public void report( Edge input, Edge output, int valency )
//            {
//            }
//        } );
//
//        offsetSkel.registerProfile( topMachine, -Math.PI/4, 0 );
//        offsetSkel.registerProfile( bottomMachine, -Math.PI/4, 0 );
//        offsetSkel.registerProfile( endMachine, -Math.PI/8, 0 );
//
//
//        List<LoopL<Edge>> oList = offsetSkel.getResults();
//        if (oList.size() == 0)
//            return;
//        LoopL<Edge> offset = oList.get( 0 );
//
//
//        g2.setColor( Color.gray );
//        for ( Loop<Edge> loop : out )
//            for ( Edge edge : loop )
//                drawLine( g2, edge.start, edge.end );
//
//        g2.setColor( Color.cyan );
//        for ( Loop<Edge> loop : offset )
//            for ( Edge edge : loop )
//                drawLine( g2, edge.start, edge.end );
//
//        // plot count
//        int pCount = (int) ( splineLength / 150 );
//        double plotLength = splineLength/(pCount+1);
//
//        // calculate the location of each house and it's normal to the street
//        double dist = plotLength/2;
//        Point2d lastP = new Point2d (spline[0], spline[1]);
//        List<House> houses = new ArrayList();
//
//        for ( i = 3; i < spline.length - 3; i += 3 )
//        {
//            Point2d p = new Point2d (spline[i], spline[i+1]);
//
//            double delta = p.distance( lastP );
//            dist += delta;
//
//            if ( dist > plotLength )
//            {
//                dist -= plotLength;
//
//                House house = new House(houses.size() + 1);
//                house.tangent = new Vector2d( lastP.x - p.x, lastP.y - p.y );
//                Vector2d l = new Vector2d( lastP.x - p.x, lastP.y - p.y );
//                l.normalize();
//                l.scale( dist ); // we overshot by this much in this direction
//
//                house.location = new Point2d( p );
//                house.location.add( l );
//
//                g2.setColor( Color.red );
//                drawPixel( g2, house.location );
//                g2.drawString( house.toString(), ma.toX( house.location.x), ma.toY( house.location.y)+50);
//
//                houses.add( house );
//            }
//
//            lastP = p;
//        }
//
////        Machine machine = new Machine (0);
//        Machine newTop = new Machine (0);
//        Machine newBottom = new Machine (0);
//        Machine newEnd = new Machine (0);
//        for ( Loop<Edge> loop : offset )
//        {
//            for (Edge edge : loop)
//            {
//                edge.start.z = 0;
//
//                if (edge.machine == topMachine)
//                    edge.machine = newTop;
//                else if (edge.machine == bottomMachine)
//                    edge.machine = newBottom;
//                else if (edge.machine == endMachine)
//                    edge.machine = newEnd;
//            }
//        }
//
//        // add the seed of each house along the ling
//        for (House house : houses)
//        {
////            house.tangent.x += Math.random();
////            house.tangent.y += Math.random();
//            // unit square based in this house
//            house.tangent.normalize();
//            Vector2d t = new Vector2d (house.tangent);
//            Vector2d b = new Vector2d (house.tangent);
//            b.scale( -1 );
//            Vector2d l = new Vector2d (house.tangent.y, -house.tangent.x);
//            Vector2d r = new Vector2d(l);
//            r.scale( -1 );
//
//            Loop<Edge> housePlot = new Loop();
//            offset.add( housePlot );
//
//            Point2d start = house.location;
//            Corner tl= new Corner (start.x, start.y);
//            start.add (t);
//            Corner tr= new Corner (start.x, start.y);
//            start.add (r);
//            Corner br= new Corner (start.x, start.y);
//            start.add (b);
//            Corner bl= new Corner (start.x, start.y);
//
//            int j = 0;
//            for (Pair<Corner,Corner> pair : new ConsecutivePairs<Corner>( Arrays.asList( tl,bl, br, tr), true ) )
////            for (Pair<Corner,Corner> pair : new ConsecutivePairs<Corner>( Arrays.asList( tl,tr,br,bl), true ) )
//            {
//                Edge e = new Edge (pair.first(), pair.second());
//                house.base[j] = e;
//                e.machine= house.machine[j++];
//                housePlot.append( e );
//            }
//        }
//
//        Skeleton s = new Skeleton( offset );
//        s.skeleton();
//
//        if ( s.output.faces != null )
//        {
//            // seed growing outline
////            for (Face f : s.output.faces.values())
////            {
////                for (Loop<Point3d> loop : f.points)
////                {
////                    for (Loopable<Point3d> p : loop.loopableIterator())
////                        g2.drawLine( ma.toX (p.get().x), ma.toY (p.get().y), ma.toX (p.getNext().get().x), ma.toY (p.getNext().get().y));
////                }
////            }
//
//
//            Output output = s.output;
//
//            mergeFootprints (s, houses, newTop, newEnd);
//
//            for (House h : houses)
//            {
//                for ( Loop<Edge> loop : h.plot )
//                {
//                    Polygon pg = new Polygon();
//                    for ( Edge e : loop ) //loop.count()
//                        pg.addPoint( ma.toX( e.start.x ), ma.toY( e.start.y ) );
//
//                    if ( pg.npoints > 2 )
//                    {
//
////                        if ( count == 0 )
//                            g2.setColor( new Color( (int) ( Math.random() * 100 ) + 100, (int) ( Math.random() * 100 ) + 100, (int) ( Math.random() * 100 ) + 100, 100 ) );
////                        else
////                            g2.setColor( Color.black ); // hole..?
//
//                        g2.drawPolygon( pg );
//                    }
//
//                    for (Edge f : loop)
//                    {
//                        int j = Arrays.asList ( h.machine ).indexOf( f.machine );
//                        g2.setColor( Arrays.asList( Color.red, Color.yellow, Color.blue, Color.green ).get( j ) );
//                        drawLine( g2, f.start, f.end );
//                    }
//                }
//            }
//
//            if (houseListChanged != null)
//                houseListChanged.houseListChanged( houses );
////            if ( g2 != null )
////            {
////                g2.setStroke( new BasicStroke( 1 ) );
////                for ( Face face : s.output.faces.values() )
////                {
////                    LoopL<Point3d> loopl = face.getLoopL();
////                    int count = 0;
////
////                    for ( Loop<Point3d> loop : loopl )
////                    {
////                        Polygon pg = new Polygon();
////                        for ( Point3d p : loop ) //loop.count()
////                            pg.addPoint( ma.toX( p.x ), ma.toY( p.y ) );
////
////                        if ( pg.npoints > 2 )
////                        {
////                            g2.setColor( Color.black );
////                            g2.drawPolygon( pg );
////
////                            if ( count == 0 )
////                                g2.setColor( new Color( (int) ( Math.random() * 100 ) + 100, (int) ( Math.random() * 100 ) + 100, (int) ( Math.random() * 100 ) + 100, 100 ) );
////                            else
////                                g2.setColor( Color.black ); // hole..?
////
////                            g2.fillPolygon( pg );
////                        }
////                        count++;
////                    }
////
////                }
////            }
//        }
//    }
//
//    void mergeFootprints( Skeleton skel, List<House> houses, Machine topMachine, Machine endMachine )
//    {
//        Output output = skel.output;
//        for ( House h : houses )
//        {
//            Set<Face> faces = new HashSet();
//            for ( Edge e : h.base )
//                faces.add( output.faces.get( e ) );
//
//            UnionWalker uw = new UnionWalker();
//            boolean first = true;
//
//            Map <Point2d, Integer> adjacencies = new LinkedHashMap();
//
//            for ( int i = 0; i < 4; i++ )
//            {
//                Face f = output.faces.get( h.base[i] );
//
//                for ( SharedEdge se : f.edges.eIterator() )
//                    if ( !faces.contains( se.getOther( f ) ) )
//                    {
//                        Point3d start = se.getStart( f );
//                        Point3d end = se.getEnd( f );
//                        Point2d start2;
//                        uw.addEdge( start2 = new Point2d (start.x, start.y), new Point2d (end.x, end.y), first );
//                        first = false;
//                        Face over = se.getOther( f );
//                        //fixme: where do we loose the topMachine here?
////                        adjacencies.put (start2, over == null? 0 : ? 1 : 2); //f.edge.machine == h.machine[0]
//                        if (over != null)
//                        {
//                            if (over.edge.getAngle() == 0)
//                            {
//                                if (over.edge.machine == endMachine)
//                                    adjacencies.put( start2, 2 );
//                                else
//                                    adjacencies.put( start2, over.edge.machine == topMachine ? 0 : 1 );
//                            }
//                            else
//                                adjacencies.put( start2, 2 );
//                        }
//                        else adjacencies.put( start2, 3 );
//                    }
//            }
//
//            LoopL<Point2d> flatPoints = uw.find();
//
//            Cache<Point2d, Corner> cache = new Cache<Point2d, Corner>() {
//                @Override
//                public Corner create( Point2d i )
//                {
//                    return new Corner( i.x, i.y, 0 );
//                }
//            };
//
//
//            for (Loop< Point2d >loop : flatPoints)
//            {
//                Loop<Edge> plotEdge = new Loop();
//                h.plot.add( plotEdge );
//                for ( Loopable<Point2d> loopable : loop.loopableIterator() )
//                {
//                    Edge e = new Edge ( cache.get( loopable.get()), cache.get (loopable.getNext().get()));
//                    e.machine = h.machine[adjacencies.get( loopable.get() )];
//                    plotEdge.append( e );
//                }
//            }
//            Edge.reverse( h.plot );
//        }
    }
    

    public interface HouseListChanged
    {
        public void houseListChanged(List<House> houses);
    }

    public static void main (String[] args)
    {
        JFrame frame=  new JFrame();
        frame.setSize( 800,800);

        WiggleUI w = new WiggleUI();
        w.setup();
        frame.setContentPane( w );

        frame.setVisible( true );
    }
}
