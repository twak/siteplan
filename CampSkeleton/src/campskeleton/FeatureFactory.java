package campskeleton;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Tag;
import straightskeleton.HeightEvent;
import straightskeleton.Machine;
import straightskeleton.Skeleton;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import utils.Line;
import utils.Line3D;
import utils.LinearForm3D;
import utils.Loop;
import utils.MultiMap;
import static straightskeleton.ui.Marker.Type.*;

/**
 * @author twak
 */
public class FeatureFactory<E extends Tag>
{

    public MultiMap<E, Marker> planSteps = new MultiMap();
    public Map<Double, FactoryEvent> events = new LinkedHashMap();
//    private MultiMap<Bar, Edge> planEdgeToBar;
    public PlanSkeleton plan;
    // map between forced-step-added-plan-edges and their features
    public Map<Edge, Set<Tag>> newEdgeFeatures = new LinkedHashMap();

    protected E feature;

    public FeatureFactory(E feature)
    {
        this.feature = feature;
    }

    /**
     * If using this function to announce the creation of new profiles edgeToBar should
     * be updated in PlanSkeleton.
     */
    public void add( Collection<Profile> newProfiles, Iterable<Bar> planBars, double heightOffset )
    {
        for ( Bar b : planBars )
            for ( Marker m : b.mould.markersOn( b ) )
                if ( m.feature.getClass() == feature.getClass() )
                    planSteps.put( (E) m.feature, m );

        for ( Global global : plan.plan.globals )
            for ( Profile profile : newProfiles ) // add each profile once
            {
                List<Loop<Bar>> chainStarts = profile.globalProfiles.get(global).chainStarts;
                // here we only loop over profiles defined by a global. Other profiles (from forced steps) aren't processed.

                for ( int valency = 0; valency < chainStarts.size(); valency++ )
                    for ( Bar bar : chainStarts.get( valency ) )
                        for ( Marker m : bar.mould.markersOn( bar ) )
                            // if the profile marker has the correct class
                            if ( feature.getClass().isInstance(m.feature) )//was class == class
                            {
                                double height = -m.y + heightOffset;
                                FactoryEvent evt = events.get( height );
                                if ( evt == null )
                                {
                                    evt = createFactoryEvent( height, global, chainStarts.size() == 1 ? 0 : valency ); // was 1-valency...
                                    events.put( height, evt );
                                }

                                // for plan markers with the same feature
                                for ( Marker planMarker : planSteps.get( (E) m.feature ) )
                                    // if the plan marker is on a bar with the correct profile
                                    if ( plan.plan.profiles.get( planMarker.bar ) == profile )
                                        // schedule an plan marker at this height, along with the profile feature
                                        evt.changesToMake.add( new Anchor( (E) m.feature, planMarker ) );
                            }
            }
    }

    public void add( PlanSkeleton plan )
    {
        this.plan = plan;
        add( new LinkedHashSet<Profile>( plan.plan.profiles.values() ), plan.plan.points.eIterator(), 0 ); // add each profile once
    }

    public FactoryEvent createFactoryEvent( double height, Global g, int valency )
    {
        return new FactoryEvent( height, g, valency );
    }

    /**
     * An anchor that hasn't yet been resolved to
     */
    public class Anchor
    {
        public E profileFeature;
        public Marker planMarker;

        private Anchor( E step, Marker planMarker )
        {
            this.profileFeature = step;
            this.planMarker = planMarker;
        }
    }

    public void addToSkeleton( Skeleton skel )
    {
        for (HeightEvent he : events.values())
            skel.qu.add( he );

        // we add more events mid-sequence, so best to keep this clear
        events.clear();
    }

    /**
     * a map from each plan marker, to the points it generated
     * @param points
     */
    public void addPoints ( MultiMap<Marker, Matrix4d> points )
    {
        // override me!
    }

    public class FactoryEvent implements HeightEvent
    {
        Global global;
        int valency;
        double height;
        public Set<Anchor> changesToMake = new LinkedHashSet();
        public Map<Loop<Bar>, Machine> profiles = new LinkedHashMap();

        public FactoryEvent( double height, Global global, int valency )
        {
            this.height = height;
            this.global = global;
            this.valency = valency;
        }

        public double getHeight()
        {
            return height;
        }

        public boolean process( Skeleton skel_ )
        {
            PlanSkeleton skel = (PlanSkeleton)skel_;
//            List<Matrix4d> pts = new ArrayList();
            MultiMap<Marker, Matrix4d> pts = new MultiMap();

            for ( Anchor anchor : changesToMake )
            {
                
                final Marker planM = anchor.planMarker;

                Set<Corner> edges = plan.findLiveEdgesFrom( planM.bar );

                // the plan is valid for these corners, but is the edge's profile valid?
                Iterator<Corner> eit = edges.iterator();
                while ( eit.hasNext() )
                {
                    Corner c = eit.next();
                    if ( skel.getGlobalForEdge( c.nextL ) != global )
                    {
                        eit.remove();
                        continue;
                    }
                    if ( skel.getValencyForEdge( c.nextL ) != valency )
                        eit.remove();
                }

                LinearForm3D ceiling = new LinearForm3D( 0, 0, 1, -height );

                edges:
                for ( Corner c : edges )
                {
                    Tuple3d start = c.nextL.linearForm.collide( c.prevL.linearForm, ceiling);
                    Tuple3d end = c.nextL.linearForm.collide( c.nextC.nextL.linearForm, ceiling);

                    // translation component of the
                    Vector3d tran = null;
                    switch ( (Marker.Type) planM.properties.get( Marker.TYPE ) )
                    {
                        case AbsEnd:
                        case AbsStart:
                            // do a straight projection (this isn't rotated to the current frame of the bar - eg: won't work with nested features)
                            Vector3d delta = new Vector3d(end); delta.sub(start);
                            Line3D atSPlane = new Line3D( start, delta );
                            Point3d res = atSPlane.projectSegment( new Point3d (planM.x, planM.y, 0 ) );
                            tran = res == null ? null : new Vector3d ( res );
                            break;
                        case Rel:
                            // relative to line length
                            Line reference = new Line( planM.bar.start, planM.bar.end );
                            double p = reference.findPPram( planM );
                            tran = new Vector3d (end);
                            tran.sub( start );
                            tran.scale( p );
                            tran.add( start ); // dir is now the translation component
                            break;
                    }

                    // point didn't project onto polygon at current height, move onto nextl
                    if (tran == null)
                        continue edges;

                    // we only compensate for rotation around the vertical line :(
                    Matrix3d orientation = new Matrix3d();
                    Vector3d eDir = c.nextL.direction();
                    eDir.normalize();

                    // fixme: rotation conversion to jme space occurs here. it shouldn't. hack!
                    AxisAngle4d aa = new AxisAngle4d( new Vector3d (0,1,0), -Math.atan2( eDir.y, eDir.x) );
                    orientation.set( aa );
                    pts.put( planM, new Matrix4d(orientation, tran, 1) );
                }
            }

            FeatureFactory.this.addPoints( pts );

           
            return false; // not doing anything :)
        }
    }
}
