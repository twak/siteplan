package org.twak.siteplan.junk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

import org.twak.camp.ui.MachineEvent;

/**
 * Doffit that represents a window event in the direciton - rack
 * @author twak
 */
public class JWindow extends JComponent implements SelectableComponent
{
    public WindowEvent direction;
    public JDirectionRack rack;
    public boolean selected;
    
    public JWindow() // unused?
    {
        setPreferredSize( new Dimension (30,30) );
        MouseAdapter ap = new DMouseListener();
        addMouseListener( ap );
        addMouseMotionListener( ap );
    }

    public JWindow (JDirectionRack rack, WindowEvent direction)
    {
        this();
        this.direction = direction;
        this.rack = rack;
    }

    @Override
    public void paint(Graphics g)
    {

        g.setColor( selected ? Color.cyan  : Color.blue );
        g.fillArc( 0,0,getWidth(), getHeight(), 0, 360 );
        g.setColor( Color.black );
        g.drawArc( 0,0,getWidth(), getHeight(), 0, 360 );
    }

    public void setSelected( boolean selected )
    {
        this.selected = selected;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public MachineEvent getMachineEvent()
    {
        return null;
    }

    public class DMouseListener extends MouseAdapter
    {
        Point old;
        @Override
        public void mousePressed( MouseEvent e )
        {
            old = e.getPoint();
            rack.selected(JWindow.this);
            repaint(); // might have been selected!
        }

        /**
         * Vertical direction gets thrown to parent, Horizontal changes angle
         * @param e
         */
        @Override
        public void mouseDragged( MouseEvent e )
        {
            int dY = e.getPoint().y - old.y;

//            direction.height -= dY * rack.pixelsToHeight(); // not good, depends on height of JDirectionRack :(
//
//            direction.height = MUtils.clamp( direction.height, 0, rack.height);

            Point sL = getLocation();
            getParent().doLayout(); //...?
            Point eL = getLocation();

            // position for mouse event deltas
            old = new Point (
                    e.getPoint().x - eL.x + sL.x,
                    e.getPoint().y - eL.y + sL.y );
            repaint();
            // hack - force root repaint to refresh skeleton
            getRootPane().repaint();
        }
        
    }
}
