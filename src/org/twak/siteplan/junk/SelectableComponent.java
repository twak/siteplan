
package org.twak.siteplan.junk;

import org.twak.straightskeleton.ui.MachineEvent;

/**
 *
 * @author twak
 */
public interface SelectableComponent
{

    public MachineEvent getMachineEvent();
    public void setSelected(boolean selected);
    public boolean isSelected();
    public void repaint();
}
