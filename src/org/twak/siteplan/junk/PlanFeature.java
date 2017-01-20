package org.twak.siteplan.junk;

import org.twak.siteplan.campskeleton.PlanUI;
import org.twak.straightskeleton.Tag;
import org.twak.straightskeleton.ui.PointEditor.BarSelected;

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
