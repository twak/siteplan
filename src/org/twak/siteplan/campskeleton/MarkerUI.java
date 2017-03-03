package org.twak.siteplan.campskeleton;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.vecmath.Point2d;

import org.twak.siteplan.anchors.Anchor;
import org.twak.straightskeleton.Tag;
import org.twak.straightskeleton.ui.Bar;
import org.twak.straightskeleton.ui.Marker;
import org.twak.straightskeleton.ui.NaiveMould.PMarker;
import org.twak.straightskeleton.ui.PointEditor;
import org.twak.utils.LContext;
import org.twak.utils.Loopable;
import org.twak.utils.ui.Colour;

/**
 * Shared features for marker manipulations (between planUI and profileUI)
 *
 * @author twak
 */
public class MarkerUI extends PointEditor
{

    boolean paintMarkers = true, paintTags = true;

    public MarkerUI( BarSelected selected )
    {
        super( selected );
    }

    public LContext<Bar> positionMarker( Marker m, Bar oldBar )
    {
        Object generator = oldBar.mould.remove( m );

        LContext<Bar> bar = getNearest( m, Double.MAX_VALUE ); //n squared. erp.
        m.bar = bar.get();
        bar.get().mould.create( m, generator );

        refreshMarkersOn( oldBar );
        refreshMarkersOn( bar.get() );
        return bar;
    }

    @Override
    public void remove( LContext<Bar> ctx, Point2d dragged )
    {

        if (CampSkeleton.instance == null)
            return;

        switch ( CampSkeleton.instance.mode )
        {
            case Tag:

                if (ctx.loopable == null)
                    return;

                if ( CampSkeleton.instance.selectedTag == null )
                {
                    JOptionPane.showMessageDialog( this, "Please select a type of tag to remove" );
                    return;
                }

                ctx.get().tags.remove( CampSkeleton.instance.selectedTag );

                break;

            case Features:


        if ( !allowRemove( ctx, dragged ) )
            return;
        
                if ( dragged instanceof Marker )
                {
                    Marker m = (Marker) dragged;
                    m.bar.mould.remove( m );
                    refreshMarkersOn( m.bar );
                    repaint();
                    return;
                }
                break;
                
            case Vertex:

                if (!allowRemove( ctx, dragged ))
                    return;

                if ( ctx.loopable == null )
                    return;
                // maybe the start of the loop if we're dealing with lines!
                Bar bar = ctx.get();

                removeMarkersFromBar( bar );

                Loopable<Bar> loopable = ctx.loopable;


                ctx.loop.remove( loopable );



                if ( bar.start == dragged )
                {
                    if ( loopable.getPrev().get().end == dragged )
                        loopable.getPrev().get().end = loopable.get().end;
                }
                else if ( bar.end == dragged )
                {
                    if ( loopable.getNext().get().start == dragged )
                        loopable.getNext().get().start = loopable.get().start;
                }
                else
                    throw new Error( "something fishy going on here" );

                for ( Bar b : ctx.loop )
                    refreshMarkersOn( b );
                break;
        }
        repaint();
    }

    @Override
    protected boolean allowRemove( LContext<Bar> ctx, Point2d corner )
    {
        if ( corner instanceof Marker )
        {
            Marker m = (Marker) corner;

            m.bar.mould.remove( (Marker) corner );
            return true;
        }
        return false;
    }

    @Override
    protected boolean allowDrag( Point2d dragged )
    {
        if ( dragged instanceof Marker )
            return paintMarkers;
        else
            return true;
    }
    Point pressed = new Point();

    @Override
    public void movePoint( LContext<Bar> ctx, Point2d pt, javax.vecmath.Point2d location, MouseEvent evt )
    {
        if ( pt instanceof Marker )
        {
            // only when in the correct mode!
            if ( CampSkeleton.instance.mode != Tool.Features )
                return;

            if ( pressed == null ) // when we start moving
                pressed = evt.getPoint();

            Marker m = (Marker) pt;

            m.set( location );

            LContext<Bar> bar = positionMarker( m, m.bar ); // projects down

            // update the thing being dragged, so we have the correct line next time
            ctx.loop = bar.loop;
            ctx.loopable = bar.loopable;
        }
        else
        {
            pt.x = location.x;
            pt.y = location.y;

            for ( Bar b : ctx.loop )
                refreshMarkersOn( b );
        }
    }

    @Override
    public void addBetween( LContext<Bar> ctx, Point l )
    {
        CampSkeleton cs = CampSkeleton.instance;
        switch ( cs.mode )
        {
            case Vertex:
                addVertex( ctx, l );

                break;

            case Tag:
                if ( cs.selectedTag == null )
                {
                    JOptionPane.showMessageDialog( this, "Please select a type of tag to add" );
                    return;
                }

                ctx.get().tags.add( cs.selectedTag );

                break;

            case Anchor:
            case Features:
//                if (cs.selectedFeature == null)
//                {
//                    JOptionPane.showMessageDialog( this, "Please select a marker type to add" );
//                    return;
//                }

                Marker m = new Marker( null );
                m.set( l.x, l.y );
                m.bar = ctx.get();
                ctx.get().mould.create( m, null );
                refreshMarkersOn( m.bar );
                releasePoint( m, ctx, null );

                break;
        }
    }

    protected void addVertex( LContext<Bar> ctx, Point l )
    {
        Point2d n = new Point2d( l.x, l.y );
        Loopable<Bar> loopable = ctx.loop.addAfter( ctx.loopable, new Bar( n, ctx.get().end ) );

        ctx.get().end = n;

        dragged = new LContext<Bar>( loopable, ctx.loop );
        dragged.hook = n;

        loopable.get().tags.addAll( ctx.get().tags );
//                loopable.get().mould = ctx.get().mould; eventually...not yet!

        edgeAdded( dragged );

        refreshMarkersOn( ctx.get() );
        refreshMarkersOn( loopable.get() );
    }

    @Override
    public void showMenu( LContext<Bar> dragged, MouseEvent evt )
    {
        if ( dragged.hook instanceof Marker )
        {
            Marker m = (Marker) dragged.hook;
            final PMarker p = (PMarker) m.generator;

            JPopupMenu popup = new JPopupMenu();
//            JMenu menu = new JMenu("type:");
//            popup.add(menu);

            final JCheckBoxMenuItem rel = new JCheckBoxMenuItem( "relative positioning" );
            rel.setSelected( p.rel );
//            popup.add(rel);

            rel.addActionListener( new ActionListener()
            {

                @Override
                public void actionPerformed( ActionEvent e )
                {
                    p.rel = rel.isSelected();
                    CampSkeleton.instance.somethingChanged();
                }
            } );

            popup.add( rel );

            popup.show( this, evt.getX(), evt.getY() );

//            Point pt = evt.getPoint();
//            pt = SwingUtilities.convertPoint(this, pt, null);
//
//            final Popup pop = PopupFactory.getSharedInstance().getPopup(CampSkeleton.instance, popup, pt.x +
//                    CampSkeleton.instance.getX(), pt.y +
//                    CampSkeleton.instance.getY());
//            pop.show();
        }
    }

    public void painting( Point2d location, Point2d offset, MouseEvent evt )
    {
        if ( location == null )
            return; // mousereleased
        CampSkeleton cs = CampSkeleton.instance;

        if ( cs == null )
            return;

        LContext<Bar> loc = getNearest( location, 10 );

        if ( loc == null )
            return;

        switch ( cs.mode )
        {
            case ProfilePaint:
                if ( cs.profileUI != null && cs.profileUI.profile != null )
                    if ( loc != null )
                        cs.assignProfileToBar( loc.get(), cs.profileUI.profile );
                break;
            case Tag:
//                if (cs.selectedTag == null)
//                {
//                    JOptionPane.showMessageDialog( this, "Please select a feature to tag with" );
//                    return;
//                }
//                if ( !evt.isControlDown() )
//                    loc.get().tags.add( cs.selectedTag );
//                else if ( !evt.isShiftDown() )
//                    loc.get().tags.remove( cs.selectedTag );
//                break;
        }
    }

    public void drawTags( Graphics2D g2, Bar bar, Plan plan )
    {
        if ( !paintTags )
            return;
        int i = 0;
        for ( Tag f : bar.tags )
        {
//            if (CampSkeleton.instance != null && CampSkeleton.instance.selectedTag != f)
//                continue;

            g2.setColor( f.color );

            AffineTransform oldTrans = g2.getTransform();
            Point target = ma.to( bar.end );
            g2.translate( target.x, target.y );
            g2.rotate( Math.atan2( bar.end.y - bar.start.y, bar.end.x - bar.start.x ) - Math.PI / 2 );
            g2.translate( 0, -i * 20 - 15 );
            g2.setColor( f.color );
            g2.fillPolygon( unitTriangle );
            g2.setTransform( oldTrans );
            i++;
        }
    }
    static Polygon unitTriangle = new Polygon();

    static
    {
        unitTriangle.addPoint( 0, 10 );
        unitTriangle.addPoint( 0, -10 );
        unitTriangle.addPoint( 10, 0 );
    }

    public void drawMarkers( Graphics2D g2 )
    {
        if ( !paintMarkers )
            return;

        for ( Point2d mark_ : handles )
            if ( mark_ instanceof Marker )
            {
                Marker mark = (Marker) mark_;

//                if (CampSkeleton.instance != null && CampSkeleton.instance.selectedFeature != mark.feature)
//                    continue;

                boolean highlit = false;
                for ( Anchor ac : CampSkeleton.instance.highlitAnchors )
                    highlit |= mark.generator.equals( ac.getProfileGen() ) || mark.generator.equals( ac.getPlanGen() );

                boolean selected = false;
                if ( CampSkeleton.instance.selectedAnchor != null )
                    selected = mark.generator.equals( CampSkeleton.instance.selectedAnchor.getProfileGen() ) || mark.generator.equals( CampSkeleton.instance.selectedAnchor.getPlanGen() );



                Color mc = highlit ? Color.yellow.brighter().brighter() : selected ? Color.yellow : Color.green;

                g2.setColor( new Color( mc.getRed(), mc.getGreen(), mc.getBlue(), selected ? 255 : 190 ) );
                int r = 6;
                g2.fillOval( ma.toX( mark.x ) - r, ma.toY( mark.y ) - r, r * 2, r * 2 );

                if ( selected )
                {
                    g2.setColor( mc.darker() );
                    g2.drawOval( ma.toX( mark.x ) - r, ma.toY( mark.y ) - r, r * 2, r * 2 );
                }
            }
    }

    public void drawSpecialMarker( Graphics2D g2, Point2d start )
    {
        int r = 10;
        g2.setColor( Colour.sky.darker() );
        g2.drawOval( ma.toX( start.x ) - r, ma.toY( start.y ) - r, r * 2, r * 2 );
    }

    void showMarkers( boolean b )
    {
        paintMarkers = b;
        repaint();
    }

    void showTags( boolean b )
    {
        paintTags = b;
        repaint();
    }
}
