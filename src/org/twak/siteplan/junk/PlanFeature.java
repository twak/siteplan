package org.twak.siteplan.junk;

import org.twak.camp.Tag;
import org.twak.camp.ui.PointEditor.BarSelected;
import org.twak.siteplan.campskeleton.PlanUI;

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
