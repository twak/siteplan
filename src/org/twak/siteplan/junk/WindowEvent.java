package org.twak.siteplan.junk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.twak.straightskeleton.Corner;
import org.twak.straightskeleton.Edge;
import org.twak.straightskeleton.EdgeCreator;
import org.twak.straightskeleton.HeightEvent;
import org.twak.straightskeleton.Machine;
import org.twak.straightskeleton.Skeleton;
import org.twak.straightskeleton.Tag;
import org.twak.straightskeleton.ui.DirectionHeightEvent;
import org.twak.straightskeleton.ui.MachineEvent;
import org.twak.utils.Line;
import org.twak.utils.LinearForm;

/**
 *
 * @author twak
 */
public class WindowEvent implements HeightEvent
{

    public List<MachineEvent> windowEvents = new ArrayList();
    static Machine upMachine;

    static
    {
        upMachine = new Machine();
        upMachine.getDirections().add( new DirectionHeightEvent( upMachine, 0 ) );
        upMachine.currentAngle = 0;
    }
    
    double height;
    Machine machine;

    public WindowEvent( Machine machine, double height )
    {
        /**
         * Untested since MachineEvent refactoring
         */
        this.machine = machine;
        this.height = height;
    }

    public double getHeight()
    {
        return height;
    }

    public boolean process( Skeleton skel )
    {
//        List<Edge> toChange = machine.findOurEdges( skel );
//        System.out.println( "creating windows at height " + height ); //skel.liveCorners
//
//        skel.replaceEdges( toChange, new EdgeCreator()
//        {
//
//            /**
//             * UNTESTED since switch from List<Edge> to List<Corner> return types
//             */
//            public List<Corner> getEdges( Edge old, Corner startH, Corner endH )
//            {
//                List<Edge> edges = new ArrayList();
//
//                Vector3d dir = new Vector3d( endH );
//                dir.sub( startH );
//
//                double l = dir.length();
//
//                dir.normalize();
//
//                LinearForm lf = new LinearForm( new Line( new Point2d( startH.x, startH.y ), new Point2d( endH.x, endH.y ) ) );
//                lf.perpendicular();
//                Vector2d pv2 = lf.unitVector();
//                Vector3d perp = new Vector3d( pv2.x, pv2.y, 0 );
//
//
//                if ( l > 100 ) // min length wall for a window
//                {
//                    double windowWidth = 50;
//                    double gapWidth = 30;
//
//                    int windows = (int) ( l / ( windowWidth + gapWidth ) );
//
//                    gapWidth = ( l / (double) windows ) - windowWidth;
//
//                    Vector3d gap = new Vector3d( dir );
//                    gap.scale( gapWidth );
//
//                    Vector3d halfGap = new Vector3d( dir );
//                    halfGap.scale( gapWidth / 2 );
//
//                    Vector3d glass = new Vector3d( dir );
//                    glass.scale( windowWidth );
//
//                    Vector3d step = new Vector3d( perp );
//                    step.scale( 20 ); // how much does a window stick out?
//
//                    Vector3d nStep = new Vector3d( perp );
//                    nStep.scale( -20 ); // how much does a window stick in?
//
//                    List<Corner> corners = new ArrayList<Corner>();
//                    Corner cc = startH;
//                    corners.add( cc );
//
//
//                    cc = new Corner( cc );
//                    cc.add( halfGap ); // added by first window...?
//
//                    for ( int i = 0; i < windows; i++ )
//                        for ( Vector3d v : Arrays.asList( new Vector3d[]
//                                {
//                                    step, glass, nStep, gap
//                                } ) )
//                        {
//                            corners.add( cc );
//                            cc = new Corner( cc );
//                            cc.add( v );
//                        }
//
//                    // above doesn't add last gap, so can now add a half
////                        cc = new Corner( cc );
////                        cc.add( halfGap ); // added by first window...?
//                    corners.add( endH );
//
//
////                        Machine upMachine = new Machine();
////                        upMachine.directions.add( new DirectionEvent( 0, 0 ) );
//
//                    startH.nextC = corners.get( 0 );
//                    endH.prevC = corners.get( corners.size() - 1 );
//
//                    cc = null;
//                    for ( Corner c : corners )
//                        if ( cc == null ) // first time around loop
//                            cc = c;
//                        else
//                        {
//                            cc.nextC = c;
//                            c.prevC = cc; // start and end already defined, so nothing to do
//                            Edge edge = new Edge( cc, c );
//                            edge.machine = machine;//upMachine;
//                            edge.setAngle( machine.currentAngle );
//                            edges.add( edge );
//                            cc = c;
//                        }
//
//                }
//                else // no windows, just same wall as ever! (should be detected before the replaceEdges call....)
//                {
//                    Edge e = new Edge( startH, endH );
//                    e.setAngle( old.getAngle() );
//                    e.machine = machine;
//                    edges.add( e );
//                }
//
//
////                    Vector3d mid = new Vector3d(endH);
////                    mid.add( startH );
////                    mid.scale( 0.5 );
////
////                    perp.scale( 20 ); // mid point displacement
////                    mid.add(perp);
////
////                    Corner nC = new Corner(mid);
////                    nC.prevC = startH;
////                    nC.nextC = endH;
////                    startH.nextC = nC;
////                    endH.prevC = nC;
////
////                    Edge
////                          one = new Edge( startH, nC ),
////                          two = new Edge( nC, endH );
////
////                    one.angle = two.angle = Machine.this.currentAngle;
////                    one.machine = two.machine = Machine.this;
////
////                    out.add( one );
////                    out.add( two );
////
//                List<Corner> corners = new ArrayList();
//                for (Edge e : edges)
//                    corners.add( e.start );
//
//                corners.add( edges.get( edges.size() -1).end);
//
//                return corners;
//            }
//
//            public Set<Feature> getFeaturesFor( Edge edgeH )
//            {
//                return new HashSet();
//            }
//        }, height );
//
//
//        machine.findNextHeight( skel );
        return true; // inform the other machine's that we've changed something...?
    }
}
