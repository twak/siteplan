package org.twak.siteplan.campskeleton;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point2d;

import org.twak.camp.ui.Bar;
import org.twak.camp.ui.PointEditor;
import org.twak.siteplan.junk.NaturalStep;
import org.twak.utils.LContext;
import org.twak.utils.Loop;

/**
 * UI for a square protusion. A natural extrude that gets melded into the plan. First and last points are fixed.
 */
public class StepUI extends PlanUI
{
    NaturalStep step;

    double[] xes, yes;

    public StepUI( NaturalStep step, PointEditor.BarSelected es )
    {
        super (Siteplan.instance.plan, step.shape , es);
        this.step = step;
    }

    @Override
    protected void createInitial()
    {
        super.createInitial();
        int i = 0;

        xes = new double[4];
        yes = new double[2];

        List<Bar> lbar = new ArrayList();
        for (Bar b : edges.get( 0 ))
            lbar.add(b);

        // coordinates of step values
        xes[0] = lbar.get( 0 ).start.x;
        xes[1] = lbar.get( 1 ).start.x;
        xes[2] = lbar.get( 2 ).end.x;
        xes[3] = lbar.get( 4 ).end.x;

        yes[0] = lbar.get( 0 ).start.y;
        yes[1] = lbar.get( 2 ).start.y;
    }

    @Override
    public void movePoint( LContext<Bar> ctx, Point2d pt,javax.vecmath.Point2d location,MouseEvent evt)
    {
        List<Bar> lbar = new ArrayList();
        for ( Bar b : edges.get( 0 ) )
            lbar.add( b );

        // propogate change to other points
        if ( pt == lbar.get( 0 ).start )
            xes[0] = location.x;
        else if ( pt == lbar.get( 1 ).start )
            xes[1] = location.x;
        else if ( pt == lbar.get( 2 ).start )
        {
            xes[1] = location.x;
            yes[1] = location.y;
        }
        else if ( pt == lbar.get( 3 ).start )
        {
            xes[2] = location.x;
            yes[1] = location.y;
        }
        else if ( pt == lbar.get( 4 ).start )
        {
            xes[2] = location.x;
        }
        else if ( pt == lbar.get( 4 ).end )
        {
            xes[3] = location.x;
        }

        // ensure no overlaps
        double min = -Double.MAX_VALUE;
        for (int i = 0; i < xes.length; i++)
        {
            if (xes[i] < min)
            {
                xes[i] = min;
            }
            else
                min = xes[i]+5;
        }

        // push values back
        lbar.get(0).start.set (xes[0], yes[0]);
        lbar.get(1).start.set (xes[1], yes[0]);
        lbar.get(2).start.set (xes[1], yes[1]);
        lbar.get(3).start.set (xes[2], yes[1]);
        lbar.get(4).start.set (xes[2], yes[0]);
        lbar.get(4).end.set (xes[3], yes[0]);

        for ( Bar b : edges.eIterator() )
            refreshMarkersOn(b);
    }



    @Override
    protected boolean allowRemove( LContext<Bar> ctx, Point2d corner )
    {
        // all points shall remain!
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

        drawMarkers( g2 );
        for (Bar b : edges.eIterator())
            drawTags( g2, b, plan );

        Loop<Bar> first = step.shape.get( 0 );
        drawSpecialMarker( g2, first.start.get().start );
        drawSpecialMarker( g2, first.start.getPrev().get().end );

        somethingChanged( null );
    }
}
