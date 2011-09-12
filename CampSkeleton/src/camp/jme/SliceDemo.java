/*
 * Copyright (c) 2003-2009 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package camp.jme;

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
import com.jme.scene.TexCoords;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Capsule;
import com.jme.scene.state.CullState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jme.util.geom.BufferUtils;
import com.jmex.font3d.math.TriangulationVertex;
import com.jmex.font3d.math.Triangulator;
import com.model.md5.importer.MD5Importer;
import com.model.md5.interfaces.mesh.IMesh;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import utils.LinearForm3D;
import utils.Loop;
import utils.LoopL;

/**
 * @author Joshua Slack
 */
public class SliceDemo extends SimpleGame {

    private Quaternion rotQuat = new Quaternion();
    private float angle = 0;
    private Vector3f axis = new Vector3f(1, 1, 0).normalizeLocal();
    private Capsule t;

    public static void main( String[] args )
    {
        SliceDemo app = new SliceDemo();
        app.start();
    }

    public SliceDemo()
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

        checkForUpdate();
    }

    protected void getAttributes()
    {
        settings = getNewSettings();
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

        rootNode.attachChild(t);

        TextureState ts = display.getRenderer().createTextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.loadTexture(SliceDemo.class
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

         checkForUpdate();
    }

    public synchronized void checkForUpdate()
    {
            /**
             * Synchornize this!
             */
//            List<Spatial> children = rootNode.getChildren();
            if ( current != null )
                rootNode.detachChild( current );
            current = toShow;
            rootNode.attachChild( current );

            toShow = null;


            try
            {
//                InputStream statue = null;
//                statue = new FileInputStream( "test.obj" );
//
//                ObjToJme o2j = new ObjToJme();
//                ByteArrayOutputStream BO = new ByteArrayOutputStream();
//                o2j.convert( statue, BO );
//                TriMesh model = (TriMesh) BinaryImporter.getInstance().load( new ByteArrayInputStream( BO.toByteArray() ) );


                MD5Importer.getInstance().loadMesh( new File( "window.md5" ).toURI().toURL(), "model" );

                IMesh mesh = MD5Importer.getInstance().getMD5Node().getMesh( 0 );

                TriMesh model = (TriMesh)mesh;

                IntBuffer faces = model.getIndexBuffer();
                FloatBuffer norms = model.getNormalBuffer();
                FloatBuffer verts = model.getVertexBuffer();

                // index of the first vert of the triangle to remove
                List<Integer> facesToGo = new ArrayList();
                List<int[]> newTris = new ArrayList();
                List<Point3d> newPoints = new ArrayList();
                List<Vector3d> newNorms = new ArrayList();

                LinearForm3D plane = new LinearForm3D( new Vector3d( -1, 0, 0 ), new Vector3d( 2, 2, 1 ) );

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

                        Vector3d dir2 = new Vector3d(o2);
                        dir2.sub( o1 );
                        Vector3d dir3 = new Vector3d(o3);
                        dir3.sub( o1 );

                        Point3d r2 = plane.collide( o1, dir2 );
                        Point3d r3 = plane.collide( o1, dir3 );

//                        System.out.println( r2 + " dist is " + plane.pointDistance( r2 ) );
//
//                        System.out.println( " old was " + new Vector3d(
//                                verts.get( ee[n1Index] * 3 + 0 ),
//                                verts.get( ee[n1Index] * 3 + 1 ),
//                                verts.get( ee[n1Index] * 3 + 2 ) ) );

                        int p1Indx = verts.limit()/3 + newPoints.size();


                        int[] t1 = new int[3];
                        t1[pos.get(0)] = ee [pos.get( 0 )];
                        t1[pos.get(1)] = ee [pos.get( 1 )];
                        t1[neg.get(0)] = p1Indx;
                        newTris.add( t1 );

                        int[] t2 = new int[3];
                        t2[pos.get(0)] = ee [pos.get( 1 )];
                        t2[pos.get(1)] = p1Indx + 1;
                        t2[neg.get(0)] = p1Indx;
                        newTris.add( t2 );



//                        newTris.add( new int[] {  p1Indx+1, p1Indx, ee [pos.get( 0 )] } );

//                        newTris.add( new int[] { ee[0], ee[1], ee[2] } );

                        newPoints.add( r2 );
                        newPoints.add( r3 );

                        for (int i = 0; i < 2; i++) // one norm for each new vertex
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

                 FloatBuffer tmpVerts = BufferUtils.createFloatBuffer( verts.limit() + newPoints.size() * 3  );
                 FloatBuffer tmpNorms = BufferUtils.createFloatBuffer( norms.limit() + newNorms.size() * 3  );

                 tmpVerts.put( verts );
                 tmpNorms.put( norms );

                 verts = tmpVerts;
                 norms = tmpNorms;

                 for (Point3d p: newPoints) //verts.position()
                 {
                     tmpVerts.put ((float)p.x);
                     tmpVerts.put ((float)p.y);
                     tmpVerts.put ((float)p.z);
                 }

                 for (Vector3d v : newNorms)
                 {
                     tmpNorms.put ((float)v.x);
                     tmpNorms.put ((float)v.y);
                     tmpNorms.put ((float)v.z);
                 }

                 IntBuffer foo = BufferUtils.createIntBuffer( faces.limit() + newTris.size() * 3);
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

                model.reconstruct( verts, norms, null, null, faces );

                model.updateGeometricState( 0, true );

                if ( oldObj != null )
                    rootNode.detachChild( oldObj );
                oldObj = model;

                rootNode.attachChild( model );
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

    TriMesh current = null;
    TriMesh toShow;
    TriMesh oldObj = null;


    public synchronized void display ( LoopL<Point3d> points)
    {
        int count = 0;

        for ( Point3d p : points.eIterator() ) // this should be loopl!
            count ++;

        FloatBuffer verticies = BufferUtils.createFloatBuffer( count *3 );
        FloatBuffer normals = BufferUtils.createFloatBuffer( count *3 );
        FloatBuffer texs = BufferUtils.createFloatBuffer( count *2 );
        FloatBuffer cols = BufferUtils.createFloatBuffer (count * 4);
        ColorRGBA col = ColorRGBA.red;

        int offset = 0;

        List<Integer> faces = new ArrayList();

        for ( Loop<Point3d> list : points ) // this should be loopl!
        {
            int polySize = list.count();

            Triangulator lator = new Triangulator();
            Integer last = null, start = 0;

            for ( Point3d point : list )
            {
//                System.out.println(" adding in preview point " +point);
                verticies.put( (float) (point.x )/60f );
                verticies.put( (float) (point.y)/60f );
                verticies.put( (float) (point.z)/60f );

                texs.put( (float)point.x/60f );
                texs.put( (float)point.y/60f );

                TriangulationVertex v = lator.addVertex( new Vector3f( (float) point.x, (float) point.y, (float) point.z ) );
                if (last != null)
                {
//                    System.out.println("adding "+last+" to "+v.getIndex());
                    lator.addEdge( last, v.getIndex() );
                }
                else
                    start = v.getIndex();
                last = v.getIndex();
            }

//            System.out.println("adding "+last+" to "+start);
            lator.addEdge( last, start );

            Vector3d o1 = new Vector3d ( list.getFirst() ),
                    o2 = new Vector3d ( list.getFirstLoopable().getNext().get() ),
                    o3 = new Vector3d ( list.getFirstLoopable().getPrev().get() );

            o1.sub( o2 );
            o3.sub( o2 );

            o2.cross( o1, o3 );
            o2.normalize();
//            o2.negate();

            for (int i = 0; i < polySize; i++)
            {
                normals.put( (float) o2.x );
                normals.put( (float) o2.y );
                normals.put( (float) o2.z );

                cols.put( col.r );
                cols.put( col.g );
                cols.put( col.b );
                cols.put( col.a );
            }

//            System.out.println("doing triangulation");
            IntBuffer ib = lator.triangulate();


            for (int i = 0; i < ib.position(); i++)
            {
                int correctedIndex = ib.get( i ) + offset;
//                System.out.println( " ib is " + correctedIndex );
                faces.add( correctedIndex );
            }

            offset += polySize;
        }

        IntBuffer faceBuffer = BufferUtils.createIntBuffer( faces.size() );

        for ( int i = 0; i < faces.size(); i += 3 )
        {
            faceBuffer.put( faces.get( i + 0) ); // reverse tri direction
            faceBuffer.put( faces.get( i + 2) );
            faceBuffer.put( faces.get( i + 1) );
        }

//        new JFrame ()
//        {
//            @Override
//            public void paint( Graphics g )
//            {
//                for ( int i = 0; i < faceBuffer.limit(); i += 3 )
//                {
//                    Polygon p = new Polygon();
//                    p.addPoint( vertices.get (faceBuffer.get(i)), vertices.get (faceBuffer.get(i+1)));
//                }
//                    System.out.println( " face buffer contains " + faceBuffer.get( i ) + ", " + faceBuffer.get( i + 1 ) + ", " + faceBuffer.get( i + 2 ) );
//            }
//        }.setVisible( true );



//        System.out.println("\n\n\n\n\n\n***********************************************************88");
//
//        for (int i = 0; i < verticies.capacity(); i = i +3)
//        {
//            System.out.println( i/3+ "==>" + verticies.get( i ) + ", " + verticies.get( i+1 ) + ", " + verticies.get( i+2 ) );
//        }
//
//        for ( int i = 0; i < faces.size(); i += 3 )
//        {
//            System.out.println( " face buffer contains " + faceBuffer.get( i ) + ", " + faceBuffer.get( i+1 ) + ", " + faceBuffer.get( i+2 ) );
//        }

        verticies.flip();
        faceBuffer.flip();

        toShow = new TriMesh("house", verticies, normals, cols, new TexCoords( texs ) , faceBuffer );

                toShow.setModelBound(new BoundingBox());
        toShow.updateModelBound();

        toShow.setDefaultColor( ColorRGBA.red );
    }

}




//        int count = 0;
//
//        for (Face face : output.faces.values())
//        {
//            count += face.getLoopL().count();
//        }
//
//        FloatBuffer verticies = BufferUtils.createFloatBuffer( count *3 );
//        FloatBuffer normals = BufferUtils.createFloatBuffer( count *3 );
//        FloatBuffer texs = BufferUtils.createFloatBuffer( count *2 );
//        FloatBuffer cols = BufferUtils.createFloatBuffer (count * 4);
//        ColorRGBA col = ColorRGBA.red;
//
//        int offset = 0;
//
//        List<Integer> faces = new ArrayList();
//
//        for (Face face : output.faces.values())
//        {
//            LoopL<Point3d> loopl = face.getLoopL();
//
//            int polySize = face.pointCount();
//
//            Triangulator lator = new Triangulator();
//            Integer last = null, start = 0;
//
//            for ( Loop<Point3d> loop : loopl )
//                for ( Point3d point : loop )
//                {
//                    System.err.println(" adding in preview point " +point);
//                    verticies.put( (float) ( point.x ) / 60f );
//                    verticies.put( (float) ( point.y ) / 60f );
//                    verticies.put( (float) ( point.z ) / 60f );
//
//                    texs.put( (float) point.x / 60f );
//                    texs.put( (float) point.y / 60f );
//
//                    TriangulationVertex v = lator.addVertex( new Vector3f( (float) point.x, (float) point.y, (float) point.z ) );
//                    if ( last != null )
//                    {
//                        System.err.println( "adding " + last + " to " + v.getIndex() );
//                        lator.addEdge( last, v.getIndex() );
//                    }
//                    else
//                        start = v.getIndex();
//                    last = v.getIndex();
//                }
//
//            System.err.println("adding "+last+" to "+start);
//            lator.addEdge( last, start );
//
//            Loop<Point3d> points = loopl.get( 0 ); // perimeter
//            Vector3d o1 = new Vector3d ( points.getFirst() ),
//                    o2 = new Vector3d ( points.getFirstLoopable().getNext().get() ),
//                    o3 = new Vector3d ( points.getFirstLoopable().getPrev().get() );
//
//            o1.sub( o2 );
//            o3.sub( o2 );
//
//            o2.cross( o1, o3 );
//            o2.normalize();
////            o2.negate();
//
//            for (int i = 0; i < polySize; i++)
//            {
//                normals.put( (float) o2.x );
//                normals.put( (float) o2.y );
//                normals.put( (float) o2.z );
//
//                cols.put( col.r );
//                cols.put( col.g );
//                cols.put( col.b );
//                cols.put( col.a );
//            }
//
////            System.out.println("doing triangulation");
//            IntBuffer ib = lator.triangulate();
//
//
//            for (int i = 0; i < ib.position(); i++)
//            {
//                int correctedIndex = ib.get( i ) + offset;
////                System.out.println( " ib is " + correctedIndex );
//                faces.add( correctedIndex );
//            }
//
//            offset += polySize;
//        }
//
//        IntBuffer faceBuffer = BufferUtils.createIntBuffer( faces.size() );
//
//        for ( int i = 0; i < faces.size(); i += 3 )
//        {
//            faceBuffer.put( faces.get( i + 0) ); // reverse tri direction
//            faceBuffer.put( faces.get( i + 2) );
//            faceBuffer.put( faces.get( i + 1) );
//        }

//        new JFrame ()
//        {
//            @Override
//            public void paint( Graphics g )
//            {
//                for ( int i = 0; i < faceBuffer.limit(); i += 3 )
//                {
//                    Polygon p = new Polygon();
//                    p.addPoint( vertices.get (faceBuffer.get(i)), vertices.get (faceBuffer.get(i+1)));
//                }
//                    System.out.println( " face buffer contains " + faceBuffer.get( i ) + ", " + faceBuffer.get( i + 1 ) + ", " + faceBuffer.get( i + 2 ) );
//            }
//        }.setVisible( true );



//        System.out.println("\n\n\n\n\n\n***********************************************************88");
//
//        for (int i = 0; i < verticies.capacity(); i = i +3)
//        {
//            System.out.println( i/3+ "==>" + verticies.get( i ) + ", " + verticies.get( i+1 ) + ", " + verticies.get( i+2 ) );
//        }
//
//        for ( int i = 0; i < faces.size(); i += 3 )
//        {
//            System.out.println( " face buffer contains " + faceBuffer.get( i ) + ", " + faceBuffer.get( i+1 ) + ", " + faceBuffer.get( i+2 ) );
//        }

//        verticies.flip();
//        faceBuffer.flip();
//
//        toShow = new TriMesh("house", verticies, normals, cols, new TexCoords( texs ) , faceBuffer );
//
//                toShow.setModelBound(new BoundingBox());
//        toShow.updateModelBound();
//
//        toShow.setDefaultColor( ColorRGBA.red );