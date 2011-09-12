package camp.junk.wiggle;

import campskeleton.CampSkeleton;
import campskeleton.Plan;
import campskeleton.Profile;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.vecmath.Point2d;
import straightskeleton.Edge;
import straightskeleton.Machine;
import straightskeleton.offset.OffsetSkeleton;
import straightskeleton.ui.Bar;
import straightskeleton.ui.PointEditor;
import utils.Cache;
import utils.ConsecutivePairs;
import utils.LContext;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;
import utils.Pair;
import utils.ReverseList;

/**
 *
 * @author twak
 */
public class StanfordWindows extends PointEditor{

    CampSkeleton camp;
    Profile defaultProf, defaultProf2;

    public StanfordWindows()
    {
        camp = new CampSkeleton();
        camp.setVisible( true );
        
        JFrame frame = new JFrame();
        frame.setContentPane( this );
        frame.setSize(800,800);
        frame.setVisible( true );
        setup();



         defaultProf = new Profile (100);
        defaultProf.points.get( 0 ).start.get().end.x += 50;

        defaultProf2 = new Profile (100);
        defaultProf2.points.get( 0 ).start.get().end.x += 20;
    }

    @Override
    public void paintPointEditor( Graphics2D g2 )
    {
//
//        g2.setStroke( new BasicStroke (3f));
//
////        g2.setColor( Color.red );
////        for ( Loop<Bar> loop : edges )
////        {
////            for ( Bar bar : loop )
////                g2.drawLine(
////                        ma.toX( bar.start.x ),
////                        ma.toY( bar.start.y ),
////                        ma.toX( bar.end.x ),
////                        ma.toY( bar.end.y ) );
////            g2.setColor( Color.orange );
////        }
//
//        Machine machine = new Machine();
//
//        LoopL<Bar> sym = new LoopL();
//
//        Cache<Point2d, Point2d> cache = new Cache<Point2d, Point2d>()
//        {
//            @Override
//            public Point2d create( Point2d i )
//            {
//                return new Point2d(i);
//            }
//        };
//
//        for (Loop<Bar> loop : edges)
//        {
//            if (edges.indexOf( loop ) == 0)
//            {
//                Loop<Bar> newLoop = Bar.clone( loop, null );
//
//                for (Bar b : newLoop)
//                {
//                    cache.put (b.start, b.start);
//                    cache.put (b.end, b.end);
//                }
//
//                Loopable last = loop.start.getPrev();
//
//                List<Bar> toAdd = new ArrayList();
//
//                Bar l = null;
//                for ( Bar b : newLoop )
//                {
//                    toAdd.add( l= new Bar( new Point2d( -b.end.x, b.end.y ), new Point2d( -b.start.x, b.start.y ) ) );
//                }
//
//                toAdd.add ( new Bar ( newLoop.start.getPrev().get().end, toAdd.get( toAdd.size()-1 ).start));
//                toAdd.add( 0, new Bar (toAdd.get(0).end, newLoop.getFirst().start));
//
//                for (Bar b : new ReverseList<Bar>( toAdd ))
//                {
//                    newLoop.append( b );
//                }
//                sym.add( newLoop );
//
//            }
//            else
//            {
//                AffineTransform af = new AffineTransform();
//                af.setToScale( -1, 1);
//                Loop<Bar> flipped = Bar.clone( loop, af );
//                flipped.reverse();
//
//                for ( Bar e : flipped )
//                {
//                    Point2d tmp = e.start;
//                    e.start = e.end;
//                    e.end = tmp;
//                }
//
//                sym.add( flipped );
//                af.setToScale( 1, 1);
//                sym.add( Bar.clone( loop, af ) );
//            }
//        }
//
//        g2.setColor( Color.green );
//        for ( Loop<Bar> loop : sym )
//        {
//            for ( Bar bar : loop )
//                g2.drawLine(
//                        ma.toX( bar.start.x ),
//                        ma.toY( bar.start.y ),
//                        ma.toX( bar.end.x ),
//                        ma.toY( bar.end.y ) );
//            g2.setColor( Color.orange );
//        }
//
//        LoopL <Edge> out = Edge.fromBar( sym );
//
//
//        for (Edge e : out.eIterator())
//            e.machine = machine;
//
//                // create the offset from the skinny centrelines to the boundary of all the hosues
//        OffsetSkeleton offsetSkel = new OffsetSkeleton( out, 50, new OffsetSkeleton.OffsetEdgeReport() {
//            public void report( Edge input, Edge output, int valency )
//            {
//            }
//        } );
//
//        offsetSkel.registerProfile( machine, -Math.PI/4, 0 );
//
//        List<LoopL<Edge>> oList = offsetSkel.getResults();
//        if (oList.size() == 0)
//            return;
//
//        LoopL<Edge> outside = oList.get(0);
//        for (Edge e : outside.eIterator())
//            g2.drawLine( ma.toX( e.start.x), ma.toY( e.start.y), ma.toX( e.end.x), ma.toY( e.end.y));
//
//        Plan plan = new Plan ();
//
//        plan.addLoop( defaultProf2.points.get( 0 ), plan.root, defaultProf2 );
//
//        plan.addLoop( defaultProf.points.get( 0 ), plan.root, defaultProf );
//
//
//
//        LoopL<Bar> inside = Bar.dupe( sym );
//        for (Bar b : inside.eIterator())
//                plan.profiles.put( b, defaultProf );
//
//        LoopL<Bar> outsideB = Bar.fromEdges(outside);
//        for ( Bar b : outsideB.eIterator() )
//            plan.profiles.put( b, defaultProf2 );
//
//
//        Bar.reverse(inside);
//
//        inside.addAll( outsideB );
//        plan.points = inside;
//
//
//
//        camp.loadPlan( plan );

    }

    @Override
    public void createSection( Point loc , boolean inside)
    {
        createCircularPoints(4, loc.x, loc.y, 50 );
//        createCircularPoints( 4, -loc.x, loc.y, 50 );
        repaint();
    }

    @Override
    public void movePoint( LContext<Bar> ctx2, Point2d pt, Point2d location, MouseEvent evt )
    {


//        LContext<Bar> dragged = null;
//        Iterator<LContext<Bar>> bit = edges.getCIterator();
//        while ( bit.hasNext() )
//        {
//            LContext<Bar> ctx = bit.next();
//
//            List<Point2d> pts = new ArrayList( Arrays.asList( new Point2d[]
//                    {
//                        ctx.get().start, ctx.get().end
//                    } ) );
//            pts.addAll( ctx.get().markers );
//
//            for ( Point2d point : pts )
//            {
//                // to screenspace!
//                double dist = point.distance( new Point2d ( -location.x, location.y ) );
//                if ( dist < 10 )
//                {
//                    if ( dragged != null )
//                        if ( dist > ( (Point2d) dragged.hook ).distance( new Point2d ( -location.x, location.y ) ) )
//                            continue;
//                    dragged = ctx;
//                    dragged.hook = point;
//                }
//            }
//        }

        super.movePoint( ctx2, pt, location, evt );
//        if (dragged != null)
//                super.movePoint( dragged, (Point2d) dragged.hook,new Point2d ( -location.x, location.y ), evt );
    }

    @Override
    protected void createInitial()
    {
                Loop<Bar> loop = new Loop();
        edges.add( loop );

        for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( Arrays.asList(
            new Point2d (100,-100),
            new Point2d (150,-50),
            new Point2d (100, 0)
                ), false ))
        {
            loop.append( new Bar( pair.first(), pair.second() ) );
        }
    }

    public static void main (String[] args)
    {
                try
        {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
        catch ( InstantiationException ex )
        {
            Logger.getLogger( CampSkeleton.class.getName() ).log( Level.SEVERE, null, ex );
        }
        catch ( IllegalAccessException ex )
        {
            Logger.getLogger( CampSkeleton.class.getName() ).log( Level.SEVERE, null, ex );
        }
        catch ( UnsupportedLookAndFeelException ex )
        {
            Logger.getLogger( CampSkeleton.class.getName() ).log( Level.SEVERE, null, ex );
        }
        catch ( ClassNotFoundException e )
        {
        }

                
        new StanfordWindows();
    }
}
