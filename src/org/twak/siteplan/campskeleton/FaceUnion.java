package org.twak.siteplan.campskeleton;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.vecmath.Point3d;

import org.twak.straightskeleton.Corner;
import org.twak.straightskeleton.Output.Face;
import org.twak.straightskeleton.Output.SharedEdge;
import org.twak.straightskeleton.debug.DebugDevice;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;
import org.twak.utils.MultiMap;

/**
 * Takes a bunch of faces and computes their union
 * @author twak
 */
public class FaceUnion <A>
{
//    public LoopL<Corner> results = new LoopL();

    public FaceUnion(){}

    MultiMap<Point3d, Point3d> map= new MultiMap();

    MultiMap<Point3d, DataEdge<A>>
            dataForPoint = new MultiMap<Point3d, DataEdge<A>>();

    static class DataEdge<A2>
    {
        A2 a;
        boolean top, bottom;
        int type;
        Point3d end;
    }

    public void add( Face face, A a )//, Edge originatingEdge, Machine inside, Machine outside, Machine sides
    {
        LoopL<Point3d> debug = new LoopL();

        for ( Loop<Point3d> loop : face.points )
        {
            Loop<Point3d> loopC = new Loop();
            debug.add( loopC );

            for ( Loopable<Point3d> loopable : loop.loopableIterator() )
            {
                // we build a map backwards :)
                map.put( new Point3d ( loopable.getNext().get()), new Point3d (  loopable.get() ) );

                loopC.append( new Point3d( loopable.get().x, loopable.get().y, loopable.get().z ) );
            }
        }

        DebugDevice.dumpPoints( "getGeometry in FaceUnion " + face, debug );

        for ( SharedEdge se : face.edges.eIterator() )
        {
            DataEdge de = new DataEdge();
            de.top = face.isTop( se );
            de.bottom = face.isBottom( se );

//            System.out.println(de.top+" is "+se);
//            System.out.println(" top "+de.top+" bot " + de.bottom+" is "+se);
            
            de.end = se.getEnd( face );
            de.a = a;

            // we index the face backwards, because that's the way we roll.
            dataForPoint.put( se.getStart( face ), de );
        }
    }


    public void add( Point3d a, Point3d b, A data )
    {
        map.put( new Point3d( a ), new Point3d( b ) );
        DataEdge<A> de = new DataEdge<A>();
        de.a = data;
        de.end = b;
        dataForPoint.put( a, de );
    }

    /**
     * If an edge is added to a map twice, it is removed! and it's meta data! for it is! an internal shared side edge!
     */
//    public void addToMap (Point3d a, Point3d b, A data)
    {
//        if ( map.get( b ).contains( a ) )
//        {
//            map.remove( b, a );
//
//            Iterator<DataEdge<A>> it = dataForPoint.get( b ).iterator();
//            while (it.hasNext())
//                if (it.next().end.equals( a) )
//                    it.remove();
//        }
//        else
            
    }

    /**
     * Valid points in output, only have one line along them
     * @return
     */
    private void findNoReturns()
    {
//        Set<Point3d> out = new HashSet();

        MultiMap<Point3d, Point3d> toKill = new MultiMap();

        for (Point3d pt : map.map.keySet())
        {
            for (Point3d goesTo : map.get(pt))
            {
                if (map.get(goesTo).contains(pt))
                {
                    toKill.put(pt, goesTo);
                    toKill.put(goesTo, pt);
                }
            }
        }
        
        for (Point3d ptKill : toKill.map.keySet())
            for (Point3d dest : toKill.get(ptKill))
            {
                map.remove(ptKill, dest);
                Iterator<DataEdge<A>> dit = dataForPoint.get(ptKill).iterator();

                while (dit.hasNext())
                {
                    DataEdge<A> de = dit.next();
                    if (de.end.equals( dest ) )
                        dit.remove();
                }
            }
    }

    public LoopL<Point3d> getGeometry() //  boolean reversed - the result is reversed from teh input
    {
//        LoopL<Point3d> debug = new LoopL();
//        for (Point3d s : map.map.keySet())
//            for  (Point3d e : map.get(s))
//            {
//                Loop<Point3d> pts = new Loop();
//                debug.add(pts);
//                pts.append(new Point3d(s));
//                pts.append(new Point3d(e));
//            }
//        DebugDevice.dumpPoints("pre", debug);
        findNoReturns();

//        debug = new LoopL();
//        for (Point3d s : map.map.keySet())
//            for  (Point3d e : map.get(s))
//            {
//                Loop<Point3d> pts = new Loop();
//                debug.add(pts);
//                pts.append(new Point3d(s));
//                pts.append(new Point3d(e));
//            }
//        DebugDevice.dumpPoints("post", debug);
        
        Set<Point3d> togo = new HashSet();
        for (Point3d lp : map.map.keySet())
            if (map.get(lp).size() > 0)
                togo.add(lp);

        LoopL<Point3d> out = new LoopL();

        while (!togo.isEmpty())
        {
            Loop<Point3d> loop = new Loop();
            out.add( loop );

            Point3d start = togo.iterator().next();
            Point3d current = start;

            int handbrake = 0;

            do
            {
                togo.remove( current );
                loop.append( current );
                
                List<Point3d> choices = map.get( current );

                for (Point3d pt : choices)
                      current = pt;

                // should only be 1
                current = choices.get( 0 );
            }
            while (!current.equals(start) && handbrake++ < 1000);

            if (handbrake >= 1000)
            {
                System.err.println("geometry error:");
                Thread.dumpStack();
            }
        }
        
        return out;
    }

    public A getData(Point3d p1)
    {
        List<DataEdge<A>> datum = dataForPoint.get( p1 );
        assert datum.size() == 1;

        return datum.get( 0 ).a;
    }

    public boolean isTop(Point3d p1)
    {
        List<DataEdge<A>> datum = dataForPoint.get( p1 );
        assert datum.size() == 1;

        return datum.get( 0 ).top;
    }

    public boolean isBottom(Point3d p1)
    {
        List<DataEdge<A>> datum = dataForPoint.get( p1 );
        assert datum.size() == 1;

        return datum.get( 0 ).bottom;
    }
}
