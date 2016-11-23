package camp.jme;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.twak.utils.SimpleFileChooser;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import campskeleton.PlanSkeleton;
import straightskeleton.Output;
import straightskeleton.Output.Face;
import straightskeleton.Output.LoopNormal;
import straightskeleton.Skeleton;

public class Preview
{


    public synchronized void display( Output output, boolean showFaces, boolean showOther, boolean showCap )
    {
        Node solid = new Node();
//        Node wire = new Node();
//
//        wire.setRenderState( blackMaterial );
//        wire.setRenderState( localWireState );

        if ( showFaces )
            for ( Face f : output.faces.values() )
                try
                {
//                    JmeFace spatial = new JmeFace( f );
                    
                } catch ( Throwable e )
                {
                    if ( f != null )
                        System.err.println( "point count is " + f.pointCount() );

                    e.printStackTrace();
                    if ( e.getCause() != null )
                        e.getCause().printStackTrace();
                }

        if ( showOther )
            for ( LoopNormal ln : output.nonSkelFaces )
                try
                {
//                    JmeLoopL spatial = new JmeLoopL( ln.loopl, ln.norm, true );
//                    if ( spatial.valid )
//                        solid.attachChild( spatial );
                } catch ( Throwable e )
                {
                    e.printStackTrace();
                    if ( e.getCause() != null )
                        e.getCause().printStackTrace();
                }


          if ( showCap )
            for ( LoopNormal ln : output.nonSkelFaces2 )
                try
                {
//                    JmeLoopL spatial = new JmeLoopL( ln.loopl, ln.norm, true );
//                    if ( spatial.valid )
//                        solid.attachChild( spatial );
                } catch ( Throwable e )
                {
                    e.printStackTrace();
                    if ( e.getCause() != null )
                        e.getCause().printStackTrace();
                }

//        Set<Output.SharedEdge> roofLines = new HashSet();
//        for (Output.SharedEdge se : output.edges.map.values())
//        {
//            if (
//                se.left.profile.contains( CampSkeleton.instance.roof ) &&
//                se.right.profile.contains( CampSkeleton.instance.roof ) )
//                    roofLines.add( se );
//        }

//        for (Output.SharedEdge se : roofLines)
//        {
//
//            Point3d start = se.getStart( se.left );
//            Point3d end = se.getEnd( se.left );
//
//            if (start.z > end.z)
//            {
//                end = start;
//                start = tmp;
//            }

//            Point3d lE = new Point3d (end.x, end.y, start.z);
//
//            Vector3d slope = new Vector3d (end);
//            slope.sub( start );
//            Vector3d flat = new Vector3d (lE);
//            flat.sub( start );
//
//            double angle = slope.angle( flat );
//
//            Cylinder c = new Cylinder( "bob", 2, 5, 0.1f, (float)(start.distance( end )/Jme.scale));
//
//            Point3d perp = new Point3d (start.x - slope.y, start.y + slope.x, start.z);
//            Vector3d pv = new Vector3d(start);
//            pv.sub( perp );
//
//
////            LinearForm lf = new LinearForm( new Line (end.x, end.y, start.x, start.y));
////            lf.perpendicular();
////            Line l = lf.toLine( 0, 10 );
//
//            Quaternion quat = new Quaternion();
//            quat.fromAngleAxis( (float)angle , Jme.toF( pv ) );
//
//            c.setLocalTranslation( Jme.toF( Jme.convert( start )));
//            c.setModelBound( new OrientedBoundingBox() );
//            c.setLocalRotation( quat );
//            c.updateModelBound();
//            out.attachChild( c );
//        }


//        if (allFaces.size() > 0)
//            out.attachChild( new Tiller( allFaces.iterator().next() ) );

//        System.out.println (" we ahve "+ out.getChildren().size() );

//        spatialsToShow.add( wire );
//        spatialsToShow.add( solid );
    }

    public void outputObj( final JFrame swingHandle ) {}

    public synchronized void dump( File file ){}

    String shot = null;
	public boolean clear;
	protected PlanSkeleton threadKey;
    public void takeScreenShot( String fileName )
    {
        shot = fileName;
    }

	public boolean isPendingUpdate() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setViewStats( double d, double e, double f, Skeleton threadKey2 ) {
		// TODO Auto-generated method stub
		
	}

	public void display( Spatial s, Skeleton threadKey2 ) {
		// TODO Auto-generated method stub
		
	}
}
