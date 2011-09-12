/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camp.junk;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.border.LineBorder;
import straightskeleton.ui.MachineEvent;

/**
 *
 * @author twak
 */
public class JDirectionRack extends JComponent
{
    // height that this rack represents
    double height = 200;
    List<MachineEvent> model = new ArrayList();
    SelectableComponent selected = null;


    public JDirectionRack()
    {
        setBorder( new LineBorder ( Color.black, 4 ));
        setPreferredSize( new Dimension (50, 200));
        setMaximumSize( new Dimension (50, Integer.MAX_VALUE));
    }

    public JDirectionRack( List<MachineEvent> model)
    {
        this();
        for (MachineEvent jd : model)
            addDirection( jd );
        this.model = model;
    }

    public Collection<MachineEvent> getDirectionsCopy()
    {
        List<MachineEvent> out = new ArrayList();
        out.addAll( model );
        Collections.sort( out, new Comparator<MachineEvent>()
        { public int compare( MachineEvent o1, MachineEvent o2 )
          { return Double.compare( o1.height, o2.height ); } } );
        return out;
    }

    @Override
    public void paint( Graphics g )
    {
        g.setColor( getBackground() );
        g.fillRect( 0,0, getWidth(), getHeight());
        super.paint( g );
    }

    String MACHINE_EVENT = "machine event";

    public void addDirection ( MachineEvent dir )
    {
//        JComponent jc = dir.createUI( this );
//        jc.putClientProperty( MACHINE_EVENT, dir);
//        add(jc);
        model.add( dir );
        doLayout();
    }

    public void removeSelectedDirection()
    {
        if (selected == null)
            return; // make an annoying sound
        removeDirection( selected.getMachineEvent() );
        selected = null;
        repaint();
    }

    public void removeDirection( MachineEvent dir )
    {
        Component togo = null;

        for ( Component c : getComponents() )
        {
            if ( c instanceof SelectableComponent )
            {
                SelectableComponent jd = (SelectableComponent) c;
                if ( dir == jd.getMachineEvent() )
                {
                    togo = c;
                }
            }
        }
        if ( togo != null )
        {
            remove( togo );
            model.remove( dir );
        }
    }

    @Override
    public void doLayout()
    {
        for (Component c : getComponents())
        {
            if (c instanceof JComponent)
            {
                JComponent jd = (JComponent)c;
                MachineEvent me = (MachineEvent)jd.getClientProperty( MACHINE_EVENT );
                if ( me != null )
                {
                    jd.setSize( jd.getPreferredSize() );
                    jd.setLocation( 0, getHeight() - jd.getHeight() - (int) ( me.height * ( getHeight() - jd.getHeight() ) / height ) );
                }
            }
        }
    }

    public double  pixelsToHeight()
    {
        return (height / (getHeight() -30 ));
    }

    void selected( SelectableComponent aThis )
    {
        if ( selected != null )
        {
            selected.setSelected( false );
            selected.repaint();
        }
        
        aThis.setSelected( true );
        selected = aThis;
    }
}
