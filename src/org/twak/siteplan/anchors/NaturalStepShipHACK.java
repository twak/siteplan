package org.twak.siteplan.anchors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.vecmath.Point2d;

import org.twak.siteplan.campskeleton.Siteplan;
import org.twak.utils.collections.Loopable;
import org.twak.camp.ui.Bar;
import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.Profile;

/**
 * hacked to animate the speed of a single edge
 * @author twak
 */
public class NaturalStepShipHACK extends NaturalStepShip
{

    @Override
    protected Instance createInstance() {
        return new NaturalInstanceHACK();
    }

    public class NaturalInstanceHACK extends NaturalInstance
    {
        @Override
        protected void update(int frame, int delta, Plan plan) {

//            if (1==1)
//                return; -2.5008333333333335
            double yOff = (70 - 30.01 )*5 / 60.;// Math.cos ( frame/20. ) * 40.;
            speeds.get(0).start.getNext().getNext().set( yOff );
            System.out.println(frame + " speed is "+yOff);
            
            double sideSpeed = yOff < 0 ? -4 : 4;
            speeds.get(0).start.getNext().set(sideSpeed);
            speeds.get(0).start.getNext().getNext().getNext().set(sideSpeed);

            double midOff = yOff < 0 ? -30 : 30;
            Loopable<Bar> mid = shape.get(0).start.getNext().getNext();
            mid.get().start = new Point2d ( mid.get().start.x,  midOff );
            mid.get().end = new Point2d ( mid.get().end.x, midOff+1);
            mid.getPrev().get().end = mid.get().start;
            mid.getNext().get().start = mid.get().end;

            Profile p = Siteplan.instance.plan.profiles.get( shape.get(0).start.getNext().get() );
            Point2d lastInP = p.points.get(0).start.getPrev().get().end;

            if ( (lastInP.x > 0 && yOff > 0) || (lastInP.x < 0 && yOff < 0 )) {
                // flip profile
                Set<Point2d> seen = new HashSet<Point2d>();
                for (Bar b : p.points.get(0)) {
                    for (Point2d pt : Arrays.asList(b.start, b.end))
                    if (!seen.contains (pt))
                    {
                        seen.add(pt);
                        pt.x = -pt.x;
                    }
                }
            }
        }
    }

}
