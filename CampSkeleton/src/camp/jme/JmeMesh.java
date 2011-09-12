package camp.jme;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.math.Quaternion;
import com.jme.math.Triangle;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.TexCoords;
import com.jme.scene.TriMesh;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.geom.BufferUtils;
import com.jmex.font3d.math.TriangulationVertex;
import com.jmex.font3d.math.Triangulator;
import com.jmex.model.converters.ObjToJme;
import com.model.md5.importer.MD5Importer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import utils.LinearForm3D;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;

/**
 *
 * @author twak
 */
public class JmeMesh extends TriMesh
{
    static Map<String, TriMesh> cachedMeshes = new HashMap();

    static
    {
        Logger.getLogger(TriMesh.class .getName()).setLevel( Level.OFF ); // quiet please
    }

    public void setTransform (Matrix4d matrix)
    {
        Matrix4d m = new Matrix4d(matrix);
//        m.mul( Jme.transform );

        Quaternion q = new Quaternion();
        q=q.fromRotationMatrix( (float)m.m00, (float)m.m01, (float)m.m02,
                (float)m.m10, (float)m.m11, (float)m.m12,
                (float)m.m20, (float)m.m21, (float)m.m22 );
//        q=q.fromRotationMatrix( (float)m.m00, (float)m.m10, (float)m.m20, // transposed matrix
//                (float)m.m01, (float)m.m11, (float)m.m21,
//                (float)m.m02, (float)m.m12, (float)m.m22 );

//        setLocalScale( (float) m.getScale() * 30f ); // seems a better fit to scale all of these up
        Vector4d zero = new Vector4d();
        zero.w = 1;
        matrix.transform( zero );
        setLocalRotation( q );
        setLocalTranslation( (float)zero.x, (float) zero.y, (float) zero.z );
//        setLocalTranslation( (float)m.m03, (float) m.m13, (float) m.m23 );
    }

    /**
     * Instance mesh, ready for slicing, without hitting the disk
     */
    static void load( String name, TriMesh target, boolean clone )
    {
        TriMesh mesh = cachedMeshes.get( name );
        if ( mesh == null )
        {
            try
            {
                if ( name.toLowerCase().endsWith( "md5" ) )
                {
                    MD5Importer.getInstance().loadMesh( new File( name ).toURI().toURL(), name );
                    mesh = (TriMesh) MD5Importer.getInstance().getMD5Node().getMesh( 0 );
                }
                else if (name.toLowerCase().endsWith( "obj" ))
                {
                    ObjToJme o2j = new ObjToJme();
                    ByteArrayOutputStream BO = new ByteArrayOutputStream();
                    o2j.convert( new FileInputStream( new File (name)),BO  );
                    mesh = (TriMesh) BinaryImporter.getInstance().load( new ByteArrayInputStream( BO.toByteArray() ) );
                }
                cachedMeshes.put( name, mesh );
            }
            catch ( FileNotFoundException e )
            {
                e.printStackTrace();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }

//        List<TexCoords> coords = new ArrayList();
//        for (TexCoords tc : mesh.getTextureCoords())
//        {
//            coords.add( TexCoords.makeNew(tc.coords.array()) );
//        }

//        return mesh;

        ColorRGBA grey = new ColorRGBA (0.8f,0.8f,0.8f,1f);

        TexCoords tc = new TexCoords();

        if (clone)
        {
            target.reconstruct(
                BufferUtils.clone( mesh.getVertexBuffer() ),
                BufferUtils.clone( mesh.getNormalBuffer() ),
                createColor( grey, mesh.getVertexCount()),
                new TexCoords ( createCoords( mesh.getVertexCount() ) ), // no texture coords!
                BufferUtils.clone( mesh.getIndexBuffer() ) );
        }
        else
        {
            target.reconstruct(
                    mesh.getVertexBuffer(),
                    mesh.getNormalBuffer(),
                    createColor( grey, mesh.getVertexCount()),
                    new TexCoords ( createCoords( mesh.getVertexCount() ) ),
                    mesh.getIndexBuffer());
        }
        
        mesh.setModelBound( new OrientedBoundingBox() );
        mesh.setCullHint( CullHint.Never );
    }

    public static FloatBuffer createCoords (int count)
    {
        FloatBuffer out = BufferUtils.createFloatBuffer( count * 2);
        for (int i = 0; i < count; i++)
        {
            out.put( 0 );
            out.put( 1 );
        }
        return out;
    }

    public static FloatBuffer createColor (ColorRGBA color, int count)
    {
        FloatBuffer out = BufferUtils.createFloatBuffer( count * 4);
        for (int i = 0; i < count; i++)
        {
            out.put( color.r );
            out.put( color.g );
            out.put( color.b );
            out.put( color.a );
        }
        return out;
    }

    public JmeMesh( String name, boolean clone )
    {
        JmeMesh.load( name, this, clone );

//        slice( new LinearForm3D( new Vector3d( 0, 0, 0 ), new Vector3d( 0, 0, 0 ) ) );
    }

    static Color3f white = new Color3f( 1f,1f,1f);
//    public void slice ()
//    {
//
//        Point3d[] v0 = new Point3d [] {
//            new Point3d (0,0,0),
//            new Point3d (0,0,3),
//            new Point3d (2,0,1.5),
//            new Point3d (1,5,1.5)
//        };
//
//        int[] i0 = new int[] {
//            0,2,1,
//            0,1,3,
//            1,2,3,
//            2,0,3
//        };
//
//        Color3f[] c0 = new Color3f[v0.length];
//        for ( int i = 0; i < c0.length; i++ )
//            c0[i] = white;
//
//        Solid other = new Solid( v0, i0, c0 );
//
//        Vector3f[] vb = BufferUtils.getVector3Array( getVertexBuffer() );
//        Point3d[] verts = new Point3d [ vb.length ];
//        for (int i = 0; i < vb.length; i++)
//        {
//            Vector3f b = vb[i];
//            verts[i] = new Point3d (b.x, b.y, b.z);
//        }
//
//        int[] indices = BufferUtils.getIntArray( getIndexBuffer() );
//
//        Color3f[] cols = new Color3f[ verts.length ];
//        for (int i = 0; i < cols.length; i++)
//            cols[i] = white;
//
//
//        Solid solid = new Solid( verts, indices, cols);
//
//        BooleanModeller bm = new BooleanModeller( solid, other );
//        Solid s = bm.getIntersection();
//
//        Point3d[] ovb = s.getVertices();
//        FloatBuffer fb = BufferUtils.createFloatBuffer( ovb.length * 3 );
//        for (int i = 0; i < ovb.length; i++)
//        {
//            Point3d p = ovb[i];
//            fb.put( (float)p.x );
//            fb.put( (float)p.y );
//            fb.put( (float)p.z );
//        }
//
//        IntBuffer ib = BufferUtils.createIntBuffer( s.getIndices() );
//
//        reconstruct( fb, null, null, null, ib);
//    }

//    /**
//     * @param union with this object
//     * @return success?
//     */
//    public boolean slice( Solid s )
//    {
//        if (s.isEmpty())
//            return false;
//
//        Vector3f[] vb = BufferUtils.getVector3Array( getVertexBuffer() );
//        Point3d[] verts = new Point3d[vb.length];
//        for ( int i = 0; i < vb.length; i++ )
//        {
//            Vector3f b = vb[i];
//            verts[i] = new Point3d( b.x, b.y, b.z );
//        }
//
//        int[] indices = BufferUtils.getIntArray( getIndexBuffer() );
//
//        Color3f[] cols = new Color3f[ verts.length ];
//        for (int i = 0; i < cols.length; i++)
//            cols[i] = white;
//
//
//        Solid solid = new Solid( verts, indices, cols);
//
//        // do the boolean op
//
//        BooleanModeller bm = new BooleanModeller( solid, s );
//        s = bm.getIntersection();
//
//        // then rebuild from the
//
//        Point3d[] ovb = s.getVertices();
//
//        FloatBuffer fb = BufferUtils.createFloatBuffer( ovb.length * 3 );
//
//        for ( int i = 0; i < ovb.length; i++ )
//        {
//            Point3d p = ovb[i];
//            fb.put( (float) p.x );
//            fb.put( (float) p.y );
//            fb.put( (float) p.z );
//        }
//
//        int[] sindices = s.getIndices();
//        IntBuffer ib = BufferUtils.createIntBuffer( sindices );
//
//        FloatBuffer normals = BufferUtils.createFloatBuffer(  sindices.length*3  );
//        normals.rewind();
//        for (int i = 0; i < sindices.length; i+= 3)
//        {
//            int aR = sindices[i +0]*3;
//
//            Vector3d a = new Vector3d(
//                    fb.get( aR + 0 ),
//                    fb.get( aR + 1 ),
//                    fb.get( aR + 2 ) );
//
//            aR = s.getIndices()[i + 1]*3;
//            Vector3d b = new Vector3d(
//                    fb.get( aR + 0 ),
//                    fb.get( aR + 1 ),
//                    fb.get( aR + 2 ) );
//
//            aR = s.getIndices()[i + 2]*3;
//            Vector3d c = new Vector3d(
//                    fb.get( aR + 0 ),
//                    fb.get( aR + 1 ),
//                    fb.get( aR + 2 ) );
//
//            Triangle tri = new Triangle( Jme.toF( a ), Jme.toF( b ), Jme.toF( c ) );
//
//            a.sub( b);
//            c.sub( b );
//            a.cross( a, c );
//            a.normalize();
//
//            if (Double.isNaN( a.x + a.y +a.z ))
//                a = new Vector3d(0,1,0);
//
//            Vector3f tn = tri.getNormal();
//
//            for ( int d = 0; d < 3; d++ )
//            {
//                normals.put( tn.x );
//                normals.put( tn.y );
//                normals.put( tn.z );
////                normals.put( (float) a.x );
////                normals.put( (float) a.y );
////                normals.put( (float) a.z );
//            }
//        }
//
//        setNormalsMode( NormalsMode.Inherit );
//        normals.rewind();
//
//        reconstruct( fb, normals, null, null, ib );
////        reconstruct( fb, null, null, null, ib );
//
//        return true; // success!
//    }

    /**
     * Plane to be specified in the same coordinates as the mesh...
     * @param plane
     */
    public void slice( LinearForm3D plane )
    {
        getIndexBuffer().rewind();
        getNormalBuffer().rewind();
        getVertexBuffer().rewind();

        try
        {
            /**
             * Prototype mesh slicing code!:
             *
             */
            IntBuffer faces = getIndexBuffer();
            FloatBuffer norms = getNormalBuffer();
            FloatBuffer verts = getVertexBuffer();

            // index of the first vert of the triangle to remove
            List<Integer> facesToGo = new ArrayList();
            List<int[]> newTris = new ArrayList();
            List<Point3d> newPoints = new ArrayList();
            List<Vector3d> newNorms = new ArrayList();


            // for each triangle
            for ( int f = 0; f < faces.limit(); f += 3 )
            {
                // edge indices
                int[] ee = new int[]
                {
                    faces.get( f + 0 ), faces.get( f + 1 ), faces.get( f + 2 )
                };

                // get the three vertices
                Vector3d[] tri = new Vector3d[3];
                for ( int i = 0; i < 3; i++ )
                    tri[i] = new Vector3d( verts.get( ee[i] * 3 + 0 ), verts.get( ee[i] * 3 + 1 ), verts.get( ee[i] * 3 + 2 ) );

                List<Integer> pos = new ArrayList(), neg = new ArrayList();

                for ( int i = 0; i < 3; i++ )
                    if ( plane.inFront( tri[i] ) )
                        neg.add( i );
                    else
                        pos.add( i );

                if ( pos.size() == 3 )
                    continue;
                else if ( neg.size() == 3 )
                    facesToGo.add( f );
                else if ( pos.size() == 1 ) // one point on the side-to-keep
                {
                    Vector3d o1 = tri[pos.get( 0 )];
                    int n2Index = neg.get( 0 ),
                            n3Index = neg.get( 1 );

                    Vector3d o2 = tri[n2Index];
                    Vector3d o3 = tri[n3Index];

                    o2.sub( o1 );
                    o3.sub( o1 );

                    Point3d r2 = plane.collide( o1, o2 );
                    Point3d r3 = plane.collide( o1, o3 );


                    verts.put( ee[n2Index] * 3 + 0, (float) r2.x );
                    verts.put( ee[n2Index] * 3 + 1, (float) r2.y );
                    verts.put( ee[n2Index] * 3 + 2, (float) r2.z );

                    verts.put( ee[n3Index] * 3 + 0, (float) r3.x );
                    verts.put( ee[n3Index] * 3 + 1, (float) r3.y );
                    verts.put( ee[n3Index] * 3 + 2, (float) r3.z );
                }
                else if ( neg.size() == 1 ) // one point on the side-to-go
                {
                    // hack warning!
                    // we should split the triangle into a quad...but that means traversing a non-existent data structure.
                    // for now we'll just move the point to the boundary

                    int n1Index = neg.get( 0 );

                    Vector3d o1 = tri[n1Index],
                            o2 = tri[pos.get( 0 )],
                            o3 = tri[pos.get( 1 )];

//                        System.out.println( "o1 is " + o1 + " " + plane.inFront( o1 ) + " pd: " + plane.pointDistance( o1 ) );
//                        System.out.println( "o2 is " + o2 + " " + plane.inFront( o2 ) + " pd: " + plane.pointDistance( o2 ) );

                    Vector3d dir2 = new Vector3d( o2 );
                    dir2.sub( o1 );
                    Vector3d dir3 = new Vector3d( o3 );
                    dir3.sub( o1 );

                    Point3d r2 = plane.collide( o1, dir2 );
                    Point3d r3 = plane.collide( o1, dir3 );

//                        System.out.println( r2 + " dist is " + plane.pointDistance( r2 ) );
//
//                        System.out.println( " old was " + new Vector3d(
//                                verts.get( ee[n1Index] * 3 + 0 ),
//                                verts.get( ee[n1Index] * 3 + 1 ),
//                                verts.get( ee[n1Index] * 3 + 2 ) ) );

                    int p1Indx = verts.limit() / 3 + newPoints.size();


                    int[] t1 = new int[3];
                    t1[pos.get( 0 )] = ee[pos.get( 0 )];
                    t1[pos.get( 1 )] = ee[pos.get( 1 )];
                    t1[neg.get( 0 )] = p1Indx;
                    newTris.add( t1 );

                    int[] t2 = new int[3];
                    t2[pos.get( 0 )] = ee[pos.get( 1 )];
                    t2[pos.get( 1 )] = p1Indx + 1;
                    t2[neg.get( 0 )] = p1Indx;
                    newTris.add( t2 );



//                        newTris.add( new int[] {  p1Indx+1, p1Indx, ee [pos.get( 0 )] } );

//                        newTris.add( new int[] { ee[0], ee[1], ee[2] } );

                    newPoints.add( r2 );
                    newPoints.add( r3 );

                    for ( int i = 0; i < 2; i++ ) // one norm for each new vertex
                        newNorms.add( new Vector3d(
                                norms.get( ee[n1Index] * 3 + 0 ),
                                norms.get( ee[n1Index] * 3 + 1 ),
                                norms.get( ee[n1Index] * 3 + 2 ) ) );

//                        verts.put( ee[n1Index] * 3 + 0, (float) r2.x );
//                        verts.put( ee[n1Index] * 3 + 1, (float) r2.y );
//                        verts.put( ee[n1Index] * 3 + 2, (float) r2.z );

                    facesToGo.add( f );
                }
                else
                    throw new Error( "Broken! - non three sided triangle?" );
            }

            FloatBuffer tmpVerts = BufferUtils.createFloatBuffer( verts.limit() + newPoints.size() * 3 );
            FloatBuffer tmpNorms = BufferUtils.createFloatBuffer( norms.limit() + newNorms.size() * 3 );

            tmpVerts.put( verts ); 
            tmpNorms.put( norms );

            verts = tmpVerts;
            norms = tmpNorms;

            for ( Point3d p : newPoints ) //verts.position()
            {
                tmpVerts.put( (float) p.x );
                tmpVerts.put( (float) p.y );
                tmpVerts.put( (float) p.z );
            }

            for ( Vector3d v : newNorms )
            {
                tmpNorms.put( (float) v.x );
                tmpNorms.put( (float) v.y );
                tmpNorms.put( (float) v.z );
            }

            IntBuffer foo = BufferUtils.createIntBuffer( faces.limit() + newTris.size() * 3 );
            foo.put( faces );
            faces = foo;

            for ( int[] vals : newTris )
                for ( int i : vals )
                    foo.put( i );

            // finally remove those unwanted faces (todo: remove from vertex array as well)
            for ( int i : facesToGo )
            {
                faces.put( i + 0, 0 );
                faces.put( i + 1, 0 );
                faces.put( i + 2, 0 );
            }

            reconstruct( verts, norms, null, null, faces );

            updateGeometricState( 0, true );

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static TriMesh toMesh(LoopL<Point2d> in) {
        Triangulator tri = new Triangulator();

        try {
            List<Point3d> allVerts = new ArrayList();
            Map<Point3d, Integer> verts = new HashMap();

            FloatBuffer vBuffer = BufferUtils.createFloatBuffer(in.count() * 3);

            for (Point2d b : in.eIterator()) {
                TriangulationVertex tv = tri.addVertex(new Vector3f((float) b.x, (float) b.y, 0f));
//                TriangulationVertex tv = tri.addVertex(new Vector3f((float) (b.x+Math.random()), (float) (b.y+Math.random()), 0f));
                Point3d key = new Point3d(b.x, 0, b.y);
                allVerts.add(key);
                verts.put(key, tv.getIndex());
                vBuffer.put(new float[]{(float) b.x, 0f, (float) b.y});
            }

            for (Loop<Point2d> loop : in) {
                for (Loopable<Point2d> b : loop.loopableIterator()) {
                    tri.addEdge(
                            verts.get(
                            new Point3d(b.get().x, 0, b.get().y)),
                            verts.get(
                            new Point3d(b.getNext().get().x, 0, b.getNext().get().y)));
                }
            }

            if (allVerts.size() == 0) {
                return null;
            }

            return new TriMesh("filled poly", vBuffer, null, null, null, tri.triangulate());
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

//    public static Solid extrudeSolid (LoopL<Point2d> in, double height)
//    {
//        Triangulator tri = new Triangulator();
//
//        List<Point3d> allVerts = new ArrayList();
//
//        Map<Point3d, Integer> verts = new HashMap();
//        for (Point2d b : in.eIterator())
//        {
//            TriangulationVertex tv = tri.addVertex( new Vector3f ((float)b.x, (float)b.y, 0f) );
//            Point3d key = new Point3d ( b.x, 0,b.y);
//            allVerts.add( key );
//            verts.put( key , tv.getIndex() );
//        }
//
//        if (allVerts.size() == 0)
//            return new Solid(); //empty!
//
//        for (Loop<Point2d> loop : in)
//            for (Loopable<Point2d> b : loop.loopableIterator())
//                tri.addEdge( verts.get(
//                new Point3d ( b.get().x, 0, b.get().y)), verts.get(
//                new Point3d ( b.getNext().get().x, 0, b.getNext().get().y)));
//
//        List<Integer> v0 = new ArrayList (  );
//        int[] bottomIndices = BufferUtils.getIntArray( tri.triangulate() );
//        for (int i = bottomIndices.length-1; i >=0; i-- )
//            v0.add( bottomIndices[i] );
//
//        int offset = allVerts.size();
//        for (Point3d p : new ArrayList<Point3d>(allVerts))
//        {
//            Point3d key = new Point3d( p.x, height, p.z );
//            allVerts.add( key );
//            verts.put( key, offset + verts.get( p ) );
//        }
//
//        List<Integer> i1 = new ArrayList(v0);
//        for (int i = i1.size() -1 ; i >= 0; i--) // reverse faces for other end cap
//            v0.add( v0.get(i)+offset );
//
//        // now add in a strip around the edge
//        for ( Loop<Point2d> loop : in )
//            for ( Loopable<Point2d> r : loop.loopableIterator() )
//            {
//                int a = verts.get( new Point3d( r.get().x, 0, r.get().y ) );
//                int b = verts.get( new Point3d( r.getNext().get().x, 0, r.getNext().get().y ) );
//                int c = verts.get( new Point3d( r.get().x, height, r.get().y ) );
//                int d = verts.get( new Point3d( r.getNext().get().x, height, r.getNext().get().y ) );
//
//                v0.addAll( Arrays.asList( a, c, b, d, b, c ) );
//            }
//
//        int[] v0_ = new int[v0.size()];
//        for (int i = 0; i < v0.size(); i++)
//            v0_[i]= v0.get( i );
//
//        Color3f white = new Color3f (1f,1f,1f);
//        Color3f[] cols = new Color3f[allVerts.size() ];
//        for (int i = 0; i < allVerts.size(); i++)
//            cols[i] = white;
//
//        return new Solid( allVerts.toArray( new Point3d[1] ),v0_, cols );
//    }
}
