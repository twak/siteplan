package camp.jme;

import campskeleton.*;
import com.jme.animation.Bone;
import com.jme.app.SimpleGame;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.input.FirstPersonHandler;
import com.jme.input.KeyboardLookHandler;
import com.jme.input.MouseInput;
import com.jme.light.PointLight;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Capsule;
import com.jme.scene.state.CullState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jme.util.export.binary.BinaryImporter;
import com.jmex.model.collada.ColladaImporter;
import com.jmex.model.converters.ObjToJme;
import com.model.md5.MD5Node;
import com.model.md5.importer.MD5Importer;
import com.model.md5.interfaces.mesh.IJoint;
import com.model.md5.interfaces.mesh.IMesh;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BoneDemo  extends SimpleGame {

    private Quaternion rotQuat = new Quaternion();
    private float angle = 0;
    private Vector3f axis = new Vector3f(1, 1, 0).normalizeLocal();
    private Capsule t;

    public static void main(String[] args) {
        BoneDemo app = new BoneDemo();
        app.start();
    }
    
    public BoneDemo()
    {
        setConfigShowMode(ConfigShowMode.AlwaysShow);
    }

    protected void simpleUpdate() {

        if (timer.getTimePerFrame() < 1) {
            angle = angle + (timer.getTimePerFrame() * 1);
            if (angle > 360) {
                angle = 0;
            }
        }
        rotQuat.fromAngleNormalAxis(angle, axis);
        t.setLocalRotation(rotQuat);
    }

    protected void getAttributes()
    {
        settings = getNewSettings();
    }

    public void print (Node spat, int depth)
    {
        for (int i = 0; i < depth; i++)
            System.out.print(" ");
        
        System.out.println (spat);

        if (spat.getChildren() != null)
        for (Spatial s  : spat.getChildren())
        {
            if (s instanceof Node && s != null)
            {
                print ((Node)s, depth+1);
            }
            
            if ( s instanceof Bone )
            {
                Bone b = (Bone) s;

                System.out.println ( b.getBindMatrix() );
                Quaternion q = new Quaternion( new float[]
                        {
                            (float)( Math.random() * Math.PI), (float)( Math.random() * Math.PI), (float)( Math.random() * Math.PI)
                        } );
                b.setLocalRotation( q );// (float) ( Math.random() * 10. ), (float) ( Math.random() * 10. ), (float) ( Math.random() * 10. ));
//                b.update();
//                b.updateGeometricState( 0, true );
//                b.propogateBoneChange( true );

            }
        }
    }

    /**
     * builds the trimesh.
     *
     * @see com.jme.app.SimpleGame#initGame()
     */
    protected void simpleInitGame() {
        display.setTitle("Cylinder Test");

        t = new Capsule("Capsule", 40, 32, 16, 2, 4);
        t.setModelBound(new BoundingBox());
        t.updateModelBound();

        CullState cs = display.getRenderer().createCullState();
        cs.setCullFace(CullState.Face.Back);
        rootNode.setRenderState(cs);

        input = new FirstPersonHandler(cam, 10f, 1f);

//        rootNode.attachChild(t);

        TextureState ts = display.getRenderer().createTextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.loadTexture(Preview.class
                .getClassLoader().getResource("jmetest/data/images/Monkey.jpg"),
                Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear));
        ts.getTexture().setWrap(Texture.WrapMode.Repeat);
        rootNode.setRenderState(ts);

        lightState.setTwoSidedLighting(true);

         input = new KeyboardLookHandler( cam, 10 , 1 );

                 /** Set up a basic, default light. */
        PointLight light = new PointLight();
        light.setDiffuse( new ColorRGBA( 0.75f, 0.75f, 0.75f, 0.75f ) );
        light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
        light.setLocation( new Vector3f( -100, -100, 100 ) );
        light.setEnabled( true );

        /** Attach the light to a lightState and the lightState to rootNode. */
        lightState = display.getRenderer().createLightState();
        lightState.attach( light );

        light = new PointLight();
        light.setDiffuse( new ColorRGBA( 0.0f, 0.75f, 0.75f, 0.75f ) );
        light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
        light.setLocation( new Vector3f( -100, 100, 100 ) );
        light.setEnabled( true );

        /** Attach the light to a lightState and the lightState to rootNode. */
        lightState.attach( light );
        rootNode.setRenderState( lightState );
//        lightState.setEnabled( false );

         MouseInput.get().setCursorVisible(true);
        try
        {

            MD5Importer.getInstance().loadMesh( new File( "window.md5" ).toURI().toURL(), "model" );

            IMesh mesh =  MD5Importer.getInstance().getMD5Node().getMesh( 0 );

            MD5Importer.getInstance().getMD5Node().clone();

            MD5Node node =  (MD5Node) MD5Importer.getInstance().getMD5Node();

//            MD5Importer.getInstance().getMD5Node().getMesh( 0 )

            IJoint[] joints = MD5Importer.getInstance().getMD5Node().getJoints();
            for (IJoint ik : joints)
            {
                if ( ik.getName().startsWith( "2" ) )
                {
                    Vector3f v = ik.getTranslation();
                    v = v.add( 4f,0,0);
                    ik.updateTransform( v, new Quaternion() );
                }
            }

            node.flagUpdate();
            node.updateGeometricState( 0, true );
            
            rootNode.attachChild( (Spatial) mesh );
            
            //tell the importer to load the mob boss
//            ColladaImporter.load( new FileInputStream("flaps.dae"), "model" );
//            ColladaImporter.load( new FileInputStream("window.dae"), "model" );
            //we can then retrieve the skin from the importer as well as the skeleton
//            SkinNode sn = ColladaImporter.getSkinNode( ColladaImporter.getSkinNodeNames().get( 0 ) );
//            Node node = ColladaImporter.getModel();
//            Bone skel = ColladaImporter.getSkeleton( ColladaImporter.getSkeletonNames().get( 0 ) );
//
//            print (node, 0);
//
//            skel.updateGeometricState( 0, true );
//            skel.propogateBoneChange( true );
////            rootNode.up
//            skel.update();
//
//
//            rootNode.attachChild( node );

//            skel.setLocalTranslation( new Vector3f (5,0,0));


            //clean up the importer as we are about to use it again.
            ColladaImporter.cleanUp();


            InputStream statue = null;
            statue = new FileInputStream( "test.obj" );

            ObjToJme o2j = new ObjToJme();
            ByteArrayOutputStream BO = new ByteArrayOutputStream();
            o2j.convert( statue, BO );
            TriMesh model = (TriMesh) BinaryImporter.getInstance().load( new ByteArrayInputStream( BO.toByteArray() ) );
            rootNode.attachChild( model );
        }
        catch ( Throwable ex )
        {
            Logger.getLogger( BoneDemo.class.getName() ).log( Level.SEVERE, null, ex );
        }

        rootNode.updateModelBound();
    }
}