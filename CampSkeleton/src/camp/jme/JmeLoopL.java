package camp.jme;

import com.jme.scene.TriMesh;
import com.jme.util.geom.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;
import org.lwjgl.util.glu.tessellation.GLUtessellatorImpl;
import utils.DHash;
import utils.Loop;
import utils.LoopL;
import utils.ReverseList;

/**
 *
 * @author twak
 */
public class JmeLoopL extends TriMesh {

    static Vector3d upDir = new Vector3d(0, 0, 1), xDir = new Vector3d(1, 0, 0);
    public boolean valid = true;
    Vector3d norm;
    LoopL<Point3d> loopl;

    public JmeLoopL() {
        super();
    }

    public JmeLoopL(LoopL<Point3d> loopl, Vector3d norm, boolean toJmeSpace) {
        super();
        if (norm != null){
        this.norm = new Vector3d(Jme.convert(norm));
        this.norm.normalize();
        }
        this.loopl = loopl;

        gluTriangulate(toJmeSpace);
    }

    Matrix4d faceToYUp() {
        Matrix3d o = new Matrix3d();

        // should just be any two vectors perp to each other and norm.. but for horizontal faces...for...now...

        Vector3d dir = new Vector3d(0, 1, 0);
        Vector3d upHill = new Vector3d(1, 0, 0);


        if (norm.y > 0) // hack :( for horiz/vert faces, norms are still perpendicular tho :(
        {
            dir = new Vector3d(0, 1, 0);
            upHill = new Vector3d(-1, 0, 0);
        }

        o.setColumn(0, dir);
        o.setColumn(1, upHill);
        o.setColumn(2, norm);

        o.transpose();


        Matrix4d sol = new Matrix4d();
        sol.setIdentity();
        sol.setRotation(o);

        if (norm != null && norm.z > 0) // hack for upwards pointing faces, normals (feeds back to above constructor). memo to self: learn math.
        {
            double tmp = norm.y;
            this.norm.y = this.norm.z;
            this.norm.z = tmp;
        }

        // to JME space!
//        sol.mul( Jme.transform);

        return sol;
    }
//    protected void triangulate(LoopL<Point3d> loopl, Vector3d normal) {
////        System.out.println("?>?"+normal.length());
//        try {
//            Matrix4d yUp = faceToYUp();
//
//            int count = loopl.count();
//
//            if (count == 0) {
//                valid = false;
//                return;
//            }
//
//            FloatBuffer verticies = BufferUtils.createFloatBuffer(count * 3);
//            FloatBuffer normals = BufferUtils.createFloatBuffer(count * 3);
//            FloatBuffer texs = BufferUtils.createFloatBuffer(count * 2);
//            FloatBuffer cols = BufferUtils.createFloatBuffer(count * 4);
//
//            ColorRGBA col = ColorRGBA.red;
//
//            int offset = 0;
//
//            int polySize = 0;
//            for (Loop<Point3d> loop : loopl) {
//                polySize += loop.count();
//            }
//
//
//            List<Integer> faces = new ArrayList();
//            {
////                int polySize = face.pointCount();
//
////            System.err.println ("starting >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>.. ");
//                Triangulator lator = new Triangulator();
//
//                for (int i = 0; i < loopl.size(); i++) {
//
//                    Integer last = null, start = 0;
//                    Loop<Point3d> loop = loopl.get(i);
////                if (i>0) // data destructive...
////                    loop.reverse();
//                    for (Point3d p : loop) {
//                        Point3d point = Jme.convert(p); // assumes no dupes in loop
//                        verticies.put((float) (point.x));
//                        verticies.put((float) (point.y));
//                        verticies.put((float) (point.z));
//
//                        texs.put((float) point.x);
//                        texs.put((float) point.y);
//
//                        // only the triangulator sees the orientated polygon (rotated onto xy plane for trinagulator)
//                        Point3d flat = new Point3d(p); // instance!
//                        yUp.transform(flat);
//
//                        TriangulationVertex v = lator.addVertex(new Vector3f((float) flat.x, (float) flat.y, 0f));
////                    System.err.println(v.getIndex() + " adding in preview point " + point);
//
//                        if (last != null) //                        System.err.println( "adding " + last + " to " + v.getIndex() );
//                        {
//                            lator.addEdge(last, v.getIndex());
//                        } else {
//                            start = v.getIndex();
//                        }
//
//                        last = v.getIndex();
//                    }
//
////                System.err.println( "adding " + last + " to " + start );
//                    lator.addEdge(last, start);
//                }
//
//                Loop<Point3d> points = loopl.get(0); // perimeter
//
//                // normals are calculated on the un-transformed data set
//                Vector3d o1 = new Vector3d(points.getFirst()),
//                        o2 = new Vector3d(points.getFirstLoopable().getNext().get()),
//                        o3 = new Vector3d(points.getFirstLoopable().getPrev().get());
//
//                o1.sub(o2);
//                o3.sub(o2);
//
//                o2.cross(o1, o3);
//                o2.normalize();
////            o2.negate();
//
//                for (int i = 0; i < count; i++) {
//                    normals.put((float) normal.x);
//                    normals.put((float) normal.y);
//                    normals.put((float) normal.z);
////                    normals.put( (float) o2.x );
////                    normals.put( (float) o2.y );
////                    normals.put( (float) o2.z );
//
//                    cols.put(col.r);
//                    cols.put(col.g);
//                    cols.put(col.b);
//                    cols.put(col.a);
//                }
//
////            System.err.println("doing triangulation");
//                IntBuffer ib = lator.triangulate();
//
//
//                for (int i = 0; i < ib.position(); i++) {
//                    int correctedIndex = ib.get(i) + offset;
////                System.out.println( " ib is " + correctedIndex );
//                    faces.add(correctedIndex);
//                }
//
//                offset += polySize;
//            }
//
//            IntBuffer faceBuffer = BufferUtils.createIntBuffer(faces.size());
//
//            for (int i = 0; i < faces.size(); i += 3) {
//                faceBuffer.put(faces.get(i + 0)); // reverse tri direction here if needed
//                faceBuffer.put(faces.get(i + 1));
//                faceBuffer.put(faces.get(i + 2));
//            }
//
////        setName( face.edge.toString() );  new TexCoords( texs )
//            reconstruct(verticies, normals, cols, null, faceBuffer);
//
//            setModelBound(new BoundingBox());
//            updateModelBound();
//        } catch (Throwable t) {
//            t.printStackTrace();
//            System.err.println("via");
//            if (t.getCause() != null) {
//                t.getCause().printStackTrace();
//            }
//            valid = false;
//        }
//    }
//    @Override

    public void gluTriangulate( boolean toJmeSpace ) {

        if (loopl.count() == 0) {
            valid = false;
            return;
        }

        final DHash<Point3d, Integer> pointToIndex = new DHash();
        final List<Integer> faceBuffer = new ArrayList();
        
        GLUtessellator tess = GLUtessellatorImpl.gluNewTess();
        GLUtessellatorCallbackAdapter tessCb = new GLUtessellatorCallbackAdapter() {

            List<Integer> indexes = new ArrayList();
            int type;
            @Override
            public void begin(int type) {
                this.type = type;
            }

            @Override
            public void vertex(Object vertexData) {
                if (!pointToIndex.containsA((Point3d) vertexData)) {
                    pointToIndex.put((Point3d) vertexData, pointToIndex.ab.size());
                }

                indexes.add(pointToIndex.get((Point3d) vertexData));
            }

            public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
                System.out.println("combine");
                Point3d pt = new Point3d(coords[0], coords[1], coords[2]);
                outData[0] = pt;
            }

            @Override
            public void end() {
                switch (type) {
                    case GL.GL_TRIANGLE_FAN: //6
                        valid = true;
                        for (int i = 1; i < indexes.size() - 1; i++) {
                            faceBuffer.add(indexes.get(0));
                            faceBuffer.add(indexes.get(i));
                            faceBuffer.add(indexes.get(i + 1));
                        }
                        break;
                    case GL.GL_TRIANGLE_STRIP: //5
                        valid = true;
                        for (int i = 0; i < indexes.size() - 2; i++) {
                            if (i % 2 == 0) {
                                faceBuffer.add(indexes.get(i));
                                faceBuffer.add(indexes.get(i + 1));
                                faceBuffer.add(indexes.get(i + 2));
                            } else {
                                faceBuffer.add(indexes.get(i + 1));
                                faceBuffer.add(indexes.get(i));
                                faceBuffer.add(indexes.get(i + 2));
                            }
                        }
                        break;
                    case GL.GL_TRIANGLES: //4
                        valid = true;
                        for (int i : indexes) {
                            faceBuffer.add(i);
                        }
                        break;
                    default:
                        throw new Error("uknown type");
                }
                indexes.clear();
            }

            @Override
            public void error(int errnum) {
                valid = false;
            }

            @Override
            public void errorData(int errnum, Object polygonData) {
                valid = false;
            }
        };

        tess.gluTessCallback(GLU.GLU_TESS_BEGIN, tessCb);
        tess.gluTessCallback(GLU.GLU_TESS_VERTEX, tessCb);
        tess.gluTessCallback(GLU.GLU_TESS_COMBINE, tessCb);
        tess.gluTessCallback(GLU.GLU_TESS_END, tessCb);
        tess.gluTessCallback(GLU.GLU_TESS_ERROR, tessCb);

        tess.gluTessBeginPolygon(null);
        tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);

        for (Loop<Point3d> pts : loopl) {
            tess.gluTessBeginContour();
            List<Point3d> pts2 = new ArrayList();

            for (Point3d pt : (pts)) {
                Point3d jmePt = Jme.convert(pt);
                if (toJmeSpace)
                    pts2.add(jmePt);
                else
                    pts2.add(pt);
            }

            for (Point3d pt : new ReverseList<Point3d>(pts2)) {
                tess.gluTessVertex(new double[]{pt.x, pt.y, pt.z}, 0, pt);
            }

            tess.gluTessEndContour();
        }
        tess.gluTessEndPolygon();
        tess.gluDeleteTess();

        FloatBuffer verticies = BufferUtils.createFloatBuffer(pointToIndex.ab.size() * 3);
        FloatBuffer normals =null;
        if (norm != null)
            normals = BufferUtils.createFloatBuffer(pointToIndex.ab.size() * 3);

        for (int i = 0; i < pointToIndex.ab.size(); i++) {
            Point3d pt = pointToIndex.teg(i);
            verticies.put((float) pt.x);
            verticies.put((float) pt.y);
            verticies.put((float) pt.z);
            if (normals != null) {
                normals.put((float) norm.x);
                normals.put((float) norm.y);
                normals.put((float) norm.z);
            }
        }

        IntBuffer faceBuffer2 = BufferUtils.createIntBuffer(faceBuffer.size());
        for (int i : faceBuffer) {
            faceBuffer2.put(i);
        }

        // could use other modes, but that would kill the obj export
        setMode(Mode.Triangles);

        reconstruct(verticies, normals, null, null, faceBuffer2);
    }
}
