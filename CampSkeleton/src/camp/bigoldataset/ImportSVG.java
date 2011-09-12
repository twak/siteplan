package camp.bigoldataset;

import campskeleton.Plan;
import campskeleton.Profile;
import java.io.File;
import java.io.IOException;
import java.net.URI;
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
public class ImportSVG
{

    public ImportSVG( File svgFile, Plan plan )
    {
        processLineSegments( svgFile, new PlotPathHandler( plan, plan.profiles.values().iterator().next() ) );
    }

    public void processLineSegments( File name, PathHandler pa )
    {
        try
        {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory( parser );
            URI uri = name.toURI();
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
            NodeList nl = myRootSVGElement.getElementsByTagName( "path" );
            visit( myRootSVGElement, 0, pa );
        } catch ( IOException ex )
        {
            Logger.getLogger( ImportSVG.class.getName() ).log( Level.SEVERE, null, ex );
        }

    }

    public void visit( Node node, int depth, PathHandler pa )
    {
        NodeList nl = node.getChildNodes();

        for ( int i = 0; i < nl.getLength(); ++i )
        {
            Node elt = (Node) nl.item( i );

            if ( elt instanceof SVGOMPathElement )
            {
                NamedNodeMap nnm = elt.getAttributes();

                final LinkedList points = new LinkedList();
                PathParser pp = new PathParser();

                pp.setPathHandler( pa );
                pp.parse( nnm.getNamedItem( "d" ).getTextContent() );

            }

            visit( elt, depth + 1, pa );
        }
    }

    private class PlotPathHandler extends DefaultPathHandler
    {

        // move to rel and line to rel. with start path at start, end path at end and close path between paths
        LoopL<Point2d> loopl;
        Loop<Point2d> loop;
        Point2d current = new Point2d(0,0);
        Profile profile;
        Plan plan;

        public PlotPathHandler( Plan plan, Profile profile )
        {
            this.plan = plan;
            this.profile = profile;
        }

        public void startPath() throws ParseException
        {
            loopl = new LoopL<Point2d>();
        }

        public void endPath() throws ParseException
        {
            for ( Loop<Point2d> loop : loopl )
            {
                Loop<Bar> bLoop = new Loop(); //loop.count()
                plan.points.add( bLoop );

                Set<Loopable<Point2d>> togo = new HashSet();
                for ( Loopable<Point2d> lo : loop.loopableIterator() )
                {
                    Bar b = new Bar( lo.get(), lo.getNext().get() );

                    // some data has start == end?!
                    if ( b.start.equals( b.end ) )
                        togo.add( lo );
                }

                for ( Loopable<Point2d> pt : togo )
                    loop.remove( pt );

                if ( loop.count() >= 3 )
                    for ( Loopable<Point2d> lo : loop.loopableIterator() )
                    {
                        Bar b = new Bar( lo.get(), lo.getNext().get() );

                        plan.profiles.put( b, profile );
                        bLoop.append( b );
                    }

            }
        }

        public void movetoRel( float arg0, float arg1 ) throws ParseException
        {
            loop = new Loop();
            loopl.add( loop );
            current = new Point2d( current.x + arg0, current.y + arg1 );
            loop.append( current );
        }

        public void movetoAbs( float x, float y ) throws ParseException
        {
            loop = new Loop();
            loopl.add( loop );
            current = new Point2d( x, y );
            loop.append( current );
        }

        public void closePath() throws ParseException
        {
            loop = null;

        }

        public void linetoRel( float arg0, float arg1 ) throws ParseException
        {
            current = new Point2d( current );
            current.add( new Point2d( arg0, arg1 ) );
            loop.append( current );
        }

        public void linetoAbs( float x, float y ) throws ParseException
        {
            current = new Point2d( x, y );
//            current.add();
            loop.append( current );
        }
    }
}
