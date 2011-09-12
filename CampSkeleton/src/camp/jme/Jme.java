package camp.jme;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

/**
 * scale factor and y/z swap between skeleton coord system (pixels in ui space) and
 * jme (meters?))
 * 
 * @author twak
 */
public class Jme
{
    public static Matrix4d transform = new Matrix4d();
    public final static double scale = 30;
    static
    {
        transform.setIdentity();
        transform.setScale( 1 / scale );
        
        double[] tmp = new double[4], tmp2 = new double[4];

        transform.getRow( 1, tmp);
        transform.getRow( 2, tmp2 );

        transform.setRow( 1, tmp2 );
        transform.setRow( 2, tmp );
//        transform.m33 = scale;
    }

    public static Matrix4d transformNoScale = new Matrix4d();
    static
    {
        transformNoScale.setIdentity();

        double[] tmp = new double[4], tmp2 = new double[4];

        transformNoScale.getRow( 1, tmp);
        transformNoScale.getRow( 2, tmp2 );

        transformNoScale.setRow( 1, tmp2 );
        transformNoScale.setRow( 2, tmp );
//        transform.m33 = scale;
    }
    
    public static Point3d convert (Tuple3d in)
    {
        Point3d out = new Point3d(in);
        transform.transform( out );
        return out;
    }

    public static Quaternion asQuat( Matrix4d m )
    {
        Quaternion rot = new Quaternion();
        rot.fromRotationMatrix(
                (float) m.m00, (float) m.m01, (float) m.m02,
                (float) m.m10, (float) m.m11, (float) m.m12,
                (float) m.m20, (float) m.m21, (float) m.m22 
                );
        return rot;
    }

    public static Vector3f toF (Tuple3d in)
    {
        return new Vector3f ((float)in.x,(float)in.y,(float)in.z);
    }
}
