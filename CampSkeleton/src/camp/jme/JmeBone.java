package camp.jme;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.RenderState.StateType;
import com.jme.scene.state.TextureState;
import com.model.md5.importer.MD5Importer;
import com.model.md5.interfaces.IMD5Node;
import com.model.md5.interfaces.mesh.IJoint;
import com.model.md5.interfaces.mesh.IMesh;
import com.model.md5.resource.mesh.Mesh;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import utils.Cache;
import utils.MultiMap;

/**
 *
 * @author twak
 */
public class JmeBone extends Node
{
    public static Map<String, IMD5Node> cache = new HashMap();

    public synchronized static IMD5Node load( String name )
    {
        IMD5Node out = cache.get( name );
//        out = null;
        if ( out == null )
            try
            {
                MD5Importer.getInstance().loadMesh( new File( name ).toURI().toURL(), "model" );
                out = MD5Importer.getInstance().getMD5Node();
                cache.put( name, out );
            }
            catch ( Throwable t )
            {
                t.printStackTrace();
            }
        if (out != null)
        {
            System.out.println ("starting clone at "+System.currentTimeMillis());
            IMD5Node tmp = out.clone();
            
            System.out.println ("ending at "+System.currentTimeMillis());
            return tmp;
        }
        else return null;
    }

    IMD5Node node = null;
    Map<String, RotTrans> originalTrans = new HashMap();
    Map<String, IJoint> fromName = new HashMap();
    MultiMap<IJoint, IJoint> children = new MultiMap();
    // scaling over all bones
    double scale = 1.;


    public JmeBone (String name)
    {
        node = JmeBone.load( name );

        for ( IMesh im : node.getMeshes() )
        {
            Mesh m = (Mesh)im; 
            m.setNormalsMode( NormalsMode.Inherit );
            m.clearTextureBuffers(); // clear textures (something in the md5 loader assumes that everything is textured)
            m.setTextureCombineMode( TextureCombineMode.Off );
            ((TextureState) m.getRenderState( StateType.Texture )).setTexture( null,0 );
        }

        Spatial s = (Spatial)node;

        s.setModelBound( new OrientedBoundingBox() );
        s.setCullHint( CullHint.Never );
        
        attachChild( s );

        IJoint[] joints = node.getJoints();
        for ( IJoint ik : joints )
        {
            originalTrans.put( ik.getName(), new RotTrans( ik.getTranslation().clone(), ik.getOrientation().clone() ) );
 
            fromName.put( ik.getName(), ik);
            children.put( ik.getParent(), ik);
        }
    }

    public void setScale(double scale)
    {
        this.scale = scale;
        setLocalScale((float)scale);
    }

    public Set<String> getBoneNames()
    {
        return originalTrans.keySet();
    }

    public static class BoneLocation
    {
        String name;
        Vector3f location;
        Quaternion orientation;

        public BoneLocation (String name, Vector3f location, Quaternion orientation)
        {
            this.name = name;
            this.location = location;
            this.orientation = orientation;
        }
    }

    public void setBones (List<BoneLocation> locs)
    {
        // parents are earlier in the list
        for (IJoint j : node.getJoints())
        {
            for (BoneLocation bl : locs)
            {
                if (bl.name.compareTo( j.getName()) == 0)
                {
                    Matrix4d m = new Matrix4d (Jme.transform);
                    Point3d pt = new Point3d( bl.location.x, bl.location.y, bl.location.z );
                    m.transform( pt );
                    pt.scale(1/scale);

                    // does m have to be orthonormal for this to work?
//                    Quaternion q = new Quaternion();
//                    Quaternion q = new Quaternion();
//                    q = q.fromRotationMatrix( (float) m.m00, (float) m.m01, (float) m.m02,
//                            (float) m.m10, (float) m.m11, (float) m.m12,
//                            (float) m.m20, (float) m.m21, (float) m.m22 );
                    
//                    Vector3f translation = new Vector3f(  (float) m.m03, (float) m.m13, (float) m.m23 );
                    Vector3f translation = new Vector3f( (float) pt.x, (float) pt.y, (float) pt.z);// (float) m.m03, (float) m.m13, (float) m.m23 );

                    // move rotations into root-space
                    Quaternion o = j.getParent().getOrientation().inverse().mult( bl.orientation );
                    Vector3f t = translation.subtract( j.getParent().getTranslation() );
                    j.getParent().getOrientation().inverse().multLocal(t);

//                    System.out.println ("for "+j.getName() + " setting to "+t+" "+o);

                    j.updateTransform(t,o );
                }
            }
            // flush new values
            j.processRelative();
        }

        node.flagUpdate();
        updateGeometricState( 0, true);

//        for ( IJoint j : node.getJoints() )
//            System.out.println( j.getName() +"bone location is "+j.getTransform() );
    }
    
    /**
     * naming convention is a sequence of digits 0,1 = bones for a pillar
     * 0,0
     * 0,1
     * 1,1
     * 1,0 == bones for a rectangular window
     */
//    public Vector3f trans0 = new Vector3f();
//    public void setBoneLocation( Matrix4d location, boolean relative, int... index )
//    {
//        String key = "";
//        for (int i : index)
//            key = key+Integer.toString( i );
//
//        System.out.println ("in Graphcis space we're looking at\n" + location);
//
//        Matrix4d m = new Matrix4d(Jme.transform);
//        m.mul (location );
//
//        System.out.println ("in jem space we're looking at\n" + m);
//
//        Quaternion q = new Quaternion();
//        q=q.fromRotationMatrix( (float)m.m00, (float)m.m01, (float)m.m02,
//                (float)m.m10, (float)m.m11, (float)m.m12,
//                (float)m.m20, (float)m.m21, (float)m.m22 );
//
//        Vector3f translation = new Vector3f ( (float) m.m03, (float)m.m13,   (float) m.m23  );
//
//        IJoint[] joints = node.getJoints();
//        for ( IJoint ik : joints )
//            if ( ik.getName().compareTo( key ) == 0 )
//            {
//                Vector3f origT = originalTrans.get( key ).trans.clone();
//                Quaternion origR = originalTrans.get( key ).quat.clone();
//
//                if (key.compareTo( "0") == 0)
//                {
//                    trans0 = origT =translation;
//                    System.out.println ("translation 0 is "+translation);
//                }
//                else if (key.compareTo( "1") == 0)
//                {
//                    origT = translation;
////                    origT = origT.subtract( translation );
////                    origT = origT.subtract( trans0 );
//                    System.out.println ("translation 1 is "+origT);
//                }
////                else
////                {
////                    origT = new Vector3f(translation);
////                    origT = origT.subtract( rootTrans);
////                    origT = new Vector3f(0,100,0);
////                }
//
////                if (key.compareTo( "0") == 0)
////                    origT = translation;
//
//
//
////                Vector3f base = originalTrans.get( "0" ).trans.clone();
////
////                translation.subtract( base );
//
////                origT = origT.add( translation );
//
////                System.out.println ("old trans for "+key+" is "+origT);
//
//
//                ik.updateTransform( origT, origR );
//
//                node.flagUpdate();
//                updateGeometricState( 0, true );
//
//                return;
//            }
//
//        throw new Error ("couldn't find the bone "+key+" in "+name);
//
//    }

    static class RotTrans
    {
        Vector3f trans;
        Quaternion quat;
        public RotTrans (Vector3f trans, Quaternion quat)
        {
            this.trans = trans;
            this.quat = quat;
        }
    }
    
    public static Cache<String, List<String>> boneCache = new Cache<String, List<String>>()
    {
        @Override
        public List<String> create(String i)
        {
            IMD5Node node = JmeBone.load(i);
            IJoint[] joints = node.getJoints();
            List<String> out = new ArrayList<String>();
            for (IJoint ik : joints) {
                out.add(ik.getName());
            }
            return out;
        }
    };
    public static List<String> getNames (String meshName)
    {
        return boneCache.get(meshName);
    }

}
