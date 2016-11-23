package campskeleton;

import straightskeleton.ui.Bar;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point2d;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Rainbow;

/**
 *
 * @author twak
 */
public class Profile
{
    public Color color = Rainbow.next( this.getClass() );
    public String name = Rainbow.lastAsString( this.getClass() )+" profile";
    // monotonic chains of points, each has a reference in the corresponding plan
    public LoopL <Bar> points = new LoopL<Bar>();

    public Set<String> properties = new LinkedHashSet(); // hack, but quite a useful one - haven't figured out when this is really needed (properties per profile rather than per bar?)
    
    public Map<Global, GlobalProfile> globalProfiles = new LinkedHashMap();

    // the user cannot see when we instance profiles at heights. This pointer to the template profile gives the expected ui behaviour
    public Profile copyOf = this;

    public Profile()
    {
    }

    public Profile( double initalDistance )
    {
        Loop<Bar> loop;
        points.add( loop = new Loop() );
        loop.append( new Bar (new Point2d( 0, 0 ), new Point2d( 0, -initalDistance ) ) );
    }

    public Profile ( Profile template, AffineTransform delta, Plan plan)
    {
        this.color = template.color;
        this.name = "dup'd "+template;
        this.copyOf = template.copyOf;
        this.properties.addAll( template.properties );

        for (Global g : template.globalProfiles.keySet())
        {
            // only move the root bar
            AffineTransform at = g == plan.root ? delta : null;

            for (Loop<Bar> loop : template.getGlobalProfile( g ).chainStarts)
            {
                Loop<Bar> dupe = Bar.clone( loop, at);
                getGlobalProfile( g ).chainStarts.add( dupe );
                points.add(dupe);
            }
            
            getGlobalProfile( g ).enabled = template.getGlobalProfile( g ).enabled;
        }
    }

    @Override
    public String toString()
    {
        return name;
    }

    public static Comparator<Profile> nameComparator = new Comparator<Profile> ()
    {
        public int compare( Profile o1, Profile o2 )
        {
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;
            return String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name);
        }
    };

    /**
     * ensure that the given global starts at the given height
     * 
     * @param y the target height that the chain should monotonically start at
     */
    public void assertHeight( Plan p, Global g, double y )
    {
        for ( Loop<Bar> l : globalProfiles.get(g).chainStarts)
        {
            double delta = y - l.start.get().start.y;
            if ( delta != 0 )
            {
                for ( Bar p2d : l )
                    p2d.end.y += delta;
                l.start.get().start.y +=delta;
            }
        }
    }
    
    public GlobalProfile getGlobalProfile (Global g)
    {
        GlobalProfile out = globalProfiles.get(g);
        if (out == null)
            globalProfiles.put(g, out = new GlobalProfile(g));
        return out;
    }

    static class BarDouble
    {
        public Bar bar; public double offset;
        public BarDouble (Bar bar, double offset)
        {
            this.bar = bar;
            this.offset = offset;
        }
    }

    public void clearCache()
    {
        for (GlobalProfile gp : globalProfiles.values())
            gp.clearCache();

        // build a list of those bars that the globals will be offset from
        ProfileIntersector pi = new ProfileIntersector();
        pi.intersectLines( this );

        for ( GlobalProfile gp : globalProfiles.values() )
            for ( int v = 0; v < gp.g.valency; v ++ )
            {
//                if (!gp.enabled)
//                    continue;

                Loop<Bar> loop = gp.chainStarts.get(v);
                ProfileChainInfo info = pi.globalToRightMostBar.get( loop );
                // valency set here as ProfileChainInfo doesn't take valency into account
                info.valency = v;
                gp.chainInfo.put( loop, info );
            }
    }

    // derrived properties of teh profile (recalcultated as required)
    public static class ProfileChainInfo
    {
        public Loop<Bar> profile;
        public Bar barToRight;
        public double distance;
        // as we travel to the right (-ve y towards +ve y) does this loop signify entry or exit from the solid?
        public boolean into;
        public int valency;
    }


    public static class GlobalProfile
    {
        Global g;
        // is this global active for this profile?
        public boolean enabled = true;

        // each global defines the start of a "loop" (really a chain). these chains enclose an area, so the primary/centre one has
        // one chain, and all others should have an even number (always two atm!)
        public List<Loop<Bar>> chainStarts = new ArrayList();

        // info that is derrived from the input
        transient Map <Loop<Bar>, ProfileChainInfo> chainInfo = new HashMap();


        public GlobalProfile (Global g)
        {
            this.g = g;
        }

        public void flipEnabled() {
            enabled = !enabled;
        }

//        @Override
//        public boolean equals( Object obj )
//        {
//            if (obj instanceof GlobalProfile)
//            {
//                GlobalProfile gp = (GlobalProfile)obj;
//                return gp.g.equals( g );
//            }
//            return false;
//        }

        public ProfileChainInfo getChainInfoForValency( int v)
        {
            return chainInfo.get(chainStarts.get(v));
        }

//        @Override
//        public int hashCode()
//        {
//            int hash = 5;
//            hash = 59 * hash + ( this.g != null ? this.g.hashCode() : 0 );
//            return hash;
//        }

        private void clearCache() {
            chainInfo = new HashMap<Loop<Bar>, ProfileChainInfo>();
        }
    }
}
