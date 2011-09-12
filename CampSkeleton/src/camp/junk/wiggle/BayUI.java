package camp.junk.wiggle;

import campskeleton.CampSkeleton;
import campskeleton.PlanUI;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import javax.vecmath.Point2d;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import straightskeleton.ui.PointEditor;
import utils.LContext;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;

/**
 *
 * @author twak
 */
public class BayUI extends PlanUI
{
    NaturalFeature step;

    public BayUI( NaturalFeature step, PointEditor.BarSelected es )
    {
        super (CampSkeleton.instance.plan, step.shape , es);
        this.step = step;
        edges = new LoopL();
        createInitial();
    }

    @Override
    protected void createInitial()
    {
        super.createInitial();
        int i = 0;
        updatePositions();
    }

    @Override
    public void movePoint( LContext<Bar> ctx, Point2d pt,javax.vecmath.Point2d location,MouseEvent evt)
    {
        if (pt instanceof Marker)
            super.movePoint( ctx, pt, location, evt );
        else
        {
            step.radius = new Point2d( 0, 0 ).distance( location );
            updatePositions();
        }
    }


    private void updatePositions()
    {
        if (step == null)
            return;
        double sideLength = step.radius *Math.cos( Math.PI / 8 );
        int sides = 5;
        double angleDelta = Math.PI/4, angle = -Math.PI/8;

        Loopable<Bar> bar = edges.get( 0 ).start;

        Point2d last = new Point2d (step.radius * Math.cos (angle), -step.radius * Math.sin( angle ));
        for (int i = 0; i < 5; i++)
        {
            angle += angleDelta;
            Point2d next = new Point2d (step.radius * Math.cos (angle), -step.radius * Math.sin( angle ));

            bar.get().start.set( last );
            bar.get().end.set( next );

            last = next;
            bar = bar.getNext();
        }
        for ( Bar b : edges.eIterator() )
            refreshMarkersOn(b);
    }


    @Override
    protected boolean allowRemove( LContext<Bar> ctx, Point2d corner )
    {
        if ( ctx.hook == null ) // it's an edge!
            return false;

        if (corner instanceof Marker)
        {
            Marker m = (Marker) corner;

//            m.bar.markers.remove( (Marker) corner );
            return true;
        }
        // all othe rpoints must remain!
        return false;
    }

//    private boolean isFixedPoint( LContext<Bar> ctx, Point2d corner)
//    {
//        return ctx.loop == step.shape.get( 0) && (ctx.loop.start.get().start == corner || ctx.loop.start.getPrev().get().end == corner);
//    }

    @Override
    public void paintPointEditor( Graphics2D g2 )
    {
        for ( Loop<Bar> e2 : edges )
            for ( Bar e : e2 )
            {
                g2.setStroke( new BasicStroke( currentBar == null ? 2f : currentBar.get() == e ? 4f : 2f ) );
                g2.setColor( plan.profiles.get( e ).color );
                g2.drawLine( ma.toX( e.start.x ), ma.toY ( e.start.y ), ma.toX(  e.end.x ), ma.toY( e.end.y ) );
            }

        g2.setColor( grass.darker().darker() );

        // first loop is inserted into existing loop, everything else comes outside
        boolean firstLoop = true;
        for ( Loop<Bar> e2 : edges )
        {
            for ( Bar e : e2 )
                    drawPixel( g2, e.start );

            drawPixel( g2, e2.start.getPrev().get().end );
            firstLoop = false;
        }


        for (Bar b : edges.eIterator())
            drawTags( g2, b, plan );

        drawMarkers( g2 );

        
        Loop<Bar> first = step.shape.get( 0 );
        drawSpecialMarker( g2, first.start.get().start );
        drawSpecialMarker( g2, first.start.getPrev().get().end );

        somethingChanged( null );
    }
}
