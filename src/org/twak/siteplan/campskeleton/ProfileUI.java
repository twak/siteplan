package org.twak.siteplan.campskeleton;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.vecmath.Point2d;
import javax.vecmath.Tuple2d;

import org.twak.camp.ui.Bar;
import org.twak.camp.ui.Marker;
import org.twak.siteplan.campskeleton.Profile.GlobalProfile;
import org.twak.utils.LContext;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;
import org.twak.utils.ui.Colourz;

/**
 * @author twak
 */
public class ProfileUI extends MarkerUI
{
    public Profile profile;
    private Plan plan;
    
    // x location of global vertical handles
    private static int handleOffset = -150;
    // cache fo the loop representing the central/main loop
    private Loop<Bar> rootLoop;

    Map <Loop<Bar>,GlobalValency> loopGV  = new HashMap();

    public static class GlobalValency
    {
        Global global;
        int valency;
        public GlobalValency (Global global, int valency)
        {
            this.global = global;
            this.valency = valency;
        }
    }

    public ProfileUI( Profile profile, Plan plan, BarSelected profileEdgeSelected )
    {
        super (profileEdgeSelected);
        this.profile = profile;
        this.plan = plan;
        setBackground( Colourz.sky );
        setup();
        ma.setZoom(3);
    }

    @Override
    protected void createInitial()
    {
        edges = profile.points;
        handles.clear();

        loopGV.clear();

        for (Global g : plan.globals)
        {
            double height = 0;
            List<Loop<Bar>> valencies = profile.globalProfiles.get(g).chainStarts;
            
            for ( int i = 0; i < valencies.size(); i++ )
            {
                Loop<Bar> loop = valencies.get(i);

                loopGV.put(loop, new GlobalValency(g, i));

                if ( g == plan.root )
                    rootLoop = loop;

                // should be the same for all within a global
                height = loop.getFirst().start.y;

                for ( Bar bar : loop )
                    refreshMarkersOn( bar );

            }
            
            // widgets for all handles, except root
            if ( g != plan.root )
            {
                Point2d handle = new GlobalHandle( handleOffset, height, g );
                handles.add( handle );
            }
        }
    }

    public class GlobalHandle extends Point2d
    {
        Global global;

        public GlobalHandle ( double x, double y, Global g)
        {
            super( x, y );
            this.global = g;
        }
    }

    public void flushtoProfile()
    {
        for ( Global g : plan.globals )
        {
            for ( Loop<Bar> l : profile.globalProfiles.get(g).chainStarts )
            {
                // set in all other profiles attached to this plan
                g.assertHeight( plan, l.getFirst().start.y );
            }
        }
    }

    @Override
    public void createSection (Point loc, boolean inside)
    {
//        flushtoProfile();
//
//        // create a new global
//        Global g = new Global();
//        g.valency = 2;
//        g.name = ""+plan.globals.size();
//        g.add( plan, new Point2d( loc.x, loc.y ) );
//
//        enforceMonotonic();
//
//        createInitial();
//        repaint();
    }

    @Override
    public boolean doSnap()
    {
        return true;
    }

    @Override
    public void moveLoop( Loop<Bar> draggedLoop, Tuple2d offset ) {
        return; // no thx!
    }

    @Override
    public void showMenu( final LContext<Bar> dragged, final MouseEvent evt )
    {
        if (dragged == null) {
            JPopupMenu popup = new JPopupMenu();

            JMenuItem edgeProfile = new JMenuItem("create offset section");
            popup.add(edgeProfile);

            edgeProfile.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    flushtoProfile();

                    // create a new global
                    Global g = new Global();
                    g.valency = 2;
                    g.name = "" + plan.globals.size();

                    Point2d loc = ma.from(evt);

                    g.add(plan, new Point2d(loc.x, loc.y));

                    enforceMonotonic();

                    createInitial();
                    repaint();
                }
            });
            popup.show(this, evt.getX(), evt.getY());
        }
        else if(dragged.hook instanceof GlobalHandle)
         {
             final GlobalHandle ph = (GlobalHandle)dragged.hook;

             if (evt.getClickCount() == 2) // enable/disable global
             {
                 JPopupMenu popup = new JPopupMenu();

                 final JCheckBoxMenuItem enabled = new JCheckBoxMenuItem("enabled");
                 popup.add(enabled);
                 enabled.setSelected(profile.globalProfiles.get(ph.global).enabled);

                 enabled.addActionListener(new ActionListener()
                 {
                     @Override
                     public void actionPerformed(ActionEvent e)
                     {
                         profile.globalProfiles.get(ph.global).flipEnabled();
                         Siteplan.instance.somethingChanged();
                         ProfileUI.this.repaint();
                     }
                 });

                 JMenuItem edgeProfile = new JMenuItem("edit edge profile");
                 popup.add(edgeProfile);

                 edgeProfile.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        if (ph.global.edgeProfile == null) // "backwards compatibility"
                            ph.global.edgeProfile = Siteplan.instance.plan.createNewProfile(null);

                        Siteplan.instance.setProfile(ph.global.edgeProfile);
                    }
                });

//                 JMenuItem delete = new JMenuItem( "delete" );
//                 popup.add( delete );
//
//                 delete.addActionListener( new ActionListener()
//                 {
//                     @Override
//                     public void actionPerformed( ActionEvent e )
//                     {
//                        remove( dragged, ma.from( evt ) );
//                     }
//                 });

                 popup.show(this, evt.getX(), evt.getY());
             }
         }
    }

    @Override
    public void movePoint( LContext<Bar> ctx, Point2d pt,javax.vecmath.Point2d location,MouseEvent evt)
    {
        if ( pt instanceof Marker )
        {
                        // only when in the correct mode!
            if (Siteplan.instance.mode != Tool.Features )
                return;

            if (pressed == null) // when we start moving
                pressed = evt.getPoint();

            Marker m = (Marker) pt;

//            m.bar.mould.remove( m );

            m.set(location);
//            m.x = location.x; // coordinates to project
//            m.y = location.y;

             LContext<Bar> bar = positionMarker( m, m.bar ); // projects down
            m.y = Math.min( m.y, -0.01 ); // height insertions have to be insertions ;) natural steps at height 0 don't work

            // update the thing being dragged, so we have the correct line next time
            ctx.loop = bar.loop;
            ctx.loopable = bar.loopable;
        }
        else if ( pt instanceof GlobalHandle)
        {
            GlobalHandle ph = (GlobalHandle)pt;

//            if (evt.getClickCount() == 2) // enable/disable global
//            {
//                profile.globalProfiles.get(ph.global).flipEnabled();
//            }
//            else // just drag
//            {
                ph.y = location.y;
                ph.global.assertHeight(plan, location.y);

                for (Loop<Bar> loop : profile.globalProfiles.get(ph.global).chainStarts) {
                    for (Bar b : loop) {
                        refreshMarkersOn(b);
                    }
//                }
            }
        }
        else
        {
            boolean fixX = false, fixY = false;

            // start of chains can only be moved horizontally using a handle
            if ( ctx.loop.start == ctx.loopable && pt == ctx.get().start )
            {
                fixY = true;


                GlobalValency gv = loopGV.get(ctx.loop);
                if (gv.global.valency > 1) // not root
                {
                    // the other loop<bar> in this global
                    Point2d other = profile.globalProfiles.get(gv.global).chainStarts.get(gv.valency == 0 ? 1 : 0).start.get().start;

                    // clamp position
                    if (other.x > pt.x)
                        location.set(Math.min(other.x - 0.001, location.x), location.y);
                    else
                        location.set(Math.max(other.x + 0.001, location.x), location.y);
                }
            }

            // don't move origin point
            if ( pt == rootLoop.getFirst().start )
                fixX = fixY = true;

            double dx = pt.x - (fixX ? pt.x : location.x);
            double dy = pt.y - (fixY ? pt.y : location.y);

            if ( ! ( fixY && fixX) && ( evt.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK ) != 0 )
            {
//                System.out.println (">>>");
                Loopable<Bar> current = ctx.loopable;
                do
                {
                    current.get().end.x -= dx;
                    current.get().end.y -= dy;
                    current = current.getNext();
                }
                while ( current != ctx.loop.start );
            }
            //   avoid rounding errors
            pt.x -= pt.x - ( fixX ? pt.x : location.x );
            pt.y -= pt.y - ( fixY ? pt.y : location.y );

            enforceMonotonic();

            for ( Bar b : ctx.loop )
                refreshMarkersOn(b);
        }
    }

    private void enforceMonotonic()
    {
        for (Loop<Bar> loop : edges)
        {
            double y = Double.MAX_VALUE;
            for (Bar b : loop)
            {
                for (Point2d p : Arrays.asList( b.start, b.end))
                {
                    if ( p.y > y )
                        p.set( p.x, y );
                    else
                        y = p.y;
                }
            }
        }
    }

    @Override
    public void releasePoint(Point2d pt, LContext<Bar> ctx, MouseEvent evt) {
//        if (pressed == null) {
//            return; // if movePoint doesn't set pressed, we don't want the menu
//        }
        // show a menu if they didn't really move the marker
        if (pt instanceof Marker) {
//            if (evt.getPoint().distanceSq(pressed) < 25) // radius of 5
            {
                Siteplan.instance.setAnchorProfile((Marker) pt);
                repaint();
            }
        }
//        pressed = null;
    }

    @Override
    protected boolean allowDrag(Loop<Bar> loop)
    {
        return false;
    }

    @Override
    protected boolean allowRemove( LContext<Bar> ctx, Point2d corner )
    {
        if (super.allowRemove( ctx, corner ))
            return true;
        
        if (corner instanceof GlobalHandle)
        {
            GlobalHandle he = (GlobalHandle)corner;
            he.global.remove(plan);
            createInitial();
            repaint();
            return true;
        }

        if (ctx.loop.count() == 1)
            return false;

        if (ctx.loop.start.get().end == corner) {

            ctx.loop.start.get().end.set( ctx.loop.start.getNext().get().end );
            ctx.loop.remove( ctx.loop.start.getNext() );

            return false;
        }

        if (ctx.loop.start.get().start == corner || ctx.loop.start.get().end == corner ) // can't remove first bar
        {
            return false;
        }
        
        if ( ctx.loop.count() <= 1 )
            return false;

        return true;
        // remove global...?
    }
    @Override
    public void paintPointEditor( Graphics2D g2 )
    {
        g2.setColor( PlanUI.grass );
        g2.fillRect( 0, ma.toY(0), getWidth(), getHeight());
        g2.setColor( PlanUI.grass.darker() );
        g2.setStroke( new BasicStroke( 3 ) );
        g2.drawLine( 0, ma.toY( 0 ), getWidth(), ma.toY( 0 ) );

        g2.setStroke( new BasicStroke( 1 ) );
        g2.setColor( Colourz.sky.darker() );
        g2.drawLine( 0,ma.toY( 0), getWidth(), ma.toY(0 ) );

        for ( Loop<Bar> e2 : edges )
        {
            g2.setColor( Colourz.sky.darker() );
            Point start = ma.to( e2.start.get().start );
            g2.drawLine( start.x, start.y, 0, start.y );
        }

        for (Bar b : edges.eIterator())
            drawTags( g2, b, plan );

        g2.setStroke( new BasicStroke( 3 ) );
        g2.setColor( Color.black );
        for ( Loop<Bar> e2 : edges )
            for ( Bar bar : e2 )
                g2.drawLine(
                        ma.toX( bar.start.x ),
                        ma.toY ( bar.start.y ),
                        ma.toX(bar.end.x ),
                        ma.toY( bar.end.y ) );

        for ( Global g : plan.globals )
            for ( Loop<Bar> loop : profile.globalProfiles.get(g).chainStarts )
            {
                g2.setColor( Color.cyan );

                Point start = ma.to ( loop.start.get().start );

                drawPixel( g2, loop.start.get().start );
                g2.setStroke( new BasicStroke( 1 ) );

                int r = 10;
                g2.setColor( profile.globalProfiles.get(g).enabled ? Color.white : Color.gray );
                g2.fillOval( (int) start.x - r, (int) start.y - r, r * 2, r * 2 );
                g2.setColor( Color.black );
                g2.drawOval( (int) start.x - r, (int) start.y - r, r * 2, r * 2 );


                g2.drawString( g.name, (float) start.x - g2.getFontMetrics().stringWidth( g.name ) / 2, (float) start.y + 5 );

                g2.setStroke( new BasicStroke( 3 ) );


                g2.setColor( Color.orange );

                for ( Bar b : loop )
                    if ( loop.getFirst() != b )
                        drawPixel( g2, b.start );

                AffineTransform oldTrans = g2.getTransform();
                Bar last = loop.start.getPrev().get();
                Point target = ma.to( last.end );
                g2.translate( target.x, target.y );
                g2.rotate( Math.atan2( last.end.y - last.start.y, last.end.x - last.start.x ) - Math.PI / 2 );
                g2.setColor( Color.black );
                g2.fillPolygon( unitArrow );
                g2.setTransform( oldTrans );
            }


        if (false)
        {
            // debug infromation for where we take offsets from.
            profile.clearCache(); // won't hurt ;) not fast :(
            
            g2.setStroke( new BasicStroke( 2f ) );
            
            for (Global g : plan.globals)
            {
                GlobalProfile gp = profile.globalProfiles.get( g );
                for (int v = 0; v < g.valency; v++)
                {
                    Profile.ProfileChainInfo info = gp.getChainInfoForValency(v);
                    g2.setColor( info.into ? Color.red : Color.blue );
                    Point2d globalStart = gp.chainStarts.get( v ).getFirst().start;
                    double offset = v*3;
                    drawLine( g2, globalStart.x, globalStart.y+offset, globalStart.x+info.distance, globalStart.y+offset );
                }
            }
        }


        drawMarkers( g2 );
        for ( Point2d start : handles )
            if ( start instanceof GlobalHandle )
            {
                Global global = ( (GlobalHandle) start ).global;
                g2.setStroke( new BasicStroke( 1 ) );

                drawSpecialMarker( g2, start );

                g2.drawString( global.name, (float) ma.toX(  start.x ) - 3, (float) ma.toY( start.y ) + 5 );

                g2.setStroke( new BasicStroke( 3 ) );
            }
        if (Siteplan.instance != null)
         Siteplan.instance.somethingChanged();
    }

    static Polygon unitArrow = new Polygon();
    static
    {
        unitArrow.addPoint( 0, 5);
        unitArrow.addPoint( -10, -5);
        unitArrow.addPoint( 10, -5);
    }

    @Override
    public boolean containsLoop( LoopL<Bar> loop, Point2d ept )
    {
        return false;
    }


//    private class GlobalLoop
//    {
//        Global global; Loop loop;
//
//        public GlobalLoop (Global global, Loop loop)
//        {
//            this.global = global;
//            this.loop = loop;
//        }
//
//        @Override
//        public boolean equals( Object obj )
//        {
//            GlobalLoop gp = (GlobalLoop)obj;
////            return global == gp.global  && loop == gp.global;
//            return global.equals( gp.global ) && loop.equals( gp.loop );
//        }
//
//        @Override
//        public int hashCode()
//        {
//            int hash = 5;
//            hash = 59 * hash + ( this.global != null ? this.global.hashCode() : 0 );
//            hash = 59 * hash + ( this.loop != null ? this.loop.hashCode() : 0 );
//            return hash;
//        }
//
//    }
}
