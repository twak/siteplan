package org.twak.siteplan.anchors;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;

public class Anchor
{
    private Object planGen;
    private Object profileGen;
    String name = "unknown";

    public Anchor( Object planGen, Object profileGen )
    {
        super();
        this.planGen = planGen;
        this.profileGen = profileGen;
    }

    public boolean matches( Object plan, Object profile )
    {
        return plan == getPlanGen() && profile == getProfileGen();
    }

    public JComponent createUI( ButtonGroup bg )
    {
        return new AnchorUI( this, bg );
    }

    /**
     * @return the planGen
     */
    public Object getPlanGen() {
        return planGen;
    }

    /**
     * @param planGen the planGen to set
     */
    public void setPlanGen(Object planGen) {
        this.planGen = planGen;
    }

    /**
     * @return the profileGen
     */
    public Object getProfileGen() {
        return profileGen;
    }

    /**
     * @param profileGen the profileGen to set
     */
    public void setProfileGen(Object profileGen) {
        this.profileGen = profileGen;
    }
}
