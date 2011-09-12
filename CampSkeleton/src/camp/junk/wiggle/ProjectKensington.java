/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ProjectKensington.java
 *
 * Created on Sep 20, 2009, 9:29:10 PM
 */
package camp.junk.wiggle;

import campskeleton.CampSkeleton;
import campskeleton.Plan;
import campskeleton.Profile;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.Tag;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import utils.Cache;
import utils.Loop;
import utils.LoopL;
import utils.Loopable;

/**
 *
 * @author twak
 */
public class ProjectKensington extends javax.swing.JFrame implements WiggleUI.HouseListChanged
{

    public static ProjectKensington instance = null;
    WiggleUI wiggle;
    CampSkeleton campSkeleton;
    //HACK --insta-callback
    public Profile front, back;
    //end HACK

        Tag roof = new Tag("roof");

    /** Creates new form ProjectKensington */
    public ProjectKensington()
    {
        instance = this;
        initComponents();

        wiggle = new WiggleUI();
        wiggle.setup();
        wiggle.houseListChanged = this;

        editorPanel.setLayout( new GridLayout( 1, 1 ) );
        editorPanel.add( wiggle );

        campSkeleton = new CampSkeleton();
        campSkeleton.setVisible( true );

        setSize( 800, 800 );
        setVisible( true );
    }
    List<House> houses;

    public void houseListChanged( List<House> houses )
    {
        this.houses = houses;
        DefaultListModel lm = new DefaultListModel();
        for ( House h : houses )
            lm.add( 0, h );

        houseList.setModel( lm );
    }
    Plan plan;

    public Plan makePlan( House h )
    {
        plan = new Plan();
        plan.name = "root (cunning) plan";

        removeParallel( h.plot );


        Profile defaultProf = new Profile( 100 );
        defaultProf.points.get( 0 ).start.get().end.x += 20;

        plan.points = new LoopL();

        Cache<Corner, Point2d> cache = new Cache<Corner, Point2d>()
        {

            @Override
            public Point2d create( Corner i )
            {
                return new Point2d( i.x, i.y );
            }
        };

//        Tag door = new MeshFeature("door.md5");
//        plan.tags.add(door);
//
//        plan.tags.add ( roof );
//
//        WindowFeature window1 = new WindowFeature( "first floor window" );
//        plan.tags.add( window1 );
//
//        WindowFeature window2 = new WindowFeature( "second floor window" );
//        plan.tags.add( window2 );
//
//        WindowFeature windowB = new WindowFeature( "bay window" ); // don't change name - hard coded mesh lookup in window factory
//        plan.tags.add( windowB );

//        BayFeature bay = new BayFeature( plan );
//        plan.features.add( bay );
//        bay.radius = 20;
//        bay.setAllProfiles( createBay( roof, windowB ), plan );
//
//        addWindows(bay.shape, windowB);
//
//        front = createFront( bay, door, window1, window2, roof );
//        back = createBack( roof );

        Profile side = createSide();


        for ( Loop<Edge> eLoop : h.plot )
        {
            Loop<Bar> bLoop = new Loop();
            plan.points.add( bLoop );
            for ( Edge e : eLoop )
            {
                Bar b = new Bar( cache.get( e.start ), cache.get( e.end ) );
                bLoop.append( b );
                switch ( Arrays.asList( h.machine ).indexOf( e.machine ) )
                {
                    case 0:
                        plan.profiles.put( b, front );
                        break;
                    case 1:
                        plan.profiles.put( b, back );
                        break;
                    case 2:
                        plan.profiles.put( b, side );
                        break;
                    default:
                        break;
                }
            }
        }

        // all continguous sequences of bars
        for ( List<Bar> lb : findChains( front, plan, plan.points ) )
        {
//            addAtFractionIfLongerThan( lb, 0.10, new Marker( bay ), 10 );

//            addAtFractionIfLongerThan( lb, 0.50, new Marker( door ), 0 );
//            addAtFractionIfLongerThan( lb, 0.80, new Marker( window1 ), 0 );
//
//            addAtFractionIfLongerThan( lb, 0.50, new Marker( window2 ), 0 );
//            addAtFractionIfLongerThan( lb, 0.80, new Marker( window2 ), 0 );
        }
//            addAtFractionIfLongerThan (lb, 0.66, new Marker (bay), 100);

//        plan.addLoop( defaultProf.points.get(0), plan.root, defaultProf );
//        new ForcedStep( plan ); // adds to features, above

        return plan;
    }

    private void addAtFractionIfLongerThan( List<Bar> lb, double i, Marker marker, double min )
    {
        double length = 0;
        for ( Bar b : lb )
            length += b.length();

        if ( length < min )
            return;

        double target = i * length;

        length = 0;
        for ( Bar b : lb )
        {
            length += b.length();
            if ( length > target )
            {
                double backtrack = length - target;
                Vector2d end = new Vector2d( b.start );
                end.sub( b.end );
                end.normalize();
                end.scale( backtrack );

                end.add( b.end );

//                b.addMarker( marker );
                marker.set( end );
                break;
            }
        }
    }

    private List<List<Bar>> findChains( Profile target, Plan plan, LoopL<Bar> surface )
    {
        List<List<Bar>> out = new ArrayList();
        List<Bar> currentChainStart = null;

        for ( Loop<Bar> loop : surface )
            for ( Loopable<Bar> loopable : loop.loopableIterator() )
            {
                Bar e = loopable.get();
                if ( plan.profiles.get( e ) == target )
                {
                    if ( currentChainStart == null )
                    {
                        currentChainStart = new ArrayList();
                        out.add( currentChainStart );
                    }

                    currentChainStart.add( e );
                }
                else
                    currentChainStart = null;
            }

        if ( out.size() > 1 )
        {
            List<Bar> last = out.get( out.size() - 1 );
            if ( last.get( last.size() - 1 ).end == out.get( 0 ).get( 0 ).start )
            {
                out.remove( last );
                out.get( 0 ).addAll( 0, last );
            }
        }

        return out;
    }

    private void addMarkersAt( Loop<Bar> bars, Tag f, double... heights )
    {
        heights:
        for ( double h : heights )
            for ( Bar b : bars )
                if ( b.start.y > h && b.end.y < h )
                {
                    Marker m = new Marker( f );
                    double t = h / ( b.end.y - b.start.y );
                    m.set( t * ( b.end.x - b.end.x ), h );
//                    b.addMarker( m );
                    continue heights;
                }
    }

    private Profile createFront( Tag bayF, Tag doorF, Tag window1, Tag window2, Tag roof )
    {
        Profile profile = makeProfile( 0.0, 0.0, 0.0, -0.01, 0.0, -5.0, 1.8946939653125776, -7.6400563469402325, 1.8946939653125776, -46.22550305288293, 3.2480467976787053, -47.83463546821422, 2.863361229117909, -89.86500113105018, -1.0692976216803927, -92.02191915215259, -0.9946337062053381, -94.95427104573193, 1.9060583757242506, -95.62366152617722, 37.5902974066442, -184.30688173730073 );

        Bar b = profile.points.get( 0 ).start.getPrev().get();
//        Marker m = new Marker( new ThreeFeature() );
        addMarkersAt(profile.points.get(0), new ThreeFeature(), -130 );
//        b.addMarker( m );

        Marker bay = new Marker( bayF );
        Bar first = profile.points.get( 0 ).start.get();
        bay.set( 0, -0.1 );
//        first.addMarker( bay );

        addMarkersAt( profile.points.get( 0 ), window2, -53, -83 );
        addMarkersAt( profile.points.get( 0 ), window1, -13, -43 );
        addMarkersAt( profile.points.get( 0 ), doorF, -0.1 );

        profile.properties.add( "front" );

        profile.points.get( 0 ).start.getPrev().get().tags.add( roof );

        return profile;
    }

    private Profile createBay( Tag roof, Tag window )
    {
        Profile profile = makeProfile( 0.0, 0.0, -0.04555165722026455, -4.521600174718042, 1.1401716649179527, -7.095325315033556, 0.9170415047695184, -44.790480741526395, 2.2528283042489003, -46.384949472906435, 2.3349133028728004, -91.86003871054635, 1.579798682162863, -92.30684904219562, 1.4894050883469858, -93.27310660390906, 4.874792601969848, -96.76537633109747, 4.928094078845125, -113.47840662590978, 3.696819099486646, -113.88883161902928, 6.597511181416231, -116.78952370095887 );
        profile.points.get( 0 ).start.getPrev().get().tags.add( roof );

        addMarkersAt( profile.points.get( 0 ), window, -13, -43, -53, -83 );
        
        return profile;
    }

    private Profile createBack( Tag roof )
    {
        Profile profile = makeProfile( 0, 0, 0, -100, 100, -200 );
        profile.properties.add( "back" );
        profile.points.get( 0 ).start.getPrev().get().tags.add( roof );
        return profile;
    }

    private Profile createSide()
    {
        Profile profile = makeProfile( 0, 0, 0, -100 );
        return profile;
    }

    private Profile makeProfile( double... points )
    {
        Loop<Bar> loop = new Loop();

        Profile out = new Profile();
        assert ( points.length % 2 == 0 );
        Point2d last = new Point2d( points[0], points[1] );
        for ( int i = 2; i < points.length; i += 2 )
        {
            Point2d current = new Point2d( points[i], points[i + 1] );
            loop.append( new Bar( last, current ) );
            last = current;
        }
        out.points.add( loop );
        plan.addLoop( loop, plan.root, out );
        return out;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings( "unchecked" )
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        editorPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        houseList = new javax.swing.JList();
        computeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        editorPanel.setPreferredSize(new java.awt.Dimension(800, 800));

        javax.swing.GroupLayout editorPanelLayout = new javax.swing.GroupLayout(editorPanel);
        editorPanel.setLayout(editorPanelLayout);
        editorPanelLayout.setHorizontalGroup(
            editorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 444, Short.MAX_VALUE)
        );
        editorPanelLayout.setVerticalGroup(
            editorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 393, Short.MAX_VALUE)
        );

        getContentPane().add(editorPanel, java.awt.BorderLayout.CENTER);

        houseList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                houseListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(houseList);

        computeButton.setText("compute all");
        computeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                computeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(computeButton, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(computeButton))
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.EAST);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void houseListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_houseListValueChanged
    {//GEN-HEADEREND:event_houseListValueChanged
        House h = (House) houseList.getSelectedValue();
        if ( h != null )
            campSkeleton.loadPlan( makePlan( h ) );
    }//GEN-LAST:event_houseListValueChanged

    private void computeButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_computeButtonActionPerformed
    {//GEN-HEADEREND:event_computeButtonActionPerformed
        Object key = new Object();
        List<Plan> plans = new ArrayList();
        for ( House h : houses )
            plans.add( makePlan( h ) );
//        campSkeleton.addToOutput( plans, key, roof );
    }//GEN-LAST:event_computeButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main( String args[] )
    {
        try
        {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
        catch ( InstantiationException ex )
        {
            Logger.getLogger( CampSkeleton.class.getName() ).log( Level.SEVERE, null, ex );
        }
        catch ( IllegalAccessException ex )
        {
            Logger.getLogger( CampSkeleton.class.getName() ).log( Level.SEVERE, null, ex );
        }
        catch ( UnsupportedLookAndFeelException ex )
        {
            Logger.getLogger( CampSkeleton.class.getName() ).log( Level.SEVERE, null, ex );
        }
        catch ( ClassNotFoundException e )
        {
        }

        java.awt.EventQueue.invokeLater( new Runnable()
        {

            public void run()
            {
                new ProjectKensington().setVisible( true );
            }
        } );
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton computeButton;
    private javax.swing.JPanel editorPanel;
    private javax.swing.JList houseList;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    private void addWindows( LoopL<Bar> shape, WindowFeature windowB )
    {
        for (Loop<Bar> loop : shape)
            for(Bar b : loop)
            {
                if ( b == loop.start.get() || b == loop.start.getPrev().get() )
                    continue;
                Point2d p = new Point2d( b.end );
                p.add( b.start );
                p.scale( 0.5 );

                Marker m = new Marker( windowB );
                m.set( p );
//                b.addMarker( m );
            }
        
    }

    private void removeParallel( LoopL<Edge> loopL )
    {
        Set<Edge> toRemove= new HashSet();
        for (Loop<Edge> loop : loopL)
        {
            Edge last = loop.start.getPrev().get();
            for (Edge e : loop)
            {
                if (e.direction().angle( last.direction() ) < 0.1 || e.start.distance( e.end) < 3)
                {
                    // remove current
                    toRemove.add(e);
                    last.end = e.end;
//                    last.end.nextC = e.end.nextC;
//                    e.end.nextC.prevC = last.end;
//                    last.end.nextL = e.end.nextL;
                }
                else
                    last = e;
            }
            
            for (Edge e : toRemove)
                loop.remove( e );
        }
    }
}
