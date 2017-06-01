package org.twak.siteplan.anchors;

import java.util.HashSet;
import java.util.Set;

import org.twak.camp.Output.Face;
import org.twak.camp.Output.SharedEdge;

/**
 * Given a set of faces, and certain shared edges, marked for merging, we must
 * return groups of faces to be merged
 *
 * @author twak
 */
class FaceMergeGraph
{

    Set<Face> faces = new HashSet();
    Set<Face> reversed = new HashSet();
    Set<SharedEdge> toMerge = new HashSet();

    /**
     * @param f
     * @param reverse
     */
    public void add( Face f, boolean reverse )
    {
        faces.add(f);
        
        if ( reverse )
            reversed.add( f );
    }

    public Set <Face> findAdjacent( Face start )
    {
        Set<Face> out = new HashSet();
        findAdjacent( start, out );
        return out;
    }

    private void findAdjacent( Face f, Set<Face> adjacent )
    {
        adjacent.add( f );

        for ( SharedEdge se : f.edges.eIterator() )
        {
            Face f2 = se.getOther( f );
            if ( f2 != null && toMerge.contains( se ) && !adjacent.contains( f2 ) )
                findAdjacent( f2, adjacent );
        }
    }

    /**
     *
     * @param se a shared edge that has already been added (above).
     * @param fileProfile the profile to associate with the edge, in results, below
     * @param merge should this edge be merged with it's neighbours? If any shared edges are merged, the faces are merged in the output
     */
    public void registerMerge( SharedEdge se )
    {
        toMerge.add( se );
    }

    /**
     *
     * @param height
     * @return A list of corners whose associated edges contain ProfileMachine
     */
    public Set<Set<Face>> getResults()
    {
        Set<Set<Face>> out = new HashSet();

        // list of processed faces
        Set<Face> togo = new HashSet( faces );
        while ( !togo.isEmpty() )
        {
            // pick any old face
            Face f = togo.iterator().next();
            Set<Face> set = new HashSet<Face>();
            out.add(set);
            // find all faces that we'll merge
            findAdjacent( f, set );
            togo.removeAll( set );

        }
        
        return out;
    }

    public boolean isReversed( Face f )
    {
        return reversed.contains( f );
    }
    /**
    // use point3d instead of corner to get correct hashing behaviour
    DAGMeta<Point3d, ProfileMachine> dm = new DAGMeta();

    // build graph from set of faces to merge
    for ( Face face : set )
    {
    DebugDevice.dumpPoints( "a face", face.getLoopL() );

    for ( SharedEdge se : face.edges.eIterator() )
    {

    Point3d start = reversed.contains( face ) ? se.getStart( face ) : se.getEnd( face );
    Point3d end = reversed.contains( face ) ? se.getEnd( face ) : se.getStart( face );
    // profile map isn't reverse
    ProfileMachine p = profiles.get( se.getStart( face ), se.getEnd( face ) );
    dm.add( start, end, p );
    }
    }

    // remove returning edges of graph to give union
    DebugDevice.dumpPoints( "dm before", (LoopL<Point3d>) dm.debug() );
    dm.removeReturning();
    DebugDevice.dumpPoints( "dm after", dm.debug() );

    // a list of all the points we have to visit
    Set<Point3d> startPoints = new HashSet( dm.map.keySet() );

    // now traverse dm to create the boundaries:- output is an arbitrary region on the plane
    while ( !startPoints.isEmpty() )
    {
    Loop<Corner> loop = new Loop();
    out.add( loop );

    Point3d start = startPoints.iterator().next();
    Point3d prev = start;

    Cache<Point3d, Corner> cCache = new Cache<Point3d, Corner>()
    {

    @Override
    public Corner create( Point3d i )
    {
    return new Corner( i.x, i.y, 0 );
    }
    };

    while ( true )
    {
    List<DAGMeta.ArcInfo<Point3d, ProfileMachine>> aiL = dm.get( prev );
    DAGMeta.ArcInfo<Point3d, ProfileMachine> ai = aiL.get( 0 );

    Corner s = cCache.get( prev ),
    e = cCache.get( ai.e );


    startPoints.remove( prev ); // startPoints.size()

    loop.append( e );
    Edge edge = new Edge( s, e );
    s.nextL = edge;
    e.prevL = edge;
    s.nextC = e;
    e.prevC = s;
    edge.machine = ai.d;

    if ( ai.e == start )
    //                        if (True)
    break;

    prev = ai.e;
    }
    }
    }

     */
}
