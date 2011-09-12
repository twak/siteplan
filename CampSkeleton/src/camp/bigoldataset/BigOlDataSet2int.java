package camp.bigoldataset;


import camp.jme.JmeFace;
import camp.jme.JmeObjDump;
import campskeleton.CampSkeleton;
import campskeleton.Global;
import campskeleton.Plan;
import campskeleton.PlanSkeleton;
import campskeleton.Profile;
import com.jme.scene.Node;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point2d;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.parser.DefaultPointsHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.PathParser;
import org.apache.batik.parser.PointsHandler;
import org.apache.batik.parser.PointsParser;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import straightskeleton.Output.Face;
import straightskeleton.ui.Bar;
import utils.Cache2;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;
import utils.PolygonArea;

/**
 * @author twak
 */
public class BigOlDataSet2int
{

    public static Atlantis floatAtlantis()
    {
        Map<Loop<Point2d>, Integer> numbers = new HashMap();
        LoopL<Point2d> loopl = loadGeom(numbers);
        Map<Integer, Double> heights = loadHeights();

        Atlantis at = new Atlantis();
        Profile prof = new Profile( 100 );

        Global root = new Global( "0" );
        root.valency = 1;

        prof.name = "default";
        at.profiles.add( prof );
        Profile.GlobalProfile gp = prof.getGlobalProfile( root );
        gp.chainStarts.add( prof.points.get( 0 ));



        Global g = new Global("v");
        g.valency = 2;


        for (int xLoc : Arrays.asList( new Integer[] { -50,0} ) )
        {
            Loop<Bar> loop = new Loop();
            Point2d loc = new Point2d (-50, -50);
            loop.append( new Bar( new Point2d( loc.x + xLoc, loc.y ), new Point2d( loc.x + 50 + xLoc, loc.y - 100 ) ) );
            prof.points.add( loop );
        }

        prof.getGlobalProfile( g ).enabled = false; // until futher notice
            prof.getGlobalProfile(g).chainStarts.add(prof.points.get(1));
            prof.getGlobalProfile(g).chainStarts.add(prof.points.get(2));


        int i = 0;
        for (Loop<Point2d> loop : loopl) {
//            if (Math.random() > 0.1)
//                continue;

            Loop<Bar> barLoop = new Loop();

            if (loop.count() == 0) {
                continue;
            }

            double height = 5;

            height = heights.get(numbers.get(loop));

            Plot plot = new Plot(barLoop, height, root);
            at.plots.add(plot);
            plot.globals.add( g );


//            plot.addLoop( prof.points.get( 1 ), g, prof ); way it was on sunday
//            plot.addLoop( prof.points.get( 2 ), g, prof );

            PolygonArea pa = new PolygonArea();

            for ( Loopable<Point2d> pt : loop.loopableIterator() )
            {
                Bar b = new Bar( pt.get(), pt.getNext().get() );
                pa.add(b.start);
                plot.profiles.put( b, prof );
                barLoop.append( b );
            }
            plot.area = pa.area();
            if (plot.area < 0)
                System.out.println("that's funny");

            System.out.println(i + " from " + loopl.size() +" size "+plot.area);
            i++;
            CampSkeleton.removeParallel( plot.points );
        }

        return at;
    }
    
    public static void oldain (String[] args)
    {

        Map<Integer, Double> heights = loadHeights();


        Logger.getLogger(Node.class.getName()).setLevel( Level.OFF );



         Map<Loop<Point2d>, Integer> numbers= new HashMap();


        LoopL<Point2d> loopl = loadGeom(numbers);

        JmeObjDump oDump = new JmeObjDump();

            CampSkeleton.instance = new CampSkeleton( true );

            dumpToSVG( loopl );


            if (false)
            {

        int i = 0;
        for (Loop<Point2d> loop : loopl)
        {
            System.out.println ( i +" from "+ loopl.size() );
            i++;
            Plan plan = new Plan();
            plan.name = "root (cunning) plan";

            Loop<Bar> barLoop = new Loop();
            plan.points.add( barLoop );

            if (loop.count() == 0)
                continue;

            double height = 5;
            
            height = heights.get( numbers.get( loop ) );

            height *= 2;

            Profile defaultProf = new Profile( height );
//            defaultProf.points.get( 0 ).start.get().end.x += 20;
            defaultProf.points.get(0).append( new Bar (  defaultProf.points.get( 0 ).getFirst().end, new Point2d (10, - height - 10) ) );


            plan.addLoop( defaultProf.points.get(0), plan.root, defaultProf );

            for (Loopable <Point2d> pt : loop.loopableIterator() )
            {
                Bar b = new Bar ( pt.get(), pt.getNext().get() );
                barLoop.append( b );
                plan.profiles.put( b, defaultProf );
            }

            CampSkeleton.removeParallel( plan.points );



            try
            {
                PlanSkeleton ps = new PlanSkeleton( plan );
                ps.skeleton();
                if ( ps.output != null )
                    for ( Face f : ps.output.faces.values() )
                        try
                        {
                            JmeFace jme = new JmeFace( f );
                            Node n = new Node();
                            n.attachChild( jme );
                            oDump.add( n );
                        }
                        catch ( Throwable t )
                        {
//                            t.printStackTrace();
                        }
            }
            catch ( Throwable t )
            {
                t.printStackTrace();
            }
//            break;
        }

        File f = new File ( "/home/twak/wooo2.obj" );
        oDump.allDone( f );
            }
    }


    public static LoopL<Point2d> loadGeom( Map<Loop<Point2d>, Integer> numbers )
    {

        BufferedReader fr= null;
        LoopL<Point2d> loopl = new LoopL();

        Cache2<Double, Double, Point2d> cCache = new Cache2<Double, Double, Point2d>() {
            @Override
            public Point2d create(Double i1, Double i2) {
                return new Point2d(i1, i2);
            }
        };

        try
        {
//            fr = new BufferedReader ( new FileReader( new File( "blocksklobg1.txt" ) ) );
            fr = new BufferedReader ( new FileReader( new File( "atlblocks1.txt" ) ) );
            String line = "";

            Loop loop = new Loop();
            loopl.add(loop);

            while ((line = fr.readLine()) != null)
            {
//                StringTokenizer st = new StringTokenizer(line, ",");
                String[] splits = line.split( ",\\ " );
                List <Double> vals = new ArrayList();
                for (String number : splits )
                {
                    if (Character.isDigit( number.charAt( 0 ) ) || number.charAt( 0 ) == '-' )
                    {
                        try
                        {
                            vals.add( Double.parseDouble( number ) );
                        }
                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        loop.reverse();
                        loop = new Loop();
                        loopl.add( loop );
                    }
                }
                if (vals.size() == 4)
                {
                    numbers.put (loop, vals.remove(0).intValue() );
                }

                if (vals.size() == 3)
                    loop.append( cCache.get (vals.get(0), vals.get(1)));
            }
            loop.reverse();
        }
        catch ( Exception ex )
        {
            Logger.getLogger( BigOlDataSet2int.class.getName() ).log( Level.SEVERE, null, ex );
        }
        finally
        {
            try
            {
                fr.close();
            }
            catch ( IOException ex )
            {
                Logger.getLogger( BigOlDataSet2int.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }

        return loopl;
    }

    static Map<Integer, Double> loadHeights()
    {
        Map<Integer, Double> out = new HashMap();

        BufferedReader fr = null;

        try
        {
            fr = new BufferedReader ( new FileReader( new File( "atlblocks1_table.txt" ) ) );
//            fr = new BufferedReader ( new FileReader( new File( "blocksklobg1_table.txt" ) ) );
            String line = "";


            while ((line = fr.readLine()) != null)
            {
//                StringTokenizer st = new StringTokenizer(line, ",");
                String[] splits = line.split( "," );
                List <Double> vals = new ArrayList();
                for (String number : splits )
                {
                    if (Character.isDigit( number.charAt( 0 ) ) || number.charAt( 0 ) == '-' )
                    {
                        try
                        {
                            vals.add( Double.parseDouble( number ) );
                        }
                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
                if (vals.size() > 0)
                    out.put( vals.get( 0 ).intValue(), vals.get(1));
            }
        }
        catch ( Exception ex )
        {
            Logger.getLogger( BigOlDataSet2int.class.getName() ).log( Level.SEVERE, null, ex );
        }
        finally
        {
            try
            {
                fr.close();
            }
            catch ( IOException ex )
            {
                Logger.getLogger( BigOlDataSet2int.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
        return out;
    }

    private static void dumpToSVG( LoopL<Point2d> loopl )
    {
        try
        {
            DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
            // Create an instance of org.w3c.dom.Document.
            String svgNS = "http://www.w3.org/2000/svg";
            Document document = domImpl.createDocument( svgNS, "svg", null );
            // Create an instance of the SVG Generator.
            SVGGraphics2D svgGenerator = new SVGGraphics2D( document );

            svgGenerator.setColor( Color.red );

            double scale = 1;
            for ( Loop<Point2d> loop : loopl )
            {
                Polygon poly = new Polygon();
                for (Point2d pt : loop)
                    poly.addPoint( (int)(scale * pt.x), (int)(scale * pt.y ) );
                svgGenerator.fill( poly );
            }
            // Finally, stream out SVG to the standard output using
            // UTF-8 encoding.
            boolean useCSS = true; // we want to use CSS style attributes
            Writer out = new FileWriter( "city.svg") ;//OutputStreamWriter( System.out, "UTF-8" );
            svgGenerator.stream( out, useCSS );
        }
        catch ( Throwable ex )
        {
            ex.printStackTrace();
        }

    }

}
