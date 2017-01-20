package org.twak.siteplan.campskeleton;

import org.twak.straightskeleton.ui.PointEditor;

/**
 *
 * @author twak
 */
public class ClipperDebug extends PointEditor
{
//
////    ClipPreview preview;
//
//    public ClipperDebug()
//    {
//        new Thread()
//        {
//
//            @Override
//            public void run()
//            {
////                preview = new ClipPreview();
////                preview.start();
//            }
//        }.start();
//    }
//
//
//    /**
//     * Extrude shape created by in to create a prism of height height
//     * This could be done with the skeleton?
//     */
//    Solid createSolid ()
//    {
//        return createSolid( edges, 50);
//    }
//
//    Solid createSolid (LoopL<Bar> in, double height)
//    {
//        Triangulator tri = new Triangulator();
//
//
//        List<Point3d> allVerts = new ArrayList();
//
//        Map<Point3d, Integer> verts = new HashMap();
//        for (Bar b : in.eIterator())
//        {
//            TriangulationVertex tv = tri.addVertex( new Vector3f ((float)b.start.x, (float)b.start.y, 0f) );
//            Point3d key = new Point3d ( b.start.x, b.start.y, 0);
//            allVerts.add( key );
//            verts.put( key , tv.getIndex() );
//        }
//
//        for (Loop<Bar> loop : in)
//            for (Bar b : loop)
//                tri.addEdge( verts.get( new Point3d ( b.start.x, b.start.y, 0)), verts.get( new Point3d ( b.end.x, b.end.y, 0)));
//
//        List<Integer> v0 = new ArrayList (  );
//        for (int i : BufferUtils.getIntArray( tri.triangulate() ) )
//            v0.add( i );
//
//        int offset = allVerts.size();
//        for (Point3d p : new ArrayList<Point3d>(allVerts))
//        {
//            Point3d key = new Point3d (p.x,p.y, height);
//            allVerts.add( key );
//            verts.put( key, offset + verts.get( p ) );
//        }
//
//        List<Integer> i1 = new ArrayList(v0);
//        for (int i = i1.size() -1 ; i >= 0; i--) // reverse faces for other end cap
//            v0.add( v0.get(i)+offset );
//
//        // now add in a strip around the edge
//        for ( Loop<Bar> loop : in )
//            for ( Bar r : loop )
//            {
//                int a = verts.get( new Point3d( r.start.x, r.start.y, 0 ) );
//                int b = verts.get( new Point3d( r.end.x, r.end.y, 0 ) );
//                int c = verts.get( new Point3d( r.start.x, r.start.y, height ) );
//                int d = verts.get( new Point3d( r.end.x, r.end.y, height ) );
//
//                v0.addAll( Arrays.asList( a, b, c, d, c, b ) );
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
//
//    public void show()
//    {
////        if (preview != null) // might take a while...
////            preview.display( this );
//    }
//
//    @Override
//    public void paintPointEditor( Graphics2D g2 )
//    {
//        double tileHeight = 50, tileWidth = 50;
//
//        g2.setColor( Color.green );
//
//        LoopL<Line> flat = new LoopL();
//        for ( Loop<Bar> pll : edges )
//        {
//            Loop<Line> loop = new Loop();
//            flat.add( loop );
//            for ( Loopable<Bar> pp : pll.loopableIterator() )
//            {
//                Line l = new Line (pp.get().start, pp.get().end);
//                loop.append( l );
//                drawLine( g2, l );
//            }
//        }
//
//        DHash<Line, SharedEdge> lineToEdge = new DHash();
//
//        List<Line> allLines = new ArrayList();
//
//        FindBounds2D bounds= new FindBounds2D();
////        for (Line l : flat.eIterator())
////        {
//        Bar line = edges.eIterator().iterator().next();
//        g2.drawLine( (int) line.start.x, (int) line.start.y, (int) line.end.x, (int) line.end.y );
//        show();
////        }
//    }
//
//    public static void main (String[] args)
//    {
//        JFrame frame = new JFrame ("argh");
//        ClipperDebug tc = new ClipperDebug() ;
//        tc.setup();
//        frame.setContentPane( tc );
//        frame.setSize (800,800);
//        frame.setVisible( true );
//        frame.setLocation (900, 100);
//    }
}
