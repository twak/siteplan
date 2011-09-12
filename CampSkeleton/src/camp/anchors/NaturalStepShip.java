package camp.anchors;

import campskeleton.CampSkeleton;
import campskeleton.Global;
import campskeleton.Plan;
import campskeleton.PlanSkeleton;
import campskeleton.PlanSkeleton.ColumnProperties;
import campskeleton.PlanUI;
import campskeleton.Profile;
import campskeleton.ProfileMachine;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JComponent;
import javax.vecmath.Point2d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import straightskeleton.ui.PointEditor.BarSelected;
import utils.Cache;
import utils.ConsecutiveItPairs;
import utils.LContext;
import utils.Line;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;
import utils.MUtils;
import utils.Pair;
import utils.WeakListener;

/**
 * @author twak
 */
public class NaturalStepShip extends Ship
{
    public String name = "n/step";

    public LoopL<Bar> shape = new LoopL();
    public LoopL<Double> speeds = new LoopL();

    private transient PlanSkeleton skel = null;

    // todo: this should be transient!
    public Cache<ProfileHeight, ProfileMachine> profileCache = new Cache<ProfileHeight, ProfileMachine>() {
        @Override
        // fixme: disabled!
        public ProfileMachine create( ProfileHeight i )
        {
            Profile p = new Profile ( i.profile, AffineTransform.getTranslateInstance( 0, -i.height ), skel.plan );
            // register all new profiles & their anchors with the skeleton
            skel.addingProfileRemapRoot( p, i.global );

            return new ProfileMachine( p, skel.plan.root, 0 ); // profile is always the first
        }


    };

    public NaturalStepShip()
    { this (CampSkeleton.instance.plan); }
    public NaturalStepShip( Plan plan )
    {
//        Plan plan = CampSkeleton.instance.plan;

        Loop<Bar>shapeLoop = new Loop();
        this.shape.add( shapeLoop );

        Profile
            profile1 = plan.createNewProfile(null),
            profile2 = plan.createNewProfile(null);

        Point2d[] coords = new Point2d[] {
            new Point2d (-200,0), new Point2d (50,0), new Point2d (50,-20), new Point2d (250,-20), new Point2d (250,0), new Point2d (500, 0) };

        Profile[] profs = new Profile[] {
            profile1, profile2, profile2, profile2, profile1
        };

        int i = 0;
        for ( Pair<Point2d, Point2d> pair : new ConsecutiveItPairs<Point2d>( Arrays.asList( coords ) ) )
        {
            Bar b = new Bar (pair.first(), pair.second());
            shapeLoop.append( b );

            plan.profiles.put( b, profs[i++] ); 
        }

        CampSkeleton.instance.profileListChanged();
        
        Loop<Double> speedLoop = new Loop();
        this.speeds.add(speedLoop);
        speedLoop.append(0.);
        speedLoop.append(-6.);
        speedLoop.append(-3.);
        speedLoop.append(-6.);
        speedLoop.append(0.);
    }

    @Override
    public void clearCache()
    {
        profileCache.cache.clear();
    }

    @Override
    public JComponent getToolInterface(WeakListener refreshAnchors, WeakListener.Changed refreshFeatureListListener, Plan plan)
    {
        return new NaturalStepPanel(this, refreshFeatureListListener);
    }

    @Override
    protected Instance createInstance()
    {
        return new NaturalInstance();
    }

    /**
     * Synchronizes the scale between the ui prievew and here
     */
    public static AffineTransform getScaleTransform()
    {
        return AffineTransform.getScaleInstance( 0.001, 0.001 ); // 19/3/2010 was 0.01. added clamp, below
    }

    public class NaturalInstance extends Ship.Instance
    {
        @Override
        public LContext<Corner> process(Anchor anchor, final LContext<Corner> toEdit, Marker planMarker, Marker profileMarker, Edge edge, final AnchorHauler.AnchorHeightEvent hauler, Corner oldLeadingCorner)
        {
            skel = hauler.skel; // the profile cache needs a plan reference
            Corner insertAfter = toEdit.get();
            Corner insertBefore = toEdit.get().nextC;
            final double height = -profileMarker.y;


            // we clamp the plan marker to be a certain distance from the start or end (19/3/2010)
            Line l = new Line ( new Point2d ( insertAfter.x, insertAfter.y), new Point2d ( insertBefore.x, insertBefore.y ) );
            double tol = 1./l.length(); // <-- need to find some science behind this (clamp input size of natural step?)
            Point2d clampedPlanMarker = l.fromFrac( MUtils.clamp( l.findFrac( planMarker ), tol, 1-tol ) );

            // clone shape from bars
            // scale to "very small"
            AffineTransform at = AffineTransform.getTranslateInstance( clampedPlanMarker.x, clampedPlanMarker.y );
            // rotate to lie parallel to edge
            at.concatenate( AffineTransform.getRotateInstance( Math.atan2( edge.end.y - edge.start.y, edge.end.x - edge.start.x ) ) );
            // and move to the correct location
//            at.concatenate( AffineTransform.getTranslateInstance( planMarker.x, planMarker.y ));
            at.concatenate( getScaleTransform() ); // erc 23

            // properties of the edge we're inserting into (could be cached once per edge in anchor hauler!)
            final ColumnProperties surroundingCP = skel.getColumn( edge );

            // instance profiles at current height from cache, are registered by the cache
            LoopL<ProfileMachine> profileLoopL = shape.new Map<ProfileMachine>()
            {
                public ProfileMachine map( Loopable<Bar> in )
                {
                    // fixme: cache disabled: causing unheight corrected profiles to be used 1/8/11
                    return profileCache.create( new ProfileHeight( skel.plan.profiles.get( in.get() ), height, surroundingCP.global, surroundingCP.valency ) );
                }
            }.run();

            LoopL<Bar> instance = Bar.clone( shape, at);

            Iterator<Loop<Bar>> blit = instance.iterator();
            Iterator<Loop<Double>> slit = speeds.iterator();
            Iterator<Loop<ProfileMachine>> plit = profileLoopL.iterator();

            // first loop is treated as an adjustment to the edge, second+ as independent loops
            boolean first = true;


            LContext<Corner> prev = toEdit;
//            new LContext(
//                    toEdit.loop.addAfter(
//                    toEdit.loopable, new Corner ( planMarker.x, planMarker.y, height )),
//                    toEdit.loop );

//            prev.loopable.get().prevC = insertAfter;
//            insertAfter.nextC = prev.loopable.get();
//            prev.loopable.get().prevL = insertAfter.nextL;
            
            while (blit.hasNext())
            {
                assert (slit.hasNext() && plit.hasNext());
                Iterator<Loopable<Bar>> bit = blit.next().loopableIterator().iterator();
                Iterator<Double> sit = slit.next().iterator();
                Iterator<ProfileMachine> pit = plit.next().iterator();

//                todo: preserve prev in !first loops
//                Loop<Corner> insertingInto = first ? prev.loop : new Ship.createAShip(s).getFeatureName()Loop<Corner>();

                while (bit.hasNext())
                {
                    assert ( sit.hasNext() && pit.hasNext() );
                    Loopable<Bar> barL = bit.next();
                    Bar bar = barL.get();
                    double speed = sit.next();
                    ProfileMachine profileMachine = pit.next();

                    // ignore last bar in unclosed chains. 
                    if ( bar.end != barL.getNext().get().start )
                        continue;


                    Corner next = new Corner (bar.end.x, bar.end.y, height);
                    next.prevL = prev.get().nextL; // will be overwritten if not the first point
                    next.prevC = prev.get();
                    prev.get().nextC = next;

                    // if not first bar, create edge from the previous point to us
                    if ( bar.start == barL.getPrev().get().end )
                    {
                        Edge e = new Edge(prev.get(), next);
                        next.prevL = prev.get().nextL = e;

                        e.machine = profileMachine;
                        
                        // say how much this edge should move
                        hauler.getOffset().registerEdge(prev.get(), speed);

                        hauler.registerInsertedEdge( prev.get(), edge,  surroundingCP.global, surroundingCP.valency, bar);
                    }
                    prev =  new LContext( toEdit.loop.addAfter( prev.loopable, next ), prev.loop );
                }

                prev.loopable.get().nextC = insertBefore;
                insertBefore.prevC = prev.loopable.get();
                prev.loopable.get().nextL = insertBefore.prevL;

                first = false;
            }

            // need to register end of orignal edge as a continuation
            hauler.cornerClone.nOSegments.put( prev.get() , oldLeadingCorner );

            // return a new corner
            return prev; // CampSkeleton.instance.plan.points.size()
        }

        @Override
        public void addUsedBars(Set<Bar> out) {
            for (Bar b : shape.eIterator())
                out.add(b);
        }
    }

    public PlanUI createPlanUI(BarSelected es)
    {
        return new NaturalStepUI( this, es );
    }

    @Override
    public String getFeatureName()
    {
        return name;
    }

    public static class ProfileHeight
    {
        public double height;
        public Profile profile;
        public Global global;
        public int valency;
        
        private ProfileHeight(Profile profile, double height, Global global, int valency)
        {
            this.height = height;
            this.profile = profile;
            this.height = height;
            this.valency = valency;
            this.global = global;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            final ProfileHeight other = (ProfileHeight) obj;
            if ( this.height != other.height )
                return false;
            if ( this.profile != other.profile )
                return false;
            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 79 * hash + (int) (Double.doubleToLongBits( this.height ) ^ (Double.doubleToLongBits( this.height ) >>> 32));
            hash = 79 * hash + (this.profile != null ? this.profile.hashCode() : 0);
            return hash;
        }
    }

    @Override
    public int countMarkerMatches(Object generator)
    {
        int count = 0;
        for (Loop<Bar> loop : shape)
            for (Bar b : loop)
                for (Marker m : b.mould.markersOn( b ))
                    if (m.generator.equals(generator))
                        count++;

        return count;
    }

    @Override
    public Ship clone(final Plan plan)
    {
        NaturalStepShip out = new NaturalStepShip();

        setupClone(out);

        out.name = name;

        final Cache <Point2d, Point2d> cCache = new Cache<Point2d, Point2d>() {

            @Override
            public Point2d create(Point2d i)
            {
                return new Point2d (i);
            }
        };

        final Cache <Profile, Profile> pCache = new Cache<Profile, Profile>() {
            @Override
            public Profile create(Profile i)
            {
                return plan.createNewProfile(i);
            }
        };

        out.shape = shape.new Map<Bar>() {
            @Override
            public Bar map(Loopable<Bar> input)
            {
                Bar out =  new Bar (cCache.get (input.get().start), cCache.get(input.get().end));
                plan.profiles.put(out, pCache.get(plan.profiles.get(input.get())));
                return out;
            }
        }.run();

        out.speeds = speeds.new Map< Double>()
        {
            @Override
            public Double map(Loopable<Double> input)
            {
                return new Double (input.get());
            }
        }.run();
        return out;
    }
}
