package camp.junk;

import campskeleton.PlanUI;
import campskeleton.PlanUI;
import straightskeleton.Tag;
import straightskeleton.ui.PointEditor.BarSelected;

/**
 *
 * @author twak
 */
public abstract class PlanFeature extends Tag
{
    public PlanFeature(String name)
    {
        super (name);
    }
    public abstract PlanUI getEditor( BarSelected bs );
}
