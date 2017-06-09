package org.twak.siteplan.anchors;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.twak.camp.Corner;
import org.twak.camp.CornerClone;
import org.twak.camp.Edge;
import org.twak.camp.HeightEvent;
import org.twak.camp.Machine;
import org.twak.camp.Skeleton;
import org.twak.camp.SkeletonCapUpdate;
import org.twak.camp.Tag;
import org.twak.camp.debug.DebugDevice;
import org.twak.camp.offset.FindNOCorner;
import org.twak.camp.offset.Offset;
import org.twak.camp.offset.PerEdgeOffsetSkeleton;
import org.twak.camp.ui.Bar;
import org.twak.camp.ui.Marker;
import org.twak.siteplan.anchors.Ship.Instance;
import org.twak.siteplan.campskeleton.Global;
import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.PlanSkeleton;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.siteplan.campskeleton.ProfileMachine;
import org.twak.utils.LContext;
import org.twak.utils.Line;
import org.twak.utils.Line.AlongLineComparator;
import org.twak.utils.collections.DHash;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;
import org.twak.utils.collections.SetCorrespondence;

/**
 * Computes the location of anchors, and dispatches them at the appropriate height to a set of handles
 *
 * @author twak
 */
public class AnchorHauler
{

//    MultiMap<Feature, Marker> planSteps = new MultiMap();
    Map<Double, AnchorHeightEvent> events = new LinkedHashMap<>();
//    private MultiMap<Bar, Edge> barToPlanEdge;
    private Plan plan;
    Map<Edge, Set<Tag>> newEdgeFeatures = new LinkedHashMap<>();
    PlanSkeleton skel;

    public AnchorHauler( PlanSkeleton skel  )
    {
        this.skel = skel;
        this.plan = skel.plan;
    }

    /**
     * At this point we assume that the anchors have been added to the bar.
     * @param skel
     */
    public void add( Collection<Profile> profiles)
    {
        for ( Profile p : new LinkedHashSet<Profile>(profiles) )
            for ( Global g : plan.globals )
                add( p, g );
    }

    public void add( Profile p )
    {
        for ( Global g : plan.globals )
                add (p, g);
    }

    public void addRemap(Profile profile, Global remapRootTo)
    {
        for ( Global g : plan.globals )
        {
            List<Loop<Bar>> chainStarts = profile.globalProfiles.get(g).chainStarts;
            add (profile, chainStarts, g == plan.root ? remapRootTo : g);
        }
    }

    private void add (Profile profile, Global g)
    {
        List<Loop<Bar>> chainStarts = profile.globalProfiles.get(g).chainStarts;
        // here we only loop over profiles defined by a global. Other profiles (from forced steps) aren't processed.
        add (profile, chainStarts, g);
    }

    private void add (Profile profile, List<Loop<Bar>> chainStarts, Global g)
    {
        for (int valency = 0; valency < chainStarts.size(); valency++)
            for (Bar bar : chainStarts.get(valency))
                if (!ProfileMachine.isApproxHoriz(bar)) // horiz. profile sections are handled in ProfileMachine
                    for (Marker m : bar.mould.getAnchorsForEditing ( bar, bar.start, bar.end))
                    {
                        AnchorHeightEvent evt = new AnchorHeightEvent(-m.y, m, g, chainStarts.size() == 1 ? 0 : valency, skel, profile);
                        events.put(evt.height, evt);
                        skel.qu.add(evt);
                    }
    }

    private static class EdgeGlobalValencyBar
    {
        Edge parent;
        Global global;
        int valency;
        Bar bar;
        public EdgeGlobalValencyBar (Edge parent, Global global, int valency, Bar bar)
        {
            this.parent = parent;
            this.global = global;
            this.valency = valency;
            this.bar = bar;
        }
    }

    public static class AnchorHeightEvent implements HeightEvent
    {
        double height;
//        public Set<Marker> planMarkers = new LinkedHashSet();
        int valency;
        public Map<Loop<Bar>, Machine> profiles = new LinkedHashMap();
        Global global;
        Marker profileMarker;
        Profile profile;
        PlanSkeleton skel;

        public CornerClone cornerClone = null;
        // an anchor may initiate, an offset (cap copy and merge back) if it requests this offset
        private PerEdgeOffsetSkeleton offset;
        private boolean polygonChanged = false;

    /**
     * Returns the offset skeleton, which anchors can use to regiseter the desired offset for each edge using offset.registerProfile( machine, angle ). Angle is
     * over 100 units, eg: -Math.atan( distance / 100 )
     *
     * If at least one anchor calls this we re-integrate the profile after all anchors at a height have been hauled. Otherwise we don't. Neat eh?
     * @return
     */
    public PerEdgeOffsetSkeleton getOffset()
    {
        polygonChanged = true;
        return offset;
    }


        public AnchorHeightEvent( double height, Marker profileMarker, Global g, int valency, PlanSkeleton skel, Profile profile )
        {
            assert(g != null);
            this.global = g;
            this.valency = valency;
            this.height = height;
            this.profileMarker = profileMarker;
            this.skel = skel;
            this.profile = profile;
        }

        public double getHeight()
        {
            return height;
        }

        public boolean process( Skeleton skel_ )
        {
            // skeleton existing loop of corners known as "base"
            final PlanSkeleton skel = (PlanSkeleton) skel_;
            SkeletonCapUpdate cap = new SkeletonCapUpdate(skel);
            // this is the reference cap "old"
            LoopL<Corner> old = cap.getCap(height);
            // this is what gets edited, called "new"
            cornerClone = new CornerClone(old);

            // reset the cache of short edge meta data
            shortToGrownEdge.clear();
            
            offset = new PerEdgeOffsetSkeleton();
            polygonChanged = false; // fixme: should be false?

            DebugDevice.dump("pre anchor hauler", cornerClone.output);

            // profile-only anchors don't need the locaiton information
            for ( Ship ship : skel.plan.ships )
                for ( Instance i : ship.getInstances() )
                    for ( Anchor anchor : i.anchors )
                        if ( anchor.matches( null, profileMarker.generator ) )
                            i.process( anchor, null, null, profileMarker, null, this, null );


            // anchors with both a plan and a profile.
            // we assume that any edits made will be very-very small - eg: not merge edges or any such clever stuff
            for ( Edge e : skel.liveEdges )
            {
                PlanSkeleton.ColumnProperties propertiesAtHeight = skel.getColumn( e );

                // if the current edge came from the correct profile section. We can't have an unused profile triggering an event.
//                if ( propertiesAtHeight.valency == valency && propertiesAtHeight.global == global )
                if ( propertiesAtHeight.valency == valency && propertiesAtHeight.global == global &&  (((ProfileMachine)e.machine).profile == profile ) )
                {
                    // properties when this edge was first introduced
                    PlanSkeleton.ColumnProperties defining = skel.getDefiningColumn( e );

                    Bar b = defining.defBar;
                    Set<Corner> sections = e.findLeadingCorners();

                    for ( Corner segment : sections )
                    {
                        Corner firstC = cornerClone.nOCorner.teg( cap.getOldBaseLookup().teg( segment ) );
                        // this is only required because we don't just rebuild cornerClone.output when we're done.
                        LContext<Corner> first = Corner.findLContext( cornerClone.output, firstC );

                        Point2d
                            defStart =  toP2d( defining.defPlanEdge.start ),
                            defEnd   =  toP2d( defining.defPlanEdge.end );

                        Point2d currentStart = toP2d( firstC ), currentEnd = toP2d( firstC.nextC );

                        // find segment location at current height
                        List<Marker> planMarkers = b.mould.getAnchorsForEditing(
                                b,
                                currentStart,
                                currentEnd,
                                new Line (currentStart, currentEnd).project(defStart, false), // project abs coords onto the edge
                                new Line (currentStart, currentEnd).project(defEnd, false));

                        // sort the list of markers
                        Collections.sort( planMarkers, new AlongLineComparator( toP2d( firstC ), toP2d( firstC.nextC ) ) );


                        //  remove markers beyond end of line, or before start.
                        Iterator<Marker> mit = planMarkers.iterator();
                        Line line = new Line(toP2d(firstC), toP2d(firstC.nextC));
                        while (mit.hasNext())
                        {
                            double pParam = line.findPPram( mit.next() );
                            if (pParam < 0 || pParam > 1)
                                mit.remove();
                        }

                        // todo: we should have an acceleration structure (built at the start of every run) for all this!
                        for (Marker planMarker : planMarkers)
                            for (Ship ship : skel.plan.ships)
                                for (Instance i : ship.getInstances())
                                    for (Anchor planAnchor : i.anchors)
                                        if (planAnchor.matches(planMarker.generator, profileMarker.generator))
                                        {
                                            // a process may call-back to offset-skeleton to getOffset().registerProfile, above to register offset distances for any machines in first
                                            first = i.process(
                                                    planAnchor, // plan anchor we're reporting to (eg window)
                                                    first,  // segment to edit (wall segment at the moment)
                                                    planMarker, //  the position on the plan at this height (eg - the location of the bottom,left of the window
                                                    profileMarker, // the profile marker
                                                    e, // the edge that this segment came from (for normals etc).
                                                    AnchorHeightEvent.this, // callback for editing the profile
                                                    cap.getOldBaseLookup().teg( segment )
                                                    );
                                            DebugDevice.dump("mid-post hauler insert", cornerClone.output);
                                        }
                                    
                    }
                }
            }

            DebugDevice.dump("post hauler insert", cornerClone.output);

            // if we are expected to change the polygon enclosed on the sweep plane (eg an enchor fired a natural event)
            if (polygonChanged)
            {

                offset.setup( cornerClone.output );
                // get final offset surfce
                Offset offsetResult = offset.getResult();

                
                // for all grown edges, add in a polygon to fill the hole
                for (Corner shortCorner : shortToGrownEdge.keySet())
                    for (Corner c : offset.oldInputSegments.getSetB(shortCorner))
                        for (Loop<Point3d> pt : offset.outputSkeleton.output.faces.get( c ) .points )
                        {
                            skel.output.addNonSkeletonOutputFace(new LoopL<Point3d>(pt).new Map<Point3d>()
                            {
                                @Override
                                public Point3d map(Loopable<Point3d> input)
                                {
                                    return new Point3d(input.get().x, input.get().y, height);
                                }
                            }.run(), new Vector3d(0, 0, offset.inputCornerToSpeed.get(shortCorner) < 0 ? -1 : 1 )); // direction from growth speed
                        }


                // to stitch it back into the original loop we need to dereference [offset -> cc -> update] for corner, edge maps
                FindNOCorner findNOCorner = new FindNOCorner(offsetResult, cornerClone.output) {

                    @Override
                    public boolean didThisOldCornerRemainUnchanged(Corner oldC) {
                        // i think this is right, not certain
                        return cornerClone.nOCorner.get(oldC) != null;
                    }
                };

                // from offset -> cc
                SetCorrespondence<Corner, Corner> nOSegments = findNOCorner.nOSegmentsUpdate;
                DHash<Corner, Corner> nOCorner = findNOCorner.nOCorner;

                // now update maps' old values to give offset -> update cap
                nOCorner.remapB(cornerClone.nOCorner.asCache());
                nOSegments = nOSegments.new ConvertB<Corner>(cornerClone.nOSegments).convert();
                
                // now we have all the edge and corner correspondences, we can apply them
                cap.update(offsetResult.shape, nOSegments, nOCorner);

                // finally register the meta data for the grown edges (the edges that go into cap.update are the same that end up in the merged skeleton)
                for (Corner shortCorner : shortToGrownEdge.keySet())
                {
                    EdgeGlobalValencyBar meta = shortToGrownEdge.get(shortCorner);
                    for (Corner grownInsertedEdge : offsetResult.nOSegments.getPrev(shortCorner)) // here we could parent using meta.parent, but then bar would be ambiguos.
                        skel.registerEdge(grownInsertedEdge.nextL, meta.global, meta.valency, meta.bar);
                }

                skel.refindAllFaceEventsLater();
            }

            DebugDevice.dump("post hauler growth", skel);

            

            return true;

//            for ( Marker m : planMarkers )
//            {
//                Set<Corner> cornersWithMarkers = skel.findLiveEdgesFrom( m.bar );
//
//                // filter for correct global and valency
//                Iterator<Corner> eit = cornersWithMarkers.iterator();
//                while ( eit.hasNext() )
//                {
//                    Corner c = eit.next();
//                    if ( skel.getGlobalForEdge( c.nextL ) != global )
//                    {
//                        eit.remove();
//                        continue;
//                    }
//                    if ( skel.getValencyForEdge( c.nextL ) != valency )
//                        eit.remove();
//                }
//
//                for ( Corner c : cornersWithMarkers )
//                    markers.put( c, m );
//            }
//
//            for ( Corner lc : markers.keySet() )
//            {
//                List<Marker> markersOnPlanBar = markers.get( lc );
//
//                // perform these based on order from start of bar (?!should be count backwards as negative!?)
//                Collections.sort( markersOnPlanBar, new PointDistanceComparator( markersOnPlanBar.get( 0 ).bar.start ) );
//
//
//                for ( Marker m : markersOnPlanBar )
//                {
//
//                    process (
//                            skel,
//                            m,
//                            lc.nextL,
//                            height,
//                            global,
//                            valency );
//                }
//            }
//
//            return true;
//        }
//
//        private void process( PlanSkeleton skel, Marker m, Edge nextL, double height, Global globalForEdge, int valencyForEdge )
//        {
//            throw new UnsupportedOperationException( "Not yet implemented" );
//        }


        }
        /**
         * After we insert the very short edges, we grow them to different edges before inserting them.
         * However, we still need to register the grown edge's meta data with the skeleton. This method
         * stores the
         */
        Map<Corner, EdgeGlobalValencyBar> shortToGrownEdge = new LinkedHashMap();

        public void registerInsertedEdge(Corner shortEdge, Edge parent, Global global, int valency, Bar bar)
        {
            shortToGrownEdge.put(shortEdge, new EdgeGlobalValencyBar(parent, global, valency, bar));
        }
    }

    private static Point2d toP2d(Point3d in)
    {
        return new Point2d(in.x, in.y);
    }
}
