package org.twak.siteplan.junk;

import java.util.Arrays;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;

import org.twak.siteplan.campskeleton.Global;
import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.PlanSkeleton;
import org.twak.siteplan.campskeleton.PlanUI;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.siteplan.campskeleton.ProfileMachine;
import org.twak.siteplan.campskeleton.StepUI;
import org.twak.straightskeleton.Corner;
import org.twak.straightskeleton.CornerClone;
import org.twak.straightskeleton.Edge;
import org.twak.straightskeleton.Tag;
import org.twak.straightskeleton.ui.Bar;
import org.twak.straightskeleton.ui.Marker;
import org.twak.straightskeleton.ui.PointEditor.BarSelected;
import org.twak.utils.ConsecutiveItPairs;
import org.twak.utils.LContext;
import org.twak.utils.Line3D;
import org.twak.utils.Loop;
import org.twak.utils.Loopable;
import org.twak.utils.MUtils;
import org.twak.utils.Pair;

/**
 *
 * @author twak
 */
public class NaturalStep extends NaturalFeature
{

    public boolean isOurs (Tag feature)
    {
        return feature instanceof NaturalStep;
    }

    public NaturalStep(Plan plan)
    {
        super(plan, "natural square step");

        Loop<Bar> line = new Loop();

        Profile profile1 = new Profile( 50 );
        Profile profile2 = new Profile( 50 );
        
        Point2d[] coords = new Point2d[] {
            new Point2d (0,0), new Point2d (50,0), new Point2d (50,-20), new Point2d (250,-20), new Point2d (250,0), new Point2d (300, 0) };

        Profile[] profs = new Profile[] {
            profile1, profile2, profile1, profile2, profile1
        };

        int i = 0;
        for ( Pair<Point2d, Point2d> pair : new ConsecutiveItPairs<Point2d>( Arrays.asList( coords ) ) )
        {
            Bar b = new Bar (pair.first(), pair.second());
            line.append( b );

            plan.profiles.put( b, profs[i++] );
        }

        shape.add( line );

//        plan.tags.add( this );
        plan.addLoop( profile1.points.get( 0 ), plan.root, profile1 );
        plan.addLoop( profile2.points.get( 0 ), plan.root, profile2 );
    }

    @Override
    public PlanUI getEditor( BarSelected bs )
    {
        return new StepUI( this, bs );
    }
    
    @Override
    public double getSize( Bar bar )
    {
        Loop<Bar> loop = shape.get( 0 );

        int count = 0;
        for (Bar b : loop)
            if (b == bar)
                break;
            else count++;

        switch ( count )
        {
            case 1:
            case 3:
            {
                Bar b = loop.start.getNext().getNext().get();
                return b.start.distance( b.end );
            }
            case 2:
                Bar b = loop.start.getNext().get();
                return b.start.y - b.end.y;//distance( b.end );
            default:
                return 0; // start or end bar, value should be ignored...
        }
    }
    
       public Corner insert(
            PlanSkeleton skel,
            Marker m,
            LContext<Corner> leadingCorner, // the edge we're editing
            Edge old,
            CornerClone cc, // the new data structure
            List<ProfileMachine> machines, // as specified by this NatrualFeature's shape LoopL<Bar>
            List<Bar> bars, // the bars from this NaturalFeature
            double height, // the height of this event
            Global global,
            int valency )
    {
        Corner insertAfter = leadingCorner.get();
        Corner insertBefore = leadingCorner.get().nextC;

        Vector2d perp = new Vector2d( m.bar.end.y - m.bar.start.y, m.bar.start.x - m.bar.end.x );
        perp.normalize();
        Vector2d perpI = new Vector2d( perp );
        perpI.scale( -1 );
        Vector2d tangent = new Vector2d( m.bar.end.x - m.bar.start.x, m.bar.end.y - m.bar.start.y );
        tangent.normalize();

        Bar verticalBar = bars.get( 1 );
        if (verticalBar.start.y - verticalBar.end.y < 0)
        {
            // protrusion should be intrusion!
            perp.scale( -1 );
            perpI.scale( -1 ); // {or just swap...}
        }

        double length = insertAfter.distance( insertAfter.nextC );
//        System.out.println("length is "+length);

        if ( length < 2 )
            return insertAfter;

        Line3D l3 = Line3D.fromStartEnd( insertAfter, insertBefore );
        double param = l3.projectParam( new Point3d( m.x, m.y, 0 ) );
        param = MUtils.clamp( param, 2. / length, 1 - 2. / length );

        double pStart = param - 0.5 / length;

        Corner insertStart = new Corner( l3.fromParam( pStart ) );

        Loopable lastLoopable = leadingCorner.loopable;

        Corner pos = insertStart;
        int i = 1; // skip first and last bars as "interface"
        for ( Vector2d v : Arrays.asList( perp, tangent, perpI ) )
        {
            Point2d loc = new Point2d( pos.x, pos.y );
            loc.add( v );
            Corner end = new Corner( loc.x, loc.y, 0 );

            Edge e = new Edge( pos, end );
            pos.nextL = e;
            lastLoopable =leadingCorner.loop.addAfter( lastLoopable, pos );

            ProfileMachine pm = machines.get( i );
            e.machine = pm;
            pm.addEdge( e, skel );

            Bar b = bars.get( i );

            skel.plan.profiles.put( b, pm.profile );
            skel.addingProfile( pm.profile );

            skel.registerEdge( e, global, valency, b );

//            created.add( new BarGlobalValency( b, global, valency, e ) );

            pos = end;
            i++;
        }

        pos.nextL = insertAfter.nextL;
        leadingCorner.loop.addAfter( lastLoopable, pos );
        cc.nOSegments.put( pos, cc.nOSegments.getSetA( insertAfter ).iterator().next() );

        for ( Loopable<Corner> c : leadingCorner.loop.loopableIterator() )
        {
            Corner p = c.get();
            Corner n = c.getNext().get();

            p.nextC = n;
            n.prevC = p;
            n.prevL = p.nextL;
        }
        return insertBefore.prevC;
    }
}
