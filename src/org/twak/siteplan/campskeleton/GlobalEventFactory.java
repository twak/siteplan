package org.twak.siteplan.campskeleton;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.twak.siteplan.campskeleton.Profile.GlobalProfile;
import org.twak.siteplan.campskeleton.Profile.ProfileChainInfo;
import org.twak.siteplan.jme.Preview;
import org.twak.straightskeleton.Corner;
import org.twak.straightskeleton.CornerClone;
import org.twak.straightskeleton.Edge;
import org.twak.straightskeleton.HeightEvent;
import org.twak.straightskeleton.Machine;
import org.twak.straightskeleton.Output;
import org.twak.straightskeleton.Skeleton;
import org.twak.straightskeleton.SkeletonCapUpdate;
import org.twak.straightskeleton.Output.Face;
import org.twak.straightskeleton.debug.DebugDevice;
import org.twak.straightskeleton.offset.OffsetSkeleton;
import org.twak.utils.Cache;
import org.twak.utils.Cache2;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;
import org.twak.utils.ToStrictSetIerable;

/**
 * Creates a bunch of events that deal with face extrude/intrude. These are added
 * directly to the skeleton queue (rather than be assicated with a particular machine)
 *
 * @author twak
 */
public class GlobalEventFactory
{
    private Plan plan;
    private Map <Global, GlobalHE> perGlobal = new LinkedHashMap();

    public GlobalEventFactory( Plan plan )
    {
        this.plan = plan;
    }

    public void add ( Global global )
    {
        GlobalHE he = perGlobal.get( global );
        
        if (he == null)
        {
            perGlobal.put( global, he = new GlobalHE( global ) );
            he.height = - plan.profiles.values().iterator().next().globalProfiles.get( global ).chainStarts.get( 0 ).getFirst().start.y;
        }
    }

    public void addToSkeleton( Skeleton skel )
    {
        for (GlobalHE he : perGlobal.values())
            skel.qu.add( he );
    }

    public class GlobalHE implements HeightEvent
    {
        private Global global;
        private double height = -1;

        public GlobalHE ( Global global )
        {
            this.global = global;   
        }

        public double getHeight()
        {
            return height;
        }

        /**
         * Build a new skeleton to give the offset surface for each section
         */
        public boolean process( Skeleton skel_ )
        {
            final PlanSkeleton skel = (PlanSkeleton)skel_;

            // the side profile is used on edges between an enabled and disabled global on consecutive edges
            ProfileMachine sideProfileMachine = new ProfileMachine (  global.edgeProfile == null ? new Profile( 100 ) : global.edgeProfile, height, 1);

            SkeletonCapUpdate update = new SkeletonCapUpdate(skel);

            LoopL<Corner> cap = update.getCap(height);

            OffsetSkeleton offset = new OffsetSkeleton(cap, 100);


            assert (global.valency == 2); // we assume that the smallest is closest to the target, and the bigger is furthest away.

            // the most negative x value is normally the lowest valency. But distances might be relative to inside or outside. this array converts between teh two systems.
            int[] order = null;
            // are we carving out space inside an existing volume, or adding in space outside existing volumes
            boolean inside = false;

            // for all machines
            for ( Machine machine : new ToStrictSetIerable<Corner, Machine>( skel.liveCorners )
            { @Override public Machine getObject( Corner a ) {  return a.nextL.machine; } } )
            {
                ProfileMachine pm = (ProfileMachine) machine;

                // we aim to process the innermost profile chain (valency) first, so we may have to process the valencies in a reversed order.
                GlobalProfile gp = pm.profile.globalProfiles.get( global );

                ProfileChainInfo val0 = gp.getChainInfoForValency( 0 );

                int[] order2 =
                        val0.barToRight == gp.getChainInfoForValency( 1 ).profile.getFirst() ?
                            new int[] {1,0} : new int[] {0,1};

                // 0th valency is (i think...) on left, so we can tell if we're inside by the left-most "going inside" flag
                inside = !val0.into;
                
                assert order == null || order[0] == order2[0]; // if the polarity of the global is different for different profiles, this'll fail.
                order = order2;

                for ( int t = 0; t < 2; t++ )
                {
                    int v= order[t];

                    // we will advance to height == 1, angle is tan distance, negative is outside offset
                    Profile.ProfileChainInfo info = gp.getChainInfoForValency( v );
                    offset.registerProfile( pm, Math.abs ( info.distance) < 1 ? 0 : -Math.atan( info.distance / 100. ), t );
                }
            }

            // why? why not?!
            if (order == null)
                return false;

            // we use the skeleton from results, rather than any of the caps in particular. first run the offset
            offset.getResults();

            CornerClone cc = new CornerClone(cap);

            // this union merges a set of faces to form the offset area.
            FaceUnion<Edge> union = new FaceUnion( );

            // add each face to map, tag with eventual propertiesS
            Output offsetOutput = offset.outputSkeleton.output;
            for (Face f : new HashSet<Face>(offsetOutput.faces.values())  )
            {
                // only for the first two offsets (assumes valency == 2)
                if (f.getParentCount() == 1)
                {
                    // find originating input edge
                    List<Corner> capCorners = offset.getInputEdge(f);
                    Corner currentSkeletonCorner = update.getOldBaseLookup().get(capCorners.get(0));

                    ProfileMachine pMachine = (ProfileMachine) currentSkeletonCorner.nextL.machine;

                    // no need to adjust nO corners or segments
                    if ( pMachine.profile.globalProfiles.get( global ).enabled )
                    {
                        union.add( f, currentSkeletonCorner.nextL );

                        if ( !inside )
                        {
                            // add the surface on the underside of the new geometry
                            skel.output.addNonSkeletonOutputFace( Corner.dupeNewAllPoints( f.points, height ), new Vector3d( 0, 0, -1 ) );
                        }
                        else
                        {
                            // ...likewise for the top of any outgoing geometry
                            LoopL<Point3d> pts = Corner.dupeNewAllPoints( f.points, height );
                            skel.output.addNonSkeletonOutputFace( pts, new Vector3d( 0, 0, 1 ) );
                        }
                    }
                }
            }

            // we take the output shape, reversed by the union class
            LoopL<Point3d> shapeP = union.getGeometry();
            DebugDevice.dumpPoints("union output", shapeP);

            // and project corners and edges from the union down tothe specified height
            Cache<Point3d, Corner> cCache= new Cache<Point3d, Corner>()
            { @Override public Corner create( Point3d i ) { return new Corner (i.x,i.y, height); } };

            Cache2<Machine, Integer,ProfileMachine> pCache = new Cache2<Machine, Integer,ProfileMachine>()
            {
                @Override
                public ProfileMachine create( Machine machine, Integer valency )
                {
                    ProfileMachine pm = (ProfileMachine)machine;

                    ProfileMachine out = new ProfileMachine ( pm.profile, global, valency ); //pm.profile.globalProfiles.get( global ).chainStarts.get( valency ) 
                    if (!pm.profile.globalProfiles.get( global ).getChainInfoForValency( valency ).into)
                        out.negate();

                    return out;
                }
            };

            for (Loop<Point3d> loopP : shapeP)
            {
                Loop<Corner> loopC = new Loop();
                cc.output.add( loopC );

                for (Loopable<Point3d> loopablePt: loopP.loopableIterator())
                {
                    Point3d pt = loopablePt.get();
                    Edge neuEdge = new Edge ( cCache.get( pt ), cCache.get( loopablePt.getNext().get() ) );

                    Edge srcEdge = union.getData( pt);
                    
                    // this is corrected to take chain locations into account below
                    int valency = union.isBottom( pt ) ? 0 : union.isTop( pt ) ? 1 : -1;
                    
                    if (valency < 0 ) // side
                    {
                        neuEdge.machine = sideProfileMachine;
                    }
                    else // inside or outside
                    {
                        valency = order[valency];
                        neuEdge.machine = pCache.get(srcEdge.machine, valency);
                        neuEdge.profileFeatures = ((ProfileMachine)neuEdge.machine).profile.points.get( 0 ).getFirst().tags;
                    }

                    neuEdge.machine.addEdge( neuEdge, skel );

                    skel.registerEdge( neuEdge, srcEdge, global, valency );

                    loopC.append( cCache.get(loopablePt.get()));
                    neuEdge.start.nextL = neuEdge;
                    neuEdge.end.prevL = neuEdge;
                    neuEdge.start.nextC = neuEdge.end;
                    neuEdge.end.prevC = neuEdge.start;

                    skel.output.newEdge( neuEdge, null, neuEdge.profileFeatures);
                    skel.output.newDefiningSegment( neuEdge.start );
                }
            }

            // finally insert the new corners and edges into the skeleton.
            update.update(cc.output, cc.nOSegments, cc.nOCorner);

            return true; // chances are something's changed
        }

        public LoopL<Corner> asLoop( LoopL<Point3d> in )
        {

            Cache<Point3d,Corner> cCache = new Cache<Point3d,Corner>()
            {
                @Override
                public Corner create(Point3d i) {
                    return new Corner (i.x,i.y, height);
                }
            };

            LoopL<Corner> out = new LoopL();
            for (Loop<Point3d> loopIn : in)
            {
//                System.out.println("loopsize:"+in.count());
                Loop<Corner> loopC = new Loop();
                out.add(loopC);

                for (Point3d pt :loopIn )
                    loopC.append(cCache.get(pt));

                loopC.reverse();

                for (Loopable<Corner> loopable : loopC.loopableIterator())
                {
                    Corner
                            next = loopable.getNext().get(),
                            corn = loopable.get();

                    corn.nextC = next;
                    next.prevC = corn;
                    corn.nextL = next.prevL = new Edge (corn, next);
                    corn.nextL.machine = new Machine(Math.PI/4);
                }
            }
            return out;
        }
    }
}
