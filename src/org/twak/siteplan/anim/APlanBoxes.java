package org.twak.siteplan.anim;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import javax.vecmath.Point2d;

import org.twak.camp.ui.Bar;
import org.twak.siteplan.campskeleton.*;
import org.twak.utils.Cache2;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;

/**
 *
 * @author twak
 */
public class APlanBoxes extends Plan
{

    public APlanBoxes()
    {
//        buildFromPlan = new BuildFromPlan(this);
    }

    // tolerance for removing coincident points
    final static double TOL = 0.001;

    @Override
    public void update( int frame, int delta )
    {
        super.update( frame, delta );

        Rectangle r1 = new Rectangle( 10, 10, 200, 200 );
        Rectangle r2 = new Rectangle( -50 + frame * 5, 50, 50, 100 );

        Area a = new Area( r1 );
//        a.transform( AffineTransform.getRotateInstance( 43 ));
        a.subtract( new Area( r2 ) );

        Cache2<Double, Double, Point2d> pCache = new Cache2<Double, Double, Point2d>()
        {

            @Override
            public Point2d create( Double i1, Double i2 )
            {
                return new Point2d( i1, i2 );
            }
        };


        // any for now
        Profile p = new Profile( 100 );//findProfiles().iterator().next();

        p.points.get( 0 ).start.get().end.x += 20;

        addLoop( p.points.get( 0 ), root, p );

        profiles.clear();

        points = new LoopL();
        Loop<Bar> loop = null;

        PathIterator pit = a.getPathIterator( null );

        Point2d start = null, last = null;

        while ( !pit.isDone() )
        {
            double[] coords = new double[6];
            switch ( pit.currentSegment( coords ) )
            {
                case PathIterator.SEG_LINETO:
                {
                    Bar b;

                    Point2d pt = new Point2d (coords[0], coords[1]);
                    if ( pt.distance( last ) > TOL )
                    {
                        loop.append( b = new Bar( pCache.get( pt.x, pt.y ), pCache.get( last.x, last.y ) ) );
                        profiles.put( b, p );
                        last = pt;
                    }
                }
                break;
                case PathIterator.SEG_MOVETO:
                    loop = new Loop();
                    start = last = new Point2d( coords[0], coords[1] );
                    points.add( loop );
                    break;
                case PathIterator.SEG_CLOSE:
                {
                    Bar b;

                    loop.append( b = new Bar( pCache.get( start.x, start.y ), pCache.get( last.x, last.y ) ) );
                    profiles.put( b, p );
                    loop.append( b );
                }
                break;
                default:
                    break;
            }
            pit.next();
        }
        points.reverseEachLoop();
    }
}
