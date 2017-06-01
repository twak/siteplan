package org.twak.siteplan.junk;

import java.util.Arrays;
import javax.vecmath.Point2d;

import org.twak.camp.ui.Bar;
import org.twak.camp.ui.PointEditor.BarSelected;
import org.twak.siteplan.campskeleton.ForcedUI;
import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.PlanUI;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.utils.ConsecutiveItPairs;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Pair;

/**
 *
 * @author twak
 */
public class ForcedStep extends PlanFeature
{
    public LoopL<Bar> shape = new LoopL();

    public ForcedStep(){ super ("a forced step"); }

    public ForcedStep(Plan plan)
    {
        this();

        Loop<Bar> line = new Loop();

        Point2d[] coords = new Point2d[] {
            new Point2d (0,0), new Point2d (50,0), new Point2d (50,-20), new Point2d (250,-20), new Point2d (250,0), new Point2d (300, 0) };

        Profile profile = new Profile( 50 );

        for ( Pair<Point2d, Point2d> pair : new ConsecutiveItPairs<Point2d>( Arrays.asList( coords ) ) )
        {
            Bar b = new Bar (pair.first(), pair.second());
            line.append( b );
            plan.profiles.put( b, profile );
        }


        shape.add( line );
        
//        plan.tags.add( this );
        plan.addLoop( profile.points.get( 0 ), plan.root, profile );

    }

    @Override
    public PlanUI getEditor( BarSelected bs )
    {
        return new ForcedUI( this, bs );
    }
}