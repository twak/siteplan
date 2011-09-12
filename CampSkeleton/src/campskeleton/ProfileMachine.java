package campskeleton;

import camp.anchors.AnchorHauler.AnchorHeightEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.vecmath.Point2d;
import straightskeleton.Edge;
import straightskeleton.HeightEvent;
import straightskeleton.Machine;
import straightskeleton.Skeleton;
import straightskeleton.ui.Bar;
import straightskeleton.ui.DirectionHeightEvent;
import straightskeleton.ui.HorizontalHeightEvent;
import straightskeleton.ui.Marker;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;
import utils.MUtils;
import utils.PointDistanceComparator;

/**
 * There is a profile machine for every chain in a profile
 *
 * @author twak
 */
public class ProfileMachine extends Machine
{
   final static double limit = (Math.PI/2) * 0.95;
   
    // the globals use the profile to determine the new angle for offset surfaces
    public Profile profile;

    Global global;
    int valency;

    public ProfileMachine ( Profile profile )
    {
        super();
        this.profile = profile;

        setup(profile.points.get(0), 0, 0);
    }

    public ProfileMachine ( Profile profile, double hOffset, double hScale )
    {
        super();
        this.profile = profile;

        setup(profile.points.get(0), hOffset, hScale);
    }

//    public ProfileMachine( Profile profile, Loop<Bar> bars )
//    {
//        this (profile, bars, 0, 1);
//    }

    public ProfileMachine (Profile profile, Global g, int valency)
    {
        this (profile, g, valency, 0, 1);
    }
    
    public ProfileMachine (Profile profile, Global g, int valency, double hOffset, double hScale)
    {
        super();

        this.global = g;
        this.valency = valency;
        this.profile = profile;

        setup (profile.getGlobalProfile(g).chainStarts.get(valency), hOffset, hScale);
    }

    private void setup (Loop<Bar> bars, double hOffset, double hScale)
    {
        getDirections().clear();

        HorizontalHeightEvent lastHe = null;

        Iterator<Loopable<Bar>> bit = bars.loopableIterator().iterator();

        double height = -bars.start.get().start.y* hScale + hOffset;

        while (bit.hasNext())
        {
            Loopable<Bar> lBar = bit.next();
            Bar bar = lBar.get();

            double angle = findAngle( bar );

            // if next bar doesn't start with our end, we're the last bar in the monotone set.
            boolean isLast = bar.end != lBar.getNext().get().start;

            angle = MUtils.clamp( angle, -Math.PI / 2, Math.PI / 2 );

            DirectionHeightEvent he = null;
            
            if ( isApproxHoriz(bar) )
            {
                 // only used if horizontal

                List<Point2d> markers = new ArrayList ( bar.mould.getAnchorsReadOnly(bar.start, bar.end) );
                markers.add( isLast ? new Point2d ( angle < 0 ?  1000 : -1000 , -height) : new Point2d ( bar.end.x,- height) );

                Collections.sort(markers, new PointDistanceComparator(bar.start));

                Point2d last = bar.start;

//                if (isLast)
//                    last = ; // <-- big numbers


                for (Point2d m_ : markers)
                {
                    final Point2d m = m_;
                    double length = last.distance(m);

                    if (bar.end.x < bar.start.x)
                        length = -length;

                    angle = angle > 0 ? Math.PI/2 : -Math.PI/2;

                    if (m instanceof Marker)
                    {
                        // we are a marker on the bar
                        he = new HorizontalHeightEvent(this, height, length)
                        {
                            @Override
                            public void whenDone(Skeleton skel)
                            {
                                new AnchorHeightEvent( height, (Marker)m, ProfileMachine.this.global, ProfileMachine.this.valency, (PlanSkeleton) skel, profile ).process(skel);
                            }
                        };
                    }
                    else
                    {
                        // we are the end of the bar
                        he = new HorizontalHeightEvent(this, height, length);
                    }
                    
                    he.profileFeatures = bar.tags;
                    
                    if (lastHe == null)
                        addHeightEvent(he);
                    else
                        lastHe.next = he;

                    lastHe = (HorizontalHeightEvent) he;


                    last = m;
                }
            }
            else
            {
                he = new DirectionHeightEvent( this, height, angle );
                he.profileFeatures = bar.tags;
                
                if (lastHe == null)
                    addHeightEvent(he);
                else
                    lastHe.next = he;
                
                lastHe = null;

                height =  ( -bar.end.y ) * hScale + hOffset;
            }
        }
    }



//                if ( bit.hasNext() )
//                {
//                    Bar nextBar = bit.next();
//                    double nextAngle = findAngle( nextBar );
//                    assert ( angle > limit || angle < -limit );
//                    he = new HorizontalHeightEvent( this, ( -bar.start.y ) * hScale + hOffset, nextAngle, length );
//                    he.profileFeatures = nextBar.tags;
//                    ((HorizontalHeightEvent)he).horizProfileFeatures = bar.tags;
//                }
//                else
//                {
//                    boolean inside = bar.start.x < bar.end.x;
//                    // pick a erc for the max. extrude distance for the final (top) bar. Angle should be unused
//                    he = new HorizontalHeightEvent( this, ( -bar.start.y ) * hScale + hOffset, 0xDEADBEEF, inside ? 1000 : -1000 );
//                    ((HorizontalHeightEvent)he).horizProfileFeatures = bar.tags;
//                }

    /**
     * Used by other routines to determine if a bar is counted as being horizontal by profile machine.
     * @param bar
     * @return
     */
    public static boolean isApproxHoriz(Bar bar)
    {
        double angle = findAngle(bar);
        return angle > limit || angle < -limit;
    }

    /**
     * Flips the orientation of each change wrt the height
     */
    public void negate()
    {
        for ( HeightEvent he : getDirections() )
        {
            DirectionHeightEvent dh = (DirectionHeightEvent)he;
            dh.newAngle = -dh.newAngle;
        }
    }

    public static Iterable<Machine> machinesIn( LoopL<Edge> edges )
    {
        Set<Machine> out = new LinkedHashSet();

        for (Edge e : edges.eIterator())
            out.add( e.machine );
        
        return out;
    }

    private static double findAngle( Bar bar )
    {
        return Math.atan2( bar.end.y - bar.start.y, bar.end.x - bar.start.x ) +Math.PI/2;
    }
}
