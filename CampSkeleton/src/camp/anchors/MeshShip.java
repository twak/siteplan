package camp.anchors;

import camp.jme.JmeBone;
import camp.jme.Preview;
import campskeleton.Plan;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.ui.Marker;
import utils.LContext;
import utils.WeakListener;

/**
 * @author twak
 */
public class MeshShip extends Ship
{
    String meshFile = "mesh.md5";
    // we're compatible as long as the bone names don't change!
    List<String> bones = new ArrayList();

    transient WeakListener.Changed refreshFeatureListener;

    // if true, we enter "chimney mode" where we don't rotate for the slope of a roof.
    boolean ignoreSlope = false;
    boolean firstNormalForAll = false;

    // all bones are scaled by this
    double scale = 1;

    public MeshShip() {
        super();
        // they have to load one first ;)
        anchorNames = new String[] {};
    }

    @Override
    public JComponent getToolInterface(WeakListener refreshToolListener, WeakListener.Changed refreshFeatureListListener, Plan plan)
    {
        this.refreshFeatureListener = refreshFeatureListListener;
        // mesh name?
        return new MeshUI(this, refreshToolListener);
    }

    @Override
    protected Instance createInstance() {
        return new MeshInstance();
    }

    @Override
    public String getFeatureName() {
        int i = meshFile.lastIndexOf(File.separator)+1;
        
        int j = meshFile.toLowerCase().lastIndexOf(".md5");
        if (j == -1)
            j = meshFile.length();
        return meshFile.substring(i, j) + (ignoreSlope ? " [vertical]" :"");
    }

    /**
     * @return the meshFile
     */
    public String getMeshFile() {
        return meshFile;
    }

    /**
     * @param meshFile the meshFile to set
     */
    public void setMeshFile(String meshFile) {

        this.meshFile = meshFile;
        List<String> bones = JmeBone.getNames(meshFile);
        setNewAnchorNames(bones);
        this.bones = bones;
        // refresh the ui?!
    }

    @Override
    public Ship clone(Plan plan)
    {
        MeshShip other = new MeshShip();

        setupClone(other);

        other.meshFile = meshFile;
        other.bones.addAll(bones);
        other.ignoreSlope = ignoreSlope;
        other.firstNormalForAll = firstNormalForAll;
        other.scale = scale;

        return other;
    }
    

    static class NormalPt
    {
        Point3d point;
        int anchorNo;
        Vector3d normal, uphill;

        public NormalPt (Point3d pt, Vector3d normal, Vector3d uphill, int anchorNo)
        {
            this.point = pt;
            this.normal = normal;
            this.uphill = uphill;
            this.anchorNo= anchorNo;
        }
    }

    public class MeshInstance extends Ship.Instance
    {
        // no need to serialize these, but saves initializing them! (memo to past self: you tit)
        @Deprecated Point3d[] points = new Point3d[anchorNames.length];
        @Deprecated Vector3d[] normals = new Vector3d[anchorNames.length];
        @Deprecated Vector3d[] uphills = new Vector3d[anchorNames.length];

        // we have more instances than anchors (if the anchor is on a repeated instance), so we use lists instead
//        transient List<Point3d> pointL = new ArrayList();
//        transient List<Integer> anchorL = new ArrayList();
//        transient List<Vector3d>
//            normalL = new ArrayList(),
//            uphillL = new ArrayList();

        transient List<NormalPt> normalPts = new ArrayList();

        @Override
        public LContext<Corner> process(Anchor anchor, LContext<Corner> toEdit, Marker planMarker, Marker profileMarker, Edge edge, AnchorHauler.AnchorHeightEvent hauler, Corner oldLeadingCorner)
        {
            for (int i = 0; i < anchors.length; i++)
                if (anchors[i].matches(anchor.getPlanGen(), anchor.getProfileGen()) )
                {
                    normalPts.add (new NormalPt( new Point3d (planMarker.x, planMarker.y, -profileMarker.y),
                    edge.getPlaneNormal(),
                    new Vector3d(edge.uphill),
                    i ) );
                    
                    return toEdit;
                }
            return toEdit;
        }

        /************************* HACK ****************************************8
         * This is a hacky method to associate different pairs of points on profile offsets
         * (two identical dormer windows with meshes on them) - we cluster by distance.
         *
         * Needs the complete-language-overhall the system's been needing for a while...
         * 
         * @param preview
         * @param key
         */
        @Override
        public void addMeshes( Preview preview , Object key)
        {
            Set<NormalPt> pointsBound = new HashSet(normalPts);

            while (!pointsBound.isEmpty())
            {
                NormalPt start = pointsBound.iterator().next();
                pointsBound.remove(start);
                
                Set<Anchor> anchorsToGo = new HashSet(Arrays.asList(anchors));
                Anchor anchor = anchors[start.anchorNo];
                anchorsToGo.remove(anchor);

                List<NormalPt> binding = new ArrayList();
                binding.add(start);

                while (!anchorsToGo.isEmpty())
                {
                    Anchor a = anchorsToGo.iterator().next();
                    anchorsToGo.remove(a);

                    double minDist = Double.MAX_VALUE;
                    NormalPt found = null;

                    for (NormalPt np : pointsBound)
                    {
                        if ( anchors[np.anchorNo] == a)
                        {
                            double dist = np.point.distance(start.point);
                            if (dist < minDist)
                            {
                                minDist = dist;
                                found = np;
                            }
                        }
                    }

                    if (found != null)
                    {
                        binding.add(found);
                    }
                }

                if (binding.size() == anchors.length)
                {
                    pointsBound.removeAll(binding);
                    bind (binding, preview, key);
                }
            }
        }

        void bind (List<NormalPt> normalPts, Preview preview , Object key)
        {
            JmeBone bone = new JmeBone(meshFile);
            List<JmeBone.BoneLocation> boneLocations = new ArrayList();

            Vector3f firstNormal = null;

            for (int i = 0; i < normalPts.size(); i++)
            {
                NormalPt normalPt = normalPts.get(i);
                Anchor anchor = anchors[normalPt.anchorNo];
                // not bound
                if (anchor.getPlanGen() == null || anchor.getProfileGen() == null)
                    continue;
                // doesn't exist in this frame
                if (normalPt.point == null)
                    continue;

                String boneName = anchorNames[normalPt.anchorNo];


                Vector3d normal_ = normalPt.normal, uphill_ = normalPt.uphill;
                Vector3f normal = new Vector3f( (float) normal_.x, (float)normal_.y, (float)normal_.z );
                Vector3f uphill = new Vector3f( (float) uphill_.x, (float)uphill_.y, (float)uphill_.z );


                if (firstNormal == null)
                    firstNormal = normal;

                if (firstNormalForAll)
                    normal = new Vector3f(firstNormal);

//                System.out.print(normal.length());
//                System.out.print(uphill.length());

                Point3d pt = normalPt.point;

                Vector3f cross = new Vector3f(uphill);
                cross = cross.cross(normal);


//                uphill.negateLocal();
//
//                normal.negateLocal();

                // 1,2, 0 a good kind of wrong

//                Matrix3f rotM = new Matrix3f();
//                rotM.setColumn(0, normal);
//                rotM.setColumn(2, uphill);
//                rotM.setColumn(1, cross);
//                rotM.setRow(1, normal);
//                rotM.setRow(0, uphill);
//                rotM.setRow(2, cross);


                /*************************** HACK ALERT ************************** transpose trick isn't playing ball, and I need to ship this to some yanks, so this'll do *******************/

                // rotation for wall angle
//                Quaternion quat2 = new Quaternion();
                javax.vecmath.Matrix3f m1 = new javax.vecmath.Matrix3f();

                float sideL = (float)Math.sqrt( normal.x * normal.x + normal.z * normal.z );

                Vector3f flatNormal = new Vector3f (normal.x,normal.y,0);
                
                flatNormal.normalize();
                float angle = -flatNormal.angleBetween(normal);// -Math.atan2(sideL, normal.y);
                if (normal.z > 0)
                    angle = -angle;

                if ( ignoreSlope )
                    angle = 0;

//                AxisAngle4f aa = new AxisAngle4f(new javax.vecmath.Vector3f ( 1f, 0f, 0f ), -angle);
                AxisAngle4f aa = new AxisAngle4f(new javax.vecmath.Vector3f ( cross.x, cross.z, cross.y ), angle );//(float)Math.PI/4f);
                m1.set(aa);


//                quat2.fromRotationMatrix(
//                            (float) m.m00, (float) m.m01, (float) m.m02,
//                            (float) m.m10, (float) m.m11, (float) m.m12,
//                            (float) m.m20, (float) m.m21, (float) m.m22
//
//                            );
                // rotation around vertical
                javax.vecmath.Matrix3f m2 = new javax.vecmath.Matrix3f();
                m2 = new javax.vecmath.Matrix3f();
//                AxisAngle4f aa2 = new AxisAngle4f(new javax.vecmath.Vector3f(uphill.x, uphill.y, uphill.z), (float) -Math.atan2(cross.y, cross.x));
                AxisAngle4f aa2 = new AxisAngle4f(new javax.vecmath.Vector3f(0f, 1f, 0f), (float) -Math.atan2(cross.y, cross.x));
                m2.set(aa2);


                m1.mul(m2);

                m2 = m1;


                Quaternion quat = new Quaternion();
                quat.fromRotationMatrix(
                            (float) m2.m00, (float) m2.m01, (float) m2.m02,
                            (float) m2.m10, (float) m2.m11, (float) m2.m12,
                            (float) m2.m20, (float) m2.m21, (float) m2.m22 // switch handedness of coord system
                            );

//                quat = quat2.add(quat);

//                rotM.setColumn(0, cross);
//                rotM.setColumn(0, normal);
//                rotM.setColumn(0, uphill);
//                quat = quat.fromRotationMatrix(rotM);

//                quat.fromRotationMatrix(orientation);
//                        (float)cross.x, (float)normal.x, (float)uphill.x,
//                        (float)cross.y, (float)normal.y, (float)uphill.y,
//                        (float)cross.z, (float)normal.z, (float)uphill.z );

//                quat.fromRotationMatrix(
//                        (float)cross.x, (float)cross.y, (float)cross.z,
//                        (float)normal.x, (float)normal.y, (float)normal.z,
//                        (float)uphill.x, (float)uphill.y, (float)uphill.z );


                JmeBone.BoneLocation loc = new JmeBone.BoneLocation(boneName, new Vector3f( (float) pt.x, (float)pt.y, (float)pt.z), quat);

                boneLocations.add(loc);
            }

                bone.setScale(scale);
                bone.setBones(boneLocations);
                preview.display(bone, key, false);
//
//                                // find rotation for bone (erk, rotation transform occurs in feature factory)
//                    Vector3d dir = new Vector3d (1,0,0);
//                    m.transform( dir );
//
//                    double angle = Math.atan2( dir.y, dir.x );
//
//                    Quaternion rot = new Quaternion();
//                    rot.fromRotationMatrix(
//                            (float)m.m00,(float)m.m01,(float)m.m02,
//                            (float)m.m10,(float)m.m11,(float)m.m12,
//                            (float)m.m20,(float)m.m21,(float)m.m22 // switch handedness of coord system
//                            );
//
//                    bone.setBones( Arrays.asList(
//                            new JmeBone.BoneLocation( "0", new Vector3f( (float) m.m03, (float) m.m13, (float) m.m23 ), rot ),
//                            new JmeBone.BoneLocation( "1", new Vector3f( (float) m.m03, (float) m.m13, (float) 0 ), rot ) ) );
//
//            for (Point3d pt : points)
//            {
//                assert(pt != null);
//
//            }
        }

        private void clearCache()
        {
            // before each run, clear the list of points to generate meshes on
            normalPts = new ArrayList();
        }
        
        @Override
        public void upgrade(List<String> names) {
            super.upgrade(names);
            points = new Point3d[names.size()];
            normals = new Vector3d[names.size()];
            uphills = new Vector3d[names.size()];
        }
    }

    @Override
    public void clearCache()
    {
        for (Instance i : getInstances() )
        {
            ((MeshInstance)i).clearCache();
        }
    }
}
