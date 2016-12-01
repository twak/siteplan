/*
 * CampSkeleton.java
 * 
 * Created on May 29, 2009, 11:44:52 AM
 */
package campskeleton;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.twak.utils.ConsecutivePairs;
import org.twak.utils.LContext;
import org.twak.utils.ListDownLayout;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.Pair;
import org.twak.utils.SimpleFileChooser;
import org.twak.utils.WeakListener;
import org.twak.utils.ui.SaveLoad;
import org.twak.utils.ui.WindowManager;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import camp.anchors.Anchor;
import camp.anchors.NaturalStepShip;
import camp.anchors.Ship;
import camp.anim.APlanBoxes;
import camp.anim.OverhangPlan;
import camp.anim.PioneerPlan;
import camp.anim.RenderAnimFrame;
import camp.jme.Jme;
import camp.jme.PillarFeature;
import camp.jme.Preview;
import camp.junk.ForcedStep;
import camp.tags.PlanTag;
import straightskeleton.Output;
import straightskeleton.Skeleton;
import straightskeleton.Tag;
import straightskeleton.debug.DebugDevice;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import straightskeleton.ui.PointEditor;
/**
 * User interface for editing the skeleton
 * @author twak
 */
public class CampSkeleton extends javax.swing.JFrame
{
    public final static String toolKey = "related tool";
    public final static boolean Is_User = false; // i can haz dev?
    
    public static CampSkeleton instance;
    public final WeakListener selectedAnchorListeners = new WeakListener();
    public final WeakListener somethingChangedListeners = new WeakListener();
    public final WeakListener profileListChangedListeners = new WeakListener();
    public Tool mode = Tool.Vertex;
    public Plan plan; // field accessed via reflection for SaveLoad
    public Bar selectedEdge;
    ProfileUI profileUI;
    PlanUI planUI;
    boolean fireEvents = true;    
    public Tag selectedTag;
    public Preview preview;

    private SaveLoad saveLoad;

    public Tag wall = new Tag ("wall"), roof = new Tag ("roof"), weakEdge = new Tag ("weak edge");

    // what we're currently working on
    public Object threadKey;
    boolean autoup = true;
    // has input changed recently?
    boolean dirty = true;

    public BufferedImage bgImage = null;

    public double gridSize = -1;


    // frame for showing tags and features
    public JFrame tagFeatureFrame = null;

    // animation frame we're on
    int frame = 0;

    public CampSkeleton ()
    {
    	this(null);
    }

    /** Creates new form CampSkeleton */
    public CampSkeleton(Plan plan)
    {

        new Thread()
            {
                @Override public void run()
                {
                	try
                	{
                    preview = new Preview();
                	}
                	catch (Throwable th)
                	{
                		th.printStackTrace();
                		if (th.getCause() != null)
                			th.getCause().printStackTrace();
                	}
                }
            }.start();

        instance = this;

        initComponents();

        centralPanel.setLayout( new ListDownLayout() );
        centralPanel.setPreferredSize( new Dimension(200, 300 ) );
        centralPanel.setMaximumSize( new Dimension(200, 30000) );

        for (Tool tl : Tool.values())
        {
            if (tl == Tool.Anchor) // not needed for now
                continue;
            
            final Tool ft = tl;
            final JToggleButton tb = new JToggleButton( ft.name().toLowerCase() );
            tb.putClientProperty( this, ft);
            paletteGroup.add( tb );
            tb.addActionListener( new ActionListener() {

                public void actionPerformed( ActionEvent e )
                {
                    if (tb.isSelected())
                        setTool( ft );
                }
            });
            palettePanel.add ( tb );
        }

        // select first button
        for (Component c : palettePanel.getComponents())
        {
            ((JToggleButton)c).setSelected( true );
            break;
        }

        saveLoad = new SaveLoad()
        {
            @Override
            public void afterLoad()
            {
                loadPlan (plan);
                // flush all tool state
                setTool(Tool.Vertex);
                setImage(null);
                showRoot();
                updateTitle();

                killTagFeature();
            }

            @Override
            public void makeNew()
            {
                setDefaultPlan();
                setImage(null);
                setTool(Tool.Vertex);
                reset();
                setPlan( plan );
                saveLoad.saveAs = null;
                updateTitle();


                killTagFeature();
            }



            @Override
            public void afterSave()
            {
                updateTitle();
            }
            @Override
            public void beforeSave()
            {
                // clean plan.profiles
                Set<Bar> used = plan.findBars();
                Iterator<Bar> itb = plan.profiles.keySet().iterator();
                while (itb.hasNext())
                    if (!used.contains(itb.next()))
                        itb.remove();

                if (profileUI != null)
                    profileUI.flushtoProfile();
            }            
        };
        saveLoad.addSaveLoadMenuItems( fileMenu, "plan", this ); // reflective reference to field plan

        if (plan == null)
        	setDefaultPlan();
        else 
        	setPlan( plan );

        setupProfileCombo();

        updateTitle();

        showRoot();
        
        setSize(800,800);
        setLocation (700,100);

        jPanel4.revalidate();

        // something to look at :)
        goButtonActionPerformed( null );
    }

    /**
     * Call this to refresh the profile list in the UI
     */
    public void profileListChanged()
    {
        setupProfileCombo();
        profileListChangedListeners.fire();
    }

    void updateTitle()
    {
        setTitle("Procedural Extrusions ");// out for demo (saveLoad.saveAs == null ? "" : saveLoad.saveAs.getName() ));
    }

    public void setTool(Tool mode)
    {
        nowSelectingFor(null);
        CampSkeleton.instance.highlightFor(new ArrayList());
        
        this.mode = mode;

        toolPanel.removeAll();
        toolPanel.setLayout(new GridLayout(1,1));

        for (MarkerUI mui : new MarkerUI[] {planUI, profileUI})
        {
            if (mui == null)
                continue; // still in setup
            flushMarkersToUI(mui);
        }

        if (mode == Tool.Anchor)
        {
            // don't like this, maybe we want a "goto root" button and only put features in the combo?
            if ( selectedTag == null && plan.tags.size() > 0 )
                planCombo.setSelectedItem( plan.tags.get( 0 ) );
        }
        else if (mode == Tool.Features)
        {
            showTagFeature();
//            toolPanel.add( new FeatureUI(plan));
        }
        else if (mode == Tool.Tag)
        {
            showTagFeature();
//            toolPanel.add ( new TagListUI(plan) );//new TagFeatureList (roof, weakEdge) );
        }

        centralPanel.revalidate();

        // quick and dirty
        for (Component c: palettePanel.getComponents())
        {
            if (c instanceof JToggleButton)
            {
                JToggleButton j= (JToggleButton)c;
                if (mode == j.getClientProperty( this ) && !j.isSelected())
                    j.doClick();
            }
        }

    }

    public void showTagFeature()
    {
        if (tagFeatureFrame == null) {
            tagFeatureFrame = new JFrame("features and tags");
            tagFeatureFrame.setContentPane( new TagsFeaturesUI( plan));
            WindowManager.register(tagFeatureFrame);
        }

        if (!tagFeatureFrame.isVisible())
        {
            tagFeatureFrame.setSize( 800,600 );
            tagFeatureFrame.setVisible( true );

            // position to right of main frame. vey annoying on laptop.
//            Point p = new Point( jPanel4.getWidth(), 0 );
//            SwingUtilities.convertPointToScreen( p, jPanel4 );
//            tagFeatureFrame.setLocation( p );//p.x+getWidth(), p.y);
        }
        tagFeatureFrame.toFront();
    }

    public void killTagFeature()
    {
        if (tagFeatureFrame == null)
            return;
        tagFeatureFrame.setVisible( false );
        tagFeatureFrame.dispose();
        tagFeatureFrame = null;
    }

    public void setProfile ( Profile profile )
    {
        if (profileUI != null)
        {
            if ( profileUI.profile == profile )
                return;
            else
                profileUI.flushtoProfile();
            flushMarkersToUI(profileUI);
        }

        if (machineCombo.getSelectedItem() != profile)
        {
            fireEvents = false;
            machineCombo.setSelectedItem( profile );
            // this occurs if we select an edge from a profile
            if (machineCombo.getSelectedItem() != profile )
                machineCombo.setSelectedItem(null);
            fireEvents = true;
        }

        profilePanel.removeAll();
        profilePanel.setLayout( new GridLayout( 1, 1 ) );

        
        machineCombo.setEnabled( profile != null );

        if ( profile != null )
        {
            profileUI = new ProfileUI( profile, plan, new ProfileEdgeSelected() )
            {
                /**
                 * Quick hacky solutions until the ui is finalized...
                 */
                @Override
                public void releasePoint( Point2d pt, LContext<Bar> ctx, MouseEvent evt )
                {
                    super.releasePoint( pt, ctx, evt );
                    if ( planUI != null )
                        planUI.repaint();
                }

                @Override
                public void paint( Graphics g )
                {
                    super.paint( g );
                    if (planUI != null)
                        planUI.somethingChanged( null );
                }
            };

            flushMarkersToUI(profileUI);
            profilePanel.add( profileUI );
            profileUI.revalidate();
        }
        else
        {
            profileUI = null;
            profilePanel.add( new JLabel( "Select an edge to edit the profile", JLabel.CENTER ) );
            profilePanel.revalidate();
        }
    }

    public void setEdge(Bar edge)
    {
        selectedEdge = edge;

        Profile profile = plan.profiles.get( edge );
        setProfile( profile );
    }

    private void reset()
    {
        selectedEdge = null;
        profileUI = null;
        planUI = null;
        selectedTag = null;

        setupProfileCombo();
    }

    private void setDefaultPlan()
    {
        plan = new Plan ();
        plan.name = "root-plan";

        Profile defaultProf = new Profile (100);
        defaultProf.points.get( 0 ).start.get().end.x += 20;

        createCross( plan, defaultProf );
        plan.addLoop( defaultProf.points.get(0), plan.root, defaultProf );
    }

    public void somethingChanged()
    {
        if (autoupdateButtom != null) // if used in dummy mode, this'll be true
        if (autoupdateButtom.isSelected())
        {
            dirty = true;
            goButtonActionPerformed( null );
        }

        somethingChangedListeners.fire();
    }

    private void setImage(BufferedImage read)
    {
        bgImage = read;
        if (planUI != null)
        {
            planUI.bgImage = read;
            planUI.repaint();
        }
    }

    private void flushMarkersToUI(MarkerUI mui) {
        mui.showMarkers(mode == Tool.Anchor || mode == Tool.Features);
        mui.showTags(mode == Tool.Tag);
    }

    void setSelectedFeature(Tag f)
    {
        selectedTag = f;
        
        if (planUI != null)
            planUI.repaint();

        if (profileUI != null)
            profileUI.repaint();
    }

    private class ProfileEdgeSelected implements PointEditor.BarSelected
    {
        public void barSelected( LContext<Bar> ctx )
        {
//            JOptionPane.showMessageDialog( CampSkeleton.this, "not implemented in campskeleton" );
        }
    }

    public class PlanEdgeSelected implements PointEditor.BarSelected
    {
        public void barSelected( LContext<Bar> ctx )
        {
            setEdge( ctx == null ? null : ctx.get() );
        }
    }


    private int countMarkerMatches(Object generator, Set<Profile> profiles)
    {
        int count = 0;
        for (Loop<Bar> loop : plan.points)
            for (Bar b : loop)
                for (Marker m : b.mould.markersOn( b ))
                    if (m.generator.equals(generator))
                        count++;

       for (Profile p : new HashSet<Profile> ( profiles ) )
           for (Bar b : p.points.eIterator())
               for (Marker m : b.mould.markersOn( b ))
                    if (m.generator.equals(generator))
                        count++;

         // todo: each feature may also have nested markers?
        for (Ship s : plan.ships)
            count += s.countMarkerMatches(generator);

         return count;
    }

    public void loadPlan( Plan plan )
    {
        CampSkeleton.this.reset();


        int nSteps = 0, nInstances = 0;

        Set<Profile> usedProf = new HashSet();

        for (Bar b : plan.points.eIterator())
            usedProf.add(plan.profiles.get(b));

        for (Ship s : plan.ships)
            if (s instanceof NaturalStepShip)
            {
                NaturalStepShip ss = (NaturalStepShip)s;
                for (Bar b : ss.shape.eIterator())
                    usedProf.add(plan.profiles.get(b));
            }


        for (Ship s : plan.ships)
            if (s instanceof NaturalStepShip)
            {
                nInstances += s.getInstances().size();

                if (!s.getInstances().isEmpty())
                    nSteps++;
            }


        statusLabel.setText (
                "loops:"+plan.points.size()+
                " verts:"+plan.points.count() +
                " profiles:"+usedProf.size() +
                " n/steps:"+nSteps +
                " n/instances:"+nInstances +
                " globals:"+(plan.globals.size()-1) );

        setPlan( plan );

        for ( Tag f : plan.tags )
        {
            meshItem.setEnabled( false );
            if ( f.name.compareTo( "wall" ) == 0 )
                wall = f;
            if ( f.name.compareTo( "roof" ) == 0 )
                roof = f;
            if ( f.name.compareTo( "weak edge" ) == 0 )
                weakEdge = f;
//            if ( f.name.compareTo( "pillar" ) == 0 )
//                pillar = (PillarFeature) f;
        }
    }

    public void setPlan( PlanUI selected )
    {
        PlanUI pu = (PlanUI) selected;
        pu.setGridSize( gridSize );
        planPanel.removeAll();
        planPanel.setLayout( new GridLayout( 1, 1 ) );
        planPanel.add( planUI = pu );
        planUI.barSelected = new PlanEdgeSelected();

        exportButton.setVisible( pu.canExport() );
        importButton.setVisible( pu.canImport() );

        flushMarkersToUI( pu );

        repaint();
    }

    public void setPlan( Plan selected )
    {
        // nothing to do?
        this.plan = (Plan) selected;
        setPlan( planUI = new PlanUI( (Plan) selected, new PlanEdgeSelected() ) ); //wtf?
        setImage( bgImage );

        setEdge( null );
        setupProfileCombo();
    }


    public void setupProfileCombo()
    {
        machineCombo.setModel(new DefaultComboBoxModel(new Vector(plan.findProfiles())));
        
        if (selectedEdge != null)
            machineCombo.setSelectedItem( plan.profiles.get( selectedEdge ) );
    }

    public void assignProfileToBar (Bar bar, Profile profile)
    {
        plan.profiles.put( bar, profile );
        repaint();
    }


    // Camp skeleton currently coordinates modes, & takes responsibility for the anchor that we're currently working on
    public Anchor selectedAnchor = null;
    List<Anchor> highlitAnchors = new ArrayList();
    
    public void setAnchorPlan (Marker m)
    {
        if (selectedAnchor != null)
        {
            selectedAnchor.setPlanGen(m.generator);
            selectedAnchorListeners.fire();
        }
    }

    public void setAnchorProfile (Marker m)
    {
        if (selectedAnchor != null)
        {
            selectedAnchor.setProfileGen(m.generator);
            selectedAnchorListeners.fire();
        }
    }

    public void nowSelectingFor(Anchor anchor)
    {
        selectedAnchor = anchor;
        selectedAnchorListeners.fire();
        if (planUI!= null)
            planUI.repaint();
        if (profileUI!= null)
            profileUI.repaint();
    }

    public void highlightFor(List<Anchor> anchors)
    {
        highlitAnchors = anchors;
        if (planUI!= null)
            planUI.repaint();
        if (profileUI!= null)
            profileUI.repaint();
    }
    
    public void showRoot()
    {
        planPanel.removeAll();
        planPanel.setLayout( new GridLayout( 1, 1 ) );
        setPlan ( new PlanUI( plan, new PlanEdgeSelected() ) );
        setTool(mode);
        setImage(bgImage);
        planPanel.add(planUI);
        planUI.revalidate();
        repaint();

    }

    public void setGrid(double val)
    {
        this.gridSize = val;
        if (planUI != null)
            planUI.setGridSize(gridSize);
    }


        public void update (int f)
    {
            update (f, true);
        }
    public void update (int f, boolean recalculate)
    {
        int nFrame = Math.max (0,f);
        int delta = nFrame - frame;
        if ( delta != 0 )
        {
            plan.update( nFrame, delta );
            frame = nFrame;

            // assign profiles based on our new shape
//            if ( plan.buildFromPlan != null )
//                plan.buildFromPlan.buildFromPlan();

            setPlan( plan ); // <- not sure about this, can planUI repain on demand? or not be edited?
            if (recalculate)
                somethingChanged(); // <- this'll redo the 3d output if "udpate" is selected
            statusLabel.setText( "f:" + frame );
            frameSpinner.setValue(nFrame);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings( "unchecked" )
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        paletteGroup = new javax.swing.ButtonGroup();
        planCombo = new javax.swing.JComboBox();
        editPlanButton = new javax.swing.JButton();
        newFeatureButton = new javax.swing.JButton();
        gridGroup = new javax.swing.ButtonGroup();
        setMachineButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        goButton = new javax.swing.JButton();
        autoupdateButtom = new javax.swing.JToggleButton();
        statusLabel = new javax.swing.JLabel();
        playButton = new javax.swing.JButton();
        jToggleButton1 = new javax.swing.JToggleButton();
        frameSpinner = new javax.swing.JSpinner();
        jPanel4 = new javax.swing.JPanel();
        planBorder = new javax.swing.JPanel();
        planPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        showRootBurron = new javax.swing.JButton();
        importButton = new javax.swing.JButton();
        exportButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        profileBorder = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        profileCard = new javax.swing.JPanel();
        addMachineButton = new javax.swing.JButton();
        machineCombo = new javax.swing.JComboBox();
        profilePanel = new javax.swing.JPanel();
        centralPanel = new javax.swing.JPanel();
        palettePanel = new javax.swing.JPanel();
        toolPanel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        objDumpItem = new javax.swing.JMenuItem();
        meshItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        backgroundItem = new javax.swing.JMenuItem();
        clearImageButton = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        gridOff = new javax.swing.JRadioButtonMenuItem();
        gridSmall = new javax.swing.JRadioButtonMenuItem();
        gridMedium = new javax.swing.JRadioButtonMenuItem();
        gridHuge = new javax.swing.JRadioButtonMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        aPlanItem = new javax.swing.JMenuItem();
        pioneersPlanItem = new javax.swing.JMenuItem();
        overhangPlanItem = new javax.swing.JMenuItem();
        timeBackItem = new javax.swing.JMenuItem();
        timeForwardItem = new javax.swing.JMenuItem();
        zeroTimeItem = new javax.swing.JMenuItem();
        editClassifier = new javax.swing.JMenuItem();
        renderAnimItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        clearPlanItem = new javax.swing.JMenuItem();
        svgImport = new javax.swing.JMenuItem();
        reversePlanItem = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        aboutItem = new javax.swing.JMenuItem();

        planCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                planComboActionPerformed(evt);
            }
        });

        editPlanButton.setText("set plan");
        editPlanButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editPlanButtonActionPerformed(evt);
            }
        });

        newFeatureButton.setText("new feature");
        newFeatureButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                newFeatureButtonMousePressed(evt);
            }
        });
        newFeatureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newFeatureButtonActionPerformed(evt);
            }
        });

        setMachineButton.setText("set");
        setMachineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setMachineButtonActionPerformed(evt);
            }
        });

       	setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        goButton.setText("go");
        goButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goButtonActionPerformed(evt);
            }
        });

        autoupdateButtom.setText("updates");
        autoupdateButtom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoupdateButtomActionPerformed(evt);
            }
        });

        playButton.setText(">");
        playButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playButtonActionPerformed(evt);
            }
        });

        jToggleButton1.setText("random");
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });

        frameSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        frameSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                frameSpinnerStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(frameSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(autoupdateButtom)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(goButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(frameSpinner, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(goButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(autoupdateButtom, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(playButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);

        jPanel4.setLayout(new java.awt.BorderLayout(3, 0));

        planBorder.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        planBorder.setPreferredSize(new java.awt.Dimension(200, 800));
        planBorder.setLayout(new java.awt.BorderLayout());

        planPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        javax.swing.GroupLayout planPanelLayout = new javax.swing.GroupLayout(planPanel);
        planPanel.setLayout(planPanelLayout);
        planPanelLayout.setHorizontalGroup(
            planPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 276, Short.MAX_VALUE)
        );
        planPanelLayout.setVerticalGroup(
            planPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 484, Short.MAX_VALUE)
        );

        planBorder.add(planPanel, java.awt.BorderLayout.CENTER);

        showRootBurron.setText("root");
        showRootBurron.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showRootBurronActionPerformed(evt);
            }
        });

        importButton.setText("import");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        exportButton.setText("export");
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(showRootBurron)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 91, Short.MAX_VALUE)
                .addComponent(exportButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(importButton))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(showRootBurron)
                .addComponent(importButton)
                .addComponent(exportButton))
        );

        planBorder.add(jPanel2, java.awt.BorderLayout.NORTH);

        jPanel4.add(planBorder, java.awt.BorderLayout.CENTER);

        jPanel5.setPreferredSize(new java.awt.Dimension(450, 518));
        jPanel5.setLayout(new java.awt.GridLayout(1, 2));

        profileBorder.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        profileBorder.setPreferredSize(new java.awt.Dimension(200, 300));
        profileBorder.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());

        addMachineButton.setText("+");
        addMachineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addMachineButtonActionPerformed(evt);
            }
        });

        machineCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                machineComboItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout profileCardLayout = new javax.swing.GroupLayout(profileCard);
        profileCard.setLayout(profileCardLayout);
        profileCardLayout.setHorizontalGroup(
            profileCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, profileCardLayout.createSequentialGroup()
                .addComponent(machineCombo, 0, 176, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addMachineButton))
        );
        profileCardLayout.setVerticalGroup(
            profileCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(profileCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(machineCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(addMachineButton))
        );

        jPanel1.add(profileCard, java.awt.BorderLayout.CENTER);

        profileBorder.add(jPanel1, java.awt.BorderLayout.NORTH);

        javax.swing.GroupLayout profilePanelLayout = new javax.swing.GroupLayout(profilePanel);
        profilePanel.setLayout(profilePanelLayout);
        profilePanelLayout.setHorizontalGroup(
            profilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 223, Short.MAX_VALUE)
        );
        profilePanelLayout.setVerticalGroup(
            profilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 486, Short.MAX_VALUE)
        );

        profileBorder.add(profilePanel, java.awt.BorderLayout.CENTER);

        jPanel5.add(profileBorder);

        centralPanel.setPreferredSize(new java.awt.Dimension(200, 800));
        centralPanel.setLayout(new java.awt.GridLayout(0, 1));

        palettePanel.setLayout(new java.awt.GridLayout(0, 1));
        centralPanel.add(palettePanel);

        toolPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        toolPanel.setLayout(new java.awt.GridLayout(1, 1));
        centralPanel.add(toolPanel);

        jPanel5.add(centralPanel);

        jPanel4.add(jPanel5, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel4, java.awt.BorderLayout.CENTER);

        fileMenu.setText("file");

        objDumpItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        objDumpItem.setText("output obj");
        objDumpItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                objDumpItemActionPerformed(evt);
            }
        });
        fileMenu.add(objDumpItem);

        meshItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_MASK));
        meshItem.setText("add meshes");
        meshItem.setEnabled(false);
        meshItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                meshItemActionPerformed(evt);
            }
        });
        fileMenu.add(meshItem);

        jMenuBar1.add(fileMenu);

        jMenu1.setText("view");

        backgroundItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_MASK));
        backgroundItem.setText("set background image");
        backgroundItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backgroundItemActionPerformed(evt);
            }
        });
        jMenu1.add(backgroundItem);

        clearImageButton.setText("clear background image");
        clearImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearImageButtonActionPerformed(evt);
            }
        });
        jMenu1.add(clearImageButton);

        jMenu3.setText("grid");

        gridGroup.add(gridOff);
        gridOff.setSelected(true);
        gridOff.setText("off");
        gridOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridOffActionPerformed(evt);
            }
        });
        jMenu3.add(gridOff);

        gridGroup.add(gridSmall);
        gridSmall.setText("small");
        gridSmall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridSmallActionPerformed(evt);
            }
        });
        jMenu3.add(gridSmall);

        gridGroup.add(gridMedium);
        gridMedium.setText("medium");
        gridMedium.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridMediumActionPerformed(evt);
            }
        });
        jMenu3.add(gridMedium);

        gridGroup.add(gridHuge);
        gridHuge.setText("big");
        gridHuge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridHugeActionPerformed(evt);
            }
        });
        jMenu3.add(gridHuge);

        jMenu1.add(jMenu3);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("plan");

        jMenu4.setText("new plan");

        aPlanItem.setText("animated plan");
        aPlanItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aPlanItemActionPerformed(evt);
            }
        });
        jMenu4.add(aPlanItem);

        pioneersPlanItem.setText("pioneers demo mode");
        pioneersPlanItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pioneersPlanItemActionPerformed(evt);
            }
        });
        jMenu4.add(pioneersPlanItem);

        overhangPlanItem.setText("overhang plan");
        overhangPlanItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overhangPlanItemActionPerformed(evt);
            }
        });
        jMenu4.add(overhangPlanItem);

        jMenu2.add(jMenu4);

        timeBackItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_COMMA, 0));
        timeBackItem.setText("time--");
        timeBackItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timeBackItemActionPerformed(evt);
            }
        });
        jMenu2.add(timeBackItem);

        timeForwardItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PERIOD, 0));
        timeForwardItem.setText("time++");
        timeForwardItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timeForwardItemActionPerformed(evt);
            }
        });
        jMenu2.add(timeForwardItem);

        zeroTimeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, 0));
        zeroTimeItem.setText("time to zero");
        zeroTimeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zeroTimeItemActionPerformed(evt);
            }
        });
        jMenu2.add(zeroTimeItem);

        editClassifier.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, java.awt.event.InputEvent.CTRL_MASK));
        editClassifier.setText("edit edge classifier");
        editClassifier.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editClassifierActionPerformed(evt);
            }
        });
        jMenu2.add(editClassifier);

        renderAnimItem.setText("anim to .obj");
        renderAnimItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                renderAnimItemActionPerformed(evt);
            }
        });
        jMenu2.add(renderAnimItem);
        jMenu2.add(jSeparator1);

        clearPlanItem.setText("clear plan");
        clearPlanItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearPlanItemActionPerformed(evt);
            }
        });
        jMenu2.add(clearPlanItem);

        svgImport.setText("import svg plan...");
        svgImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                svgImportActionPerformed(evt);
            }
        });
        jMenu2.add(svgImport);

        reversePlanItem.setText("reverse plan direction");
        reversePlanItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reversePlanItemActionPerformed(evt);
            }
        });
        jMenu2.add(reversePlanItem);

        jMenuBar1.add(jMenu2);

        jMenu5.setText("help");
        jMenu5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu5ActionPerformed(evt);
            }
        });

        aboutItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        aboutItem.setText("about");
        aboutItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutItemActionPerformed(evt);
            }
        });
        jMenu5.add(aboutItem);

        jMenuBar1.add(jMenu5);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void machineComboItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_machineComboItemStateChanged
    {//GEN-HEADEREND:event_machineComboItemStateChanged
        if (!fireEvents)
            return;
        Profile p = (Profile) machineCombo.getSelectedItem();
        setProfile ( p );

        if (p != null)
            plan.profiles.put ( selectedEdge,  p);

        repaint();
}//GEN-LAST:event_machineComboItemStateChanged

    private void addMachineButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_addMachineButtonActionPerformed
    {//GEN-HEADEREND:event_addMachineButtonActionPerformed
        if (selectedEdge == null)
        {
            JOptionPane.showMessageDialog( this, "Please select an edge first, or middle click a plan to create one", "No selected edge", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Profile p = plan.createNewProfile(plan.profiles.get(selectedEdge));

        plan.profiles.put( selectedEdge, p );

        setProfile( p );
        setupProfileCombo();
        
        repaint();
}//GEN-LAST:event_addMachineButtonActionPerformed

    private void newFeatureButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_newFeatureButtonActionPerformed
    {//GEN-HEADEREND:event_newFeatureButtonActionPerformed
        
}//GEN-LAST:event_newFeatureButtonActionPerformed

    private void planComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_planComboActionPerformed
    {//GEN-HEADEREND:event_planComboActionPerformed
        if (!fireEvents)
            return;
        Object o = planCombo.getSelectedItem();
        if ( o instanceof Tag)
        {
            selectedTag = (Tag)o;
            
            if ( planUI != null )
                planUI.repaint();

            if (o instanceof ForcedStep || o instanceof PillarFeature )
                setTool( Tool.Anchor );
            else
                setTool (Tool.Tag);
        }
}//GEN-LAST:event_planComboActionPerformed

    private void editPlanButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_editPlanButtonActionPerformed
    {//GEN-HEADEREND:event_editPlanButtonActionPerformed
        setPlan( (Plan) planCombo.getSelectedItem() );
    }//GEN-LAST:event_editPlanButtonActionPerformed

    private void meshItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_meshItemActionPerformed
    {//GEN-HEADEREND:event_meshItemActionPerformed
        
        if (preview != null && output != null)
        {
            preview.clear = true;

           show( output, (Skeleton) threadKey );

//            preview.display( output );
//

           for (PlanTag pt : plan.tags)
           {
               pt.postProcess ( output, preview, threadKey );
           }

            

            ((PlanSkeleton)output.skeleton).addMeshesTo(preview);

            // "this would be an eccumenical matter"
//            PlanSkeleton ps = (PlanSkeleton)threadKey;
//            if (ps.pillarFactory != null)
//                ps.pillarFactory.addTo (preview, threadKey);
//            if (ps.windowFactory != null)
//                ps.windowFactory.addTo (preview, threadKey);
//            if (ps.meshFactory != null)
//                ps.meshFactory.addTo (praview, threadKey);

//            JmeObjDump dump = new JmeObjDump();
//            dump.add( preview.model );
//            dump.allDone( new File ("out.obj"));
        }
    }//GEN-LAST:event_meshItemActionPerformed

    private void objDumpItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_objDumpItemActionPerformed
    {//GEN-HEADEREND:event_objDumpItemActionPerformed
        if (preview != null)
            preview.outputObj(this);
    }//GEN-LAST:event_objDumpItemActionPerformed

//    public void addToOutput( Collection<Plan> plans, Object key, Tag roof )
//    {
//        this.threadKey = key;
//        this.roof = roof;
//        preview.clear = true;
//        preview.threadKey = key;
//
//        Set<Face> allFaces = new HashSet();
//        for ( Plan p : plans ) //plans.indexOf(p)
//        {
//            PlanSkeleton s = new PlanSkeleton( p );
//            s.skeleton();
//
//            if ( s.output.faces != null )
//            {
//                preview.display( s.output );
//
//                for ( Face f : s.output.faces.values() )
//                    if ( f.profile.contains( CampSkeleton.instance.roof ) )
//                        allFaces.add( f ); //allFaces.size()
//
//                // "this would be an eccumenical matter"
//                if ( s.pillarFactory != null )
//                    s.pillarFactory.addTo( preview, key );
//                if ( s.windowFactory != null )
//                    s.windowFactory.addTo( preview, key );
//                if ( s.meshFactory != null )
//                    s.meshFactory.addTo( preview, key );
//            }
//        }
//        new Tiller( preview, allFaces, key );
//    }


    boolean busy = false;
    private void goButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_goButtonActionPerformed
    {//GEN-HEADEREND:event_goButtonActionPerformed
        if ( !busy )
        {
            busy = true;
            dirty = false;
            goButton.setText( "[working]" );


            new Thread()
            {
                @Override
                public void run() {
                    try
                    {
                        DebugDevice.reset();
                        Skeleton s = new PlanSkeleton( plan );
                        s.skeleton();

                        if ( s.output.faces != null )
                        {
                            Output output = s.output;
                            show( output, s );
                        }

//                        do {
//                            try {
//                                Thread.sleep(200);
//                            } catch (InterruptedException ex) {
//                                ex.printStackTrace();
//                            }
//                        }
//                        while (preview.isPendingUpdate());
//
//                         preview.dump(new File ( "C:\\Users\\twak\\step_frames\\"+frame+".obj") );
                    }
                    finally
                    {
                        busy = false;
                        goButton.setText( "go" );
                        // if something's changed since we started work, run again...
                        if (dirty)
                            SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                    goButtonActionPerformed(null);
                            }
                        });
                    }

                }
            }.start();
        }
    }//GEN-LAST:event_goButtonActionPerformed
// TODO add your handling code here:
    private void autoupdateButtomActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_autoupdateButtomActionPerformed
    {//GEN-HEADEREND:event_autoupdateButtomActionPerformed
//        autoup = autoupdateButtom.isSelected();
        if (autoupdateButtom.isSelected())
            somethingChanged();
    }//GEN-LAST:event_autoupdateButtomActionPerformed

    private void newFeatureButtonMousePressed(java.awt.event.MouseEvent evt)//GEN-FIRST:event_newFeatureButtonMousePressed
    {//GEN-HEADEREND:event_newFeatureButtonMousePressed

        DefaultListModel dlm = new DefaultListModel();

        abstract class Clickable
        {
            String name;
            public Clickable(String name)
            {
                this.name = name;
            }

            public abstract Tag makeFeature();

            @Override
            public String toString()
            {
                return name;
            }
        }

        dlm.addElement( new Clickable( "forced step" )
        {
            @Override
            public Tag makeFeature()
            {
                return new ForcedStep( plan );
            }
        } );
        
        final JList list = new JList(dlm);

        Point pt = evt.getPoint();
        pt = SwingUtilities.convertPoint( newFeatureButton, pt, null );

        final Popup pop = PopupFactory.getSharedInstance().getPopup( this, list, pt.x + getX(), pt.y + getY());
        pop.show();

        list.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseExited( MouseEvent e )
            {
                pop.hide();
            }
        });

        list.getSelectionModel().addListSelectionListener( new ListSelectionListener()
        {
            public void valueChanged( ListSelectionEvent e )
            {
                Object o = list.getSelectedValue();
                if (o != null && o instanceof Clickable)
                {
                    // adds to plan!
                    ((Clickable)o).makeFeature();
                }
                pop.hide();
            }
        });
    }

     enum PlaybackStatus { PLAY, PAUSE }
    PlaybackStatus playbackStatus = PlaybackStatus.PAUSE;
    PlaybackThread playbackThread = new PlaybackThread();
    {
        playbackThread.start();
    }

    public class PlaybackThread extends Thread
    {
        public boolean run = false;

        @Override
        public void run()
        {
            while (true)
            {
                try {
                    if (run && !busy)
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            int nF = frame +1;
                            if (nF > 100)
                                nF = 0;

                            if (jToggleButton1.isSelected())
                            {

                                nF = (int)(Math.random()*100);
                            }
                            

                            update(nF);
                        }
                    });
                    Thread.sleep( 1000 );
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        }

    }

    public void togglePlaybackStatus()
    {
        if (playbackStatus == PlaybackStatus.PLAY)
            playbackStatus = playbackStatus.PAUSE;
        else
            playbackStatus = playbackStatus.PLAY;

        playbackThread.run = playbackStatus == playbackStatus.PLAY;
    }//GEN-LAST:event_newFeatureButtonMousePressed

    private void showRootBurronActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showRootBurronActionPerformed
        showRoot();
    }//GEN-LAST:event_showRootBurronActionPerformed

    File currentFolder = new File(".");
    private void backgroundItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_backgroundItemActionPerformed
    {//GEN-HEADEREND:event_backgroundItemActionPerformed
        
        new SimpleFileChooser(this, false, "select background image")
        {
            @Override
            public void heresTheFile(File f) throws Throwable
            {
                setImage( ImageIO.read(f) );
            }
        };
    }//GEN-LAST:event_backgroundItemActionPerformed

    private void clearImageButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearImageButtonActionPerformed
    {//GEN-HEADEREND:event_clearImageButtonActionPerformed
        setImage(null);
    }//GEN-LAST:event_clearImageButtonActionPerformed

    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exportButtonActionPerformed
    {//GEN-HEADEREND:event_exportButtonActionPerformed
        new SimpleFileChooser(this, true, "save feature info" )
        {
            @Override
            public void heresTheFile(File f)
            {
                planUI.exportt(f);
            }
        };
    }//GEN-LAST:event_exportButtonActionPerformed

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_importButtonActionPerformed
    {//GEN-HEADEREND:event_importButtonActionPerformed
        new SimpleFileChooser(this, false, "load feature info")
        {
            @Override
            public void heresTheFile(File f)
            {
                planUI.importt(f);
            }
        };
    }//GEN-LAST:event_importButtonActionPerformed

    private void gridOffActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_gridOffActionPerformed
    {//GEN-HEADEREND:event_gridOffActionPerformed
        setGrid(-1);
    }//GEN-LAST:event_gridOffActionPerformed

    private void gridSmallActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_gridSmallActionPerformed
    {//GEN-HEADEREND:event_gridSmallActionPerformed
        setGrid(1);
    }//GEN-LAST:event_gridSmallActionPerformed

    private void gridMediumActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_gridMediumActionPerformed
    {//GEN-HEADEREND:event_gridMediumActionPerformed
        setGrid(5);
    }//GEN-LAST:event_gridMediumActionPerformed

    private void gridHugeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_gridHugeActionPerformed
    {//GEN-HEADEREND:event_gridHugeActionPerformed
        setGrid(10);
    }//GEN-LAST:event_gridHugeActionPerformed

    private void setMachineButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setMachineButtonActionPerformed
    {//GEN-HEADEREND:event_setMachineButtonActionPerformed
        Profile p = (Profile) machineCombo.getSelectedItem();
        if (p != null)
            plan.profiles.put ( selectedEdge,  p);
        repaint();
    }//GEN-LAST:event_setMachineButtonActionPerformed

    private void timeBackItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_timeBackItemActionPerformed
    {//GEN-HEADEREND:event_timeBackItemActionPerformed
        update( frame-1 );
    }//GEN-LAST:event_timeBackItemActionPerformed

    private void zeroTimeItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_zeroTimeItemActionPerformed
    {//GEN-HEADEREND:event_zeroTimeItemActionPerformed
        update( 0 );
    }//GEN-LAST:event_zeroTimeItemActionPerformed

    private void timeForwardItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_timeForwardItemActionPerformed
    {//GEN-HEADEREND:event_timeForwardItemActionPerformed
        update( frame + (evt.getModifiers() != 0  ? 10 : 1));
    }//GEN-LAST:event_timeForwardItemActionPerformed

    private void renderAnimItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_renderAnimItemActionPerformed
    {//GEN-HEADEREND:event_renderAnimItemActionPerformed
        new RenderAnimFrame();
    }//GEN-LAST:event_renderAnimItemActionPerformed

    private void aPlanItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_aPlanItemActionPerformed
    {//GEN-HEADEREND:event_aPlanItemActionPerformed
        setPlan( new APlanBoxes() );
        frame = 0;
        update( 1 );
        update( 0 );
    }//GEN-LAST:event_aPlanItemActionPerformed

    private void svgImportActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_svgImportActionPerformed
    {//GEN-HEADEREND:event_svgImportActionPerformed
        new SimpleFileChooser(this, false, "select svg to import as plan")
        {
            @Override
            public void heresTheFile(File f) throws Throwable
            {
//                new ImportSVG( f, plan );
                planUI.repaint();
            }
        };
    }//GEN-LAST:event_svgImportActionPerformed

    private void reversePlanItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_reversePlanItemActionPerformed
    {//GEN-HEADEREND:event_reversePlanItemActionPerformed
        Bar.reverse( plan.points );
        planUI.repaint();
        somethingChanged();
    }//GEN-LAST:event_reversePlanItemActionPerformed

    private void clearPlanItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearPlanItemActionPerformed
    {//GEN-HEADEREND:event_clearPlanItemActionPerformed
        plan.points.clear();
        planUI.repaint();
        somethingChanged();
    }//GEN-LAST:event_clearPlanItemActionPerformed

    private void editClassifierActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_editClassifierActionPerformed
    {//GEN-HEADEREND:event_editClassifierActionPerformed
//        if (plan.buildFromPlan != null)
//            plan.buildFromPlan.showUI();
//        else
            JOptionPane.showMessageDialog( this, "Must be using a procedural floorplan", "Null editor not available", JOptionPane.ERROR_MESSAGE);

    }//GEN-LAST:event_editClassifierActionPerformed

    private void pioneersPlanItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pioneersPlanItemActionPerformed
        setPlan(new PioneerPlan());
        frame = 0;
        update(0);
    }//GEN-LAST:event_pioneersPlanItemActionPerformed


    private void playButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playButtonActionPerformed
        togglePlaybackStatus();
        if (playbackStatus == PlaybackStatus.PAUSE)
            playButton.setText(">");
        else
            playButton.setText("||");
    }//GEN-LAST:event_playButtonActionPerformed

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jMenu5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu5ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu5ActionPerformed

    private void aboutItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutItemActionPerformed
        new AboutBox();
    }//GEN-LAST:event_aboutItemActionPerformed

    private void overhangPlanItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overhangPlanItemActionPerformed
        setPlan(new OverhangPlan());
        frame = 0;
        update(0);
    }//GEN-LAST:event_overhangPlanItemActionPerformed

    private void frameSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_frameSpinnerStateChanged
        update ( (Integer) frameSpinner.getValue() );
    }//GEN-LAST:event_frameSpinnerStateChanged

    public static void main (String [] args)
    {
        WindowManager.iconName = "/camp/resources/icon32.png";
        try
        {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
        catch ( Throwable ex )
        {
            ex.printStackTrace();
        }
        
        CampSkeleton cs = new CampSkeleton();
        cs.setVisible(true);
        WindowManager.register(cs);

        // debug - show diaglog
//        CampSkeleton.instance.aPlanItemActionPerformed( null );
//        CampSkeleton.instance.editClassifierActionPerformed( null );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aPlanItem;
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JButton addMachineButton;
    private javax.swing.JToggleButton autoupdateButtom;
    private javax.swing.JMenuItem backgroundItem;
    private javax.swing.JPanel centralPanel;
    private javax.swing.JMenuItem clearImageButton;
    private javax.swing.JMenuItem clearPlanItem;
    private javax.swing.JMenuItem editClassifier;
    private javax.swing.JButton editPlanButton;
    private javax.swing.JButton exportButton;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JSpinner frameSpinner;
    private javax.swing.JButton goButton;
    private javax.swing.ButtonGroup gridGroup;
    private javax.swing.JRadioButtonMenuItem gridHuge;
    private javax.swing.JRadioButtonMenuItem gridMedium;
    private javax.swing.JRadioButtonMenuItem gridOff;
    private javax.swing.JRadioButtonMenuItem gridSmall;
    private javax.swing.JButton importButton;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JComboBox machineCombo;
    private javax.swing.JMenuItem meshItem;
    private javax.swing.JButton newFeatureButton;
    private javax.swing.JMenuItem objDumpItem;
    private javax.swing.JMenuItem overhangPlanItem;
    private javax.swing.ButtonGroup paletteGroup;
    private javax.swing.JPanel palettePanel;
    private javax.swing.JMenuItem pioneersPlanItem;
    private javax.swing.JPanel planBorder;
    private javax.swing.JComboBox planCombo;
    private javax.swing.JPanel planPanel;
    private javax.swing.JButton playButton;
    private javax.swing.JPanel profileBorder;
    private javax.swing.JPanel profileCard;
    private javax.swing.JPanel profilePanel;
    private javax.swing.JMenuItem renderAnimItem;
    private javax.swing.JMenuItem reversePlanItem;
    private javax.swing.JButton setMachineButton;
    private javax.swing.JButton showRootBurron;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JMenuItem svgImport;
    private javax.swing.JMenuItem timeBackItem;
    private javax.swing.JMenuItem timeForwardItem;
    private javax.swing.JPanel toolPanel;
    private javax.swing.JMenuItem zeroTimeItem;
    // End of variables declaration//GEN-END:variables

    /**
     * Bootstrap method - template should come from file?
     */
    public static LoopL<Bar> createCircularPoints( int count, int x, int y, int rad, Profile profile, Plan plan )
    {
        double delta = Math.PI * 2 / count;

        LoopL<Bar> loopl = new LoopL();
        Loop<Bar> loop = new Loop();
        loopl.add( loop );

        Point2d prev = null, start = null;
//        boolean odd = true;
        for ( int i = 0; i < count; i++ )
        {
            Point2d c = new Point2d(
                    x + (int) ( Math.cos( (i-0.5) * delta ) * rad ),
                    y + (int) ( Math.sin( (i-0.5) * delta ) * rad ) );


            if ( prev != null )
            {
                Bar e = new Bar( prev, c );
                loop.append( e );
                plan.profiles.put( e, profile );
            }
            else start = c;
            prev = c;

//            odd = !odd;
//            if (odd)
//            {
//                double r = Math.sqrt( 2* rad* rad - 2 * rad * rad * Math.cos (Math.PI/8) );
//                double d2 = r * Math.sin( Math.toRadians( 135 )) / (Math.cos (Math.toRadians( 45)) * Math.sin (Math.toRadians( 45./2. )));
//                Point2d d = new Point2d(
//                    x + (int) ( Math.cos( (i+0.5) * delta ) * d2/2 ),
//                    y + (int) ( Math.sin( (i+0.5) * delta ) * d2/2 ) );
//                Bar e = new Bar (prev, d);
//                loop.append(e);
//                plan.profiles.put(e,profile);
//                prev = d;
//            }
        }
        
        Bar e = new Bar( prev, start );
        loop.append( e );
        plan.profiles.put( e, profile );
        
        return loopl;
    }

    public static LoopL<Bar> createfourSqaures( Plan plan, Profile profile )
    {
        LoopL<Bar> loopl = new LoopL();
        

         List<Point2d> pts = Arrays.asList(
//            new Point2d (100, 250),
//            new Point2d (250,250),
//            new Point2d (250,100),
//            new Point2d (350,100),
//            new Point2d (400,600)

            new Point2d (0,0),
            new Point2d (-50,0),
            new Point2d (-50,-50),
            new Point2d (0, -50)
                );

//         for (Point2d p : pts)
//             p.scale( 0.3 );

         List<List<Point2d>> stp = new ArrayList();

        for ( Point2d offset : new Point2d[]
                {
                    new Point2d( 0, 150 ), new Point2d( 0, -150 ), new Point2d( 150, 0 ), new Point2d( -150, 0 )
                } )
        {
            List<Point2d> t = new ArrayList();
            stp.add(t);

            for (Point2d pt : pts)
                t.add( new Point2d (offset.x + pt.x, offset.y + pt.y));
        }


        for (List<Point2d> pts2 : stp)
        {
            Loop<Bar> loop = new Loop();
            loopl.add( loop );

            for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( pts2, true ) )
            {
                Bar b;
                loop.append( b = new Bar( pair.first(), pair.second() ) );
                plan.profiles.put( b, profile );
            }
        }
        
        return loopl;
    }

    public static void createCross( Plan plan, Profile profile )
    {
        LoopL<Bar> loopl = new LoopL();
        Loop<Bar> loop = new Loop();
        loopl.add( loop );

         List<Point2d> pts = Arrays.asList(
//            new Point2d (100, 250),
//            new Point2d (250,250),
//            new Point2d (250,100),
//            new Point2d (350,100),
//            new Point2d (400,600)

            new Point2d (250,100),
            new Point2d (350,100),
            new Point2d (350,250),
            new Point2d (500, 250),
            new Point2d (500, 350),
            new Point2d (350, 350),
            new Point2d (350, 500),
            new Point2d (250, 500),
            new Point2d (250, 350),
            new Point2d (100, 350),
            new Point2d (100, 250),
            new Point2d (250,250)
                );

         for (Point2d p : pts)
             p.scale( 0.3 );

                for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( pts, true ))
        {
            Bar b;
            loop.append( b = new Bar( pair.first(), pair.second() ) );
            plan.profiles.put( b, profile );
        }
        plan.points = loopl;
    }

    /**
     * Debug locations, only use this method before the skeleton is complete
     * @param mat
     */
    List<Spatial> markers = new ArrayList();
    public void addDebugMarker( Matrix4d mat )
    {
        Point3d loc = Jme.convert( new Vector3d ( mat.getM03(), mat.getM13(), mat.getM23() ) );
        System.out.println(mat);
        System.out.println(loc);
        Box box = new Box(new Vector3f((float)loc.x, (float)loc.y, (float)loc.z), 0.3f,0.3f,0.3f);
    }

    /**
     * Thread key ensures that only additional meshes (tiles, pillars) that were designed
     * for the current input are added
     */
    Output output;
    public void show( Output output, Skeleton threadKey )
    {
        this.threadKey = threadKey;
        meshItem.setEnabled( true );
        this.output = output;
        if (preview != null) // might take a while...
        {
            preview.clear = true;

            Point2d pt = getMiddle( plan.points );
            preview.setViewStats( pt.x / Jme.scale, threadKey.height / Jme.scale, pt.y / Jme.scale, threadKey );

            if (!markers.isEmpty())
            {
                for (Spatial s : markers)
                    preview.display( s, threadKey );
            }
            markers.clear();
        }
    }

    public static Point2d getMiddle (LoopL<Bar> plan)
    {
        Point2d middle = new Point2d();

        int count = 0;

        for (Bar b : plan.eIterator())
        {
            middle.add( b.start );
            count++;
        }

        middle.scale( 1./count);
        return middle;
    }


    public static LoopL<Bar> createWonka( Plan plan, Profile...profile )
    {
        LoopL<Bar> loopl = new LoopL();

        Random randy = new Random();

        for (int i = 0; i < asdf.length; i++)
        {
            Loop<Bar> loop = new Loop();
            loopl.add( loop );
            List<Point2d> pts= new ArrayList();

            for (int j = asdf[i].length-1; j >= 0; j--)
            {
                pts.add( new Point2d (asdf[i][j][0], asdf[i][j][1]));
            }

            for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( pts, true ) )
            {
                Bar b;
                loop.append( b = new Bar( pair.first(), pair.second() ) );
                plan.profiles.put( b, profile [randy.nextInt(profile.length)] );
            }
        }
        removeParallel( loopl );

        return loopl;
    }

    public static void removeParallel( LoopL<Bar> loopL )
    {
        Set<Bar> toRemove = new HashSet();
        for ( Loop<Bar> loop : loopL )
        {
            Bar last = loop.start.getPrev().get();
            for ( Bar e : loop )
                if ( e.start.distance( e.end ) < 0.1 )
                {
                    // remove current
                    toRemove.add( e );
                    last.end = e.end;
//                    last.end.nextC = e.end.nextC;
//                    e.end.nextC.prevC = last.end;
//                    last.end.nextL = e.end.nextL;
                }
                else
                    last = e;

            for ( Bar e : toRemove )
                loop.remove( e );
        }
    }

    static double[][][] asdf = new double[][][]{{
{372.0625,146.96875},
{322.125,146.9375},
{323.8125,148.90625},
{325.875,148.8125},
{326.4375,151.15625},
{326.21875,152.4375},
{324.125,155.46875},
{327.0625,159.09375},
{328.59375,161.125},
{324.09375,162.65625},
{321.375,162.40625},
{323.625,163.5625},
{324.03125,165.8125},
{320.65625,167.53125},
{317.1875,165.71875},
{317.625,162.59375},
{315.62946,160.1116},
{314.125,160.53125},
{315.15625,157.875},
{314.375,154.78125},
{311.875,155.4375},
{309.5625,154.875},
{307.84375,151.8125},
{306.78125,151.03125},
{302.75,150.75},
{302.03125,153.625},
{305.75,156.6875},
{310.71875,165.40625},
{300.90625,166.53125},
{287.34375,168.96875},
{276.28125,174.84375},
{263.21875,178.75},
{257.5625,177.03125},
{248.78125,183.1875},
{247.71875,190.5},
{239.03125,197.40625},
{215.3125,215.78125},
{213.375,225.6875},
{207.9375,243.375},
{207.84375,254.71875},
{207.09375,283.25},
{207.28125,304.125},
{203.53125,320.90625},
{202.15625,355.78125},
{203.6875,371.625},
{197.03125,372.3125},
{193.5625,367.0625},
{182.75,366.375},
{175.875,368.5625},
{170.625,370.71875},
{166.65625,374.84375},
{163.375,377.4375},
{159.75,384.125},
{157.9375,389.65625},
{154.25,395.71875},
{153.53125,408.90625},
{161.53125,425.65625},
{168.03125,432.5625},
{172.375,437.625},
{134.59375,468.21875},
{125.4375,485.96875},
{125.4375,612.375},
{459.46875,612.375},
{443.34375,610.9375},
{432.46875,605.375},
{441.9375,601.125},
{461.3125,600.6875},
{476.4375,602.0625},
{468.28125,611.03125},
{478.9375,610.90625},
{500.75,612.375},
{548.5625,612.375},
{548.40625,530.4375},
{544.0625,536.84375},
{539.15625,535.59375},
{522.96875,517.46875},
{511.4375,503.21875},
{501.15625,494.6875},
{502.9375,486.9375},
{509.03125,477.1875},
{509.75,469.40625},
{507.96875,463.25},
{506.125,466.03125},
{504.28125,469.03125},
{500.3125,475.375},
{498.25,477.21875},
{496.125,476.75},
{496.34375,470.46875},
{497.9375,464.78125},
{500.90625,461.59375},
{504.40625,457.90625},
{503.375,443.625},
{493.1875,432.0625},
{483.53125,444.34375},
{479.875,442.75},
{476.75,442.4375},
{472.84375,440.53125},
{469.25,440.0625},
{463.8125,437.21875},
{462.125,433.59375},
{459.84375,429.625},
{458.15625,425.40625},
{460.21875,420.875},
{459.25,418.21875},
{457.5,413.84375},
{456.0,405.71875},
{454.03125,401.125},
{453.0,394.46875},
{453.96875,391.375},
{456.1875,386.53125},
{457.8125,383.28125},
{458.875,379.125},
{458.34375,374.4375},
{456.84375,371.0625},
{454.53125,367.59375},
{452.34375,366.5625},
{450.84375,364.34375},
{448.09375,363.4375},
{445.5,360.8125},
{444.34375,356.3125},
{444.84375,349.59375},
{444.6875,345.53125},
{446.8125,345.40625},
{454.28125,347.0},
{459.34375,348.09375},
{470.5,346.625},
{483.96875,342.3125},
{489.875,342.0},
{492.9375,336.25},
{489.5625,326.78125},
{478.03125,324.3125},
{472.40625,323.75},
{467.03125,325.0},
{464.84375,324.0},
{467.84375,322.84375},
{469.4375,320.34375},
{470.15625,317.625},
{467.9375,316.34375},
{464.53125,314.125},
{461.15625,314.4375},
{460.59375,311.84375},
{459.625,312.3125},
{457.1875,314.03125},
{457.53125,309.78125},
{456.9375,303.75},
{455.96875,303.5},
{456.5,299.15625},
{457.59375,297.9375},
{458.15625,294.71875},
{461.5,293.59375},
{463.21875,290.59375},
{464.6875,288.6875},
{467.8125,289.71875},
{468.28125,286.34375},
{471.6875,285.90625},
{473.0625,285.53125},
{474.8125,286.84375},
{477.5,284.5625},
{480.84375,285.9375},
{491.25,284.21875},
{501.59375,280.5625},
{506.6875,274.625},
{506.03125,263.625},
{516.125,254.09375},
{509.71875,251.8125},
{495.125,256.375},
{461.84375,232.71875},
{422.59375,219.5625},
{399.6875,207.75},
{385.1875,196.0},
{390.40625,192.78125},
{386.09375,189.9375},
{379.625,188.84375},
{376.78125,184.25},
{381.96875,179.3125},
{394.71875,172.53125},
{405.9375,169.90625},
{419.625,163.90625},
{441.71875,149.84375},
{433.25,146.9375},
{394.25,146.9375},
{395.5625,151.21875},
{393.1875,151.6875},
{389.09375,150.65625},
{386.84375,150.34375},
{384.40625,147.25},
{383.6875,146.8125}
 },{
{133.625,149.8125},
{137.5625,153.1875},
{142.96875,157.25},
{146.34375,156.34375},
{151.25,157.09375},
{152.84375,153.34375},
{154.34375,149.40625},
{147.0625,146.9375},
{129.78125,146.9375}
 },{
{359.59375,158.5625},
{356.21875,158.875},
{357.375,155.125},
 },{
{344.21875,169.125},
{339.65625,168.875},
{343.1518,163.87054},
 },{
{357.2232,171.1116},
{356.05804,174.88393},
{354.96875,177.03125},
{354.71875,172.0625},
{358.125,167.71875},
 },{
{364.2768,178.58035},
{360.49106,177.4375},
{362.67856,173.125},
 },{
{538.59375,238.4375},
{527.46875,247.90625},
{538.28125,242.25},
{543.6875,237.78125},
{543.0625,235.4375},
 },{
{338.40625,252.75},
{342.40625,257.0},
{343.28125,259.1875},
{345.03125,262.15625},
{345.4375,267.71875},
{346.5,275.3125},
{353.71875,276.5},
{371.09375,277.625},
{386.75,267.65625},
{392.0,259.4375},
{390.59375,266.0625},
{397.9375,264.75},
{403.15625,270.28125},
{405.6875,278.84375},
{407.5,287.1875},
{403.34375,324.875},
{396.03125,318.71875},
{396.78125,324.84375},
{394.3125,325.71875},
{399.125,325.5625},
{402.8125,340.4375},
{399.84375,379.09375},
{406.6875,372.0},
{411.125,360.09375},
{413.90625,360.875},
{394.96875,404.75},
{386.5,415.46875},
{378.53125,431.625},
{365.46875,448.65625},
{356.21875,457.375},
{351.59375,461.625},
{349.90625,461.78125},
{345.53125,459.59375},
{343.15625,463.09375},
{337.5,463.40625},
{332.5,466.0},
{329.0625,465.09375},
{325.5,465.0625},
{319.34375,465.34375},
{316.875,461.375},
{312.03125,457.59375},
{306.8125,453.0625},
{305.71875,456.9375},
{302.46875,457.65625},
{301.53125,449.0},
{300.125,446.34375},
{299.0625,442.40625},
{298.4375,444.8125},
{291.9375,443.09375},
{287.8125,441.46875},
{284.96875,437.96875},
{285.71875,435.875},
{285.15625,430.96875},
{289.03125,433.03125},
{291.65625,435.875},
{292.125,432.5625},
{294.8125,434.71875},
{297.5625,432.3125},
{303.28125,431.8125},
{306.78125,431.8125},
{310.125,435.90625},
{317.46875,436.1875},
{322.96875,435.0625},
{324.1875,429.9375},
{327.84375,428.3125},
{323.8125,425.25},
{313.28125,422.03125},
{306.21875,418.78125},
{310.71875,414.0625},
{316.0625,416.59375},
{321.5,416.59375},
{337.6875,418.0},
{348.84375,415.46875},
{358.125,414.65625},
{362.9375,408.1875},
{358.125,406.96875},
{349.15625,406.1875},
{344.0,404.625},
{337.375,402.3125},
{329.09375,399.96875},
{317.28125,396.03125},
{316.75,393.34375},
{325.25,391.46875},
{326.9375,392.3125},
{328.40625,392.4375},
{334.1875,392.90625},
{336.125,386.125},
{345.03125,385.78125},
{340.6875,382.9375},
{336.375,377.40625},
{331.03125,373.40625},
{329.59375,366.5},
{332.46875,366.1875},
{334.28125,359.96875},
{334.0,355.53125},
{335.03125,347.0625},
{332.4375,340.03125},
{334.625,333.21875},
{334.90625,329.84375},
{335.625,323.46875},
{336.5625,316.65625},
{331.59375,314.0},
{329.625,312.8125},
{328.09375,305.4375},
{325.25,312.9375},
{321.25,312.4375},
{318.5,305.09375},
{315.125,302.1875},
{310.8125,301.0625},
{309.78125,293.65625},
{308.0,291.1875},
{306.53125,285.28125},
{309.5,285.625},
{311.25,281.25},
{313.0625,281.40625},
{313.0,275.65625},
{310.5,277.90625},
{310.625,275.3125},
{307.21875,276.5625},
{306.9375,272.25},
{305.25,268.21875},
{306.375,264.90625},
{304.96875,257.53125},
{310.96875,255.71875},
{313.625,252.28125},
{315.96875,253.90625},
{318.5,251.78125},
{322.96875,250.78125},
{328.40625,251.5},
{332.0,249.09375},
 },{
{351.125,264.375},
{348.09375,263.3125},
{353.75,261.21875},
 },{
{450.65625,306.1875},
{453.34375,308.71875},
{452.9375,311.21875},
{448.46875,310.6875},
{444.375,307.4375},
{446.53125,303.15625},
{447.5,305.8125},
{448.78125,307.625},
{448.46875,303.8125},
{448.25,302.4375},
 },{
{360.78125,320.65625},
{351.625,328.8125},
{354.0,335.4375},
{362.4375,342.59375},
{365.96875,348.3125},
{380.09375,352.5625},
{364.875,342.09375},
{370.9375,340.53125},
{378.15625,341.0625},
{382.625,340.96875},
{384.4375,343.625},
{393.90625,346.46875},
{373.46875,330.4375},
{383.6875,328.21875},
{384.625,323.59375},
{375.59375,319.46875},
{373.6875,319.1875},
 },{
{298.4375,332.375},
{293.3125,333.34375},
{287.8125,328.71875},
{290.40625,327.65625},
 },{
{490.90625,344.3125},
{492.46875,348.9375},
{496.3125,352.6875},
{498.75,352.71875},
{499.09375,346.8125},
{494.375,342.375},
{493.0625,341.40625},
 },{
{284.34375,348.375},
{288.3125,350.6875},
{291.5,352.25},
{294.8125,354.59375},
{299.4375,355.9375},
{302.3125,357.8125},
{296.9375,362.15625},
{290.8125,366.5},
{285.40625,370.84375},
{281.34375,372.125},
{278.0625,374.75},
{274.34375,373.15625},
{271.34375,369.46875},
{270.75,364.625},
{273.21875,363.3125},
{272.375,359.5625},
{275.0,352.84375},
{277.46875,351.59375},
{277.65625,349.4375},
{280.875,345.0625},
 },{
{499.875,358.59375},
{495.5625,367.21875},
{489.1875,366.03125},
{482.4375,371.40625},
{487.375,384.0625},
{504.21875,380.59375},
{512.34375,376.96875},
{501.875,377.125},
{494.0625,374.34375},
{503.09375,373.0625},
{510.3125,365.71875},
{509.65625,360.65625},
{505.0,354.375},
{501.84375,353.875},
 },{
{357.3125,381.65625},
{362.5,396.0},
{367.53125,409.0625},
{370.84375,399.625},
{368.40625,390.59375},
{364.71875,381.78125},
{363.53125,376.65625},
{358.125,369.34375},
{353.28125,362.65625},
 },{
{174.25,374.0625},
{175.5,379.1875},
{173.65625,382.5},
{168.4375,378.34375},
{171.625,373.8125},
{172.0625,372.9375},
 },{
{341.21115,391.60544},
{345.78125,391.46875},
{342.6875,388.1875},
 },{
{272.15625,396.28125},
{274.25,404.28125},
{276.375,411.96875},
{275.625,422.0},
{274.21875,416.4375},
{268.46875,408.65625},
{270.28125,398.125},
{271.15625,396.0625},
{272.3125,393.75},
 },{
{476.40625,398.0},
{473.40625,407.75},
{479.15625,408.875},
{484.4375,411.28125},
{486.8125,413.6875},
{484.28125,417.09375},
{487.8125,421.625},
{491.84375,413.9375},
{487.90625,409.34375},
{485.09375,408.1875},
{487.0625,400.34375},
{498.0,397.96875},
{485.28125,397.8125},
{480.84375,397.0},
{480.3125,395.1875},
 },{
{322.09375,404.625},
{335.46875,405.9375},
{342.875,410.75},
{336.78125,411.75},
{326.13007,415.2086},
{319.15625,413.9375},
{312.96875,408.96875},
{308.375,402.4375},
{311.1875,401.46875},
 },{
{469.53125,459.65625},
{472.625,465.40625},
{479.71875,477.6875},
{483.75,482.4375},
{486.90625,487.25},
{482.625,487.09375},
{455.25,473.59375},
{437.59375,461.15625},
{456.6875,453.6875},
{467.78125,452.5625},
 },{
{420.1875,491.75},
{389.75,505.46875},
{371.59375,491.96875},
{371.84375,486.46875},
{373.34375,482.78125},
{374.40625,481.1875},
{375.75,479.15625},
{375.5625,476.75},
{378.875,477.0625},
{380.8125,473.6875},
{382.21875,474.8125},
{387.125,473.84375},
{389.75,477.0},
{394.34375,481.0},
{397.6875,485.78125},
{400.78125,484.53125},
{403.8125,482.59375},
{406.0,481.53125},
{412.0625,479.15625},
{422.9375,470.59375},
{425.28125,467.78125},
 },{
{334.4375,489.375},
{299.625,573.28125},
{281.15625,584.03125},
{208.96875,538.09375},
{182.9375,485.03125},
{183.8125,482.25},
{187.875,482.28125},
{192.59375,484.96875},
{196.53125,489.28125},
{198.25,491.25},
{203.5,495.1875},
{206.84375,496.09375},
{208.71875,500.21875},
{211.40625,503.4375},
{216.0625,506.0},
{218.46875,509.53125},
{222.75,513.6875},
{226.03125,517.28125},
{227.09375,513.9375},
{228.5625,509.90625},
{230.96875,509.34375},
{231.75,509.5},
{233.90625,503.3125},
{234.84375,501.40625},
{235.84375,502.25},
{234.875,506.59375},
{237.3125,505.03125},
{239.75,503.3125},
{243.71875,502.0625},
{248.875,503.09375},
{250.03125,500.96875},
{255.71875,500.46875},
{259.53125,501.4375},
{266.59375,503.84375},
{270.65625,505.0625},
{281.75,514.25},
{289.375,519.15625},
{293.71875,521.3125},
{289.5,517.84375},
{284.90625,512.625},
{287.90625,511.65625},
{292.375,515.90625},
{298.1875,517.8125},
{304.21875,517.0},
{309.40625,509.375},
{308.21875,506.46875},
{308.25,499.25},
{314.1875,493.46875},
{324.65625,487.28125},
{333.1875,480.125},
{335.15625,479.40625},
 },
};

}
