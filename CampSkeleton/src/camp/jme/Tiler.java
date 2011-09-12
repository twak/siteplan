package camp.jme;

import camp.tags.RoofTag;
import campskeleton.TileDebugger;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.TriMesh;
import com.jme.util.geom.BufferUtils;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3d;
import straightskeleton.Output.Face;
import straightskeleton.Output.SharedEdge;
import utils.DHash;
import utils.FindBounds2D;
import utils.IntegerCartesian;
import utils.Intersector;
import utils.Intersector.Collision;
import utils.Line;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;
import utils.MUtils;
import utils.MultiMap;
import utils.MultiMapSet;
import utils.ReverseList;
import utils.UnionWalker;

/**
 *
 * @author twak
 */
public class Tiler extends Node {

//    DHash<Line, SharedEdge> lineEdge = new DHash();
    double tileHeight = 4, tileWidth = 2.6;
    double jitter = 0.5;
    boolean stagger = true;
    // threading vars
    Preview preview;
    List<Face> faces = new ArrayList();
    // tmp variables:
    MultiMap<Line, Collision> winding;
    Matrix4d zUp;
    Face face;
    public List<Line> allLines;
    public List<Intersector.Collision> collisions = new ArrayList();
    Object threadKey;

    /**
     * Debug harness
     */
    public Tiler(Preview preview, Set<Face> allFaces, Object threadKey) {

        this.preview = preview;
        this.faces.addAll(allFaces);
        this.threadKey = threadKey;
        go();

    }

    public Tiler(Preview preview, Set<Face> allFaces, Object threadKey, RoofTag roof) {
        tileHeight = roof.width * 5;
        tileWidth = roof.height * 5;

        tileHeight = Math.max(tileHeight, 0.1);
        tileWidth = Math.max(tileWidth, 0.1);

        jitter = roof.jitter;
        stagger = roof.stagger;

        this.preview = preview;
        this.faces.addAll(allFaces);
        this.threadKey = threadKey;
        go();
    }

    public void go() {
        new Thread() {

            @Override
            public void run() {
                for (Face f : faces) {
                    try {
                        doJob(f);
                    } catch (Throwable ht) {
                        ht.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void doJob(Face face) {

        this.face = face;

        try {
            zUp = faceToZUp(face);
            LoopL<Line> loop = toFlat(face, zUp);

            outVerts.clear();
            outFaces.clear();

            findTiles2(loop); // creates outVerts, outFaces

            FloatBuffer verts = BufferUtils.createFloatBuffer(outVerts.ab.size() * 3);
            for (int i = 0; i < outVerts.ab.size(); i++) {
                Vector3f loc = outVerts.teg(i);
                verts.put(loc.x);
                verts.put(loc.y);
                verts.put(loc.z);
            }

            IntBuffer tileFaces = BufferUtils.createIntBuffer(outFaces.size());

            for (int i : outFaces) {
                tileFaces.put(i);
            }

            TriMesh tile = new TriMesh("tiles", verts, null, null, null, tileFaces);

            Matrix4d toLoc = new Matrix4d();

            toLoc.setIdentity();
            toLoc.setTranslation(new Vector3d(0, 0, 0.6f));

            Matrix4d toGraphics = new Matrix4d(zUp);

            toGraphics.invert(); // transpose!

            Matrix4d toJme = new Matrix4d(Jme.transform);
            toJme.mul(toGraphics);
            toJme.mul(toLoc);

            tile.setLocalTranslation((float) toJme.m03, (float) toJme.m13, (float) toJme.m23);

            tile.setLocalScale(1f / (float) Jme.scale);

            // and the rotation (inverse of zUp...)
            Matrix3d m = new Matrix3d();

            Vector3d dir = face.edge.direction();
            dir.normalize();
            m.setColumn(0, dir);

            Vector3d upHill = face.edge.uphill;
            m.setColumn(2, upHill);

            m.setColumn(1, face.edge.getPlaneNormal());

            tile.setLocalRotation(new Matrix3f(
                    (float) m.m00, (float) m.m01, (float) m.m02,
                    (float) m.m20, (float) m.m21, (float) m.m22,
                    (float) m.m10, (float) m.m11, (float) m.m12));

            preview.display(tile, threadKey, false);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    LoopL<Line> toFlat(Face f, Matrix4d trans) {

        LoopL<Point3d> tmp = new LoopL();

        for (Loop<SharedEdge> pll : f.edges) {
            Loop<Point3d> loop = new Loop();
            tmp.add(loop);
            for (Loopable<SharedEdge> pp : pll.loopableIterator()) {
                Point3d s = new Point3d(pp.get().getStart(f));
                trans.transform(s);
                loop.append(s);
            }
        }


        /**
         * This is a horrible mess of hacks within hacks to simply cast to a
         * 2d loop, but remove near-horizontal lines.
         */
        LoopL<Line> flat = new LoopL();
        Map<Point3d, Point2d> ptCache = new HashMap();
        for (Loop<Point3d> ptLoop : tmp) {
            Loop<Line> loop = new Loop();
            flat.add(loop);

            for (Loopable<Point3d> pt : ptLoop.loopableIterator()) {
                Point3d s = pt.get();
                Point3d e = pt.getNext().get();

                // line collision impl doesn't work well with near-horizontal lines (fixme:3+ near horiz sections)
                if (Math.abs(s.y - e.y) < 0.0001) {
                    if (ptCache.containsKey(s)) {
                        Point2d t = ptCache.get(s);
                        ptCache.remove(s);
                        t.y = e.y;
                        s.y = e.y;
                        ptCache.put(s, t);
                    } else {
                        s.y = e.y;
                    }
                }
//                    s.y += 1+Math.random();
                Point2d s2 = ptCache.get(s);
                Point2d e2 = ptCache.get(e);

                if (s2 == null) {
                    s2 = new Point2d(s.x, s.y);
                    ptCache.put(s, s2);
                }
                if (e2 == null) {
                    e2 = new Point2d(e.x, e.y);
                    ptCache.put(e, e2);
                }

                Line line = new Line(s2, e2);
                loop.append(line);
            }
        }

        return flat;
    }

    private Matrix4d faceToZUp(Face face) {
        Matrix3d o = new Matrix3d();

        Vector3d dir = face.edge.direction();
        dir.normalize();
        o.setColumn(0, dir);

        Vector3d upHill = face.edge.uphill;
        o.setColumn(1, upHill);

        o.setColumn(2, face.edge.getPlaneNormal());

        o.transpose();

        Matrix4d sol = new Matrix4d();
        sol.setIdentity();
        sol.setRotation(o);

        Point3d pt = new Point3d(face.definingSE.iterator().next().getStart(face));
        sol.transform(pt);

        pt.scale(-1);
        sol.setTranslation(new Vector3d(pt));

        // to JME space!
//        Matrix4d jme = new Matrix4d(Jme.transform);
//        jme.mul( sol );

        return sol;
    }
    int xCount, yCount;
    double dX, dY;
    FindBounds2D bounds;
    DHash<Integer, Line> horiz = new DHash(), vert = new DHash();

    int toAWT(double cord) {
        return (int) (cord * 1000);
    }

    public double fromAWT(double cord) {
        return cord / 1000;
    }

    Point2D toAWT(Point2d cord) {
        return new Point2D.Double(toAWT(cord.x), toAWT(cord.y));
    }

    private LoopL<Point2d> AWTAreaToLoopL(Area pg) {

        LoopL<Point2d> loopl = new LoopL();

        PathIterator pit = pg.getPathIterator(null);
        Loop loop = null;

        double[] pt = new double[2];
        while (!pit.isDone()) {
            switch (pit.currentSegment(pt)) {
                case PathIterator.SEG_MOVETO:
                    loop = new Loop<Point2d>();
                    loopl.add(loop);
                    loop.append(new Point2d(fromAWT(pt[0]), fromAWT(pt[1])));
                    break;
                case PathIterator.SEG_LINETO:
                    loop.append(new Point2d(fromAWT(pt[0]), fromAWT(pt[1])));
                    break;
                case PathIterator.SEG_QUADTO:
                case PathIterator.SEG_CUBICTO:
                default:
                    throw new Error();
                case PathIterator.SEG_CLOSE:
                    break;
            }
            pit.next();
        }
        return loopl;
    }

    Area toPoly(LoopL<Line> loopl) {

        Area out = new Area();

        boolean first = true;
        for (Loop<Line> ll : loopl) {
            Polygon polu = new Polygon();

            for (Line l : ll) {
                polu.addPoint(toAWT(l.start.x), toAWT(l.start.y));
            }

            if (first) {
                out.add(new Area(polu));
            } else {
                out.subtract(new Area(polu)); // possibly wrong if diconnected outer...
            }
        }

        return out;
    }

    public void findTiles2(LoopL<Line> flat) {

        Area fArea = toPoly(flat);

        bounds = new FindBounds2D();
        allLines = new ArrayList();

        for (Line l : flat.eIterator()) {
            bounds.add(l.start);
            allLines.add(l);
        }

        // some padding so all lines are really inside bounds.
        bounds.increaseMax(2, 2);
        bounds.move(-1, -1);

        xCount = (int) (Math.ceil((bounds.getWidth()) / tileWidth));
        yCount = (int) (Math.ceil((bounds.getHeight()) / tileHeight));
        dX = (bounds.getWidth()) / (double) xCount;
        dY = (bounds.getHeight()) / (double) yCount;

        for (int x = 0; x <= xCount; x++) {
            for (int y = 0; y <= yCount; y++) {
                double yy = bounds.minY + y * dY;
                double xx = bounds.minX + x * dX;

                boolean tl = fArea.contains(toAWT(new Point2d(xx, yy))),
                        tr = fArea.contains(toAWT(new Point2d(xx + tileWidth, yy))),
                        br = fArea.contains(toAWT(new Point2d(xx + tileWidth, yy + tileHeight))),
                        bl = fArea.contains(toAWT(new Point2d(xx, yy + tileHeight)));

                if (tl && tr && br && bl) {
                    addTile(xx, yy, null, x, y);
                } else if (tl || tr || br || bl) {
                    Polygon pg = new Polygon();

                    for (Vector2d v : Arrays.asList(new Vector2d(xx, yy),
                            new Vector2d(xx, yy + tileHeight),
                            new Vector2d(xx + tileWidth, yy + tileHeight),
                            new Vector2d(xx + tileWidth, yy))) {
                        pg.addPoint(toAWT(v.x), toAWT(v.y));
                    }

                    Area pga = new Area(pg);
                    pga.intersect(fArea);

                    LoopL<Point2d> sec = AWTAreaToLoopL(pga);
                    sec.reverseEachLoop();
                    addTile(xx, yy, sec, x, y);
                }
            }
        }
    }
    /**
     * Adds a tile at the given coordinates (in unprojected space). If the
     * clip isn't null, we'll
     */
    public DHash<Vector3f, Integer> outVerts = new DHash();
    public List<Integer> outFaces = new ArrayList();

    public int addVert(double x, double y, Matrix4d trans) {

        Point3d pt = new Point3d (x,0,y);
        trans.transform(pt);
        Vector3f loc = new Vector3f((float) pt.x, (float)pt.y, (float) pt.z);
        Integer i = outVerts.get(loc);
        if (i == null) {
            outVerts.put(loc, i = outVerts.ab.size());
        }
        return i;
    }

    public void addTile(double x, double y, LoopL<Point2d> clip, int ix, int iy) {

        final Matrix4d toO = new Matrix4d(); toO.setIdentity();
        toO.setTranslation(new Vector3d (x,0,y));
        Matrix4d fromO = new Matrix4d(); fromO.setIdentity();
        fromO.setTranslation(new Vector3d (-x,0,-y));

        Matrix4d scatter = new Matrix4d();
        scatter.setIdentity();
        scatter.set(new AxisAngle4d(new Vector3d(1, 0, 0), Math.random() * 0.1  * jitter));

        toO.mul(scatter);
        toO.mul(fromO);

        if (clip != null) {

            LoopL<Point3d> p3clip = clip.new Map<Point3d>() {

                @Override
                public Point3d map(Loopable<Point2d> lp) {
                    Point3d pt = new Point3d(lp.get().x, 0, lp.get().y);
                    toO.transform(pt);
                    return pt;
                }
            }.run();

            TriMesh tm = new JmeLoopL(p3clip, null, false);

            if (tm == null) {
                return;
            }

            int offset = outVerts.ab.size();
            for (int i = 0; i < tm.getIndexBuffer().limit(); i++) {
                outFaces.add(offset + tm.getIndexBuffer().get(i));
            }

            FloatBuffer fv = tm.getVertexBuffer();

            for (int i = 0; i < fv.limit(); i += 3) {
                Vector3f v = new Vector3f(fv.get(i), fv.get(i + 1), fv.get(i + 2));
                outVerts.put(v, outVerts.ab.size());
            }

        } else {
            int v0 = addVert((double) x, (double) y, toO),
                    v1 = addVert(x, y + tileHeight, toO),
                    v2 = addVert(x + tileWidth, y + tileHeight, toO),
                    v3 = addVert(x + tileWidth, y, toO);
            outFaces.addAll(Arrays.asList(v0, v1, v3, v1, v2, v3));
        }
    }

    /**
     * Could half the number of calls here by storing the walker entries between adjacent cells. Eg: an array of walkers.
     */
    private void buildWalker(
            Line line, boolean isHoriz, double min, double max,
            UnionWalker union, boolean reverse, MultiMap<Line, Point2d> otherLines) {
        List<Collision> cols = winding.get(line); // list is still sorted from earlier

        int start = Collections.binarySearch(cols,
                new Collision(new Point2d(min, min), null), getGridComparator(isHoriz));
        int end = Collections.binarySearch(cols,
                new Collision(new Point2d(max, max), null), getGridComparator(isHoriz));

        Point2d last = null;
        if (start >= 0 && end >= 0) {
            cols = cols.subList(start, end + 1);
            cols = reverse ? new ReverseList<Collision>(cols) : cols;
            for (Collision c : cols) {
                for (Line l : c.lines) {
                    otherLines.put(l, c.location);
                }

                if (last != null) {
                    union.addEdge(last, c.location, false);
                }

                last = c.location;
            }
        }
    }

    public class Crossing {

        SharedEdge edge;
        Line line;
        Point2d position;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Line) {
                return line.equals((Line) obj);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return line.hashCode();
        }
    }

    public static GridComparator getGridComparator(boolean horiz) {
        return horiz ? gridComparatorH : gridComparatorV;
    }
    public static GridComparator gridComparatorH = new GridComparator(true);
    public static GridComparator gridComparatorV = new GridComparator(false);

    public static class GridComparator implements Comparator<Collision> {

        boolean horiz;

        public GridComparator(boolean horiz) {
            this.horiz = horiz;
        }

        public int compare(Collision o1, Collision o2) {
            if (horiz) {
                return Double.compare(o1.location.x, o2.location.x);
            } else {
                return Double.compare(o1.location.y, o2.location.y);
            }
        }
    }
}
