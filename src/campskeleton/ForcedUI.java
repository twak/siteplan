package campskeleton;

import camp.junk.ForcedStep;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.vecmath.Point2d;
import straightskeleton.ui.Bar;
import straightskeleton.ui.PointEditor;
import org.twak.utils.LContext;
import org.twak.utils.Loop;

/**
 * UI for an-almost-plan. A step edge adjustment. First and last points are fixed.
 * @author twak
 */
public class ForcedUI extends PlanUI
{
    ForcedStep step;

    public ForcedUI( ForcedStep forcedStep, PointEditor.BarSelected es )
    {
        super (CampSkeleton.instance.plan, forcedStep.shape , es);
        this.step = forcedStep;
    }

    @Override
    public void movePoint( LContext<Bar> ctx, Point2d pt,javax.vecmath.Point2d location,MouseEvent evt)
    {
        if ( isFixedPoint( ctx, pt ))
            return; // can't move start or end
        
        super.movePoint( ctx, pt, location, null);
    }

    @Override
    protected boolean allowRemove( LContext<Bar> ctx, Point2d corner )
    {
        if ( ctx.hook == null ) // it's an edge!
            return false;

        if ( isFixedPoint( ctx, corner ))
            return false; // don't remove the first or last points

        return super.allowRemove( ctx, corner );
    }

    private boolean isFixedPoint( LContext<Bar> ctx, Point2d corner)
    {
        return ctx.loop == step.shape.get( 0) && (ctx.loop.start.get().start == corner || ctx.loop.start.getPrev().get().end == corner);
    }

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
                if ( !firstLoop || e != e2.getFirst() )
                    drawPixel( g2, e.start );
            firstLoop = false;
        }

        drawMarkers( g2 );

        Loop<Bar> first = step.shape.get( 0 );
        drawSpecialMarker( g2, first.start.get().start );
        drawSpecialMarker( g2, first.start.getPrev().get().end );

        somethingChanged( null );
    }
}
