package org.twak.siteplan.jme;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.twak.camp.Output;
import org.twak.camp.Output.Face;
import org.twak.siteplan.campskeleton.PlanSkeleton;
import org.twak.utils.Mathz;
import org.twak.utils.collections.Arrayz;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.Loopable;
import org.twak.utils.ui.WindowManager;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;

public class Preview extends SimpleApplication
{
	Node skel = new Node();
	
	public Preview () {
		JFrame jf = new JFrame();
		WindowManager.register( jf );
		jf.setSize( 800, 600 );
		jf.setContentPane( getPanel() );
		jf.pack();
		jf.setVisible( true );
	}
	
    private Container getPanel() {
    	Dimension d3Dim = new Dimension (800, 600);
		
		AppSettings settings = new AppSettings(true);
		
		settings.setWidth(d3Dim.width);
		settings.setHeight(d3Dim.height);
		settings.setSamples(4);
		settings.setVSync(true);
		settings.setFrameRate(60);
		
		setSettings(settings);
		createCanvas();
		JmeCanvasContext ctx = (JmeCanvasContext) getContext();
		ctx.setSystemListener(this);
		
		Canvas canvas = ctx.getCanvas();
		canvas.setPreferredSize(d3Dim);
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add( canvas, BorderLayout.CENTER );
		
		return panel;
	}

	public synchronized void display( Output output, boolean showFaces, boolean showOther, boolean showCap )
    {
		enqueue( new Runnable() {
			
			@Override
			public void run() {
				
				for (Spatial s : skel.getChildren())
					s.removeFromParent();
				
				Geometry geom = new Geometry("mesh", createMesh( output ));
				
				geom.setMaterial( Jme3z.lit (Preview.this, 0.5, 0.5, 0.5) );
				
				skel.attachChild( geom );
				
				rootNode.updateGeometricState();
				rootNode.updateModelBound();
				
				gainFocus();
			}
		} );
    }

	private Mesh createMesh( Output output ) {
		List<Float> pos = new ArrayList<>(), norms = new ArrayList<>();
		List<Integer> ind = new ArrayList<>();

		if ( output.faces != null )
			f:
			for ( Face f : output.faces.values() ) {

				for ( Loop<Point3d> ll : f.getLoopL() ) {
					for (Loopable<Point3d> lll : ll.loopableIterator())
						if (lll.get().distance( lll.getNext().get() ) > 10000)
							continue f;
				}
				
				for ( Loop<Point3d> ll : f.getLoopL() ) {
					org.twak.utils.collections.Loopz.triangulate( ll, true, ind, pos, norms );
				}
			}

		// output is z-up, we're y-up
		for ( int i = 0; i < pos.size(); i += 3 ) {
			swap( pos, i + 1, i + 2 );
			swap( norms, i + 1, i + 2 );
		}
		Mesh m = new Mesh();

		m.setMode( Mesh.Mode.Triangles );

		m.setBuffer( VertexBuffer.Type.Position, 3, Arrayz.toFloatArray( pos ) );
		m.setBuffer( VertexBuffer.Type.Normal, 3, Arrayz.toFloatArray( norms ) );
		m.setBuffer( VertexBuffer.Type.Index, 3, Arrayz.toIntArray( ind ) );
		return m;
	}
    
	private void swap( List<Float> array, int i, int j ) {
		float tmp = array.get( i );
		array.set( i, array.get( j ) );
		array.set( j, tmp );
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


	public final static String 
			CLICK = "Click", 
			MOUSE_MOVE = "MouseyMousey", 
			SPEED_UP = "SpeedUp", SPEED_DOWN = "SpeedDown",
			AMBIENT_UP = "AmbientUp", AMBIENT_DOWN = "AmbientDown", 
			TOGGLE_ORTHO = "ToggleOrtho", 
			FOV_UP ="FovUp", FOV_DOWN = "FovDown";
	
	private static final boolean SSAO = true;

	public Matrix4d toOrigin, fromOrigin;
	
	private AmbientLight ambient;
	private DirectionalLight sun;
	private PointLight point;
	
	public Node debug;
	
	@Override
	public void reshape( int w, int h ) {
		super.reshape( w, h );
	}
	
	public void simpleInitApp() {

		
		point = new PointLight();
		point.setEnabled( true );
		point.setColor( ColorRGBA.White.mult(4) );
		point.setRadius( 50 );
		rootNode.addLight( point );
		
		viewPort.setBackgroundColor( new ColorRGBA( 0.5f, 0.5f, 0.8f, 1 ) );
		
		sun = new DirectionalLight();
//		sun.setDirection(new Vector3f(-0.0f, -1f, -0f).normalizeLocal());
		sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f).normalizeLocal());
		sun.setColor(new ColorRGBA(1f, 0.95f, 0.99f, 1f));
		rootNode.addLight(sun);

	    renderManager.setPreferredLightMode(LightMode.SinglePass); // enable multiple lights
	    renderManager.setSinglePassLightBatchSize(16);
		
		ambient = new AmbientLight();
		rootNode.addLight(ambient);
		setAmbient( 0 );

		assetManager.registerLocator( new File(System.getProperty("user.home")).getParent(), FileLocator.class);

		setDisplayFps(false);
		setDisplayStatView(false);
		
		getFlyByCamera().setDragToRotate(true);

		debug = new Node( "dbg" );

		rootNode.attachChild( debug );
		rootNode.attachChild( skel );
		
		cam.setLocation( new Vector3f(19.421421f, 141.33781f, 153.73846f) );
		cam.setRotation( new Quaternion(0.13721414f, 0.8588392f, -0.30176666f, 0.39051798f) );
	
		setFov(0);
		setCameraSpeed( 0 );
		
		if ( SSAO ) {
			FilterPostProcessor fpp = new FilterPostProcessor( assetManager );
			SSAOFilter filter = new SSAOFilter( 0997847f, 1.440001f, 1.39999998f, 0 );
//			fpp.addFilter( new ColorOverlayFilter( ColorRGBA.Magenta ));
			fpp.addFilter( filter );
			fpp.addFilter( new FXAAFilter() );
			viewPort.addProcessor( fpp );
		}
		
		inputManager.addMapping(MOUSE_MOVE, new MouseAxisTrigger( MouseInput.AXIS_X, false ) );
		inputManager.addMapping(MOUSE_MOVE, new MouseAxisTrigger( MouseInput.AXIS_Y, false ) );
		inputManager.addMapping(MOUSE_MOVE, new MouseAxisTrigger( MouseInput.AXIS_X, true ) );
		inputManager.addMapping(MOUSE_MOVE, new MouseAxisTrigger( MouseInput.AXIS_Y, true ) );
		inputManager.addListener( moveListener, MOUSE_MOVE );
		
		inputManager.addMapping(CLICK, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
		inputManager.addListener(analogListener, CLICK);
		
		inputManager.addMapping( SPEED_UP, new KeyTrigger( KeyInput.KEY_UP ) );
		inputManager.addMapping( SPEED_DOWN, new KeyTrigger( KeyInput.KEY_DOWN ) );
		
		inputManager.addMapping( AMBIENT_UP, new KeyTrigger( KeyInput.KEY_RIGHT ) );
		inputManager.addMapping( AMBIENT_DOWN, new KeyTrigger( KeyInput.KEY_LEFT ) );
		
		inputManager.addMapping( FOV_UP, new KeyTrigger( KeyInput.KEY_PGUP ) );
		inputManager.addMapping( FOV_DOWN, new KeyTrigger( KeyInput.KEY_PGDN ) );
		
		inputManager.addMapping( TOGGLE_ORTHO, new KeyTrigger( KeyInput.KEY_O ) );
		
		inputManager.addListener( new com.jme3.input.controls.ActionListener() {
			
			@Override
			public void onAction( String name, boolean isPressed, float tpf ) {
				if (name == SPEED_UP)
					setCameraSpeed(+1);
				else
					setCameraSpeed(-1);
				
			}
		}, SPEED_UP, SPEED_DOWN );
		
		inputManager.addListener( new com.jme3.input.controls.ActionListener() {
			
			@Override
			public void onAction( String name, boolean isPressed, float tpf ) {
				
				if (!isPressed)
					return;
				
				if (name == AMBIENT_UP)
					setAmbient(+1);
				else
					setAmbient(-1);
			}
		}, AMBIENT_UP, AMBIENT_DOWN );
		
		inputManager.addListener( new com.jme3.input.controls.ActionListener() {
			
			@Override
			public void onAction( String name, boolean isPressed, float tpf ) {
				
				if (!isPressed)
					return;
				
				if (name == FOV_UP)
					setFov(+1);
				else
					setFov(-1);
			}
		}, FOV_UP, FOV_DOWN );
		
		inputManager.addListener( new com.jme3.input.controls.ActionListener() {
			

			@Override
			public void onAction( String name, boolean isPressed, float tpf ) {
				if ( isPressed ) {
					ortho = !ortho;
					setCameraPerspective();
				}
			}
		}, TOGGLE_ORTHO );
		
		Cylinder c = new Cylinder( 1, 32, 800, 1f, true );
		Geometry cg = new Geometry("floor", c);
		cg.setLocalRotation( new Quaternion().fromAngles( FastMath.HALF_PI, 0, 0 ) );
		cg.setMaterial( Jme3z.lit( Preview.this,  0.4, 0.6, 0.4 ) );
		rootNode.attachChild( cg );
	}
	
	private float fov = 0, cameraSpeed = 0, ambientS = 0.5f;
	private boolean  ortho = false;

	private void setCameraPerspective() {
		
		if ( ortho ) {

			cam.setParallelProjection( true );
			float frustumSize = fov*10 +100;
			float aspect = (float) cam.getWidth() / cam.getHeight();
			cam.setFrustum( -1000, 1000, -aspect * frustumSize, aspect * frustumSize, frustumSize, -frustumSize );
			
		} else {
			cam.setFrustumPerspective(  fov*10 +100, 16 / 9f, 0.1f, 1e3f );
			cam.setFrustumFar( 1e4f );
		}
	}

	private void setFov( int i ) {
		
		fov = Mathz.clamp( fov + i, -100, 100 );
		System.out.println("fov now " + fov);
		setCameraPerspective();
	}

	private void setCameraSpeed( int i ) {
		
		cameraSpeed = Mathz.clamp(cameraSpeed+i, -25, 3 );
		System.out.println("camera speed now " + cameraSpeed);
		getFlyByCamera().setMoveSpeed( (float) Math.pow (2, (cameraSpeed /2 ) + 8) );
	}
	
	private void setAmbient( int i ) {
		
		ambientS = Mathz.clamp( ambientS + i * 0.1f, 0, 2 );
		System.out.println("ambient now " + ambientS);
		ambient.setColor(ColorRGBA.White.mult( ambientS));
  		sun.setColor( new ColorRGBA(1f, 0.95f, 0.99f, 1f).mult( 2- ambientS) );
	}
	
	int oldWidth, oldHeight;
	
	public void simpleUpdate(float tpf) {
		
//		cam.setFrustumFar( 1e4f );
		
		if (oldWidth != cam.getWidth() || oldHeight != cam.getHeight()) {
			oldWidth = cam.getWidth();
			oldHeight = cam.getHeight();
		}
		
		
	}
	
	
	Vector3d cursorPosition;
	
	private AnalogListener moveListener = new AnalogListener() {
		@Override
		public void onAnalog( String name, float value, float tpf ) {
			CollisionResult cr = getClicked();

			Vector3f pos = null;
			
			if (cr != null) 
				pos = cr.getContactPoint();
	
			
			if (pos == null) {
				Vector3f dir = cam.getWorldCoordinates( getInputManager().getCursorPosition(), -10 );
				dir.subtractLocal( cam.getLocation() );
				new Ray( cam.getLocation(), dir ).intersectsWherePlane( new Plane(Jme3z.UP, 0), pos = new Vector3f() );
			}
			
			cursorPosition = Jme3z.from( pos );
			
			if (pos != null)
				point.setPosition( pos.add ( cam.getDirection().mult( -0.3f ) ));
		}
	};

	private AnalogListener analogListener = new AnalogListener() {
		public void onAnalog(String name, float intensity, float tpf) {
			if (name.equals(CLICK)) {

			}
		}
	};


	private CollisionResult getClicked() {
		
		CollisionResults results = new CollisionResults();
		Vector2f click2d = inputManager.getCursorPosition();
		Vector3f click3d = cam.getWorldCoordinates(
		    new Vector2f(click2d.x, click2d.y), 0f).clone();
		Vector3f dir = cam.getWorldCoordinates(
		    new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
		Ray ray = new Ray(click3d, dir);
		
		rootNode.collideWith(ray, results);
		
		if (results.size() > 0) 
			return results.getClosestCollision();
		else
			return null;
	}



	public void resetCamera() {
		cam.setLocation( new Vector3f() );
		cam.setRotation( new Quaternion() );
		setCameraSpeed( 0 );
		setCameraPerspective();
	}

	public boolean isPendingUpdate() {
		return false;
	}

}
