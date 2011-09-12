package camp.junk.wiggle;

import camp.jme.*;
import campskeleton.FeatureFactory;
import camp.jme.Preview;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.vecmath.Matrix4d;
import straightskeleton.Tag;
import straightskeleton.ui.Marker;
import utils.MultiMap;

/**
 *
 * @author twak
 */
public class MeshFactory extends FeatureFactory<MeshFeature>
{

    Map<Tag, MultiMap<Marker, Matrix4d>> points = new LinkedHashMap();

    public MeshFactory()
    {
        super( new MeshFeature( "not.here.md5" ) );
    }

    @Override
    public void addPoints( MultiMap<Marker, Matrix4d> pointsIn )
    {
        for ( Marker m : pointsIn.keySet() )
        {
            MultiMap<Marker, Matrix4d> multi = points.get( m.feature );
            if ( multi == null )
                points.put( m.feature, multi = new MultiMap() );

            for ( Matrix4d matrix : pointsIn.get( m ) )
                multi.put( m, matrix );
        }
    }

    public void addTo (Node node)
    {
        for ( Tag f : points.keySet() )
                {
                    MultiMap<Marker, Matrix4d> multi = points.get( f );
                    for ( Marker marker : multi.keySet() )
                        for ( Matrix4d m : multi.get( marker ) )
                        {
                            JmeBone bone = new JmeBone( f.name );

                            Quaternion rot = new Quaternion();
                            rot.fromRotationMatrix(
                                    (float) m.m00, (float) m.m01, (float) m.m02,
                                    (float) m.m10, (float) m.m11, (float) m.m12,
                                    (float) m.m20, (float) m.m21, (float) m.m22 // switch handedness of coord system
                                    );
//
                            float scale = (float)((MeshFeature)f).scale;
                            float iScale = 1/scale;

                            bone.setBones( Arrays.asList(
                                    new JmeBone.BoneLocation( "0",
                                    new Vector3f( (float) m.m03 * iScale, (float) m.m13 * iScale, (float) m.m23 *iScale ), rot ) ) );
//                                    new JmeBone.BoneLocation( "0", new Vector3f( (float) m.m03, (float) m.m13 + 3f, (float) m.m23 ), rot ) ) );

//                            bone.setLocalRotation( rot );
//                            bone.setLocalTranslation( new Vector3f( (float) m.m03, (float) m.m13, (float) m.m23 ) );

                            bone.setLocalScale(scale);
                            node.attachChild(bone);
                        }
                }
    }

    public void addTo( final Preview preview, final Object threadKey )
    {
        new Thread()
        {
            @Override
            public void run()
            {
                // contains a list of same class of feature, tho may have d
//                for ( Feature f : points.keySet() )
//                {
//                    MultiMap<Marker, Matrix4d> multi = points.get( f );
//                    for ( Marker marker : multi.keySet() )
//                        for ( Matrix4d m : multi.get( marker ) )
//                        {
//                            JmeBone bone = new JmeBone( f.name );
//
//                            Quaternion rot = new Quaternion();
//                            rot.fromRotationMatrix(
//                                    (float) m.m00, (float) m.m01, (float) m.m02,
//                                    (float) m.m10, (float) m.m11, (float) m.m12,
//                                    (float) m.m20, (float) m.m21, (float) m.m22 // switch handedness of coord system
//                                    );
////
//                            float scale = 0.15f;
//                            float iScale = 1/scale;
//
//                            bone.setBones( Arrays.asList(
//                                    new JmeBone.BoneLocation( "0",
//                                    new Vector3f( (float) m.m03 * iScale, (float) m.m13 * iScale, (float) m.m23 *iScale ), rot ) ) );
////                                    new JmeBone.BoneLocation( "0", new Vector3f( (float) m.m03, (float) m.m13 + 3f, (float) m.m23 ), rot ) ) );
//
////                            bone.setLocalRotation( rot );
////                            bone.setLocalTranslation( new Vector3f( (float) m.m03, (float) m.m13, (float) m.m23 ) );
//
//                            bone.setLocalScale(scale);
//                            preview.display( bone, threadKey );
                            Node n = new Node();
                            addTo(n);
                            preview.display( n, threadKey );
//                        }
//                }
            }
        }.start();
    }
}
