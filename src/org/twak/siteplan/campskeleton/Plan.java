package org.twak.siteplan.campskeleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Point2d;

import org.twak.camp.ui.Bar;
import org.twak.camp.ui.Marker;
import org.twak.siteplan.anchors.Anchor;
import org.twak.siteplan.anchors.ProfileAnchor;
import org.twak.siteplan.anchors.Ship;
import org.twak.siteplan.tags.PlanTag;
import org.twak.utils.Pair;
import org.twak.utils.collections.ConsecutivePairs;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;

/**
 * @author twak
 */
public class Plan
{
    public String name = "unnamed plan";
    
    // data structure (just used here as it has a start and end, other fields ignored)
    public LoopL<Bar> points = new LoopL();
    // a global starts a monotonic chain in a profile
    public List<Global> globals = new ArrayList();
    public Global root;
    // plan-bar to profile map
    public Map <Bar, Profile> profiles = new LinkedHashMap();

    // The features for profile segments
    public List<PlanTag> tags = new ArrayList();
    
    // instances of events that are attached to anchors
    public List<Ship> ships = new ArrayList();

//    public BuildFromPlan buildFromPlan = null;

    public Plan()
    {
        this ( new Global("0") );
    }

    public Plan(Global root)
    {
        this.root = root;
        root.valency = 1;
        globals.add( root );
    }

    public void addLoop (Loop<Bar> newLoop, Global g, Profile p)
    {
        List<Loop<Bar>> listOLoops = p.getGlobalProfile(g).chainStarts;
        listOLoops.add( newLoop );
    }
    /**
     * Creates a new profile associated with this plan. Adds
     * stubs for all existing globals!
     * 
     * @return
     */
    public Profile createNewProfile(Profile eg)
    {
        Profile example = eg == null ? profiles.values().iterator().next() : eg;

        Profile p = new Profile();

        String s = p.name; int i = 0; boolean seen = true;

        do
        {
            s = p.name;
            if (i++ > 0) {
                s += "(" + i + ")";
            }

            seen = false;
            for (Profile other : profiles.values()) {
                if (other.name.compareTo(s) == 0) {
                    seen = true;
                }
            }
        }
        while (seen);
        
        p.name = s;
        

        
        for ( Global g : globals )
        {
            for ( Loop<Bar> template : example.globalProfiles.get(g).chainStarts )
                if ( template != null )
                {
                    //  copy profile from template
                    Loop<Bar> loop = new Loop();

                    Point2d prev = null;
                    for ( Bar b : template )
                    {
                        if (prev == null)
                            prev = new Point2d (b.start);
                        loop.append( new Bar( prev, prev = new Point2d (b.end) ) );
                    }

                    addLoop( loop, g, p );
//                loopStarts.put( new GlobalProfile( g, p ), loop);

                    p.points.add( loop );
                }
                else
                    // just make something up? delete existing profiles? - example was probably null!
                    throw new Error();
             p.getGlobalProfile(g).enabled = example.getGlobalProfile(g).enabled;
        }

//        addLoop( p.points.get( 0 ), root, p );
//        loopStarts.put( new Plan.GlobalProfile( root, p),  );

        return p;
    }

    @Override
    public String toString()
    {
        return name;
    }

    public void clearCache()
    {
        for (Profile p : findProfiles())
            p.clearCache();
        
        for ( Ship s : ships )
            s.clearCache();
    }

    public int countMarkerMatches(Object generator)
    {
        int count = 0;
        for (Loop<Bar> loop : points)
            for (Bar b : loop)
                for (Marker m : b.mould.markersOn( b ))
                    if (m.generator.equals(generator))
                        count++;

       for (Profile p :findProfiles() )
           for (Bar b : p.points.eIterator())
               for (Marker m : b.mould.markersOn( b ))
                    if (m.generator.equals(generator))
                        count++;

         // todo: each feature may also have nested markers?
        for (Ship s : ships)
            count += s.countMarkerMatches(generator);

         return count;
    }

    public List<Profile> findProfiles()
    {
        List<Profile> vProfiles = new ArrayList ();
        for (Profile p : profiles.values())
            if (p != null && !vProfiles.contains( p ) )
                vProfiles.add(p); // todo: gah - rewriteme

        for (PlanTag t : tags)
            t.addUsedProfiles (vProfiles);

        // also collect from the subdivide events
        for (Ship s : ships)
            s.addUsedProfiles (vProfiles);

        // remove dupes
        vProfiles = new ArrayList(new HashSet (vProfiles));

        Collections.sort( vProfiles, Profile.nameComparator );
        return vProfiles;
    }

    public Set<Bar> findBars()
    {
        /**
         * Really we should use a weak map in plan.profiles?
         */
        Set<Bar> out = new LinkedHashSet <Bar>();
        for (Bar b : points.eIterator())
            out.add(b);

        for (PlanTag t : tags)
            t.addUsedBars(out);

        for (Ship s : ships)
            for (Ship.Instance si : s.getInstances())
                si.addUsedBars (out);
            
        return out;
    }

//    public void cleanAnchors(Set<)
//    {
//        // given that markers may have been removed, this updates the associated anchors
//        for (Ship s : ships)
//            for (Ship.Instance i : s.getInstances())
//                for (Ship.Anchor a : i.anchors)
//                {
//                    if (a)
//                }
//    }

    /**
     * There are some types of plans that animate throught time. Overriding instances
     * can specify the plan points in this update frame
     *
     */
    public void update (int frame, int delta )
    {
        for ( PlanTag t : tags )
            t.update(frame);

        // also collect from the subdivide events
        for ( Ship s : ships )
            s.update( frame, delta, this );
    }

    public enum AnchorType
    {
        PROFILE, PROFILE_PLAN
    }

    public Anchor createAnchor ( AnchorType type, Object... args)
    {
        switch (type)
        {
            case PROFILE:
                return new ProfileAnchor( args[0]);
            case PROFILE_PLAN:
                return new Anchor (args[0], args[1]);
        }
        return null;
    }

    public void setFrom (double[][] ptData, Profile[] profs)
    {
        points = new LoopL();
        Loop<Bar> loop = new Loop();
        points.add( loop );

        List<Point2d> lPoints = new ArrayList();
        for (double[] dv : ptData) {
            lPoints.add(new Point2d(dv[0], dv[1]));
        }

        int i = 0;
        for (Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>(lPoints, true)) {
            Bar b;
            loop.append(b = new Bar(pair.first(), pair.second()));
            profiles.put(b, profs[i++]);
        }
    }
}
