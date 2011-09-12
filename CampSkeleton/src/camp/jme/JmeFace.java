package camp.jme;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import straightskeleton.Output;
import straightskeleton.Output.Face;
import utils.Loop;
import utils.LoopL;

/**
 * Triangulate a polygon mit holes into a TriMesh
 * @author twak
 */
public class JmeFace extends JmeLoopL
{
    Face face;

    public JmeFace (Face face)
    {
        super();
        this.face = face;

        norm = new Vector3d();
        Vector3d dir = face.edge.direction();
        Vector3d uphill = new Vector3d(face.edge.uphill);
        dir.normalize();
        uphill.normalize();
        norm.cross( dir,  uphill);
        norm = new Vector3d( Jme.convert( norm ) );
        norm.normalize();
        
        loopl = face.getLoopL();
        
        gluTriangulate(true);


//        for (Loop<Point3d> lp : loopl)
//        {
//            LoopL<Point3d> tmp = new LoopL();
//            tmp.add(lp);
//            triangulate(tmp, normal );
//            break;
//        }
//
//        valid = false;

//        triangulate(loopl, normal );
//        this.loopl = loopl;
//        draw2(null);
    }

    /**
     * Triangulator assumes y-uppyness - rotate so that normal lies along z axis and the edge direction is along x
     *
     * Face to Y up in JME space
     */
    Matrix4d faceToYUp()
    {
        Matrix3d o = new Matrix3d();

        Vector3d dir = face.edge.direction();
        dir.normalize();
        o.setColumn( 0, dir );

        Vector3d upHill = face.edge.uphill;
        o.setColumn( 1, upHill );

        o.setColumn( 2, face.edge.getPlaneNormal());

        o.transpose();

        Matrix4d sol = new Matrix4d (); sol.setIdentity();
        sol.setRotation( o );

        // to JME space!
//        sol.mul( Jme.transform);

        return sol;
    }


    public static void main (String[] args)
    {
        Output o = new Output( null );


                final LoopL<Point3d> loopl = new LoopL();
                Loop<Point3d> loop = new Loop();
                loopl.add( loop );
                
//                loop.append( new Point3d( 0, 10, 0 ) );
//                loop.append( new Point3d( 10, 10, 0 ) );
//                loop.append( new Point3d( 10, 0, 0 ) );
//                loop.append( new Point3d( 5, 3, 0 ) );
//                loop.append( new Point3d( 0, 0, 0 ) );

////                loop.append( new Point3d(0, 0, 0 ) ); // anticlockwise
//                loop.append( new Point3d( 5, 3, 0 ) );
//                loop.append( new Point3d( 10, 0, 0 ) );
//                loop.append( new Point3d( 10, 10, 0 ) );
//                loop.append( new Point3d( 0, 10, 0 ) );

                loop.append( new Point3d( 108.04772006108185, 219.1874714070383, 0 ) ); // anticlockwise
                loop.append( new Point3d( 22.306679253788648, 252.5389221640081,0 ) );
                loop.append( new Point3d( -33.01371483328644, 145.08438423254447, 0 ) );
                loop.append( new Point3d( 253.04921530621976, 145.0843842325445, 0 ) );
                loop.append( new Point3d( 196.06555733493352, 251.93624853608227, 0 ) );

//                loop.append( new Point3d( 196.06555733493352, 251.93624853608227, 0 ) );
//                loop.append( new Point3d( 253.04921530621976, 145.0843842325445, 0 ) );
//                loop.append( new Point3d( -33.01371483328644, 145.08438423254447, 0 ) );
//                loop.append( new Point3d( 22.306679253788648, 252.5389221640081,0 ) );
//                loop.append( new Point3d( 108.04772006108185, 219.1874714070383, 0 ) );

        Face f = o.new Face()
        {
            @Override
            public LoopL<Point3d> getLoopL()
            {
                return loopl;
            }

            @Override
            public int pointCount()
            {
                return loopl.count();
            }
        };

        JmeFace jme = new JmeFace( f );
    }

}