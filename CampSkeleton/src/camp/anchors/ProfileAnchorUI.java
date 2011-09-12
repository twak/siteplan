package camp.anchors;

import campskeleton.CampSkeleton;
import java.awt.Color;
import javax.swing.ButtonGroup;

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

        if (CampSkeleton.instance.plan.countMarkerMatches(anchor.getProfileGen()) > 0)
            count++;

        selectButton.setText( count + "/1" );
        selectButton.setForeground( count < 1 ? Color.red : Color.green );
    }
}
