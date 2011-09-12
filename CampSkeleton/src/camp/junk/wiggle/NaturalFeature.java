package camp.junk.wiggle;

import campskeleton.Global;
import camp.junk.PlanFeature;
import campskeleton.Plan;
import campskeleton.PlanSkeleton;
import campskeleton.PlanUI;
import campskeleton.Profile;
import campskeleton.ProfileMachine;
import java.awt.Color;
import java.util.List;
import straightskeleton.Corner;
import straightskeleton.CornerClone;
import straightskeleton.Edge;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import straightskeleton.ui.PointEditor.BarSelected;
import utils.LContext;
import utils.LoopL;

/**
 * dec@cs.
 * @author twak
 */
public abstract class NaturalFeature extends PlanFeature
{

    double radius = 20;
    Profile[] profiles = new Profile[5];
    public LoopL<Bar> shape = new LoopL();

    /**
     * Bar is the plan-bar in LoopL<Bar> shape, above
     * @return the distance we want to expand this bar by.
     */
    public double getSize( Bar bar )
    {
        return radius;
    }

    public NaturalFeature( Plan plan, String name )
    {
        super( name );

    }

    public Color getColor()
    {
        return Color.pink;
    }

    void setAllProfiles( Profile createBay, Plan plan )
    {
        for ( int i = 0; i < profiles.length; i++ )
            profiles[i] = createBay;

        for ( Bar b : shape.eIterator() )
            plan.profiles.put( b, createBay );
    }

    abstract public PlanUI getEditor( BarSelected bs );


    /** context overwhelming **/
    public abstract Corner insert(  // returns the last corner in the object we've just added.
            PlanSkeleton skel,
            Marker m,
            LContext<Corner> leadingCorner, // the edge we're editing
            Edge old,
            CornerClone cc, // the new data structure
            List<ProfileMachine> machines, // as specified by this NatrualFeature's shape LoopL<Bar>
            List<Bar> bars, // the bars from this NaturalFeature
            double height, // the height of this event
            Global global, 
            int valency );
}
