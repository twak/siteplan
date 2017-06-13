package org.twak.siteplan.anchors;

import java.awt.Color;
import javax.swing.ButtonGroup;

import org.twak.siteplan.campskeleton.SitePlan;

/**
 * Type of anchor that only has a height. Okay the class hierarchy is upside down, but this is
 * academia :p
 *
 * @author twak
 */
public class ProfileAnchorUI extends AnchorUI
{
    public ProfileAnchorUI( ProfileAnchor anchor, ButtonGroup bg )
    {
        super ( anchor, bg );
    }

    @Override
    public void updateButton()
    {
        int count = 0;

        if (SitePlan.instance.plan.countMarkerMatches(anchor.getProfileGen()) > 0)
            count++;

        selectButton.setText( count + "/1" );
        selectButton.setForeground( count < 1 ? Color.red : Color.green );
    }
}
