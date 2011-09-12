package camp.bigoldataset;

import campskeleton.Global;
import campskeleton.Profile;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point2d;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGOMPathElement;
import org.apache.batik.dom.svg.SVGOMSVGElement;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.PathParser;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import straightskeleton.ui.Bar;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;

/**
 *
 * @author twak
 */
public class SVG2Int {

    Global root,g;
    Atlantis at = new Atlantis();
    Profile prof = new Profile(100);

    public SVG2Int()
    {
        root = new Global("r");
        root.valency = 1;
        prof.name = "default";
        at.profiles.add(prof);
        Profile.GlobalProfile gp = prof.getGlobalProfile(root);
        gp.chainStarts.add(prof.points.get(0));
        root.valency = 1;

        g = new Global("v");
        g.valency = 2;


        for (int xLoc : Arrays.asList(new Integer[]{-50, 0})) {
            Loop<Bar> loop = new Loop();
            Point2d loc = new Point2d(-50, -50);
            loop.append(new Bar(new Point2d(loc.x + xLoc, loc.y), new Point2d(loc.x + 50 + xLoc, loc.y - 100)));
            prof.points.add(loop);
        }

        prof.getGlobalProfile(g).enabled = false; // until futher notice
        
        prof.getGlobalProfile(g).chainStarts.add(prof.points.get(1));
        prof.getGlobalProfile(g).chainStarts.add(prof.points.get(2));

        processLineSegments("city.svg", new PlotPathHandler());
        processLineSegments("roads.svg", new RoadPathHandler());
    }

        public void processLineSegments(String name, PathHandler pa)
        {
        try
        {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory( parser );
            URI uri = new File( name ).toURI(); // the URI of your SVG document
            Document doc = f.createDocument( uri.toString() );
            UserAgent userAgent;
            DocumentLoader loader;
            BridgeContext ctx;
            GVTBuilder builder;
            GraphicsNode rootGN;
            userAgent = new UserAgentAdapter();
            loader = new DocumentLoader( userAgent );
            ctx = new BridgeContext( userAgent, loader );
            ctx.setDynamicState( BridgeContext.DYNAMIC );
            builder = new GVTBuilder();
            rootGN = builder.build( ctx, doc );
            SVGOMSVGElement myRootSVGElement = (SVGOMSVGElement) doc.getDocumentElement();
            //I want all the "path" elements for example
            NodeList nl = myRootSVGElement.getElementsByTagName( "path" );
//            System.out.println("float[] shape = new float[]{");
            visit( myRootSVGElement, 0,  pa );
        }
        catch ( IOException ex )
        {
            Logger.getLogger( SVG2Int.class.getName() ).log( Level.SEVERE, null, ex );
        }

    }

    public void visit (Node node, int depth, PathHandler pa )
    {
        NodeList nl = node.getChildNodes();

        for ( int i = 0; i < nl.getLength (); ++i )
        {
            Node elt = (Node) nl.item ( i );

            if (elt instanceof SVGOMPathElement)
            {
                NamedNodeMap nnm = elt.getAttributes();

                final LinkedList points = new LinkedList();
//                StrangeFormat pp = new StrangeFormat();
                PathParser pp = new PathParser();
//                PointsHandler ph = new DefaultPointsHandler() {
//
//                    public void point(float x, float y) throws ParseException {
//                        System.out.println( "{"+x+","+y+"},");
//                    }
//                };

                pp.setPathHandler( pa );//pointsHandler(ph);
                pp.parse( nnm.getNamedItem("d").getTextContent() );

            }

            visit(elt, depth+1, pa);
        }
    }

    public Atlantis get()
    {
        return at;
    }

    private class PlotPathHandler extends DefaultPathHandler {

        // move to rel and line to rel. with start path at start, end path at end and close path between paths
        LoopL<Point2d> loopl;
        Loop<Point2d> loop;
        Point2d current;

        public PlotPathHandler() {
        }

        public void startPath() throws ParseException {
            loopl = new LoopL<Point2d>();
        }

        public void endPath() throws ParseException
        {
            LoopL<Bar> bLoopL = new LoopL();
            Plot p = new Plot (bLoopL, -0xDEADBEEF, root );
            p.points = bLoopL;
            p.globals.add( g );

            for (Loop<Point2d> loop : loopl)
            {
               Loop<Bar> bLoop = new Loop(); //loop.count()
               bLoopL.add(bLoop);

               Set<Loopable<Point2d>> togo = new HashSet();
               for (Loopable<Point2d> lo : loop.loopableIterator())
               {
                   Bar b = new Bar (lo.get(), lo.getNext().get());

                   // some data has start == end?!
                   if (b.start.equals(b.end)) {
                       togo.add(lo);
                   }
               }
               for (Loopable<Point2d> pt : togo)
                loop.remove(pt);

               if (loop.count() >= 3)
               for (Loopable<Point2d> lo : loop.loopableIterator())
               {
                   Bar b = new Bar (lo.get(), lo.getNext().get());

//                   if (!b.start.equals(b.end)) {
                       p.profiles.put(b, prof);
                       bLoop.append(b);
//                   }
               }

            }

            at.plots.add(p);
        }

        public void movetoRel(float arg0, float arg1) throws ParseException {
            loop = new Loop();
            loopl.add(loop);
            current = new Point2d(current.x + arg0, current.y + arg1);
            loop.append( current );
        }

        public void movetoAbs(float x, float y) throws ParseException {
            loop = new Loop();
            loopl.add( loop );
            current = new Point2d( x, y );
            loop.append( current );
        }

        public void closePath() throws ParseException {
            loop = null;

        }

        public void linetoRel(float arg0, float arg1) throws ParseException {
            current = new Point2d(current);
            current.add(new Point2d(arg0, arg1));
            loop.append(current);
        }

        public void linetoAbs(float x, float y) throws ParseException {
            current = new Point2d(x, y);
//            current.add();
            loop.append(current);
        }

    }

     private class RoadPathHandler extends DefaultPathHandler {
        Point2d last;

        public RoadPathHandler() {
        }

        public void startPath() throws ParseException {
            
        }

        public void endPath() throws ParseException
        {
        }

        public void movetoRel(float arg0, float arg1) throws ParseException {
            last = new Point2d(arg0, arg1);
        }

        public void movetoAbs(float x, float y) throws ParseException {
            last = new Point2d( x, y );
        }

        public void closePath() throws ParseException {
            last = null;

        }

        public void linetoRel(float arg0, float arg1) throws ParseException {
            Point2d neu = new Point2d(last);
            neu.add(new Point2d(arg0, arg1));

            at.roadPoints.add (neu);
            at.roadPoints.add (last);
            at.roads.add( neu, last );

            last = neu;
        }

        public void linetoAbs(float x, float y) throws ParseException {
            Point2d next = new Point2d(x,y);
            at.roadPoints.add(next);
            at.roadPoints.add(last);
            at.roads.add(last, next);
            last = next;
        }
    }
}
