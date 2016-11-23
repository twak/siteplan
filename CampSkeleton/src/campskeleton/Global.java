package campskeleton;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import javax.vecmath.Point2d;
import straightskeleton.ui.Bar;
import org.twak.utils.Loop;

/**
 * There's one of these for every inflection over a plan. All profiles inflect at the same point to preserve
 * loop-properties.
 * 
 * @author twak
 */
public class Global
{
    public String name = "0";

    public int valency = 2;

    public Profile edgeProfile;

    Global()
    {
    }

    public Global( String string )
    {
        this();
        this.name = string;
    }

    public void assertHeight( Plan plan, double y )
    {
          for (Profile prof : new LinkedHashSet<Profile> ( plan.profiles.values()) ) // might repeat :(
          {
              prof.assertHeight ( plan, this, y );
          }
    }

    public void remove( Plan plan )
    {
        if (this == plan.root)
            throw new Error ("Sorry dave, I can't do that");
        plan.globals.remove( this );
        for (Profile prof : new LinkedHashSet<Profile> ( plan.profiles.values()) ) // might repeat :(
        {
            List<Loop<Bar>> bars = prof.globalProfiles.get(this).chainStarts;
            if ( bars != null )
            {
                prof.points.removeAll( bars );
                prof.globalProfiles.remove( this );
            }
        }
    }

    public void add( Plan plan, Point2d loc )
    {
        plan.globals.add(this);
        for (Profile p : new LinkedHashSet<Profile> ( plan.profiles.values() ))
        {
            for (int xLoc : Arrays.asList( new Integer[] { -50,0} ) )
            {
                Loop<Bar> loop = new Loop();
                loop.append( new Bar ( new Point2d( loc.x + xLoc, loc.y ) , new Point2d( loc.x + 50 + xLoc, loc.y - 100 ) ) );
                p.points.add( loop );
                plan.addLoop( loop, this, p );//loopStarts.put( new Plan.GlobalProfile( g, p ), loop );
            }
        }
    }
}
