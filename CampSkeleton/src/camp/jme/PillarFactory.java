package camp.jme;

import campskeleton.CampSkeleton;
import campskeleton.FeatureFactory;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import straightskeleton.ui.Marker;
import utils.MultiMap;

/**
 *
 * @author twak
 */
public class PillarFactory extends FeatureFactory<PillarFeature>
{
    List<Matrix4d> points = new ArrayList();

    public PillarFactory()
    {
        super ( null );//CampSkeleton.instance.pillar);
    }

    @Override
    public void addPoints ( MultiMap<Marker, Matrix4d> points )
    {
        for ( Marker m : points.keySet() )
        {
            this.points.addAll( points.get( m ) );
        }
        
        for (Matrix4d mat : this.points)
        {
            CampSkeleton.instance.addDebugMarker(mat);
        }
    }

    public void addTo( final Preview preview, final Object threadKey )
    {
        new Thread()
        {
            @Override
            public void run()
            {
                for ( Matrix4d m : points )
                {
                    JmeBone bone = new JmeBone( "pillar.md5" );

                    // find rotation for bone (erk, rotation transform occurs in feature factory)
                    Vector3d dir = new Vector3d (1,0,0);
                    m.transform( dir );

                    double angle = Math.atan2( dir.y, dir.x );
                    
                    Quaternion rot = new Quaternion();
                    rot.fromRotationMatrix(
                            (float)m.m00,(float)m.m01,(float)m.m02,
                            (float)m.m10,(float)m.m11,(float)m.m12,
                            (float)m.m20,(float)m.m21,(float)m.m22 // switch handedness of coord system
                            );
                    bone.setBones( Arrays.asList(
                            new JmeBone.BoneLocation( "0", new Vector3f( (float) m.m03, (float) m.m13, (float) m.m23 ), rot ),
                            new JmeBone.BoneLocation( "1", new Vector3f( (float) m.m03, (float) m.m13, (float) 0 ), rot ) ) );

//                    bone.setBoneLocation( top, false, 0 );
//                    bone.setBoneLocation( bottom,true,  1 );
                    preview.display( bone, threadKey );
                }
            }
        }.start();
    }
}
