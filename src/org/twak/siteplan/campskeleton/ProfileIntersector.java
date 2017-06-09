package org.twak.siteplan.campskeleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import javax.vecmath.Point2d;

import org.twak.camp.ui.Bar;
import org.twak.siteplan.campskeleton.Profile.GlobalProfile;
import org.twak.utils.Line;
import org.twak.utils.collections.ItComb;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;

/**
 * A hoodicky to calculate the nearest still active bar, at the start of a chain.
 *
 * Remember that you're working in the graphics' space. that is, origin at top left (or base of root chain...)
 * 
 * @author twak
 */
public class ProfileIntersector
{

    PriorityQueue <Event> events;
    TreeSet<ILine> beachline = new TreeSet( new XOrderLineComparator() );

    // height of sweep line through line-field (used for entering stuff into the beachline)
    Point2d processing;

    final static int END = 2, INTERSECT = 4, GLOBAL = 8;

    // the main result. as each global is added, this map stores the bar immediately to the right
    public Map< Loop<Bar>, Profile.ProfileChainInfo> globalToRightMostBar = new HashMap();

    /**
     */
    public void intersectLines ( Profile profile )
    {
         LoopL<Bar> a = profile.points;
        index_ = 0;
        events = new PriorityQueue<Event>();

        // add first bar of each loop into the collision tree
//        for ( Global g : profile.globalProfiles.keySet() )
//        {
//            GlobalProfile gp = profile.getGlobalProfile(g);
//            if (!gp.enabled)
//                continue;
//
//            for (Loop<Bar> loop : gp.chainStarts)
//            {
//                Loopable<Bar> orig = loop.getFirstLoopable();
//
//                ILine iL = new ILine(orig, loop);
//
//                Event gS = new GlobalEvent(iL.start, loop);
//                gS.a = iL;
//
//                events.add(gS);
//            }
//        }

        for ( Loop<Bar> loop : a )
        {
            Loopable<Bar> orig = loop.getFirstLoopable();

            ILine iL = new ILine( orig, loop );

            Event gS = new GlobalEvent( iL.start, loop );
            gS.a = iL;
            events.add( gS );
        }

        // get all events at the equivelent location
        while ( events.size() > 0 )
        {
            // cosited events of different types
            Set<Event> ends = new HashSet(), intersects = new HashSet(), globals = new HashSet();

            Event e = events.peek();
            Event f = e;

            while ( e.location.equals(f.location) )
            {
                Event toAdd = events.poll();
                switch (toAdd.type)
                {
                    case INTERSECT:
                        intersects.add(toAdd);
                        break;
                    case END:
                        ends.add(toAdd);
                        break;
                    case GLOBAL:
                        globals.add(toAdd);
                        break;
                }

                f = events.peek();
                if (f == null)
                    break;
            }

//            dumpBeach();
            process( ends, intersects, globals, e.location );
        }

        /**
         * two chains are normally added per global
         * however the order that this sweep line processes them is always left (-ve x) to right. This means that sometimes the outermost chain is
         * added first, and sometime the innermost. (Depending on if we're currently inside or outside the volume).
         *
         * we fix this up here.
         *
         * sorry. twak. xxx.
         */
        for ( Profile.GlobalProfile go : profile.globalProfiles.values())
        {
//            if (!go.enabled)
//                continue;
            
            if (go.g.valency == 2)
            {
                Profile.ProfileChainInfo i1 = globalToRightMostBar.get ( go.chainStarts.get( 0 ));
                Profile.ProfileChainInfo i2 = globalToRightMostBar.get ( go.chainStarts.get( 1 ));

                // if one references the other then we're a-okay.
                if (i1.barToRight == i2.profile.getFirst() || i2.barToRight == i1.profile.getFirst())
                {
                    
                }
                else // both should reference the same bar. fix up the one with the highest absolute distance
                {
                    assert (i1.barToRight == i2.barToRight);
                    if (Math.abs(i1.distance) < Math.abs( i2.distance ))
                    {
                        i2.distance = - (i1.distance - i2.distance);
                        i2.barToRight = i1.profile.getFirst();
                    }
                    else
                    {
                        i1.distance = - (i2.distance - i1.distance);
                        i1.barToRight = i2.profile.getFirst();
                    }
                }

            }
        }

    }

    private void collide( ILine a_, ILine b_ )
    {
        ILine a = a_, b = b_;

        // if final lines, extend to "infinity"

        double big = -10000;

        if (a.isFinal())
            a = new ILine (a.start, new Point2d ( a.xAtY( big ), big ) );


        if (b.isFinal())
            b = new ILine (b.start, new Point2d ( b.xAtY( big ), big ) );

        Point2d collide = a.intersects( b );

        if (collide == null)
            return;

        for (ILine l : new ILine[] {a,b})
        {
            if (l.isHoriz())
                collide.y = l.start.y;
            if (l.isVert())
                collide.x = l.start.x;
        }

        if ( a.start.equals( b.end ) || a.start.equals( b.start ))
            collide = a.start;

        if ( a.end.equals( b.end ) || a.end.equals( b.start ))
            collide = a.end;

        // don't report collisions behind the line
        if (collide.y > processing.y || (collide. y == processing.y && collide.x <= processing.x))
            return;


        Event e = new Event(collide, INTERSECT);
        e.a = a_;
        e.b = b_;
        events.add(e);
    }

    /**
     *
     * @param starts line-start events
     * @param ends line-end events
     * @param intersects line-line intersect events
     */
    private void process (Set<Event> ends, Set<Event> intersects, Set<Event> globals, Point2d location )
    {
        // just gimme an event!
        Event sample =
                globals.size() > 0 ? globals.iterator().next() :
            ends.size() > 0 ?
                ends.iterator().next() :
            intersects.iterator().next();


        Set<Line> ending = new HashSet();
        for (Event e : ends)
            ending.add (e.a);

        for ( Event e : new ItComb<Event> (ends, intersects ) ) // intersects.get(0).b.equals (intersects.get(1).b)
        {
            boolean removed = beachline.remove(e.a);
//            assert (removed);


            if (e.b != null)
            {
                removed = beachline.remove(e.b);
//                assert(removed);
            }
        }

        // remove end events for intersecting lines.
        for ( Event e : intersects )
        {
           events.remove( e.a.endEvent );
           events.remove( e.b.endEvent );
        }


        // to be able to remove horizontal lines, the value of processing must be the same as when they were last touched
        // (otherwise they're lost in the table). horiztonal lines should be involved in every event at their level
        processing = location;

        // all elements in 3 lists have same x value. Higer will return next higher value in beachlines
        ILine nextLine = beachline.higher ( sample.a );
        ILine prevLine = beachline.lower( sample.a );

        Set<Line> processed = new HashSet();
        
        for (Event e : ends)
        {
            Loopable<Bar> next = e.a.barC.getNext();
            // if not back at the start!
            if (next != e.a.bottom.getFirstLoopable() )
            {
                addBar(next, e.a.bottom, processed);
            }
        }

        for (Event e_ : globals)
        {
            GlobalEvent e = (GlobalEvent)e_;

            addToResults (location, e.bottom );

            // add in end-event for the line
            addBar (e.a.barC, e.bottom, processed );
        }

        
        for (Event e : ends)
            processed.remove( e.a );

        // if we've removed everything we processed
        if (processed.size() == 0)
        {
            if (prevLine != null && nextLine != null ) // not at end or start of beachline
                collide( prevLine, nextLine);
        }
        else
        {
            // go through start and intersections Pto find min and maximum
            // deal with horizontal lines!

            List<ILine> p = new ArrayList(processed);
            Collections.sort( p, new XOrderLineComparator() );

            Set<ILine> horiz= new HashSet();
            for ( ILine l : p )
                if ( l.isHoriz() )
                    horiz.add( l );

            if (nextLine != null )
            {
                ILine h = p.get( p.size() - 1 );
                if ( !horiz.isEmpty() )
                    for ( ILine l : horiz )
                        collide( l, nextLine );
                else // business as normal
                    collide( h, nextLine );

            }

            if (prevLine != null)
            {
                ILine l = p.get( 0 );
                collide( l, prevLine );
            }
        }
    }

    /**
     * The next bar is being added. The bar is contained in the Loop<Bar> bottom. The new line is added to processed
     * so that it may have collision checks performed against it.
     *
     * The most important thing to do here is to check that this isn't a horizontal line. If it is, we don't add it, but
     * remove whatever it collides with from the beachfront.
     *
     * doesn't deal with a horizontal meeting another horizontal.
     *
     */
    private void addBar( Loopable<Bar> next, Loop<Bar> bottom, Set<Line> processed )
    {
        ILine neu = new ILine( next, bottom );

        if (neu.isHoriz())
        {
            // horizontal lines collide with the line to the left/right.
            boolean goingRight = neu.end.x > neu.start.x;
            
            ILine collidesWith = null;

            if ( goingRight )
            {
                collidesWith = beachline.floor( verticalLineAt( neu.start.x ) );

                if ( collidesWith != null && !neu.isFinal() && collidesWith.xAtY( neu.start.y ) > neu.end.x )
                {
                    // not a collision - we stop going horizontally before we get this far
                    addBar (next.getNext(), bottom, processed);
                    return;
                }

            }
            else // goingLeft
            {
                collidesWith = beachline.ceiling( verticalLineAt( neu.start.x ) );
                if (collidesWith != null && !neu.isFinal() && collidesWith.xAtY( neu.start.y ) < neu.end.x )
                {
                    // not a collision - we stop going horizontally before we get this far
                    addBar (next.getNext(), bottom, processed);
                    return;
                }
            }


            if ( collidesWith != null )
            {
                events.remove( collidesWith.endEvent );
                beachline.remove( collidesWith );
            }
            else if (!neu.isFinal())// nothign to collide against, add in next bar
            {
                addBar (next.getNext(), bottom, processed);
            }
            return;
        }

        processed.add( neu );
        beachline.add( neu );

        // if not the final element in a profile, add an end in
        if ( next != bottom.getFirstLoopable().getPrev() )
        {
            neu.endEvent = new Event( neu.end, END );
            neu.endEvent.a = neu;
            events.add( neu.endEvent );
        }
    }

    private ILine verticalLineAt( double x )
    {
        return new ILine( new Point2d ( x, Double.MAX_VALUE), new Point2d ( x, -Double.MAX_VALUE));
    }

    private void addToResults( Point2d location, Loop<Bar> bottom )
    {
        ILine il = verticalLineAt(location.x);

        //find next bar in the right direction, else left, else null
        ILine right = beachline.floor( il );

        right = right == null ? beachline.ceiling( il ) : right;
        Bar bar = right == null ? null : right.barC.get();

        Profile.ProfileChainInfo out = new Profile.ProfileChainInfo();

        out.barToRight = bar;
        out.distance = horizDistance( bar, bottom.getFirst().start );
        out.profile = bottom;

        // to find out if this is an inside or outside pointing chain, we count the number of bars to left on the sweep plane.
        // we assume that the left most global is always processed first.
        out.into = beachline.subSet( il, verticalLineAt( -Double.MAX_VALUE )).size() % 2 == 0;

        globalToRightMostBar.put( bottom, out );
    }

    /**
     * Positive if start on the left of bar, -ve otherwise. Direct horizontal distance from start
     * to bar. Public as it's used to debug the globalprofile.nearestBars data structure.
     *
     * @return
     */
    public static double horizDistance( Bar bar, Point2d start )
    {
//        assert (start.y <= bar.start.y && start.y >= bar.end.y);

        if (bar == null)
            return -start.x; // just distance to 0

        double barXatY = new Line( bar.start, bar.end ).xAtY( start.y );
        return barXatY - start.x;
    }


    private static class PointComparator implements Comparator<Point2d>
    {
        public int compare( Point2d o1, Point2d o2 )
        {
            double r = o1.y - o2.y;

            if (r < 0)
                return 1;
            if (r > 0)
                return -1;

            r = o1.x - o2.x;

            if (r < 0)
                return -1;
            if (r > 0)
                return 1;
            return 0;
        }
    }

    static PointComparator pointComparator = new PointComparator();

    private class GlobalEvent extends Event
    {
        public final Loop<Bar> bottom;
        public GlobalEvent (Point2d location, Loop<Bar> bottom)
        {
            super (location, GLOBAL);
            this.bottom = bottom;
        }
    }

    private class Event implements Comparable<Event>
    {
        Point2d location = new Point2d(Double.MAX_VALUE, Double.MAX_VALUE);
        int type = 0;
        ILine a,b;

        public Event (Point2d location, int type)
        {
            this.location = location;
            this.type = type;
        }

        public int compareTo( Event o )
        {
            return pointComparator.compare(location, o.location);
        }

        @Override
        public String toString()
        {
            switch (type)
            {
                case GLOBAL:
                    return "global "+location;
                case END:
                    return "end   "+location;
                case INTERSECT:
                    return "inter "+location;
            }

            return "invalid type in Event!";
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 29 * hash + ( this.location != null ? this.location.hashCode() : 0 );
            hash = 29 * hash + this.type;
            hash = 29 * hash + ( this.a != null ? this.a.hashCode() : 0 )+ ( this.b != null ? this.b.hashCode() : 0 );
            return hash;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            final Event other = (Event) obj;
            if ( !this.location.equals  ( other.location ) )
                return false;
            if ( this.type != other.type )
                return false;
            if ( this.b != null  )
            {
                if ( this.a.equals( other.a ) )
                    return ( this.b.equals( other.b ) );
                else if ( this.b.equals( other.a ) )
                    return ( this.a.equals( other.b ) );
            }
            else if ( !this.a.equals( other.a ) )
                return false;

            return true;
        }
    }


    private void dumpBeach()

    {
        System.out.print( processing+":  " );
        for (Line l : beachline)
        {
            System.out.print(l+"["+l.xAtY( processing.y )+"]:: ");
        }
        System.out.println();
    }

    private class XOrderLineComparator implements Comparator<Line>
    {
        public int compare( Line o1, Line o2 )
        {
            Line horiz = null;
            Line notHoriz = null;
            int factor = 1;

            if (o1.equals( o2 ))
                return 0;

            // if both are horizontal, reults are undefined.
            if ( o1.isHoriz() && o2.isHoriz())
            {
                 if ( o1.start.y == o2.start.y )
                 {
                    // assign an arbitrary order
                    return o1.hashCode() - o2.hashCode();
//                if (o1.start.x != o2.start.x);
//                    return Double.compare( o1.start.x, o2.start.x );\
                 }
                 else
                 {
                     // shouldn't really be used!
                     return -Double.compare( o1.start.y, o1.end.y);
                 }
            }
            else if ( o1.isHoriz() )
            {
                horiz = o1;
                notHoriz = o2;
                factor = 1;
            }
            else if (o2.isHoriz())
            {
                horiz = o2;
                notHoriz = o1;
                factor = -1; // swap ordering
            }

            if ( horiz != null )
            {
                double x = notHoriz.xAtY( processing.y );

                if ( notHoriz.isVert() )
                    x = notHoriz.start.x;

                if (x < horiz.start.x)
                {
                    return 1 * factor;
                }
                else if (x > horiz.end.x)
                {
                    return -1 * factor;
                }
                else if ( processing.x < x ) // intersection between start and end
                {
                        return -1 * factor;
                }
                else if (processing.x > x)
                {
                        return 1 * factor;
                }
                else
                {
                    // horizontal is always second in the beachline to a vertical (if we are processing tha tpoint)
                    return factor;
                }
            }
            else
            {

            /*
             * We compare a little higher, so when
             * sweepLine == intersect point the order reflects the line's new positions
             */
                double height = processing.y + 0.0001;

                double
                h1 = o1.xAtY( height ),
                h2 = o2.xAtY( height );

                if (o1.isVert())
                    h1 = o1.start.x;
                if (o2.isVert())
                    h2 = o2.start.x;

                if (h1 == h2)
                {
                    // assign an arbitrary order to vertical lines
                    return o1.hashCode() - o2.hashCode();
                }
                else return -Double.compare( h1, h2 );
            }
        }
    }

   static int index_ = 0;

    public static class Collision
    {
        public Point2d location;
        public List<Line> lines;
        public int index = index_++;
        public Collision (Point2d loc, List<Line> lines)
        {
            this.location = loc;
            this.lines = lines;
        }
    }


    /**
     * We change the line directions to be homogenious. But we need to return a
     * list of the original Lines with the collisions. This class orders the original
     * line correctly, while keeping a reference to the original.
     */
    static class ILine extends Line
    {
        Loopable<Bar> barC;
        Loop<Bar> bottom;
        Event endEvent;

        public ILine (Point2d start, Point2d end)
        {
            super (start, end);
        }

        public ILine (Loopable<Bar> b_, Loop<Bar> bottom)
        {
            super ();

            Bar b = b_.get();

//            if (pointComparator.compare(b.start, b.end) > 0)
//            {
//                start = b.end;
//                end = b.start;
//            }
//            else
//            {
                start = b.start;
                end = b.end;
//            }

            this.barC = b_;
            this.bottom = bottom;
        }

        public boolean isFinal()
        {
            return bottom.getFirstLoopable().getPrev() == barC;
        }
    }


//    public static void main (String[] args)
//    {
//        int count = 100;
//        Random randy = new Random();
//        while (true)
//        {
//            count += 100;
//
//            List<Line> list = new ArrayList();
//            for (int i = 0; i < count; i++)
//            {
//                list.add (new Line (
//                        new Point2d(randy.nextInt( 600 ),randy.nextInt( 600 )),
//                        new Point2d(randy.nextInt( 600 ),randy.nextInt( 600 ))));
//            }
//            long t = System.currentTimeMillis();
//            ProfileIntersector i = new ProfileIntersector();
//            i.intersectLines( list );
//            System.out.printf ( "%d, %d \n", count, System.currentTimeMillis() - t);
//        }
//    }
}
