package campskeleton;

import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.twak.utils.Line;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Loopable;

import straightskeleton.ui.Bar;
import straightskeleton.ui.PointEditor;

/**
 *
 * @author twak
 */
public class TileDebugger extends PointEditor
{
    LoopL<Line> flat;
    public TileDebugger (LoopL<Line> flat)
    {
        this.flat = flat;
    }
    
    @Override
    protected void createInitial()
    {
        for (Loop<Line> loop : flat)
        {
            Loop<Bar> loopB = new Loop();
            edges.add( loopB );
            for (Loopable<Line> l : loop.loopableIterator())
            {
                loopB.append( new Bar (l.get().start, l.getNext().get().start));
//                System.out.println ("[][] "+l);
            }
        }
    }

    @Override
    public void paintPointEditor( Graphics2D g2 )
    {
//        // routine should be rewritten to handle loopable's all the way through...
//        g2.setColor( Color.orange );
//
//        LoopL<Line> fromEdges = new LoopL();
//        for ( Loop<Bar> pll : edges )
//        {
//            Loop<Line> loop = new Loop();
//            fromEdges.add( loop );
//            for ( Loopable<Bar> pp : pll.loopableIterator() )
//            {
//                Line l = new Line (pp.get().start, pp.getNext().get().start);
//                drawLine( g2, l );
//                loop.append( l );
//            }
//
//        }
//
//        final List<LoopL<Point2d>> out = new ArrayList();
//
//        final List<Point2d> other = new ArrayList();
//
//        final Map <LoopL<Point2d>, Integer> xes = new HashMap();
//        final Map <LoopL<Point2d>, Integer> yes = new HashMap();
//        
//        Tiler tiller = new Tiler( null, new HashSet(), null)
//        {
//            @Override
//            public void addTile( double x, double y, LoopL<Point2d> clip , int ix, int iy)
//            {
//                if (clip != null)
//                {
//                    out.add( clip );
//                    xes.put( clip, ix );
//                    yes.put( clip, iy );
//                }
//                else
//                    other.add(new Point2d(x,y));
//
//            }
//        };
////        tiller.findTiles( fromEdges );
//
//        int i = 0;
//        for (Point2d pt : other)
//        {
//            g2.setColor (Color.pink);
//            g2.fillRect( ma.toX( pt.x ), ma.toY(  pt.y ), ma.toZoom( 3 ) , ma.toZoom(3 ));
//        }
//
//        g2.setColor (Color.red);
//        for (Intersector.Collision c : tiller.collisions)
//        {
//            drawPixel( g2, c.location);
////            g2.drawString( c.location.toString() , ma.toX( c.location.x ), ma.toY(  c.location.y ));
//        }
//
//        g2.setColor( Color.gray );
//        for (Line l : tiller.allLines)
//        {
//            drawLine( g2, l );
//        }
//
//        for (LoopL<Point2d> ll : out)
//        {
//            for (Loop<Point2d> loop : ll)
//            {
//                Polygon pg = new Polygon();
//                boolean first = true;
//                for ( Point2d p : loop )
//                {
//                    if (first)
//                    {
//                        g2.setColor (Color.black);
//                        g2.drawString( xes.get( ll ) + "," + yes.get( ll ), ma.toX( p.x ), ma.toY( p.y ) );
//                        first = false;
//                    }
//                    pg.addPoint( ma.toX( p.x + Math.random() * 3 ), ma.toY( p.y + Math.random() * 3 ) );
//                }
//
//                if ( AngleAccumulator.sign (loop))
//                    g2.setColor( Colour.transparent( Rainbow.next( this ), 50 ) );
//                else
//                    g2.setColor (Color.black);
//
//                if ( pg.npoints >= 3 )
//                    g2.fillPolygon( pg );
//            }
//        }

    }

    static boolean open = false;
    public static void debug (LoopL<Line> flat)
    {
        if ( ! open)
        {
        JFrame frame = new JFrame ("argh");
        TileDebugger tc = new TileDebugger(flat) ;
        tc.setup();
        frame.setContentPane( tc );
        frame.setSize (800,800);
        frame.setVisible( true );
        open = true;
            frame.addWindowListener( new WindowAdapter()
            {
                @Override
                public void windowClosing( WindowEvent e )
                {
                    open = false;
                }
            } );
        }
    }
}
