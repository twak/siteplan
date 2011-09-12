/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camp.bigoldataset;

import campskeleton.Profile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Vector2d;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import utils.Cluster1D;
import utils.Loop;
import utils.Loopable;

/**
 *
 * @author twak
 */
class SaturdayAlgo extends Algo
{
    Map<String, Profile> mapping = new HashMap();

    final public static String
            FM = "front main",
            FS = "front second",
            FP = "front perp",

            SM = "side main",
            SS = "side second",
            SP = "side perp",

            BM = "back main",
            BS = "back second",
            BP = "back perp";


    static String[][] args = new String[][] {{FM,FP},{SM,SP},{BM,BP}};

    public SaturdayAlgo()
    {
        for (String[] ss : args)
            for (String s : ss)
                mapping.put( s, null );
    }

    public Map<String, Profile> getProperties()
    {
        return mapping;
    }

    /**
     * Algo here is to:
     * 1) cluster sides by normal (Gaussian image)
     * 2) group image via orientation of nearest road (NW to NE, NE to SE...)
     * 3) Bars between two of the same group are assigned "perpendicular" profile
     * 4) We cluster again by perp location, and find the longest cluster to be the main
     * 5) The other clusters are second
     * 
     * @param mapping
     * @param plots
     */
    @Override
    public void doit( Map<String, Profile> mapping, List<Plot> plots )
    {
        for (Plot plot_ : plots)
        {
            final Plot plot = plot_;

//            for (Bar b : plot.points.eIterator())
//                b.markers.clear();
            if (true)
                throw new Error ("tom: you have to fix this case, since we added moulds");


            // gaussian image - cluster by angle from front of building
            Cluster1D<Bar> cluster = new Cluster1D<Bar>( plot.points.eIterator() ) {
                @Override
                public double getVal( Bar d )
                {
                    Vector2d directionOfFront = new Vector2d(plot.nearestRoad);
                    directionOfFront.sub( d.start );
                    directionOfFront.normalize();

                    double ee = new Vector2d( - (d.end.y - d.start.y), d.end.x - d.start.x).angle(directionOfFront) ;

                    return new Vector2d( - (d.end.y - d.start.y), d.end.x - d.start.x).angle(directionOfFront);
                }
            };

            // perp sides "captured" here
            Set<Bar> assigned = new HashSet();

            sides:
            for (Object [] stuff :new Object[][] { 
                { -Math.PI/4, Math.PI/4, BM, BS, BP, false, true },
                { Math.PI/4, 3.*Math.PI/4, SM, SS, SP, false, false },
                { -3.*Math.PI/4, -Math.PI/4, SM, SS, SP, false, false },
                { -3.*Math.PI/4, 3.*Math.PI/4, FM, FS, FP, true, true }} )
            {
                double min = (Double)stuff[0];
                double max = (Double)stuff[1];
                boolean disjoint = (Boolean)stuff[5];
                boolean doPerp = (Boolean)stuff[6];

                Profile M = mapping.get( (String) stuff[2]),
                        S = mapping.get( (String) stuff[3]),
                        P = mapping.get( (String) stuff[4]);
                
                Set<Bar> clustered;
                if (!disjoint)
                 clustered = cluster.getStuffBetween( min, max );
                else
                {
                    clustered = new HashSet();
                    clustered.addAll( cluster.getStuffBetween( -Double.MAX_VALUE, min) );
                    clustered.addAll( cluster.getStuffBetween( max, Double.MAX_VALUE ) );
                }

                double maxL = -Double.MAX_VALUE;
                Bar longest = null;
                for (Bar b : clustered)
                {
                    plot.profiles.put(b, M);
                    double length = b.length();
                    if (length > maxL)
                    {
                        longest = b;
                        maxL = length;
                    }
                }

                boolean isFront = (((String) stuff[2]) == FM);
                boolean isBack = (((String) stuff[2]) == BM);

                if (plot.area > 40 && (isFront || isBack)) // for the front of the house
                // for all faces, add a window
                {
                    for (Bar b : clustered) {
                        double length = b.length();
                        if (length < 3)
                            continue;

                        // 3 padding at start, end
                        double padding = Math.min( 3, length/3);

                        double toAssign = length - padding*2;
                        if (toAssign > 0) { // always true
                            int count = (int) Math.floor(toAssign / 2.);

                            Vector2d dir = new Vector2d(b.end);
                            dir.sub(b.start);
                            dir.normalize();

                            double fLength = toAssign / Math.max( 1, (count - 1) ); // space between windows
                            Vector2d delta = new Vector2d(dir);
                            delta.scale(fLength);
//                            delta.add(b.start);

                            Vector2d start = new Vector2d(dir);
                            start.scale(3);
                            start.add(b.start);
                            
                            if (count <= 1)
                            {
                                start = new Vector2d(b.end);
                                start.add(b.start);
                                start.scale(0.5);
                            }

                            for (int i = 0; i < count; i++)
                            {
                                System.out.println("adding " + i);

                                Vector2d d2 = new Vector2d(delta);
                                d2.scale(i);
                                d2.add(start);

                                // add a door to the longest
                                if (isFront ) {

                                    if (i == 0 && longest == b) {
                                        System.out.println("adding a door ");
                                        Marker m = new Marker(AtlantisEditor.instance.at.doorFeature);
                                        m.set(d2);
                                        b.mould.create( m, null );
                                        m.bar = b;
                                    } 
                                    else
                                    {
                                        // and second+floor windows to the others
                                        Marker groundWin = new Marker(AtlantisEditor.instance.at.groundWindowFeature);
                                        groundWin.set(d2);
                                        b.mould.create( groundWin, null );
                                        groundWin.bar = b;
                                    }
                                }
                                if (true) // always add second floor windows.
                                {
                                    if (Math.random() > 0.15)
                                    {
                                        Marker groundWin = new Marker(AtlantisEditor.instance.at.groundWindowFeature);
                                        groundWin.set(d2);
                                        b.mould.create( groundWin, null );
                                        groundWin.bar = b;
                                    }

                                    if (Math.random() > 0.05) {
                                        // and just ground floor windows to the rest of the first floor
                                        Marker window = new Marker(AtlantisEditor.instance.at.windowFeature);
                                        window.set(d2);
                                        b.mould.create( window, null );
                                        window.bar = b;
                                    }
                                }
                            }
                        }
                    }
                }

                if (doPerp)
                {
                for ( Loop<Bar> loop : plot.points)
                    for ( Loopable<Bar> loopable : loop.loopableIterator() )
                    {
                        Bar b = loopable.get();
                        if (clustered.contains( b ) && clustered.contains(  loopable.getNext().getNext().get()) )
                        {
                            Bar perp = loopable.getNext().get();
                            assigned.add( perp );
                            plot.profiles.put( perp, P );
                        }
                    }
                }
//
//                clustered.removeAll(assigned); // don't reassign perp edges
//
//                if (clustered.isEmpty())
//                    continue sides;
//
//                Bar sample = clustered.iterator().next();
//                final Line ref = new Line ( 0,0, - (sample.start.y - sample.end.y), sample.start.x - sample.end.x );
//
//                Cluster1D<Bar> perpCluster = new Cluster1D<Bar>( clustered ) {
//                @Override
//                public double getVal( Bar d )
//                {
//                    Point2d loc = new Line (d.start, d.end).intersects( ref, false );
//                    if (loc == null)
//                        return 0;
//                    return loc.distance( plot.nearestRoad );
//                }
//            };
//
//                Set<Bar> togo = new HashSet(clustered);
//
//                Map <Double, Set<Bar>> distanceToBar = new HashMap();
//
//                while (!togo.isEmpty())
//                {
//                    Bar b = togo.iterator().next();
//                    Double val =  perpCluster.getVal( b );
//
//                    Set<Bar> near = perpCluster.getNear (val, 2. );
//                    assert (near.contains(b));
//                    togo.removeAll( near );
//                    togo.remove(b);
//                    distanceToBar.put (val, near);
//                }
//
//                List<Double> vals = new ArrayList(distanceToBar.keySet());
//                Collections.sort(vals);
//
//                // main is the longest
//                for ( Bar b : distanceToBar.get(vals.get(vals.size()-1)) )
//                    plot.profiles.put( b, M );
//
//                for (int i = 0; i < vals.size()-1; i++)
//                    for ( Bar b : distanceToBar.get( vals.get( i ) ) )
//                    {
//                        plot.profiles.put( b, S);
//                    }
            }
        }
    }

}
