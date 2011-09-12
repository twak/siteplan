package camp.jme;

import com.jme.app.AbstractGame.ConfigShowMode;
import com.jme.app.BaseGame;
import com.jme.app.SimplePassGame;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.input.ChaseCamera;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.MouseInput;
import com.jme.light.DirectionalLight;
import com.jme.light.PointLight;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.lwjgl.LWJGLRenderer;
import com.jme.renderer.pass.ShadowedRenderPass;
import com.jme.scene.Node;
import com.jme.scene.Skybox;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh.Mode;
import com.jme.scene.shape.Disk;
import com.jme.scene.state.CullState;
import com.jme.scene.state.FogState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jmex.terrain.TerrainPage;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;
import org.lwjgl.util.glu.tessellation.GLUtessellatorImpl;
import straightskeleton.Output;
import straightskeleton.Output.Face;
import straightskeleton.Output.LoopNormal;
import utils.SimpleFileChooser;

public class Preview extends SimplePassGame
{
    public static String PATH = "camp/jme/resources/";
    private Node model = new Node();
    private Node unshadowedModel = new Node();
//    private Node occluders;
    private ChaseCamera chaser;
    private TerrainPage page;
    private FogState fs;
    private Vector3f normal = new Vector3f();
    private static ShadowedRenderPass shadowPass = new ShadowedRenderPass();
    private static boolean debug = true;
//    WireframeState localWireState;
//    MaterialState blackMaterial;
    SketchOverRenderPass sketchPass;
    Spatial floor;
    SpinCameraController scc;


    // this is a switch to create the white-and-blue output for the videos
    public static boolean DO_BLUE = false;

    static
    {
        Logger.getLogger( "com.jme.scene.Node" ).setLevel( Level.OFF );
        Logger.getLogger( "com.jme" ).setLevel( Level.OFF );
        Logger.getLogger( Node.class.getName() ).setLevel( Level.OFF );
        Logger.getLogger( LWJGLRenderer.class.getName() ).setLevel( Level.OFF );
        Logger.getLogger( BaseGame.class.getName() ).setLevel( Level.OFF );
    }

    /**
     * Entry point for the test,
     *
     * @param args
     */
    public static void main( String[] args )
    {
        Preview app = new Preview();
//        if (debug) new ShadowTweaker(sPass).setVisible(true);

        app.setConfigShowMode( ConfigShowMode.AlwaysShow );
        app.start();
    }

    public Preview()
    {
        stencilBits = 4; // we need a minimum stencil buffer at least.
//        if (CampSkeleton.Is_User)
//            setConfigShowMode(ConfigShowMode.AlwaysShow);
//        settings.setFullscreen(false);
    }

    public void clearSettings()
    {
        try {
            File f = new File ("properties.cfg");
            if (f.exists())
                f.delete();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * builds the scene.
     *
     * @see com.jme.app.BaseGame#initGame()
     */
    protected void simpleInitGame()
    {
        // this limits speed somewhat - stops it chewing up an entire core.
        display.setVSyncEnabled( true );

        MouseInput.get().setCursorVisible( true ); // need to control ui!

//        display.getRenderer().setBackgroundColor( new ColorRGBA (140f/255f,200f/255f, 1f, 1f ) ); // nice blue!
        display.getRenderer().setBackgroundColor( new ColorRGBA( 230 / 255f, 230f / 255f, 230f / 255f, 1f ) );
        display.setTitle( "Preview window" );

        input = scc = new SpinCameraController( cam );//KeyboardLookHandler( cam, 10 , 1 );
        cam.setLocation( new Vector3f( 5.0f, 5.0f, 25.0f ) );

        setupTerrain();

        MaterialState materialState = display.getRenderer().createMaterialState();
        if ( DO_BLUE )
        {
            ColorRGBA b = new ColorRGBA (140f/255f,200f/255f, 1f, 1f );
            materialState.setAmbient( b );
            materialState.setDiffuse( new ColorRGBA (0f,0f,0f,0f)  );
            materialState.setSpecular( new ColorRGBA (0f,0f,0f,0f) );

        }
        else
        {
            materialState.setAmbient( new ColorRGBA( 0.4f, 0.4f, 0.4f, 1.0f ) );
            materialState.setDiffuse( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
            materialState.setSpecular( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
        }
        materialState.setShininess( 0.2f );
        materialState.setEmissive( ColorRGBA.black );
        materialState.setEnabled( true );
        rootNode.setRenderState( materialState );

        MaterialState floorMaterial = display.getRenderer().createMaterialState();
        floorMaterial.setAmbient( new ColorRGBA( 1f,1f,1f, 1.0f ) );
        floorMaterial.setDiffuse( new ColorRGBA( 1f,1f,1f, 1.0f ) );
        floorMaterial.setSpecular( ColorRGBA.black );
        floorMaterial.setShininess( 0.0f );
        floorMaterial.setEmissive( ColorRGBA.black );
        floorMaterial.setEnabled( true );

        TextureState tState = display.getRenderer().createTextureState();

        try
        {
            if ( !DO_BLUE )
            {
                Texture t = TextureManager.loadTexture( Preview.class.getResource( "/" + PATH + "grass.png" ), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear );
                t.setScale( new Vector3f( 200f, 200f, 200f ) );
                t.setWrap( Texture.WrapMode.Repeat );

//            t.setCombineFuncRGB(Texture.CombinerFunctionRGB.AddSigned);
//            t.setCombineSrc1RGB(Texture.CombinerSource.CurrentTexture);
//            t.setCombineSrc2RGB(Texture.CombinerSource.Constant);
//            t.setApply(Texture.ApplyMode.Combine);

//            Texture t2 = TextureManager.loadTexture( new File( "grass.png" ).toURI().toURL(), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear) ;
////            Texture t2 = TextureManager.loadTexture( new File( "blur.png" ).toURI().toURL(), Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear) ;
//            t2.setScale( new Vector3f(10f,10f,10f ) );
//            t2.setWrap( Texture.WrapMode.Repeat );
//            t2.setCombineFuncRGB(Texture.CombinerFunctionRGB.AddSigned);
//
//            t2.setCombineSrc1RGB(Texture.CombinerSource.CurrentTexture);
//            t2.setCombineSrc2RGB(Texture.CombinerSource.Constant);
//            t2.setApply(Texture.ApplyMode.Combine);

                tState.setTexture( t, 0 );
            }

//            floor.setTextureCombineMode( Spatial.TextureCombineMode.CombineClosest );
//            tState.setTexture(t2, 1  );

//            BlendState as = DisplaySystem.getDisplaySystem().getRenderer().createBlendState();
//            as.setBlendEnabled( false );
//            as.setTestEnabled( true );
//            as.setTestFunction( BlendState.TestFunction.NotEqualTo );
//            as.setReference( 0.5f );
//            as.setEnabled( true );

//            floor.setRenderState
            floor = new Disk( "floor", 20, 20, 1000f );//300f,300f);
//        ((Disk)floor).copyTextureCoordinates( 0, 1, 1f);
//        floor = d;

            floor.setModelBound( new BoundingBox() );
            floor.updateModelBound();
//        t.setLocalTranslation( 6f,0,0);
            Matrix3f mat = new Matrix3f();
            mat.fromAngleAxis( (float) Math.PI / 2f, new Vector3f( -1f, 0f, 0f ) );
            floor.setLocalRotation( mat );
//        floor.setRenderState(floorMaterial);
            floor.setRenderState( DO_BLUE ? floorMaterial : tState );

            Skybox sky = new Skybox( "sky", 2000f, 2000f, 2000f );
//            Skybox sky = new Skybox("sky", 20f, 20f, 20f);
            sky.setTexture( Skybox.Face.East, TextureManager.loadTexture( Preview.class.getResource( "/" + PATH + "east.png" ), Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear ) );
            sky.setTexture( Skybox.Face.West, TextureManager.loadTexture( Preview.class.getResource( "/" + PATH + "west.png" ), Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear ) );
            sky.setTexture( Skybox.Face.North, TextureManager.loadTexture( Preview.class.getResource( "/" + PATH + "north.png" ), Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear ) );
            sky.setTexture( Skybox.Face.South, TextureManager.loadTexture( Preview.class.getResource( "/" + PATH + "south.png" ), Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear ) );
            sky.setTexture( Skybox.Face.Up, TextureManager.loadTexture( Preview.class.getResource( "/" + PATH + "top.png" ), Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear ) );
            rootNode.attachChild( sky );

            rootNode.attachChild( floor );

        } catch ( Throwable t )
        {
            t.printStackTrace();
        }
//        floorMaterial.setColorMaterial( new MaterialState.ColorMaterial())

//        blackMaterial = display.getRenderer().createMaterialState();
//        blackMaterial.setAmbient( ColorRGBA.black );
//        blackMaterial.setDiffuse( ColorRGBA.black );
//        blackMaterial.setSpecular( ColorRGBA.black );
//        blackMaterial.setShininess(0.0f);
//        blackMaterial.setEmissive(ColorRGBA.black);
//        blackMaterial.setEnabled(true);

        rootNode.attachChild( model );
        rootNode.attachChild( unshadowedModel );


        rootNode.setRenderQueueMode( Renderer.QUEUE_OPAQUE );
        /** Assign key X to action "toggle_shadows". */
        KeyBindingManager.getKeyBindingManager().set( "toggle_shadows",
                                                      KeyInput.KEY_X );


        // setup shadows
        if ( true )
        {
            shadowPass.add( rootNode );
            shadowPass.addOccluder( model );
            shadowPass.setRenderShadows( true );
//            shadowPass.setLightingMethod( ShadowedRenderPass.LightingMethod.Additive );
            pManager.add( shadowPass );

            if (!DO_BLUE)
            {
            sketchPass = new SketchOverRenderPass( cam, 1 );
            sketchPass.setDepthMult( 0f ); // this gives us some grief with parallel, overlapping faces.
            sketchPass.setEnabled( true );
            pManager.add( sketchPass );
            }
        }


//
//        GameStateManager.getInstance().attachChild(state);
//        state.setActive(true);
//
//
//        try
//        {
//            SketchPassState sketchPassState;
//            sketchPassState = GameTaskQueueManager.getManager().update( new Callable<SketchPassState>()
//            {
//
//                public SketchPassState call() throws Exception {
//                    return new SketchPassState( "sketchPassState", game.getCamera() );
//                }
//            } ).get();
//
//            GameStateManager.getInstance().attachChild( sketchPassState );
//            sketchPassState.setActive( true );
//        }
//        catch ( Exception ex )
//        {
//            Logger.getLogger( Preview.class.getName() ).log( Level.SEVERE, null, ex );
//        }



//        wireState.setEnabled( true );

//        localWireState = display.getRenderer().createWireframeState();
//        localWireState.setLineWidth( 3f );
//        localWireState.setAntialiased( true );
//        localWireState.setEnabled( true );
//        rootNode.updateRenderState();

//        RenderPass rPass = new RenderPass();
//        rPass.add(statNode);
//        pManager.add(rPass);


        fs = display.getRenderer().createFogState();
        fs.setDensity( 0.5f );
        fs.setEnabled( true );
//        fs.setColor(new ColorRGBA(0f, 0.2f, 0f, 1f));
//        fs.setColor(new ColorRGBA(76/256f, 146/256f, 0f, 1f)); - dusty green
        fs.setColor( ColorRGBA.white );//new ColorRGBA( 0.8f,0.8f,0.8f, 1f ));
        fs.setEnd( 1000 );
        fs.setStart( 200 );
        fs.setDensityFunction( FogState.DensityFunction.Linear );
        fs.setQuality( FogState.Quality.PerVertex );

        floor.setRenderState( fs );

        FogState fs2 = display.getRenderer().createFogState();
        fs2.setDensity( 0.8f );
        fs2.setEnabled( true );
//        fs.setColor(new ColorRGBA(0f, 0.2f, 0f, 1f));
//        fs.setColor(new ColorRGBA(76/256f, 146/256f, 0f, 1f)); - dusty green
        fs2.setColor( ColorRGBA.white );//new ColorRGBA( 0.8f,0.8f,0.8f, 1f ));
        fs2.setEnd( 5000 );
        fs2.setStart( 200 );
        fs2.setDensityFunction( FogState.DensityFunction.Linear );
        fs2.setQuality( FogState.Quality.PerVertex );

        rootNode.setRenderState( fs2 );

        MouseInput.get().setCursorVisible( true );
    }

    protected void simpleUpdate()
    {
//        foo();
        checkForUpdate();
//        chaser.update(tpf);
//        float characterMinHeight = page.getHeight(m_character
//                .getLocalTranslation())+((BoundingBox)m_character.getWorldBound()).yExtent;
//        if (!Float.isInfinite(characterMinHeight) && !Float.isNaN(characterMinHeight)) {
//            m_character.getLocalTranslation().y = characterMinHeight;
//        }
//
//        float camMinHeight = characterMinHeight + 150f;
//        if (!Float.isInfinite(camMinHeight) && !Float.isNaN(camMinHeight)
//                && cam.getLocation().y <= camMinHeight) {
//            cam.getLocation().y = camMinHeight;
//            cam.update();
//        }
//
//
//        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
//                "toggle_shadows", false)) {
//            sPass.setRenderShadows(!sPass.getRenderShadows());
//        }
        Thread.yield();
    }

    private void setupTerrain()
    {
        lightState.detachAll();

        DirectionalLight dr = new DirectionalLight();
        dr.setEnabled( true );
        dr.setDiffuse( new ColorRGBA( 1.0f, 1.0f, 1.0f, 1.0f ) );
        dr.setAmbient( new ColorRGBA( .2f, .2f, .2f, .3f ) );
        dr.setDirection( new Vector3f( 0.5f, -0.2f, -0.05f ).normalizeLocal() ); // z here was +0.1
        dr.setShadowCaster( !DO_BLUE );
        lightState.attach( dr );

        
            PointLight pl = new PointLight();
            pl.setEnabled( true );
            pl.setDiffuse( new ColorRGBA( .5f, .5f, .5f, 0.5f ) );
            pl.setAmbient( new ColorRGBA( .25f, .25f, .25f, .25f ) );
            pl.setLocation( new Vector3f( 200, 500, 0 ) );
            pl.setShadowCaster( !DO_BLUE );
            lightState.attach( pl );

            DirectionalLight dr2 = new DirectionalLight();
            dr2.setEnabled( true );
            dr2.setDiffuse( new ColorRGBA( 0.1f, 0.1f, 0.1f, 0.1f ) );
            dr2.setAmbient( new ColorRGBA( .2f, .2f, .2f, .1f ) );
            dr2.setDirection( new Vector3f( -0.01f, -0.2f, .2f ).normalizeLocal() );
            dr2.setShadowCaster( !DO_BLUE );
            lightState.attach( dr2 );
        

        CullState cs = display.getRenderer().createCullState();
        cs.setCullFace( CullState.Face.Back );
        cs.setEnabled( true );
        rootNode.setRenderState( cs );

        
        lightState.setGlobalAmbient( new ColorRGBA( 0.6f, 0.6f, 0.6f, 1.0f ) );

//        FaultFractalHeightMap heightMap = new FaultFractalHeightMap(257, 32, 0,
//                255, 0.55f);
//        Vector3f terrainScale = new Vector3f(10, 1, 10);
//        heightMap.setHeightScale(0.001f);
//        page = new TerrainPage("Terrain", 33, heightMap.getSize(),
//                terrainScale, heightMap.getHeightMap());
//
//        page.setDetailTexture(1, 16);
//        rootNode.attachChild(page);

//        ProceduralTextureGenerator pt = new ProceduralTextureGenerator(
//                heightMap);
//        pt.addTexture(new ImageIcon("/home/twak/svn/code/lib/jme2.0.1/src/jmetest/data/texture/grassb.png"), -128, 0, 128);
//        pt.addTexture(new ImageIcon("/home/twak/svn/code/lib/jme2.0.1/src/jmetest/data/texture/dirt.jpg"), 0, 128, 255);
//        pt.addTexture(new ImageIcon("/home/twak/svn/code/lib/jme2.0.1/src/jmetest/data/texture/highest.jpg"), 128, 255,384);
//
//        pt.createTexture(512);
//
//        TextureState ts = display.getRenderer().createTextureState();
//        ts.setEnabled(true);
//        Texture t1 = TextureManager.loadTexture(pt.getImageIcon().getImage(),
//                Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear, true);
//        ts.setTexture(t1, 0);
//
//        Texture t2 = TextureManager.loadTexture(Tmp.class
//                .getClassLoader()
//                .getResource("jmetest/data/texture/Detail.jpg"),
//                Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear);
//        ts.setTexture(t2, 1);
//        t2.setWrap(Texture.WrapMode.Repeat);
//
//        t1.setApply(Texture.ApplyMode.Combine);
//        t1.setCombineFuncRGB(Texture.CombinerFunctionRGB.Modulate);
//        t1.setCombineSrc0RGB(Texture.CombinerSource.CurrentTexture);
//        t1.setCombineOp0RGB(Texture.CombinerOperandRGB.SourceColor);
//        t1.setCombineSrc1RGB(Texture.CombinerSource.PrimaryColor);
//        t1.setCombineOp1RGB(Texture.CombinerOperandRGB.SourceColor);
//
//        t2.setApply(Texture.ApplyMode.Combine);
//        t2.setCombineFuncRGB(Texture.CombinerFunctionRGB.AddSigned);
//        t2.setCombineSrc0RGB(Texture.CombinerSource.CurrentTexture);
//        t2.setCombineOp0RGB(Texture.CombinerOperandRGB.SourceColor);
//        t2.setCombineSrc1RGB(Texture.CombinerSource.Previous);
//        t2.setCombineOp1RGB(Texture.CombinerOperandRGB.SourceColor);
//        rootNode.setRenderState(ts);


        cam.setFrustumFar( 5000f );
    }

    public synchronized void checkForUpdate()
    {
        if (shot != null)
        {
            display.getRenderer().takeScreenShot( shot );
            shot = null;
        }
        
        if ( clear )
        {
            shadowPass.clearOccluders();

            shadowPass.addOccluder( floor );
            shadowPass.addOccluder( model );


            if (!DO_BLUE)
            {
                sketchPass.removeAll();
                sketchPass.add( model );
            }
//            sketchPass.add(unshadowedModel);

            if ( model != null )
                model.detachAllChildren();

            if ( unshadowedModel != null )
                unshadowedModel.detachAllChildren();
        }

        clear = false;

        if ( toShow != null )
        {

            model.attachChild( toShow );
//            sketchPass.add (toShow);

            toShow = null;
        }

        if ( !spatialsToShow.isEmpty() )
        {
            for ( Spatial s : spatialsToShow )
                model.attachChild( s );

            spatialsToShow.clear();
        }

        if ( !unshadowedSpatialsToShow.isEmpty() )
        {
            for ( Spatial s : unshadowedSpatialsToShow )
                unshadowedModel.attachChild( s );

            unshadowedSpatialsToShow.clear();
        }

        rootNode.updateRenderState();
        rootNode.updateModelBound();
        rootNode.updateGeometricState( 0, true );

    }

    public synchronized boolean isPendingUpdate()
    {
        return unshadowedSpatialsToShow.size() > 1 || spatialsToShow.size() > 1 || toShow != null;
    }

    public boolean clear = false;
    Spatial toShow;
    List<Spatial> spatialsToShow = new ArrayList();
    List<Spatial> unshadowedSpatialsToShow = new ArrayList();
    // to stop thread-problems, objects are only added if they generated their data for the current data set (their runkey is current)
    public Object threadKey = new Object();

    public void setViewStats( double x, double y, double z, Object key )
    {
        if ( key == threadKey && scc != null )
            scc.targetOffset.set( (float) x, (float) y / 2f, (float) z );
    }

    public synchronized void display( Spatial spat, Object key )
    {
        display( spat, key, true );
    }

    public synchronized void display( Spatial spat, Object key, boolean shadow )
    {
        if ( key == threadKey )
            (shadow ? spatialsToShow : unshadowedSpatialsToShow).add( spat );
    }

    public void display( Output output, boolean showFaces, boolean showOther ) {
        display (output, showFaces, showOther, showOther );
    }

    public synchronized void display( Output output, boolean showFaces, boolean showOther, boolean showCap )
    {
        clear = true;
        spatialsToShow.clear();
        unshadowedSpatialsToShow.clear();
        Node solid = new Node();
//        Node wire = new Node();
//
//        wire.setRenderState( blackMaterial );
//        wire.setRenderState( localWireState );

        if ( showFaces )
            for ( Face f : output.faces.values() )
                if (!DO_BLUE)
                try
                {
                    JmeFace spatial = new JmeFace( f );

                    if ( spatial.valid ) // && spatial.getMode() ) == Mode.Triangles ) //solid.getChildren() == null )
                        solid.attachChild( spatial );
                    
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
                    JmeLoopL spatial = new JmeLoopL( ln.loopl, ln.norm, true );
                    if ( spatial.valid )
                        solid.attachChild( spatial );
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
                    JmeLoopL spatial = new JmeLoopL( ln.loopl, ln.norm, true );
                    if ( spatial.valid )
                        solid.attachChild( spatial );
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
//                Point3d tmp = end;
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
        spatialsToShow.add( solid );
    }

    public void outputObj( final JFrame swingHandle )
    {
        new SimpleFileChooser(swingHandle, true, "save obj location") {
            @Override
            public void heresTheFile(File f) throws Throwable {

                try {
                    dump(f);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(swingHandle, "error saving file :(");
                    ex.printStackTrace();
                }
            }
        };
    }

    public synchronized void dump( File file )
    {
        checkForUpdate();
        
        JmeObjDump dump = new JmeObjDump();
        dump.add( model );
        dump.add( unshadowedModel );
        dump.allDone( file );
    }

    String shot = null;
    public void takeScreenShot( String fileName )
    {
        shot = fileName;
    }
}
