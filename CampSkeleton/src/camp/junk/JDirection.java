package camp.junk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import straightskeleton.ui.DirectionEvent;
import straightskeleton.ui.MachineEvent;
import utils.MUtils;

/**
 *
 * @author twak
 */
public class JDirection extends JComponent implements SelectableComponent
{
    public DirectionEvent direction;
    public JDirectionRack rack;
    public boolean selected;
    
    public JDirection() // unused?
    {
        setPreferredSize( new Dimension (30,30) );
        MouseAdapter ap = new DMouseListener();
        addMouseListener( ap );
        addMouseMotionListener( ap );
    }

    public JDirection (JDirectionRack rack, DirectionEvent direction)
    {
        this();
        this.rack = rack;
        setDirection( direction );
    }

    public void setDirection(DirectionEvent direction)
    {
        this.direction = direction;
        repaint();
    }

    public void paint(Graphics g)
    {
        g.setColor( selected ? Color.yellow  : Color.lightGray);
        g.fillArc( 0,0,getWidth(), getHeight(), 0, 360 );
        g.setColor( Color.darkGray );
        g.drawLine( getWidth()/2, getHeight()/2,
                (int)((getWidth()/2) *(Math.sin( direction.angle ) + 1 )) ,
                (int)((getHeight()/2) *(-Math.cos( direction.angle) + 1 )) );
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
        return direction;
    }

    public class DMouseListener extends MouseAdapter
    {
        Point old;
        @Override
        public void mousePressed( MouseEvent e )
        {
            old = e.getPoint();
            rack.selected(JDirection.this);
            repaint(); // might have been selected!
        }

        /**
         * Vertical direction gets thrown to parent, Horizontal changes angle
         * @param e
         */
        @Override
        public void mouseDragged( MouseEvent e )
        {
            int dX = e.getPoint().x - old.x;
            int dY = e.getPoint().y - old.y;

            direction.angle += dX / (double) 50;
            direction.height -= dY * rack.pixelsToHeight(); // not good, depends on height of JDirectionRack :(

            direction.angle = MUtils.clamp( direction.angle, -Math.PI/2+.1, Math.PI/2-.1);
            direction.height = MUtils.clamp( direction.height, 0, rack.height);

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
