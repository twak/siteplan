package org.twak.siteplan.campskeleton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point2d;

import org.twak.camp.Corner;
import org.twak.camp.Edge;
import org.twak.camp.Machine;
import org.twak.camp.Skeleton;
import org.twak.camp.Tag;
import org.twak.camp.Output.Face;
import org.twak.camp.ui.Bar;
import org.twak.siteplan.anchors.AnchorHauler;
import org.twak.siteplan.anchors.Ship;
import org.twak.siteplan.campskeleton.Profile.GlobalProfile;
import org.twak.siteplan.jme.Preview;
import org.twak.siteplan.tags.LowPriorityTag;
import org.twak.utils.Cache;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.MultiMap;

/**
 * Root class for the straight skeleton extensions.
 * @author twak
 */
public class PlanSkeleton extends Skeleton
{
    public GlobalEventFactory globalFac = null;

    public static String[][] getShipTypes(){ return new String[][] {
        {"org.twak.siteplan.anchors.MeshShip", "Mesh"},
        {"org.twak.siteplan.anchors.VerticalMeshShip", "Upright Mesh"},
        {"org.twak.siteplan.anchors.NaturalStepShip", "Natural Step"},
        {"org.twak.siteplan.anchors.SubdivideShip", "Subdivide"},
        {"org.twak.siteplan.anchors.CapShip", "Cap"},
    }; }

    public static String[][] tagTypes = new String[][]
    {
        {"org.twak.siteplan.tags.RoofTag", "Roof"},
        {"org.twak.siteplan.tags.LowPriorityTag", "Low Priority"},
        {"org.twak.siteplan.tags.SubdivideTag", "Subdivide"},
    };


    // the basic skeleton implementation doesn't know about bars, profiles etc... this structure
    // contains additional informaiton about a horiztonal stack of edges, indexed by the lowest(defining) edge.
    // this edge can be found in Output.
    private Map <Edge, ColumnProperties> columnProperties = new LinkedHashMap<Edge, ColumnProperties>();
    // acceleration structure for column properties
    public MultiMap<Bar, Edge> planEdgeToBar;

    public Plan plan;

    // the side profile is used on edges between an enabled and disabled global on consecutive edges
//    public ProfileMachine sideProfileMachine;

    public transient AnchorHauler anchorHauler;
	public double capArea;


    public PlanSkeleton()
    {
        super();
        this.name="plan";
    }

    public PlanSkeleton (Plan plan)
    {
        super();
        this.name="plan";

        this.plan = plan;
        init();
    }

    public void init()
    {
        // reset data structures that assume plan unchanged
        plan.clearCache();

        LoopL <Corner> out = new LoopL<>();

        Map <GlobalProfile, List<Machine>> machines = new LinkedHashMap<>();
        globalFac = new GlobalEventFactory ( plan );
        planEdgeToBar = new MultiMap<Bar, Edge>();

        for ( Global g : plan.globals )
        {
            if ( g != plan.root )
                 globalFac.add( g );
           

            for ( Loop<Bar> lb : plan.points)
            {
                Loop<Corner> loop = new Loop<>();

                Cache<Point2d, Corner> barCache = new Cache<Point2d, Corner>() {

                    @Override
                    public Corner create(Point2d i) {
                        return new Corner(i.x, i.y, 0);
                    }
                };

                for ( Bar bar : lb )
                {
                    Profile profile = plan.profiles.get ( bar );
                    GlobalProfile gp = profile.globalProfiles.get ( g );
                    List<Machine> m = machines.get( gp );

                    // once for each profile
                    if ( m == null )
                    {
                        m = new ArrayList<Machine>();
                        for (int i = 0; i < g.valency; i++)
                            m.add( new ProfileMachine(  profile, g, i ));
                        machines.put( gp, m );
                    }

                    // build the root LoopL<Corner> from the set of bars
                    if ( g == plan.root )
                    {
                        out.add( loop );

                        Edge e = new Edge(
                                barCache.get(bar.start),
                                barCache.get(bar.end),
                                Math.PI / 4 );

                        e.start.nextL = e;
                        e.end.prevL = e;
                        e.start.nextC = e.end;
                        e.end.prevC = e.start;

                        planEdgeToBar.put( bar, e );
                        setPlanTags( e, bar.tags );

                        registerEdge( e, plan.root, 0, bar ); // plan.root is global 0

                        e.profileFeatures.addAll( profile.points.get( 0).start.get().tags );

                        loop.append( e.start );
                        e.machine = machines.get(gp).get(0); // assumes root has valency 1

                    }
                    // patch starts and ends to same point
//                    for (Loopable<Corner> le : loop.loopableIterator()) {
//                        le.get().nextL.end = le.getNext().get().nextL.start;
//                    }
                }
            }
        }

//        Profile sideProfile = new Profile( 100 );
//        sideProfileMachine = new ProfileMachine( sideProfile );

        setup (out);
        
        for ( Edge e : liveEdges )
            if ( e.machine instanceof ProfileMachine )
            {
                ProfileMachine pm = (ProfileMachine)e.machine;
                output.faces.get( e.start ).profile = pm.profile.points.get( 0 ).start.get().tags;
            }

        
        globalFac.addToSkeleton( this );


        anchorHauler = new AnchorHauler( this );
        anchorHauler.add( plan.profiles.values() ) ;

//        if (CampSkeleton.instance != null)
//        {
//
//        pillarFactory = new PillarFactory();
//        pillarFactory.add( this );
//        pillarFactory.addToSkeleton( this );
//
//        windowFactory = new WindowFactory();
//        windowFactory.add( this );
//        windowFactory.addToSkeleton( this );
//
//        bayFactory = new NaturalFeatureFactory();
//        bayFactory.add( plan, planEdgeToBar );
//        bayFactory.addToSkeleton( this );
//
//        meshFactory = new MeshFactory();
//        meshFactory.add( this );
//        meshFactory.addToSkeleton( this );
//
//        CapFeatureFactory cff = new CapFeatureFactory();
//        cff.add( this );
//        cff.addToSkeleton( this );
//
//        RepeatNaturalStepFactory rnsf = new RepeatNaturalStepFactory();
//        rnsf.add( this );
//        rnsf.addToSkeleton( this );
////
////        if ( ProjectKensington.instance != null )
////        {
////            threeFactory = new ThreeFactory();
////            threeFactory.addToSkeleton( this );
////        }
//        }

//        ForcedStepFactory fsf = new ForcedStepFactory();
//        fsf.add( plan, planEdgeToBar );
//        fsf.addToSkeleton( this );
    }

    public void newProfile( Profile profile )
    {
//        for ( Global g : plan.globals)
//            globalFac.add( g, profile );
        profile.clearCache();
    }

    public void registerEdge (Edge edge, Global global, int valency, Bar bar)
    {
        ColumnProperties cp = new ColumnProperties( edge, global, valency );
        cp.parentColumn = null; // no parent
        cp.defBar = bar;
        columnProperties.put( edge, cp );
        planEdgeToBar.put( bar, edge );
    }

    // this event fires whenever a subsystem parents a face to another (normally direction height event)
    @Override
    public void parent( Face child, Face parent )
    {
        registerEdge( child.edge, parent.edge );
    }

    public void registerEdge (Edge edge, Edge parentColumn)
    {
        ColumnProperties p = getColumn( parentColumn );
        if (p == null)
        {
            System.err.println("failure to parent edge "+edge);
            return;
        }
        registerEdge( edge, parentColumn, p.global, p.valency );
    }

    public void registerEdge (Edge edge, Edge parentEdge, Global global, int valency)
    {
        ColumnProperties cp = new ColumnProperties( edge, global, valency );
        ColumnProperties pcp = getColumn( parentEdge );
        cp.parentColumn = pcp.parentColumn;
        cp.defBar = pcp.defBar;
        columnProperties.put( edge, cp );
        planEdgeToBar.put( cp.defBar, edge );
    }

    public Global getGlobalForEdge (Edge edge)
    {
        return getColumn(edge).global;
    }

    public int getValencyForEdge (Edge edge)
    {
        return getColumn(edge).valency;
    }


    public ColumnProperties getColumn (Edge edge)
    {
        Edge originator = output.getGreatestGrandParent( output.faces.get( edge.start ) ).edge;
        return columnProperties.get(originator);
    }

    /**
     * returns the lowest-down property set of a profile (eg - jumps between globals to get to the bottom)
     */
    public ColumnProperties getDefiningColumn (Edge edge)
    {
    	Face f = output.getGreatestGrandParent( output.faces.get( edge.start ) );
    	
    	if (f == null)
    		return null;
    	
        edge = f.edge;

        ColumnProperties cp =  columnProperties.get( edge );

        while (cp.parentColumn != null)
        {
            cp = columnProperties.get ( cp.parentColumn );
        }

        return cp;
    }

    public void addingProfile( Profile profile )
    {
        profile.clearCache();
        anchorHauler.add( profile );
    }

    /**
     * Sometimes we insert profiles that are defined as root, into non-root surface. when we do that,
     * we need to change the association of the edges that the profile makes
     */
    public void addingProfileRemapRoot( Profile profile, Global remapRootTo )
    {
        profile.clearCache();
        anchorHauler.addRemap (profile, remapRootTo);
    }

    /**
     * Information about a stack of edges.
     */
    public static class ColumnProperties
    {
        public Global global;
        public int valency;

        public Edge defPlanEdge; // the bottom of this stack (bottom of a global) - what we're indexed by
        
        public Edge parentColumn; // the bottom edge of the originating stack
        public Bar defBar; // and the bar it came from

        public ColumnProperties( Edge defPlanEdge, Global global, int valency )
        {
            this.defPlanEdge = defPlanEdge;
            this.global = global;
            this.valency = valency;
        }
    }

    /**
     * One bar can create multiple edges, it might be instanced as a feature, or
     * the edge might be involved in a split event. This will return a set of live
     * corners that originated from the given bar.
     */
    public Set<Corner> findLiveEdgesFrom( Bar planBar )
    {
        Set<Edge> allEdges = new HashSet<Edge> ( planEdgeToBar.get( planBar ) );
        Set<Corner> out = new HashSet<>();
        for (Corner c : liveCorners)
            if (allEdges.contains( c.nextL) )
                out.add(c);

        return out;
    }



    public void addMeshesTo(Preview preview)
    {
        for (Ship s : plan.ships)
            for (Ship.Instance i : s.getInstances())
                i.addMeshes(preview, this);
    }

    @Override
    public Comparator<Edge> getHorizontalComparator()
    {
        return new Comparator<Edge>()
        {
            @Override
            public int compare(Edge o1, Edge o2)
            {
                // allows an edge to take priority
                boolean w1 = false, w2 = false;

                for (Tag t : o1.profileFeatures)
                    w1 |= t instanceof LowPriorityTag;
                for (Tag t : o2.profileFeatures)
                    w2 |= t instanceof LowPriorityTag;


                if (w1 && ! w2)
                    return 1;

                if (w2 && ! w1)
                    return -1;

                return Double.compare(o1.getAngle(), o2.getAngle());
            }
        };
    }
}
