package camp.junk.wiggle;

import camp.jme.*;
import campskeleton.CampSkeleton;
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
import utils.ConsecutiveItPairsGap;
import utils.MultiMap;
import utils.Pair;

/**
 *
 * @author twak
 */
public class WindowFactory extends FeatureFactory<WindowFeature>
{
    Map<Tag, MultiMap<Marker, Matrix4d>> points = new LinkedHashMap();

    public WindowFactory()
    {
        super( null );// CampSkeleton.instance.window );
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
            {
                if (! multi.get(m).contains( matrix )) // todo: why do we get repeated matricies here?
                    multi.put( m, matrix );
            }
        }
    }

    public void addTo (Node node)
    {
        for (Tag f : points.keySet()) {
            MultiMap<Marker, Matrix4d> multi = points.get(f);
            for (Marker marker : multi.keySet()) {
                for (Pair<Matrix4d, Matrix4d> pair : new ConsecutiveItPairsGap<Matrix4d>(multi.get(marker))) {
                    String file = f.name.contains("bay window") ? "window_sm.md5" : "window.md5"; // HACK!
                    JmeBone bone = new JmeBone(file);

                    Matrix4d m = pair.first();
                    Matrix4d m2 = pair.second();

                    float scale = (float)((WindowFeature)f).scale;
                    float iScale = 1 / scale;

                    // find rotation for bone (erk, rotation transform occurs in feature factory)
//                        Vector3d dir = new Vector3d( 2, 200, 0 );
//                        m.transform( dir );
//                        m2.transform( dir );
//                        double angle = Math.atan2( dir.y, dir.x );


                    Quaternion rot = new Quaternion();
                    rot.fromRotationMatrix(
                            (float) m.m00, (float) m.m01, (float) m.m02,
                            (float) m.m10, (float) m.m11, (float) m.m12,
                            (float) m.m20, (float) m.m21, (float) m.m22 // switch handedness of coord system
                            );

                    bone.setBones(Arrays.asList(
                            new JmeBone.BoneLocation("1", new Vector3f((float) m.m03 * iScale, ((float) m.m13) * iScale, (float) m2.m23 * iScale), rot),
                            new JmeBone.BoneLocation("0", new Vector3f((float) m.m03 * iScale, ((float) m.m13) * iScale, (float) m.m23 * iScale), rot)));

//                    bone.setBoneLocation( top, false, 0 );
//                    bone.setBoneLocation( bottom,true,  1 );
                    bone.setLocalScale(scale);
                    node.attachChild(bone);
                }

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
                Node n = new Node();
                addTo(n);
                preview.display( n, threadKey );
            }
        }.start();
    }
}
