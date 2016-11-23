/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package camp.anim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import straightskeleton.ui.Bar;
import org.twak.utils.Cluster1D;
import org.twak.utils.Line;
import org.twak.utils.LoopL;

/**
 *
 * @author twak
 */
public class SaturdayPA
{
	/*
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
    
    static String[][] args = new String[][]
    {{FM},{SM},{BM}};
//    {{FM, FS},{SM, SS},{BM, BS}};

    public Map<Bar, String> assignProfiles( LoopL<Bar> plan )
    { //plan.get( 0 ).count();
        Map<Bar, String> out = new HashMap();
        
        Cluster1D<Bar> cluster = new Cluster1D<Bar>( plan.eIterator() )
        {
            @Override
            public double getVal( Bar d )
            {
//                Vector2d directionOfFront = new Vector2d( 0, -1 );
//                directionOfFront.sub( d.start );
//                directionOfFront.normalize();
//                double ee = new Vector2d( -(d.end.y - d.start.y), d.end.x - d.start.x ).angle( directionOfFront );

                return Math.atan2( -(d.end.y - d.start.y), d.end.x - d.start.x );

//                return new Vector2d( -(d.end.y - d.start.y), d.end.x - d.start.x ).angle( directionOfFront );
            }
        };

        // perp sides "captured" here
        Set<Bar> assigned = new HashSet();

        sides:
        for ( Object[] stuff : new Object[][]
                {
                    {
                        -Math.PI / 4, Math.PI / 4, BM, BS, BP, false, true
                    },
                    {
                        Math.PI / 4, 3. * Math.PI / 4, SM, SS, SP, false, false
                    },
                    {
                        -3. * Math.PI / 4, -Math.PI / 4, SM, SS, SP, false, false
                    },
                    {
                        -3. * Math.PI / 4, 3. * Math.PI / 4, FM, FS, FP, true, true
                    }
                } )
        {
            double min = (Double) stuff[0];
            double max = (Double) stuff[1];
            boolean disjoint = (Boolean) stuff[5];
            boolean doPerp = (Boolean) stuff[6];

            String M = (String) stuff[2],
                   S = (String) stuff[3],
                   P = (String) stuff[4];

            Set<Bar> clustered;
            if ( !disjoint )
                clustered = cluster.getStuffBetween( min, max );
            else
            {
                clustered = new HashSet();
                clustered.addAll( cluster.getStuffBetween( -Double.MAX_VALUE, min ) );
                clustered.addAll( cluster.getStuffBetween( max, Double.MAX_VALUE ) );
            }

            double maxL = -Double.MAX_VALUE;
            Bar longest = null;
            for ( Bar b : clustered )
            {
                out.put( b, M );
                double length = b.length();
                if ( length > maxL )
                {
                    longest = b;
                    maxL = length;
                }
            }

//            boolean isFront = (((String) stuff[2]) == FM);
//            boolean isBack = (((String) stuff[2]) == BM);
//
//            if ( doPerp )
//                for ( Loop<Bar> loop : plan )
//                    for ( Loopable<Bar> loopable : loop.loopableIterator() )
//                    {
//                        Bar b = loopable.get();
//                        if ( clustered.contains( b ) && clustered.contains( loopable.getNext().getNext().get() ) )
//                        {
//                            Bar perp = loopable.getNext().get();
//                            assigned.add( perp );
//                            out.put( perp, P );
//                        }
//                    }

            clustered.removeAll( assigned ); // don't reassign perp edges

            if ( clustered.isEmpty() )
                continue sides;

            Bar sample = clustered.iterator().next();
            final Line ref = new Line( 0, 0, -(sample.start.y - sample.end.y), sample.start.x - sample.end.x );

            // pick the front-most bar as the main, everything else is secondary
//            Cluster1DList<Bar> perpCluster = new Cluster1DList<Bar>( clustered )
//            {
//                @Override
//                public double getVal( Bar d )
//                {
//                    Point2d loc = new Line( d.start, d.end ).intersects( ref, false );
//                    if ( loc == null )
//                        return 0;
//                    return loc.distance( new Point2d (0, -1000 ) );
//                }
//            };
//
//            Set<Bar> togo = new HashSet( clustered );
//
//            Map<Double, Set<Bar>> distanceToBar = new HashMap();
//
//            while ( !togo.isEmpty() )
//            {
//                Bar b = togo.iterator().next();
//                Double val = perpCluster.getVal( b );
//
//                Set<Bar> near = perpCluster.getNear( val, 2. );
//                assert (near.contains( b ));
//                togo.removeAll( near );
//                togo.remove( b );
//                distanceToBar.put( val, near );
//            }

//            List<Double> vals = new ArrayList( distanceToBar.keySet() );
//            Collections.sort( vals );

            // main is the longest
//            for ( Bar b : distanceToBar.get( vals.get( vals.size() - 1 ) ) )
//                out.put( b, M );

//            for ( int i = 0; i < vals.size() - 1; i++ )
//                    out.put( b, S );
//            for ( int i = 0; i < vals.size(); i++ )
//                for ( Bar b : distanceToBar.get( vals.get( i ) ) )
//                    out.put( b, M );
        }
        
        return out;
    }*/
}
