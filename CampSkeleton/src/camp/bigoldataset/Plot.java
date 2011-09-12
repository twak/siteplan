/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camp.bigoldataset;

import campskeleton.Global;
import campskeleton.Plan;
import javax.vecmath.Point2d;
import straightskeleton.ui.Bar;
import utils.Loop;
import utils.LoopL;

/**
 *
 * @author twak
 */
public class Plot extends Plan
{
//    public LoopL<Bar> plot = new LoopL();
//    public Map<Bar, Profile> profiles = new HashMap();
    public double height;
    public Point2d nearestRoad = new Point2d( Math.random() * 10000 + 5000, Math.random() * 10000 + 5000 );
    double area = 0;

    public Plot(Loop<Bar> barLoop, double height, Global root) {
        points.add( barLoop );
        this.height = height;


        globals.clear();
        assert (root.valency == 1);
        globals.add( root );
        this.root = root;
    }

    public Plot(LoopL<Bar> barLoop, double height, Global root) {
        points = barLoop;
        this.height = height;


        globals.clear();
        assert (root.valency == 1);
        globals.add( root );
        this.root = root;
    }
    
}
