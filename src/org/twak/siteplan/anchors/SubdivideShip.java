/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.twak.siteplan.anchors;

import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.twak.camp.Corner;
import org.twak.camp.CornerClone;
import org.twak.camp.Edge;
import org.twak.camp.SkeletonCapUpdate;
import org.twak.camp.Tag;
import org.twak.camp.Output.Face;
import org.twak.camp.Output.SharedEdge;
import org.twak.camp.offset.PerEdgeOffsetSkeleton;
import org.twak.camp.ui.Bar;
import org.twak.camp.ui.Marker;
import org.twak.siteplan.anchors.AnchorHauler.AnchorHeightEvent;
import org.twak.siteplan.campskeleton.Siteplan;
import org.twak.siteplan.campskeleton.FaceUnion;
import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.PlanSkeleton;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.siteplan.campskeleton.ProfileMachine;
import org.twak.siteplan.tags.SubdivideTag;
import org.twak.siteplan.tags.SubdivideTag.ProfileMerge;
import org.twak.utils.Cache;
import org.twak.utils.DHash;
import org.twak.utils.LContext;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;
import org.twak.utils.SetCorrespondence;
import org.twak.utils.WeakListener;
import org.twak.utils.WeakListener.Changed;

/**
 *
 * @author twak
 */
public class SubdivideShip extends Ship
{

    Map<Profile, Profile> volumeSubdivide = new LinkedHashMap();
    // when we subdivide, do the original edges remain?
    boolean keepOriginal = false;
    String name = "subdivide";

    @Override
    public JComponent getToolInterface( WeakListener refreshAnchors, Changed refreshFeatureListListener, Plan plan )
    {
        return new SubdivideShipUI( plan, this, refreshFeatureListListener );
    }

    @Override
    protected Instance createInstance()
    {
        return new SubdivideShipInstance();
    }

    @Override
    public String getFeatureName()
    {
        return "Subdiv: " + name;
    }

    @Override
    public Ship clone( Plan plan )
    {
        return null;
    }

    @Override
    protected Anchor createNewAnchor()
    {
        return Siteplan.instance.plan.createAnchor( Plan.AnchorType.PROFILE, (Object)null );
    }

    public class SubdivideShipInstance extends Instance
    {

        @Override
        public LContext<Corner> process( Anchor anchor, LContext<Corner> toEdit, Marker planMarker, Marker profileMarker, Edge edge, AnchorHeightEvent hauler, Corner oldLeadingCorner )
        {
            final PlanSkeleton oSkel = hauler.skel;

            final double height = -profileMarker.y;

            SkeletonCapUpdate update = new SkeletonCapUpdate( oSkel );
            LoopL<Corner> oLoop = update.getCap( height );


            // default offset is 0
            final Profile vertical = new Profile( 100 );

            // we store the replacemap in replacementCap
            CornerClone replacementCap = new CornerClone( oLoop );

            if ( !keepOriginal )
            {
                oSkel.output.addNonSkeletonOutputFace( oLoop.new Map<Point3d>()
                {

                    @Override
                    public Point3d map( Loopable<Corner> input )
                    {
                        return new Point3d( input.get().x, input.get().y, height );
                    }
                }.run(),
                                                       new Vector3d( 0, 0, 1 ) );
                replacementCap.output = new LoopL();
                replacementCap.nOCorner = new DHash();
                replacementCap.nOSegments = new SetCorrespondence<Corner, Corner>();
            }
            else
            {
                // nothing to do, replacementCap is valid
            }

            final Plan plan = new Plan( oSkel.plan.root ); // the two skeletons share one root Global, so that the profiles are interchangeable
            plan.addLoop( vertical.points.get( 0 ), plan.root, vertical );

            final Cache<Corner, Point2d> cCache = new Cache<Corner, Point2d>()
            {

                @Override
                public Point2d create( Corner i )
                {
                    return new Point2d( i.x, i.y );
                }
            };

            //create the new skeleton's input from the current cap
            plan.points = oLoop.new Map<Bar>()
            {

                @Override
                public Bar map( Loopable<Corner> input )
                {
                    Bar b = new Bar( cCache.get( input.get() ), cCache.get( input.getNext().get() ) );

                    Profile profile = volumeSubdivide.get( ((ProfileMachine) input.get().nextL.machine).profile.copyOf );

                    if ( profile == null )
                        profile = vertical;

                    plan.profiles.put( b, profile );
                    return b;
                }
            }.run();

            PlanSkeleton nSkel = new PlanSkeleton( plan );
            nSkel.name = getFeatureName()+"@"+height;
            nSkel.skeleton();

            SubdivideTag defaultTag = new SubdivideTag();
            defaultTag.enabled = false;

            ProfileMerge defaultProfileMerge = new ProfileMerge( vertical, false );

            FaceMergeGraph fm = new FaceMergeGraph();

            // we isntance all profiles on demand
            Cache<Profile, ProfileMachine> pCache = new Cache<Profile, ProfileMachine>()
            {
                @Override
                public ProfileMachine create( Profile i )
                {
                    Profile p = new Profile( i, AffineTransform.getTranslateInstance( 0, -height ), oSkel.plan );
                    // register all new profiles & their anchors with the skeleton
                    oSkel.addingProfile( p ); // all new geometry is root?

                    return new ProfileMachine( p, oSkel.plan.root, 0 ); // profile is always the first
                }
            };

            // register the information for the face merge routine
            for ( Face f : nSkel.output.faces.values() )
            {
                if ( !findTag( f, defaultTag ).enabled )
                    continue;

                fm.add( f, f.edge.getPlaneNormal().z < 0 );

                for ( SharedEdge se : f.edges.eIterator() )
                {
                    ProfileMerge prof = findProf( f, se, defaultTag, defaultProfileMerge );


                    Face other = se.getOther( f );

                    // if other face 1) exists 2) se set to merge and 3) has a subdivide tag
                    if ( other != null && prof.merge && findTag( other, null ) != null )
                        fm.registerMerge( se );
                }
            }


            Set<Face> allFaces = new HashSet( fm.faces );
            while ( !allFaces.isEmpty() )
            {
                Set<Face> toMerge = fm.findAdjacent( allFaces.iterator().next() );
                allFaces.removeAll( toMerge );

                FaceUnion<ProfileMachine> fu = new FaceUnion();

                for ( Face f : toMerge )
                    for ( SharedEdge se : f.edges.eIterator() )
                        fu.add(
                                fm.isReversed( f ) ? se.getStart( f ) : se.getEnd( f ),
                                fm.isReversed( f ) ? se.getEnd( f ) : se.getStart( f ),
                                pCache.get(
                                findProf( f, se, defaultTag, defaultProfileMerge ).profile ) );


                Cache<Point3d, Corner> kCache= new Cache<Point3d, Corner>()
                { @Override public Corner create( Point3d i ) { return new Corner (i.x,i.y, height); } };


                LoopL<Corner> unShrunk = new LoopL();

                for (Loop<Point3d> inLoop : fu.getGeometry())
                {
                    Loop<Corner> outLoop = new Loop();
                    unShrunk.add( outLoop );
                    for (Loopable<Point3d> p : inLoop.loopableIterator())
                    {
                        Corner s = kCache.get(p.get()),
                               e = kCache.get(p.getNext().get());
                        outLoop.append( s);

                        Edge neuEdge = new Edge (s,e);
                        neuEdge.machine = fu.getData( p.get() );
                        neuEdge.profileFeatures = ((ProfileMachine)neuEdge.machine).profile.points.get( 0 ).getFirst().tags;
//                        neuEdge.machine.addEdge( neuEdge, oSkel);

                        s.nextL = neuEdge;
                        e.prevL = neuEdge;
                        s.nextC = e;
                        e.prevC = s;

                    }
                }

                // we now shrink the sections in a small amount to ensure they don't collide with each other.
                LoopL<Corner> shrunk = new PerEdgeOffsetSkeleton( unShrunk, 0.1 ).getResult().shape;

                for ( Corner c : shrunk.eIterator() )
                {
                    Corner s = c, e = s.nextC;

                    oSkel.registerEdge( c.nextL, oSkel.plan.root, 0,
                                        new Bar(
                            new Point2d( s.x, s.y ),
                            new Point2d( e.x, e.y ) ) );

//                    oSkel.output.newDefiningSegment( s );
                }

                replacementCap.output.addAll ( shrunk );
            }

            // run the cap update
            update.update( replacementCap.output, replacementCap.nOSegments, replacementCap.nOCorner );

            return null;
        }

        private SubdivideTag findTag( Face f, SubdivideTag defaultTag )
        {
            SubdivideTag sdt = defaultTag;

            for ( Tag t : f.profile )
                if ( t instanceof SubdivideTag )
                {
                    sdt = (SubdivideTag) t;
                    break;
                }
            return sdt;
        }

        private ProfileMerge findProf( Face f, SharedEdge se, SubdivideTag defaultTag, ProfileMerge defaultProfileMerge )
        {
            SubdivideTag sdt = findTag( f, defaultTag );

            ProfileMerge prof =
                    f.topSE.contains( se ) ? sdt.assignments.get( SubdivideTag.top )
                    : f.definingSE.contains( se ) ? sdt.assignments.get( SubdivideTag.bottom ) : sdt.assignments.get( SubdivideTag.side );

            return prof == null ? defaultProfileMerge : prof;
        }
    }

    @Override
    public void addUsedProfiles( List<Profile> vProfiles )
    {
        super.addUsedProfiles( vProfiles );
        for ( Profile p : volumeSubdivide.values() )
            if (p != null)
                vProfiles.add( p );
    }
}
