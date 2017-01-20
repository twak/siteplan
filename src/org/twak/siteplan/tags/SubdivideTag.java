package org.twak.siteplan.tags;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.utils.WeakListener.Changed;

/**
 *
 * @author twak
 */
public class SubdivideTag extends PlanTag
{
    public final static String side = "side", bottom = "bottom", top = "top";
    public final static List<String> types = Arrays.asList( new String[]{side, bottom, top} );

    public Map<String, ProfileMerge> assignments = new HashMap();
    public boolean enabled = true;

    public SubdivideTag()
    {
        super("subdivide");
    }

    @Override
    public JComponent getToolInterface(Changed rebuildFeatureList, Plan plan) {
        return new SubdivideTagUI (rebuildFeatureList, plan, this);
    }
    
    public static class ProfileMerge
    {
        public Profile profile;
        public boolean merge;

        public ProfileMerge( Profile profile, boolean merge )
        {
            this.profile = profile;
            this.merge = merge;
        }
    }

    @Override
    public void addUsedProfiles( List<Profile> vProfiles )
    {
        for (ProfileMerge pm : assignments.values())
            if (pm.profile != null)
                vProfiles.add( pm.profile );
    }
}
