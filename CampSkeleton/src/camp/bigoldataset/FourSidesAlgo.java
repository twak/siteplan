/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camp.bigoldataset;

import campskeleton.Profile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import straightskeleton.ui.Bar;
import utils.Loop;

/**
 *
 * @author twak
 */
class FourSidesAlgo extends Algo
{
    Map<String, Profile> mapping = new HashMap();

    final public static String
            p1 = "profile 1",
            p2 = "profile 2",
            p3 = "profile 3",
            p4 = "profile 4";

    static String[] args = new String[]{p1, p2, p3, p4};

    public FourSidesAlgo()
    {
//        for (String[] ss : args)
            for (String s : args)
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
    public void doit(Map<String, Profile> mapping, List<Plot> plots) {
        for (Plot plot_ : plots) {
            final Plot plot = plot_;


            int i = 0;
            for (Loop<Bar> loopl : plot.points)
                for (Bar b : loopl)
                {
                    Profile p = mapping.get(args[i]);
                    i = (i + 1) % args.length;

                    plot.profiles.put(b, p);
                }
            

        }
    }
}
