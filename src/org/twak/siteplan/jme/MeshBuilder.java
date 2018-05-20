package org.twak.siteplan.jme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import org.twak.utils.Mathz;
import org.twak.utils.collections.Arrayz;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.collections.Loopable;
import org.twak.utils.collections.Loopz;
import org.twak.utils.geom.DRectangle;
import org.twak.utils.geom.Line3d;
import org.twak.utils.triangulate.EarCutTriangulator;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

public class MeshBuilder {
	
	public List<Vector3f> verts = new ArrayList<>(), norms = new ArrayList<>();
	public List<Vector2f> uvs = null;
	public List<Integer> inds = new ArrayList<>();

	private static final short[] GEOMETRY_INDICES_DATA = {
			2,  1,  0,  3,  2,  0, // back
			6,  5,  4,  7,  6,  4, // right
			10,  9,  8, 11, 10,  8, // front
			14, 13, 12, 15, 14, 12, // left
			18, 17, 16, 19, 18, 16, // top
			22, 21, 20, 23, 22, 20  // bottom
	};
	
	private static final int[] CORNER_INDICES_DATA = new int[] {
			0,1,2,3,    // left + (1,0)
			1,4,6,2,    // top + (0,1)
			4,5,7,6,    // right + (-1,0)
			5,0,3,7,    // bottom + (0,-1)
			2,6,7,3,    // back
			0,5,4,1     // front
	};
	
	private static final int[] NORMAL_INDICES_DATA = new int[] {
			5, 5, 5, 5,
			0, 0, 0, 0,
			2, 2, 2, 2,
			3, 3, 3, 3,
			1, 1, 1, 1,
			4, 4, 4, 4
	};
	
	
	public MeshBuilder ensureUVs() {
		if (uvs != null)
			return this;
		
		if (!verts.isEmpty() )
			throw new Error("no uvs on earlier data :(");
		
		uvs = new ArrayList<>();
		
		return this;
	}

	public void addCube (Vector3f corner, Vector3f up, Vector3f along, Vector3f in, float upL, float alongL, float inL ) {
	
        Vector3f[] axes = {
                along.mult( alongL ),
                up   .mult( upL    ),
                in   .mult( inL    )
        };
        
        Vector3f[] v = new Vector3f[] {
        		corner,
        		corner.add(axes[0]),
        		corner.add(axes[0]).addLocal(axes[1]),
        		corner.add(axes[1]),
        		corner.add(axes[0]).addLocal(axes[2]),
        		corner.add(axes[2]),
                corner.add(axes[0]).addLocal(axes[1]).addLocal(axes[2]),
                corner.add(axes[1]).addLocal(axes[2])
        };
        
        Vector3f[] n = new Vector3f[] {
        		axes[0],
        		axes[1],
        		axes[2],
        		axes[0].mult( -1 ),
        		axes[1].mult( -1 ),
        		axes[2].mult( -1 ),
        };
        
        
        int offset = verts.size();
        
        for (int c : CORNER_INDICES_DATA) 
        	verts.add(v[c]);
        	
        for (int m : NORMAL_INDICES_DATA)
        	norms.add(n[m]);
        
		for (int i : GEOMETRY_INDICES_DATA)
			inds.add( offset + i );
	}
	
	public void addCube (Tuple3d corner, Tuple3d up, Tuple3d along, double upL, double alongL, double inL ) {
		Vector3d in = new Vector3d();
		in.cross( new Vector3d(up), new Vector3d(along) );
		addCube (
				Jme3z.to( corner ), Jme3z.to( up ).normalizeLocal(), 
				Jme3z.to( along ).normalizeLocal(), Jme3z.to( in ).normalizeLocal(), 
				(float) upL, (float) alongL, (float) inL );
	}

	public void solidLine( Line3d oLine, double d ) {

		Vector3d along = oLine.dir();
		
		Point3d start = new Point3d ( oLine.start );
		
		Vector3d tmp = new Vector3d( Mathz.Y_UP );
		tmp.scale( d/2 );
		start.add( tmp );
		tmp.cross( Mathz.Y_UP, along );
		tmp.scale( -d / ( 2 * tmp.length()  ) );
		start.add( tmp );
		
		addCube ( start , Mathz.Y_UP, along, -d, oLine.length(), d);
		
	}
	
	public void addInsideRect (
			Vector3f corner, Vector3f up, Vector3f along, Vector3f in, 
			float upL, float alongL, float inL,
			float[][] uv, boolean hasBack ) {
		
		Vector3f[] axes = {
				along.mult( alongL ),
				up   .mult( upL    ),
				in   .mult( inL    )
		};
		
		Vector3f[] v = new Vector3f[] {
				corner,
				corner.add(axes[0]),
				corner.add(axes[0]).addLocal(axes[1]),
				corner.add(axes[1]),
				corner.add(axes[0]).addLocal(axes[2]),
				corner.add(axes[2]),
				corner.add(axes[0]).addLocal(axes[1]).addLocal(axes[2]),
				corner.add(axes[1]).addLocal(axes[2])
		};
		
		Vector3f[] n = new Vector3f[] {
				axes[0].mult( -1 ),
				axes[1].mult( -1 ),
				axes[2].mult( -1 ),
				axes[0],
				axes[1],
				axes[2],
		};
		
		if (upL > 0) 
		{
			for (Vector3f vv : n)
				vv.multLocal( -1 );
		}
		
		Vector2f[] u = null;
		
		if (uv != null) {
			
			Vector2f s = new Vector2f(uv[0][0], uv[0][1]);
			Vector2f e = new Vector2f(uv[1][0], uv[1][1]);
			
			float io = 0f, oo = 0;
			
			u = new Vector2f[] {
					
				new Vector2f( s.x + oo, s.y + oo ), 
				new Vector2f( e.x + oo, s.y + io ),
				new Vector2f( e.x + oo, s.y + oo ),
				new Vector2f( s.x + oo, s.y + oo ),
				
				new Vector2f( e.x + io, e.y + io ), // "in"
				new Vector2f( s.x + io, e.y + io ),
				new Vector2f( e.x + io, e.y + io ),
				new Vector2f( s.x + io, e.y + io ),
				
//				new Vector2f( s.x + oo, s.y + oo ), 
//				new Vector2f( e.x + oo, s.y + io ),
//				new Vector2f( e.x + oo, s.y + oo ),
//				new Vector2f( s.x + oo, s.y + oo ),
//				
//				new Vector2f( e.x + io, e.y + io ), // "in"
//				new Vector2f( s.x + io, e.y + io ),
//				new Vector2f( e.x + io, e.y + io ),
//				new Vector2f( s.x + io, e.y + io ),
			};
		}
		
		int offset = verts.size();
		
		for (int i = 0; i < CORNER_INDICES_DATA.length-(hasBack ? 4 : 8); i++)
			verts.add( v[ CORNER_INDICES_DATA[ i ] ] );
		
		if (uv != null) {
			ensureUVs();
			for (int i = 0; i < CORNER_INDICES_DATA.length -(hasBack ? 4 : 8); i++) {
				Vector2f coord = new Vector2f ( u[ CORNER_INDICES_DATA[ i ] ] );
				
				if ( i < CORNER_INDICES_DATA.length -8 ) /* mirror uvs if not back */
					if (i % 4 >= 2) /*inner coords */
						if ( (i / 4) % 2 == 1) 
							coord.x += (( i < 8 ) ? -1 /*left*/ : 1 /*right*/ ) * upL * (uv[1][0] - uv[0][0]) / alongL;
						else
							coord.y += (( i < 8 ) ? 1 /*bottom*/ : -1 /*top*/ ) * upL * (uv[1][1] - uv[0][1]) / inL;
				
				uvs.add( coord );
			}
		}
		
		for (int i = 0; i < NORMAL_INDICES_DATA.length -(hasBack ? 4 : 8); i++)
			norms.add( n[ NORMAL_INDICES_DATA[ i ] ] );
		
		for (int j = 0; j < GEOMETRY_INDICES_DATA.length / 3 - (hasBack ? 2 : 4); j++)
			for (int i = 2; i >=0 ; i--) { 
				inds.add (offset + GEOMETRY_INDICES_DATA[j * 3 + i]);
			}
	}
	
	public void add (LoopL<? extends Point2d> flat, Matrix4d to3d) {
		LoopL<Point3d> td = Loopz.transform( Loopz.to3d( flat, 0, 1 ), to3d );
		add( td, null, true );
	}
	
	public void add ( LoopL<? extends Point2d> flat, LoopL<Point2d> uvs, Matrix4d to3d) {
		LoopL<Point3d> td = Loopz.transform( Loopz.to3d( flat, 0, 1 ), to3d );
		add( td, uvs, true );
	}
	
	public void add ( LoopL<? extends Point2d> flat, LoopL<Point2d> uvs, Matrix4d to3d, double depth) {
		LoopL<Point3d> td = Loopz.transform( Loopz.to3d( flat, depth, 1 ), to3d );
		add( td, uvs, true );
	}
	
	public void add3d (LoopL<? extends Point3d> ll, Matrix4d to3d) {
		LoopL<Point3d> td = Loopz.transform( ll, to3d );
		add( td, null, true );
	}

	public void add( DRectangle dRectangle, Matrix4d to3d ) {
		add( dRectangle, null, to3d );
	}
	
	public void add( DRectangle dRectangle, DRectangle uvs, Matrix4d to3d ) {
		add (dRectangle, uvs, to3d, 0);
	}
	
	public void add( DRectangle dRectangle, DRectangle uvs, Matrix4d to3d, double depth ) {
		
		if (uvs != null)
			ensureUVs();
		
		LoopL<Point2d> flat = new LoopL<>(), uvFlat = null;
		Loop<Point2d> loop = flat.loop(), uvLoop = null;
		Point2d[] points = dRectangle.points(), uvPts = null;
		
		if (uvs != null) {
			uvFlat = new LoopL<>();
			uvLoop = uvFlat.loop();
			uvPts = uvs.points();
		}
		
		for (int i = 0; i < 4; i++) {
			loop.append( points[i] );
			if (uvs != null)
				uvLoop.append( uvPts[i] );
		}
		
		add(flat, uvFlat, to3d, depth);
	}
	
	public void add( Point3d ...pts  ) {
		add( new Loop<Point3d> (pts).singleton(), null, true );
	}
	public void add( 
			LoopL<? extends Point3d> loopl, 
			LoopL<Point2d> uvl, 
			boolean reverseTriangles ) {
		
		fixForTriangulator( loopl, uvl );
		
		if (uvl != null)
			ensureUVs();
		
		if (loopl.count() <= 2)
			return;
		
		for (int li = 0; li < loopl.size(); li++) {

			Loop<? extends Point3d> loop = loopl.get( li );
			Loop<Point2d> uvLoop = uvl != null ? uvl.get( li ) : null;
			
			if (loop.start == null)
				continue;
			
			List<Float> 
					pos = new ArrayList<>(),
					u = new ArrayList<>();
			
			List<Integer> ids = new ArrayList<>();

			Vector3d normal = new Vector3d();

			int[] order = reverseTriangles ? new int[] { 2, 1, 0 } : new int[] { 0, 1, 2 };

			
			Iterator<Loopable<Point2d>> uvIt = (uvLoop == null) ? null : uvLoop.loopableIterator().iterator();
			
			for ( Loopable<? extends Point3d> pt : loop.loopableIterator() ) {

				
				ids.add( ids.size() );

				Point3d p = pt.get();

				pos.add( (float) p.x );
				pos.add( (float) p.y );
				pos.add( (float) p.z );

				Vector3d l = new Vector3d( pt.get() );
				l.sub( pt.getPrev().get() );

				Vector3d n = new Vector3d( pt.getNext().get() );
				n.sub( pt.get() );
				if ( l.lengthSquared() > 0 && n.lengthSquared() > 0 ) {
					l.normalize();
					n.normalize();

					l.cross( l, n );
					normal.add( l );
				}
				
				Point2d uvd = null;
				if (uvIt != null) {
					uvd = uvIt.next().get();
					u.add ( (float) uvd.x );
					u.add ( (float) uvd.y );
				}
			}

			float[] n = new float[] { (float) normal.x, (float) normal.y, (float) normal.z };
			float[] p = Arrayz.toFloatArray( pos );
			int[] i = Arrayz.toIntArray( ids ), ti = new int[i.length * 3];

			int tris = EarCutTriangulator.triangulateConcavePolygon( p, 0, ids.size(), i, ti, n );

			if ( tris > 0 ) {
				//			ti = Arrays.copyOf( ti, tris * 3 );

				int offset = verts.size();

				for ( int j = 0; j < tris * 3; j += 3 ) {
					this.inds.add(ti[j+order[0]] + offset);
					this.inds.add(ti[j+order[1]] + offset);
					this.inds.add(ti[j+order[2]] + offset);
				}

				for ( int j = 0; j < pos.size() / 3; j ++ ) {
					
					int pi = j * 3;
					verts.add( new Vector3f( pos.get( pi + 0 ), pos.get( pi + 1 ), pos.get( pi + 2 ) ) );
					norms.add( new Vector3f( -n[ 0 ], -n[ 1 ], -n[ 2 ] ) );
					if ( uvl != null) {
						int ui = j * 2;
						uvs.add( new Vector2f( u.get( ui + 0 ), u.get( ui + 1 ) ) );
					}
				}
			}
		}
	}

	private void fixForTriangulator( 
			LoopL<? extends Point3d> loopl,
			LoopL<? extends Point2d> uvl 
		) {
		
		for (int i = 0; i < loopl.size(); i++) {
			
			Loop<? extends Point3d> loop = loopl.get( i );
			Loop<? extends Point2d> uvLoop = null;
			
			if (uvl != null)
				uvLoop = uvl.get( i );
			
			Loopable<? extends Point3d> start = loop.start, current = start;
			Loopable<? extends Point2d> uvCurrent = null;
			
			if (uvl != null)
				uvCurrent = uvLoop.start;
			
			if (current == null)
				continue;
			
			do
			{
				Vector3d a = new Vector3d( current.getNext().get() ),
						 b = new Vector3d( current.get() );
				
				a.sub (current.get());
				b.sub (current.getPrev().get());
				
				if ( current.getNext() == current ) {
					loop.start = null;
					
					if (uvLoop != null)
						uvLoop.start = null;
					
					break;
				}
					if ( a.lengthSquared() == 0 ||
						 a.angle( b ) > Math.PI - 0.001) {
						
						loop.remove( (Loopable) current );
						start = current = current.getPrev();
						
						if (uvCurrent != null) {
							uvLoop.remove( (Loopable ) uvCurrent );
							uvCurrent = uvCurrent.getPrev();
						}
					}
				
				current = current.next;
				
				if (uvCurrent != null)
					uvCurrent = uvCurrent.next;
				
			} while (current != start);
			
		}
		
//		new Plot(loopl);
	}

	
	public Mesh getMesh() {
		
		Mesh mesh = new Mesh();
		
		mesh.setMode( Mode.Triangles );
		
		mesh.setBuffer( VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(  verts.toArray( new Vector3f[verts.size()] ) ) );
		mesh.setBuffer( VertexBuffer.Type.Normal  , 3, BufferUtils.createFloatBuffer( norms.toArray( new Vector3f[norms.size()] ) ) );
		mesh.setBuffer( VertexBuffer.Type.Index   , 3, BufferUtils.createIntBuffer( Arrayz.toIntArray( inds ) ) );
		
		if (uvs != null)
			mesh.setBuffer( VertexBuffer.Type.TexCoord   , 2, BufferUtils.createFloatBuffer( uvs.toArray( new Vector2f[uvs.size()] ) ) );
		
		mesh.updateBound();
		
		return mesh;
	}
}