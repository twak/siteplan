package org.twak.siteplan.jme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
//import javax.vecmath.Vector3f;
import javax.vecmath.Vector3d;

import org.twak.utils.collections.Arrayz;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;
import org.twak.utils.collections.Loopz;
import org.twak.utils.geom.Line3d;
import org.twak.utils.geom.ObjDump;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Box;

public class Jme3z {

	public static final Vector3f UP = new Vector3f(0,1,0);

	public static Matrix4f toJme( Matrix4d m ) {
		return new Matrix4f( (float) m.m00, (float) m.m01, (float) m.m02, (float) m.m03, (float) m.m10, (float) m.m11, (float) m.m12, (float) m.m13, (float) m.m20, (float) m.m21, (float) m.m22, (float) m.m23, (float) m.m30, (float) m.m31, (float) m.m32, (float) m.m33 );
	}

	public static Transform toJmeTransform( Matrix4d m ) {
		Transform out = new Transform();
		out.fromTransformMatrix( toJme( m ) );
		return out;
	}

	public static Matrix4d fromMatrix( Matrix4f m ) {

		return new Matrix4d( m.m00, m.m01, m.m02, m.m03, m.m10, m.m11, m.m12, m.m13, m.m20, m.m21, m.m22, m.m23, m.m30, m.m31, m.m32, m.m33 );
	}

	public static Mesh fromLoop( Loop<Point3d> in ) {

		if ( !in.holes.isEmpty() ) {
			LoopL<Point3d> res =new LoopL<Point3d>( in );
			in = res.isEmpty() ? in : res.get( 0 );
		}

		Mesh m = new Mesh();

		m.setMode( Mesh.Mode.Triangles );

		List<Integer> inds = new ArrayList<>();
		List<Float> pos = new ArrayList<>();
		List<Float> norms = new ArrayList<>();

		Loopz.triangulate( in, false, inds, pos, norms );

		m.setBuffer( Type.Index, 3, Arrayz.toIntArray( inds ) );
		m.setBuffer( Type.Position, 3, Arrayz.toFloatArray( pos ) );
		m.setBuffer( Type.Normal, 3, Arrayz.toFloatArray( norms ) );

		return m;
	}

	public static Spatial fromLoop( AssetManager am, LoopL<Point2d> gis, double h ) {

		Node out = new Node();

		for ( Loop<Point2d> loop : gis ) {
			Mesh m = new Mesh();

			m.setMode( Mesh.Mode.Lines );

			List<Float> coords = new ArrayList<>();
			List<Integer> inds = new ArrayList<>();

			for ( Loopable<Point2d> ll : loop.loopableIterator() ) {

				inds.add( inds.size() );
				inds.add( inds.size() );

				Point3d a = new Point3d( ll.get().x, h, ll.get().y ), b = new Point3d( ll.getNext().get().x, h, ll.getNext().get().y );

				coords.add( (float) a.x );
				coords.add( (float) a.y );
				coords.add( (float) a.z );
				coords.add( (float) b.x );
				coords.add( (float) b.y );
				coords.add( (float) b.z );

				{
					Box box1 = new Box( 0.5f, 0.5f, 0.5f );
					Geometry geom = new Geometry( "Box", box1 );
					Material mat1 = new Material( am, "Common/MatDefs/Misc/Unshaded.j3md" );
					mat1.setColor( "Color", ColorRGBA.Magenta );
					geom.setMaterial( mat1 );
					geom.setLocalTranslation( Jme3z.toJmeVec( a ) );
					out.attachChild( geom );
				}

			}

			m.setBuffer( VertexBuffer.Type.Position, 3, Arrayz.toFloatArray( coords ) );
			m.setBuffer( VertexBuffer.Type.Index, 2, Arrayz.toIntArray( inds ) );

			Geometry geom = new Geometry( "profile", m );

			Material lineMaterial = new Material( am, "Common/MatDefs/Misc/Unshaded.j3md" );

			lineMaterial.getAdditionalRenderState().setLineWidth( 3 );

			lineMaterial.setColor( "Color", ColorRGBA.Pink );
			geom.setMaterial( lineMaterial );

			out.attachChild( geom );
		}

		return out;
	}

	public static com.jme3.math.Vector3f toJmeVec( Tuple3d a ) {
		return new com.jme3.math.Vector3f( (float) a.x, (float) a.y, (float) a.z );
	}

	public static Spatial lines( AssetManager am, List<Line3d> roofLines, ColorRGBA color, float width ) {

		Mesh m = new Mesh();

		m.setMode( Mesh.Mode.Lines );

		List<Float> coords = new ArrayList<>();
		List<Integer> inds = new ArrayList<>();

		for ( Line3d line : roofLines ) {

			inds.add( inds.size() );
			inds.add( inds.size() );

			coords.add( (float) line.start.x );
			coords.add( (float) line.start.y );
			coords.add( (float) line.start.z );
			coords.add( (float) line.end.x );
			coords.add( (float) line.end.y );
			coords.add( (float) line.end.z );

		}

		m.setBuffer( VertexBuffer.Type.Position, 3, Arrayz.toFloatArray( coords ) );
		m.setBuffer( VertexBuffer.Type.Index, 2, Arrayz.toIntArray( inds ) );

		Geometry geom = new Geometry( "jmez lines", m );

		Material lineMaterial = new Material( am, "Common/MatDefs/Misc/Unshaded.j3md" );

		lineMaterial.getAdditionalRenderState().setLineWidth( width );

		lineMaterial.setColor( "Color", color == null ? ColorRGBA.Pink : color );
		geom.setMaterial( lineMaterial );

		
		geom.updateGeometricState();
		geom.updateModelBound();
		
		return geom;
	}

	public static void toObj( Mesh m, ObjDump dump, Transform transform ) {

		float[][] verts = new float[3][3];
		Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f();

		Matrix4f mat = transform.toTransformMatrix();
		
		for ( int t = 0; t < m.getTriangleCount(); t++ ) {

			try {

				m.getTriangle( t, a, b, c );

				a=mat.mult(a);
				b=mat.mult(b);
				c=mat.mult(c);
				
				verts[ 0 ][ 0 ] = a.x;
				verts[ 0 ][ 1 ] = a.y;
				verts[ 0 ][ 2 ] = a.z;
				verts[ 1 ][ 0 ] = b.x;
				verts[ 1 ][ 1 ] = b.y;
				verts[ 1 ][ 2 ] = b.z;
				verts[ 2 ][ 0 ] = c.x;
				verts[ 2 ][ 1 ] = c.y;
				verts[ 2 ][ 2 ] = c.z;

				dump.addFace( verts, null, null );
			} catch ( Throwable th ) {
				th.printStackTrace();
			}

		}
	}

	
	public static String MAT_KEY = "material";
	
	public static void dump( ObjDump dump, Spatial spat, int i ) {
		
		if ( spat instanceof Node ) {
//			int c = 0;
			for ( Spatial s : ( (Node) spat ).getChildren() ) {
				
				Color color = null;
				if (s instanceof Geometry) {
					MatParam mp =  ( (Geometry) s ).getMaterial().getParam( "Diffuse" );
					if (mp != null) {
						ColorRGBA gCol = (ColorRGBA) mp.getValue();
						if (gCol != null)
							color = new Color(gCol.getRed(), gCol.getGreen(), gCol.getBlue());
					}
				}
				
//				if (i >= 0)
//					color = Rainbow.getColour( c++ );
				
				if (color != null)
					dump.setCurrentMaterial( s.getUserData( MAT_KEY ), color, 0.2 );
				
				dump( dump, s, i+1 );
			}
		}
		else if ( spat instanceof Geometry ) {
			Mesh m = ( (Geometry) spat ).getMesh();
			
			if (m.getMode() == Mode.Lines || m.getMode() == Mode.LineLoop || m.getMode() == Mode.LineStrip )
				return;
			
			Jme3z.toObj( m, dump, ((Geometry) spat).getLocalTransform() );
		}
	}
	
	public static Vector3f to( Tuple3d l ) {
		return new Vector3f( (float) l.x, (float) l.y, (float) l.z );
	}
	
	public static Vector3f toJme( javax.vecmath.Vector3f l ) {
		return new Vector3f( l.x, l.y, l.z);
	}

	public static Vector3f toJmeV( double x, double y, double z ) {
		return new Vector3f( (float)x, (float) y, (float) z );
	}

	public static Vector3d from( com.jme3.math.Vector3f dir ) {
		return new Vector3d( dir.x, dir.y, dir.z);
	}

	public static void removeAllChildren( Node debug ) {
		for (Spatial s : debug.getChildren() )
			s.removeFromParent();
	}

	public static ColorRGBA toJme(Color c) {
		return new ColorRGBA( c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f );
	}

	public static Material lit( SimpleApplication sa, double r, double g, double b ) {

		Material mat = new Material( sa.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md" );
		mat.setColor( "Diffuse", new ColorRGBA( (float) r, (float) g, (float) b, 1 ) );
		mat.setColor( "Ambient", new ColorRGBA( (float) r, (float) g, (float) b, 1 ) );
		mat.setBoolean( "UseMaterialColors", true );
		return mat;
	}
}
