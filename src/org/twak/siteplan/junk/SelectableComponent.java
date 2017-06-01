
package org.twak.siteplan.junk;

import org.twak.camp.ui.MachineEvent;

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
