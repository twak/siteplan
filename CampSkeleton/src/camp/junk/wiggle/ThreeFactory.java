package camp.junk.wiggle;


import campskeleton.CampSkeleton;
import campskeleton.FeatureFactory;
import campskeleton.PlanSkeleton;
import campskeleton.Profile;
import campskeleton.ProfileMachine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Tag;
import straightskeleton.HeightEvent;
import straightskeleton.Machine;
import straightskeleton.offset.OffsetSkeleton;
import straightskeleton.Output;
import straightskeleton.Output.Face;
import straightskeleton.Output.SharedEdge;
import straightskeleton.Skeleton;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import utils.Cache;
import utils.ConsecutivePairs;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;
import utils.MultiMap;
import utils.Pair;
import utils.UnionWalker;

/**
 * @author twak
 */
public class ThreeFactory extends FeatureFactory<ThreeFeature>
{
    Set<Double> heightSeens = new HashSet();
    MultiMap<Marker, Matrix4d> points = new MultiMap();
    Profile front, back;

    public ThreeFactory()
    {
        super( null );//CampSkeleton.instance.three );
    }

    public void addToSkeleton( PlanSkeleton skel )
    {
        for ( Profile prof : skel.plan.profiles.values() )
            for ( Bar b : prof.points.eIterator() )
                for ( Marker m : b.mould.markersOn( b ) )
                    if ( m.feature.getClass() == feature.getClass() )
                    {
                        if (heightSeens.add( -m.y ))
                            skel.qu.add( new ThreeFactoryEvent( -m.y ) );
                    }
    }

    public class ThreeFactoryEvent implements HeightEvent
    {

        double height;

        public ThreeFactoryEvent( double d )
        {
            this.height = d;
        }

        public double getHeight()
        {
            return height;
        }

        public boolean process( Skeleton skel_ )
        {
//            PlanSkeleton skel = (PlanSkeleton)skel_;
//            LoopL<Edge> loopl = skel.capCopy( height );
//
//            int maxCount = 0;
//            Loop<Edge> bigSurface = loopl.get( 0 );
//            for ( Loop<Edge> loop : loopl )
//            {
//                int count = loop.count();
//                if ( count > maxCount )
//                {
//                    bigSurface = loop;
//                    maxCount = count;
//                }
//            }
//
//            for (Edge e : loopl.eIterator())
//            {
//                if (e.machine instanceof ProfileMachine)
//                {
//                    ProfileMachine pm = (ProfileMachine)e.machine;
//                    if ( pm.profile.properties.contains("front"))
//                        front = pm.profile;
//                    else if (pm.profile.properties.contains("back"))
//                        back = pm.profile;
//                }
//            }
//
//            Point3d tStart = midPointOfLongestChain( bigSurface, back ),
//                    tEnd = midPointOfLongestChain( bigSurface, front );
//
//            if ( loopl.count() == 0 || tStart == null || tEnd == null )
//                return false; // nothign to do!
//
//            double width = tStart.distance( tEnd );
//
//            // cap everything below
//            for (Corner c : skel.liveCorners)
//            {
//                Corner top = skel.cornerMap.teg( c );
//                skel.output.addOutputSideTo( c, top, c.nextL, c.prevL );
//                skel.output.addOutputSideTo( top, top.nextC, c.nextL );
//            }
//
//            // get rid of this rubbish
//            skel.liveEdges.clear();
//            skel.liveCorners.clear();
//
//            for (Edge e : loopl.eIterator())
//                e.start.z = 0;
//
//            Machine frontMachine = null, backMachine = null;
//            for ( Edge e : loopl.eIterator() )
//                if ( match( e.machine, front ) )
//                    frontMachine = e.machine;
//                else if ( match( e.machine, back ) )
//                    backMachine = e.machine;
//
//
//            OffsetSkeleton offset = new OffsetSkeleton( loopl, 100, new OffsetSkeleton.OffsetEdgeReport()
//            {
//                public void report( Edge input, Edge output, int valency )
//                {
//                }
//            } );
//
//            if ( frontMachine != null )
//                offset.registerProfile( frontMachine, Math.atan( width / 300. ), 0 );
//            if ( backMachine != null )
//                offset.registerProfile( backMachine, Math.atan( width / 300. ), 0 );
//
//            List<LoopL<Edge>> shapes = offset.getResults(true);
//
//            LoopL<Edge> outTriple = OffsetSkeleton.shrink( shapes.get(0) , 1);
//            Edge.reverse( outTriple );
//
//            outTriple.addAll( OffsetSkeleton.shrink( mergeCellsWithMachine( offset.outputShape.output, frontMachine, height ), 1 ) );
//            outTriple.addAll( OffsetSkeleton.shrink( mergeCellsWithMachine( offset.outputShape.output, backMachine, height ), 1 ) );
//
//            Machine triRoofMachine = new Machine ( 0.9 );
//
//            Edge.reverse( outTriple );
//
//            for (Edge e : outTriple.eIterator())
//            {
//                e.machine = triRoofMachine;
//                e.profileFeatures.add( CampSkeleton.instance.roof );
//            }
//
////            DebugWindow.showIfNotShown( outTriple );
//
//
//            skel.insertPlanAtHeight( outTriple, height );

            return true;
        }


        public boolean old (LoopL<Edge> loopl, Skeleton skel)
        {
            int maxCount = 0;
            Loop<Edge> bigSurface = loopl.get( 0 );
            for ( Loop<Edge> loop : loopl )
            {
                int count = loop.count();
                if ( count > maxCount )
                {
                    bigSurface = loop;
                    maxCount = count;
                }
            }

            Point3d tStart = midPointOfLongestChain( bigSurface, back ),
                    tEnd = midPointOfLongestChain( bigSurface, front );

            Vector3d diff = new Vector3d( tEnd );
            diff.sub( tStart );
            double tLength = diff.length();
            diff.normalize();


            List<Point3d> centrums = new ArrayList();
            List<Vector3d> tangents = new ArrayList();
            
            for ( double d = 1 / 6.; d < 0.99; d += 1 / 3. )
            {
                Vector3d dir = new Vector3d( diff );
                dir.scale( tLength * d );
                Point3d c = new Point3d( tStart );
                c.add( dir );
                centrums.add( c );
                tangents.add( new Vector3d( diff ) );
            }

            skel.liveCorners.clear();
            skel.liveEdges.clear();

            Machine side = new Machine (Math.PI/2+0.01);
            Machine forward = new Machine (Math.PI/5);

            LoopL<Edge> newEdges = cellurize( loopl, centrums, tangents, new Machine (Math.PI/4),
                     side, forward, side, forward);

            for (Edge e : newEdges.eIterator())
            {
                e.start.z = height;
                skel.liveCorners.add( e.start );
                skel.liveEdges.add( e );
                e.start.nextC = e.end;
                e.end.prevC = e.start;
                e.start.nextL = e;
                e.end.prevL = e;

            }

            skel.refindAllFaceEventsLater();

            return true;
        }

        public LoopL<Edge> cellurize( LoopL<Edge> boundary, List<Point3d> centrums, List<Vector3d> majorDirs, Machine newMachine, Machine ... machines )
        {
//            LoopL<Edge> out = new LoopL();
//            assert (centrums.size() == majorDirs.size());
//
//            List<Edge>[] edges = new List[centrums.size()];
//
//            Machine nullMachine = new Machine (0);
//            for (Edge e : boundary.eIterator())
//            {
//                e.start.z = 0;
//                e.machine = nullMachine;
//            }
//
//            for ( int x = 0; x < centrums.size(); x++ )
//            {
//                Vector3d majorDir = majorDirs.get( x );
//                Point3d center = centrums.get( x );
//
//                // unit square based in this house
//                Vector2d tangent = new Vector2d (majorDir.x, majorDir.y);
//                Vector2d t = new Vector2d( tangent );
//                Vector2d b = new Vector2d( tangent );
//                b.scale( -1 );
//                Vector2d l = new Vector2d( tangent.y, -tangent.x );
//                Vector2d r = new Vector2d( l );
//                r.scale( -1 );
//
//                Point2d start = new Point2d( center.x, center.y );
//                Corner tl = new Corner( start.x, start.y );
//                start.add( t );
//                Corner tr = new Corner( start.x, start.y );
//                start.add( r );
//                Corner br = new Corner( start.x, start.y );
//                start.add( b );
//                Corner bl = new Corner( start.x, start.y );
//
//
//                Loop<Edge> cell = new Loop();
//                boundary.add( cell );
//
//                edges[x] = new ArrayList();
//
//                int j = 0;
//                for ( Pair<Corner, Corner> pair : new ConsecutivePairs<Corner>( Arrays.asList( tl, bl, br, tr ), true ) )
//                {
//                    Edge e = new Edge( pair.first(), pair.second() );
//                    edges[x].add( e );
//                    e.machine = machines[j++];
//                    cell.append( e );
//                }
//            }
//
////            DebugWindow.showIfNotShown( boundary );
//
//            Skeleton s = new Skeleton( boundary );
//            s.skeleton();
//            Output output= s.output;
//
//            for ( int x = 0; x < centrums.size(); x++ )
//            {
//                Set<Face> faces = new HashSet();
//                for ( Edge e : edges[x] )
//                    faces.add( output.faces.get( e ) );
//
//                UnionWalker uw = new UnionWalker();
//                boolean first = true;
//
//                Map<Point2d, Integer> adjacencies = new LinkedHashMap();
//
//                for ( int i = 0; i < 4; i++ )
//                {
//                    Face f = output.faces.get( (Edge)edges[x].get( i ) );
//
//                    if (f != null)
//                    for ( SharedEdge se : f.edges.eIterator() )
//                        if ( !faces.contains( se.getOther( f ) ) )
//                        {
//                            Point3d start = se.getStart( f );
//                            Point3d end = se.getEnd( f );
//                            Point2d start2;
//                            uw.addEdge( start2 = new Point2d( start.x, start.y ), new Point2d( end.x, end.y ), first );
//                            first = false;
//                            Face over = se.getOther( f );
//                            //fixme: where do we loose the topMachine here?
////                        adjacencies.put (start2, over == null? 0 : ? 1 : 2); //f.edge.machine == h.machine[0]
////                            if ( over != null )
////                                if ( over.edge.angle == 0 )
////                                    if ( over.edge.machine == endMachine )
////                                        adjacencies.put( start2, 2 );
////                                    else
////                                        adjacencies.put( start2, over.edge.machine == topMachine ? 0 : 1 );
////                                else
////                                    adjacencies.put( start2, 2 );
////                            else
////                                adjacencies.put( start2, 3 );
//                        }
//                }
//
//                LoopL<Point2d> flatPoints = uw.find();
//
//                Cache<Point2d, Corner> cache = new Cache<Point2d, Corner>()
//                {
//
//                    @Override
//                    public Corner create( Point2d i )
//                    {
//                        return new Corner( i.x, i.y, 0 );
//                    }
//                };
//
//
//                for ( Loop<Point2d> loop : flatPoints )
//                {
//                    Loop<Edge> plotEdge = new Loop();
//                    out.add( plotEdge );
//                    for ( Loopable<Point2d> loopable : loop.loopableIterator() )
//                    {
//                        Edge e = new Edge( cache.get( loopable.get() ), cache.get( loopable.getNext().get() ) );
//                        e.machine = newMachine;
//                        plotEdge.append( e );
//                    }
//                }
//            }
//
//            Edge.reverse( out );
//
//            return out;
            return null;
        }

        void mergeFootprints( Skeleton skel, List<House> houses, Machine topMachine, Machine endMachine )
        {
            Output output = skel.output;
            for ( House h : houses )
            {
                Set<Face> faces = new HashSet();
                for ( Edge e : h.base )
                    faces.add( output.faces.get( e ) );

                UnionWalker uw = new UnionWalker();
                boolean first = true;

                Map<Point2d, Integer> adjacencies = new LinkedHashMap();

                for ( int i = 0; i < 4; i++ )
                {
                    Face f = output.faces.get( h.base[i] );

                    for ( SharedEdge se : f.edges.eIterator() )
                        if ( !faces.contains( se.getOther( f ) ) )
                        {
                            Point3d start = se.getStart( f );
                            Point3d end = se.getEnd( f );
                            Point2d start2;
                            uw.addEdge( start2 = new Point2d( start.x, start.y ), new Point2d( end.x, end.y ), first );
                            first = false;
                            Face over = se.getOther( f );
                            //fixme: where do we loose the topMachine here?
//                        adjacencies.put (start2, over == null? 0 : ? 1 : 2); //f.edge.machine == h.machine[0]
                            if ( over != null )
                                if ( over.edge.getAngle() == 0 )
                                    if ( over.edge.machine == endMachine )
                                        adjacencies.put( start2, 2 );
                                    else
                                        adjacencies.put( start2, over.edge.machine == topMachine ? 0 : 1 );
                                else
                                    adjacencies.put( start2, 2 );
                            else
                                adjacencies.put( start2, 3 );
                        }
                }

                LoopL<Point2d> flatPoints = uw.find();

                Cache<Point2d, Corner> cache = new Cache<Point2d, Corner>()
                {

                    @Override
                    public Corner create( Point2d i )
                    {
                        return new Corner( i.x, i.y, 0 );
                    }
                };


                for ( Loop<Point2d> loop : flatPoints )
                {
                    Loop<Edge> plotEdge = new Loop();
                    h.plot.add( plotEdge );
                    for ( Loopable<Point2d> loopable : loop.loopableIterator() )
                    {
                        Edge e = new Edge( cache.get( loopable.get() ), cache.get( loopable.getNext().get() ) );
                        e.machine = h.machine[adjacencies.get( loopable.get() )];
                        plotEdge.append( e );
                    }
                }
                Edge.reverse( h.plot );
            }
        }

        boolean match (Machine machine, Profile profile)
        {
            if (machine instanceof ProfileMachine)
            {
                ProfileMachine pm = (ProfileMachine)machine;
                return pm.profile == profile;
            }
            return false;
        }

         Point3d midPointOfLongestChain( Loop<Edge> surface, Profile profile )
        {
            double longestChain = 0, chainLength = 0;
            Loopable<Edge> start = null, currentChainStart = null;

            for ( Loopable<Edge> loopable : surface.loopableIterator() )
            {
                Edge e = loopable.get();
                if ( match ( e.machine, profile ) )
                {
                    if ( currentChainStart == null )
                        currentChainStart = loopable;

                    chainLength+=e.start.distance( e.end);
                    if ( chainLength > longestChain )
                    {
                        start = currentChainStart;
                        longestChain = chainLength;
                    }
                }
                else
                {
                    chainLength = 0;
                    currentChainStart = null;
                }
            }

            if ( start == null )
                return new Point3d( surface.getFirst().start );

            Loopable<Edge> g = start;
            longestChain /= 2.;

            g = start;
            double lengthSoFar = 0;
            while ( match ( g.get().machine, profile ) )
            {
                double edgeL = g.get().start.distance( g.get().end );
                lengthSoFar += edgeL;
                if ( lengthSoFar > longestChain )
                {
                    double reverse = lengthSoFar - longestChain;
                    Vector3d end = new Vector3d( g.get().start );
                    end.sub( g.get().end );
                    end.normalize();
                    end.scale( reverse );

                    Point3d out = new Point3d( g.get().end );
                    out.add( end );
                    return out;
                }
                g = g.getNext();
            }
            return null;
        }

        private LoopL<Edge> mergeCellsWithMachine( Output output, Machine frontMachine, final double h )
        {
            Set<Face> toMerge = new HashSet();
            for (Face f :output.faces.values())
            {
                if (f.edge.machine == frontMachine)
                {
                    toMerge.add( f );
                }
            }

            Cache<Point3d, Corner> cache = new Cache<Point3d, Corner>() {

                @Override
                public Corner create( Point3d i )
                {
                    return new Corner (i.x, i.y, h);
                }
            };

            Map<Corner, Corner> exteriorEdges = new LinkedHashMap();

            /** find all exterior edges */
//            while ( !toMerge.isEmpty() )
            for ( Face f : toMerge )
                for ( SharedEdge se : f.edges.eIterator() )
                    if ( se.getOther( f ) == null || !( toMerge.contains( se.getOther( f ) ) ) )
                        exteriorEdges.put( cache.get( se.getStart( f ) ), cache.get( se.getEnd( f ) ) );

           LoopL<Edge> out = new LoopL();
           while (!exteriorEdges.isEmpty())
           {
               Loop<Edge> loop = new Loop();
               out.add( loop );

               Corner start = exteriorEdges.keySet().iterator().next();
               Corner last = null;
               while (exteriorEdges.containsKey( start ))
               {
                   start = exteriorEdges.remove( start );

                   if (last != null)
                   {
                       loop.append( new Edge (last, start));
                   }
                   last = start;
               }
               loop.append( new Edge ( last, loop.getFirst().start) );
           }

           for (Edge e : out.eIterator())
           {
               e.start.nextL = e;
               e.end.prevL = e;
               e.start.nextC = e.end;
               e.end.prevC = e.start;
               e.machine = new Machine();
           }

           return out;
        }
    }
}
