package org.twak.siteplan.jme;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Point3d;

import org.twak.siteplan.campskeleton.PlanSkeleton;
import org.twak.straightskeleton.Output;
import org.twak.straightskeleton.Skeleton;
import org.twak.straightskeleton.Output.Face;
import org.twak.utils.Arrayz;
import org.twak.utils.Loop;
import org.twak.utils.Loopable;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;

public class Preview extends SimpleApplication
{

	Node skel = new Node();
	
	public JPanel getPanel() {
		JPanel out = new JPanel(new BorderLayout());
		
		Dimension d3Dim = new Dimension (100, 640);
		
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
		
		startCanvas();
		
		out.add (canvas, BorderLayout.CENTER);
		
		rootNode.attachChild( skel );
		
		return out;
	}
	

	@Override
	public void simpleInitApp() {
		DirectionalLight sun = new DirectionalLight();
		sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f).normalizeLocal());
		sun.setColor(new ColorRGBA(1f, 0.95f, 0.99f, 1f));
		rootNode.addLight(sun);

		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.White.mult(0.3f));
		rootNode.addLight(al);

		assetManager.registerLocator("/home/twak/", FileLocator.class);

		// FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
		// SSAOFilter ssao = new SSAOFilter(0.49997783f, 42.598858f, 35.999966f,
		// 0.39299846f );
		// fpp.addFilter(ssao);
		// viewPort.addProcessor(fpp);

		setDisplayFps(false);
		setDisplayStatView(false);	
		
		getFlyByCamera().setDragToRotate(true);
		getFlyByCamera().setMoveSpeed(100);
		cam.setFrustumFar( 1e4f );
	}

    public synchronized void display( Output output, boolean showFaces, boolean showOther, boolean showCap )
    {
    	for (Spatial s : skel.getChildren())
    		s.removeFromParent();
    	
    	Geometry geom = new Geometry("mesh", createMesh( output ));
    	
    	Material mat = new Material( getAssetManager(), "Common/MatDefs/Light/Lighting.j3md" );
//		mat.setColor( "Diffuse", new ColorRGBA( randy.nextFloat(), randy.nextFloat(), randy.nextFloat(), 1f ) );

		geom.setMaterial( mat );
    	
    	rootNode.attachChild( geom );
    }

	private Mesh createMesh( Output output ) {
		List<Float> pos = new ArrayList<>(), norms = new ArrayList();
		List<Integer> ind = new ArrayList<>();

		if ( output.faces != null )
			f:
			for ( Face f : output.faces.values() ) {

				for ( Loop<Point3d> ll : f.getLoopL() ) {
					for (Loopable<Point3d> lll : ll.loopableIterator())
						if (lll.get().distance( lll.getNext().get() ) > 100)
							continue f;
				}
				
				for ( Loop<Point3d> ll : f.getLoopL() ) {
					org.twak.utils.Loopz.triangulate( ll, true, ind, pos, norms );
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
