package camp.anchors;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;

public class ProfileAnchor extends Anchor
{
    public ProfileAnchor( Object profileGen )
    {
        super(null, profileGen);
        this.setProfileGen(profileGen);
    }

    @Override
    public boolean matches( Object plan, Object profile )
    {
        return plan == null && profile == getProfileGen();
    }

    @Override
    public JComponent createUI( ButtonGroup bg )
    {
        return new ProfileAnchorUI( this, bg );
    }

    @Override
    public Object getPlanGen() {
        // we have no such gen! there never was! the class hierarchy is broken!
        return null;
    }
}
