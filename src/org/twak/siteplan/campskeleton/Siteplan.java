/*
 * CampSkeleton.java
 * 
 * Created on May 29, 2009, 11:44:52 AM
 */
package org.twak.siteplan.campskeleton;

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

import org.twak.camp.Output;
import org.twak.camp.Skeleton;
import org.twak.camp.Tag;
import org.twak.camp.debug.DebugDevice;
import org.twak.camp.ui.Bar;
import org.twak.camp.ui.Marker;
import org.twak.camp.ui.PointEditor;
import org.twak.siteplan.anchors.Anchor;
import org.twak.siteplan.anchors.NaturalStepShip;
import org.twak.siteplan.anchors.Ship;
import org.twak.siteplan.anim.APlanBoxes;
import org.twak.siteplan.anim.OverhangPlan;
import org.twak.siteplan.anim.PioneerPlan;
import org.twak.siteplan.anim.RenderAnimFrame;
import org.twak.siteplan.jme.Jme;
import org.twak.siteplan.jme.PillarFeature;
import org.twak.siteplan.jme.Preview;
import org.twak.siteplan.junk.ForcedStep;
import org.twak.siteplan.tags.PlanTag;
import org.twak.utils.LContext;
import org.twak.utils.Pair;
import org.twak.utils.WeakListener;
import org.twak.utils.collections.ConsecutivePairs;
import org.twak.utils.collections.Loop;
import org.twak.utils.collections.LoopL;
import org.twak.utils.ui.ListDownLayout;
import org.twak.utils.ui.SaveLoad;
import org.twak.utils.ui.SimpleFileChooser;
import org.twak.utils.ui.WindowManager;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

/**
 * User interface for editing the skeleton
 * 
 * @author twak
 */
public class Siteplan extends javax.swing.JFrame {
	private static final String GO = "go";
	public final static String toolKey = "related tool";
	public final static boolean Is_User = false; // dev flag

	public static Siteplan instance;
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

	public Tag wall = new Tag( "wall" ), roof = new Tag( "roof" ), weakEdge = new Tag( "weak edge" );

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

	public Siteplan() {
		this( null, true );
	}

	/** Creates new form CampSkeleton */
	public Siteplan( Plan plan, boolean showPreview ) {
		
		instance = this;
		
		if (showPreview)
			preview = new Preview();

		initComponents();
		
		centralPanel.setLayout( new ListDownLayout() );
		centralPanel.setPreferredSize( new Dimension( 200, 300 ) );
		centralPanel.setMaximumSize( new Dimension( 200, 30000 ) );

		for ( Tool tl : Tool.values() ) {
			if ( tl == Tool.Anchor ) // not needed for now
				continue;

			final Tool ft = tl;
			final JToggleButton tb = new JToggleButton( ft.name().toLowerCase() );
			tb.putClientProperty( this, ft );
			paletteGroup.add( tb );
			tb.addActionListener( new ActionListener() {

				public void actionPerformed( ActionEvent e ) {
					if ( tb.isSelected() )
						setTool( ft );
				}
			} );
			palettePanel.add( tb );
		}

		// select first button
		for ( Component c : palettePanel.getComponents() ) {
			( (JToggleButton) c ).setSelected( true );
			break;
		}

		saveLoad = new SaveLoad() {
			@Override
			public void afterLoad() {
				loadPlan( plan );
				// flush all tool state
				setTool( Tool.Vertex );
				setImage( null );
				showRoot();
				updateTitle();

				killTagFeature();
			}

			@Override
			public void makeNew() {
				setDefaultPlan();
				setImage( null );
				setTool( Tool.Vertex );
				reset();
				setPlan( plan );
				saveLoad.saveAs = null;
				updateTitle();

				killTagFeature();
			}

			@Override
			public void afterSave() {
				updateTitle();
			}

			@Override
			public void beforeSave() {
				// clean plan.profiles
				Set<Bar> used = plan.findBars();
				Iterator<Bar> itb = plan.profiles.keySet().iterator();
				while ( itb.hasNext() )
					if ( !used.contains( itb.next() ) )
						itb.remove();

				if ( profileUI != null )
					profileUI.flushtoProfile();
			}
		};
		saveLoad.addSaveLoadMenuItems( fileMenu, "plan", this ); // reflective reference to field plan

		if ( plan == null )
			setDefaultPlan();
		else
			setPlan( plan );

		setupProfileCombo();

		updateTitle();

		showRoot();

		setSize( 800, 800 );
		setLocation( 700, 100 );

		rootPanel.revalidate();

		// something to look at :)
		goButtonActionPerformed( null );
	}

	/**
	 * Call this to refresh the profile list in the UI
	 */
	public void profileListChanged() {
		setupProfileCombo();
		profileListChangedListeners.fire();
	}

	void updateTitle() {
		setTitle( "siteplan" );
	}

	public void setTool( Tool mode ) {
		nowSelectingFor( null );
		Siteplan.instance.highlightFor( new ArrayList() );

		this.mode = mode;

		toolPanel.removeAll();
		toolPanel.setLayout( new GridLayout( 1, 1 ) );

		for ( MarkerUI mui : new MarkerUI[] { planUI, profileUI } ) {
			if ( mui == null )
				continue; // still in setup
			flushMarkersToUI( mui );
		}

		if ( mode == Tool.Anchor ) {
			// don't like this, maybe we want a "goto root" button and only put features in the combo?
			if ( selectedTag == null && plan.tags.size() > 0 )
				planCombo.setSelectedItem( plan.tags.get( 0 ) );
		} else if ( mode == Tool.Features ) {
			showTagFeature();
			//            toolPanel.add( new FeatureUI(plan));
		} else if ( mode == Tool.Tag ) {
			showTagFeature();
			//            toolPanel.add ( new TagListUI(plan) );//new TagFeatureList (roof, weakEdge) );
		}

		centralPanel.revalidate();

		// quick and dirty
		for ( Component c : palettePanel.getComponents() ) {
			if ( c instanceof JToggleButton ) {
				JToggleButton j = (JToggleButton) c;
				if ( mode == j.getClientProperty( this ) && !j.isSelected() )
					j.doClick();
			}
		}

	}

	public void showTagFeature() {
		if ( tagFeatureFrame == null ) {
			tagFeatureFrame = new JFrame( "features and tags" );
			tagFeatureFrame.setContentPane( new TagsFeaturesUI( plan ) );
			WindowManager.register( tagFeatureFrame );
		}

		if ( !tagFeatureFrame.isVisible() ) {
			tagFeatureFrame.setSize( 800, 600 );
			tagFeatureFrame.setVisible( true );

			// position to right of main frame. vey annoying on laptop.
			//            Point p = new Point( jPanel4.getWidth(), 0 );
			//            SwingUtilities.convertPointToScreen( p, jPanel4 );
			//            tagFeatureFrame.setLocation( p );//p.x+getWidth(), p.y);
		}
		tagFeatureFrame.toFront();
	}

	public void killTagFeature() {
		if ( tagFeatureFrame == null )
			return;
		tagFeatureFrame.setVisible( false );
		tagFeatureFrame.dispose();
		tagFeatureFrame = null;
	}

	public void setProfile( Profile profile ) {
		if ( profileUI != null ) {
			if ( profileUI.profile == profile )
				return;
			else
				profileUI.flushtoProfile();
			flushMarkersToUI( profileUI );
		}

		if ( machineCombo.getSelectedItem() != profile ) {
			fireEvents = false;
			machineCombo.setSelectedItem( profile );
			// this occurs if we select an edge from a profile
			if ( machineCombo.getSelectedItem() != profile )
				machineCombo.setSelectedItem( null );
			fireEvents = true;
		}

		profilePanel.removeAll();
		profilePanel.setLayout( new GridLayout( 1, 1 ) );

		machineCombo.setEnabled( profile != null );

		if ( profile != null ) {
			profileUI = new ProfileUI( profile, plan, new ProfileEdgeSelected() ) {
				/**
				 * Quick hacky solutions until the ui is finalized...
				 */
				@Override
				public void releasePoint( Point2d pt, LContext<Bar> ctx, MouseEvent evt ) {
					super.releasePoint( pt, ctx, evt );
					if ( planUI != null )
						planUI.repaint();
				}

				@Override
				public void paint( Graphics g ) {
					super.paint( g );
					if ( planUI != null )
						planUI.somethingChanged( null );
				}
			};

			flushMarkersToUI( profileUI );
			profilePanel.add( profileUI );
			profileUI.revalidate();
		} else {
			profileUI = null;
			profilePanel.add( new JLabel( "Select an edge to edit the profile", JLabel.CENTER ) );
			profilePanel.revalidate();
		}
	}

	public void setEdge( Bar edge ) {
		selectedEdge = edge;

		Profile profile = plan.profiles.get( edge );
		setProfile( profile );
	}

	private void reset() {
		selectedEdge = null;
		profileUI = null;
		planUI = null;
		selectedTag = null;

		setupProfileCombo();
	}

	private void setDefaultPlan() {
		plan = new Plan();
		plan.name = "root-plan";

		Profile defaultProf = new Profile( 50 );
		defaultProf.points.get(0).append(new Bar( defaultProf.points.get(0).start.get().end, new Point2d (50,-100) ) );
//		defaultProf.points.get( 0 ).start.get().end.x += 20;
		

		createCross( plan, defaultProf );
		plan.addLoop( defaultProf.points.get( 0 ), plan.root, defaultProf );
	}

	public void somethingChanged() {
		if ( autoupdateButtom != null ) // if used in dummy mode, this'll be true
			if ( autoupdateButtom.isSelected() ) {
				dirty = true;
				goButtonActionPerformed( null );
			}

		somethingChangedListeners.fire();
	}

	private void setImage( BufferedImage read ) {
		bgImage = read;
		if ( planUI != null ) {
			planUI.bgImage = read;
			planUI.repaint();
		}
	}

	private void flushMarkersToUI( MarkerUI mui ) {
		mui.showMarkers( mode == Tool.Anchor || mode == Tool.Features );
		mui.showTags( mode == Tool.Tag );
	}

	void setSelectedFeature( Tag f ) {
		selectedTag = f;

		if ( planUI != null )
			planUI.repaint();

		if ( profileUI != null )
			profileUI.repaint();
	}

	private class ProfileEdgeSelected implements PointEditor.BarSelected {
		public void barSelected( LContext<Bar> ctx ) {
			//            JOptionPane.showMessageDialog( CampSkeleton.this, "not implemented in campskeleton" );
		}
	}

	public class PlanEdgeSelected implements PointEditor.BarSelected {
		public void barSelected( LContext<Bar> ctx ) {
			setEdge( ctx == null ? null : ctx.get() );
		}
	}

	private int countMarkerMatches( Object generator, Set<Profile> profiles ) {
		int count = 0;
		for ( Loop<Bar> loop : plan.points )
			for ( Bar b : loop )
				for ( Marker m : b.mould.markersOn( b ) )
					if ( m.generator.equals( generator ) )
						count++;

		for ( Profile p : new HashSet<Profile>( profiles ) )
			for ( Bar b : p.points.eIterator() )
				for ( Marker m : b.mould.markersOn( b ) )
					if ( m.generator.equals( generator ) )
						count++;

		// todo: each feature may also have nested markers?
		for ( Ship s : plan.ships )
			count += s.countMarkerMatches( generator );

		return count;
	}

	public void loadPlan( Plan plan ) {
		Siteplan.this.reset();

		int nSteps = 0, nInstances = 0;

		Set<Profile> usedProf = new HashSet();

		for ( Bar b : plan.points.eIterator() )
			usedProf.add( plan.profiles.get( b ) );

		for ( Ship s : plan.ships )
			if ( s instanceof NaturalStepShip ) {
				NaturalStepShip ss = (NaturalStepShip) s;
				for ( Bar b : ss.shape.eIterator() )
					usedProf.add( plan.profiles.get( b ) );
			}

		for ( Ship s : plan.ships )
			if ( s instanceof NaturalStepShip ) {
				nInstances += s.getInstances().size();

				if ( !s.getInstances().isEmpty() )
					nSteps++;
			}

		statusLabel.setText( "loops:" + plan.points.size() + " verts:" + plan.points.count() + " profiles:" + usedProf.size() + " n/steps:" + nSteps + " n/instances:" + nInstances + " globals:" + ( plan.globals.size() - 1 ) );

		setPlan( plan );

		for ( Tag f : plan.tags ) {
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

	public void setPlan( PlanUI selected ) {
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

	public void setPlan( Plan selected ) {
		// nothing to do?
		this.plan = (Plan) selected;
		setPlan( planUI = new PlanUI( (Plan) selected, new PlanEdgeSelected() ) ); 
		setImage( bgImage );

		setEdge( null );
		setupProfileCombo();
	}

	public void setupProfileCombo() {
		machineCombo.setModel( new DefaultComboBoxModel( new Vector( plan.findProfiles() ) ) );

		if ( selectedEdge != null )
			machineCombo.setSelectedItem( plan.profiles.get( selectedEdge ) );
	}

	public void assignProfileToBar( Bar bar, Profile profile ) {
		plan.profiles.put( bar, profile );
		repaint();
	}

	// Camp skeleton currently coordinates modes, & takes responsibility for the anchor that we're currently working on
	public Anchor selectedAnchor = null;
	List<Anchor> highlitAnchors = new ArrayList();

	public void setAnchorPlan( Marker m ) {
		if ( selectedAnchor != null ) {
			selectedAnchor.setPlanGen( m.generator );
			selectedAnchorListeners.fire();
		}
	}

	public void setAnchorProfile( Marker m ) {
		if ( selectedAnchor != null ) {
			selectedAnchor.setProfileGen( m.generator );
			selectedAnchorListeners.fire();
		}
	}

	public void nowSelectingFor( Anchor anchor ) {
		selectedAnchor = anchor;
		selectedAnchorListeners.fire();
		if ( planUI != null )
			planUI.repaint();
		if ( profileUI != null )
			profileUI.repaint();
	}

	public void highlightFor( List<Anchor> anchors ) {
		highlitAnchors = anchors;
		if ( planUI != null )
			planUI.repaint();
		if ( profileUI != null )
			profileUI.repaint();
	}

	public void showRoot() {
		planPanel.removeAll();
		planPanel.setLayout( new GridLayout( 1, 1 ) );
		setPlan( new PlanUI( plan, new PlanEdgeSelected() ) );
		setTool( mode );
		setImage( bgImage );
		planPanel.add( planUI );
		planUI.revalidate();
		repaint();

	}

	public void setGrid( double val ) {
		this.gridSize = val;
		if ( planUI != null )
			planUI.setGridSize( gridSize );
	}

	public void update( int f ) {
		update( f, true );
	}

	public void update( int f, boolean recalculate ) {
		int nFrame = Math.max( 0, f );
		int delta = nFrame - frame;
		if ( delta != 0 ) {
			plan.update( nFrame, delta );
			frame = nFrame;

			// assign profiles based on our new shape
			//            if ( plan.buildFromPlan != null )
			//                plan.buildFromPlan.buildFromPlan();

			setPlan( plan ); // <- not sure about this, can planUI repain on demand? or not be edited?
			if ( recalculate )
				somethingChanged(); // <- this'll redo the 3d output if "udpate" is selected
			statusLabel.setText( "f:" + frame );
			frameSpinner.setValue( nFrame );
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
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
		rootPanel = new javax.swing.JPanel();
		planBorder = new javax.swing.JPanel();
		planPanel = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		showRootButton = new javax.swing.JButton();
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

		planCombo.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				planComboActionPerformed( evt );
			}
		} );

		editPlanButton.setText( "set plan" );
		editPlanButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				editPlanButtonActionPerformed( evt );
			}
		} );

		newFeatureButton.setText( "new feature" );
		newFeatureButton.addMouseListener( new java.awt.event.MouseAdapter() {
			public void mousePressed( java.awt.event.MouseEvent evt ) {
				newFeatureButtonMousePressed( evt );
			}
		} );
		newFeatureButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				newFeatureButtonActionPerformed( evt );
			}
		} );

		setMachineButton.setText( "set" );
		setMachineButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				setMachineButtonActionPerformed( evt );
			}
		} );

		setDefaultCloseOperation( javax.swing.WindowConstants.EXIT_ON_CLOSE );

		goButton.setText( GO );
		goButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				goButtonActionPerformed( evt );
			}
		} );

		autoupdateButtom.setText( "updates" );
		autoupdateButtom.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				autoupdateButtomActionPerformed( evt );
			}
		} );

		playButton.setText( ">" );
		playButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				playButtonActionPerformed( evt );
			}
		} );

		jToggleButton1.setText( "random" );
		jToggleButton1.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				jToggleButton1ActionPerformed( evt );
			}
		} );

		frameSpinner.setModel( new javax.swing.SpinnerNumberModel( Integer.valueOf( 0 ), Integer.valueOf( 0 ), null, Integer.valueOf( 1 ) ) );
		frameSpinner.addChangeListener( new javax.swing.event.ChangeListener() {
			public void stateChanged( javax.swing.event.ChangeEvent evt ) {
				frameSpinnerStateChanged( evt );
			}
		} );

		javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout( jPanel3 );
		jPanel3.setLayout( jPanel3Layout );
		jPanel3Layout.setHorizontalGroup( jPanel3Layout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGroup( javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup().addContainerGap().addComponent( frameSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE ).addPreferredGap( javax.swing.LayoutStyle.ComponentPlacement.RELATED ).addComponent( statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE ).addPreferredGap( javax.swing.LayoutStyle.ComponentPlacement.RELATED ).addComponent( jToggleButton1 ).addPreferredGap( javax.swing.LayoutStyle.ComponentPlacement.RELATED ).addComponent( playButton ).addPreferredGap( javax.swing.LayoutStyle.ComponentPlacement.RELATED ).addComponent( autoupdateButtom ).addPreferredGap( javax.swing.LayoutStyle.ComponentPlacement.RELATED ).addComponent( goButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE ).addContainerGap() ) );
		jPanel3Layout.setVerticalGroup( jPanel3Layout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGroup( jPanel3Layout.createSequentialGroup().addContainerGap().addGroup( jPanel3Layout.createParallelGroup( javax.swing.GroupLayout.Alignment.TRAILING, false ).addComponent( frameSpinner, javax.swing.GroupLayout.Alignment.LEADING ).addComponent( goButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE ).addComponent( autoupdateButtom, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE ).addComponent( playButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE ).addComponent( jToggleButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE ).addGroup( jPanel3Layout.createParallelGroup( javax.swing.GroupLayout.Alignment.BASELINE ).addComponent( statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20,
				javax.swing.GroupLayout.PREFERRED_SIZE ) ) ).addContainerGap( 14, Short.MAX_VALUE ) ) );

		getContentPane().add( jPanel3, java.awt.BorderLayout.SOUTH );

		rootPanel.setLayout( new java.awt.BorderLayout( 3, 0 ) );

		planBorder.setBorder( javax.swing.BorderFactory.createLineBorder( new java.awt.Color( 0, 0, 0 ) ) );
		planBorder.setPreferredSize( new java.awt.Dimension( 200, 800 ) );
		planBorder.setLayout( new java.awt.BorderLayout() );

		planPanel.setBorder( javax.swing.BorderFactory.createEmptyBorder( 1, 1, 1, 1 ) );

		javax.swing.GroupLayout planPanelLayout = new javax.swing.GroupLayout( planPanel );
		planPanel.setLayout( planPanelLayout );
		planPanelLayout.setHorizontalGroup( planPanelLayout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGap( 0, 276, Short.MAX_VALUE ) );
		planPanelLayout.setVerticalGroup( planPanelLayout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGap( 0, 484, Short.MAX_VALUE ) );

		planBorder.add( planPanel, java.awt.BorderLayout.CENTER );

		showRootButton.setText( "root" );
		showRootButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				showRootBurronActionPerformed( evt );
			}
		} );

		importButton.setText( "import" );
		importButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				importButtonActionPerformed( evt );
			}
		} );

		exportButton.setText( "export" );
		exportButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				exportButtonActionPerformed( evt );
			}
		} );

		javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout( jPanel2 );
		jPanel2.setLayout( jPanel2Layout );
		jPanel2Layout.setHorizontalGroup( jPanel2Layout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGroup( jPanel2Layout.createSequentialGroup().addComponent( showRootButton ).addPreferredGap( javax.swing.LayoutStyle.ComponentPlacement.RELATED, 91, Short.MAX_VALUE ).addComponent( exportButton ).addPreferredGap( javax.swing.LayoutStyle.ComponentPlacement.RELATED ).addComponent( importButton ) ) );
		jPanel2Layout.setVerticalGroup( jPanel2Layout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGroup( jPanel2Layout.createParallelGroup( javax.swing.GroupLayout.Alignment.BASELINE ).addComponent( showRootButton ).addComponent( importButton ).addComponent( exportButton ) ) );

		planBorder.add( jPanel2, java.awt.BorderLayout.NORTH );

		rootPanel.add( planBorder, java.awt.BorderLayout.CENTER );

		jPanel5.setPreferredSize( new java.awt.Dimension( 450, 518 ) );
		jPanel5.setLayout( new java.awt.GridLayout( 1, 2 ) );

		profileBorder.setBorder( javax.swing.BorderFactory.createLineBorder( new java.awt.Color( 0, 0, 0 ) ) );
		profileBorder.setPreferredSize( new java.awt.Dimension( 200, 300 ) );
		profileBorder.setLayout( new java.awt.BorderLayout() );

		jPanel1.setLayout( new java.awt.BorderLayout() );

		addMachineButton.setText( "+" );
		addMachineButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				addMachineButtonActionPerformed( evt );
			}
		} );

		machineCombo.addItemListener( new java.awt.event.ItemListener() {
			public void itemStateChanged( java.awt.event.ItemEvent evt ) {
				machineComboItemStateChanged( evt );
			}
		} );

		javax.swing.GroupLayout profileCardLayout = new javax.swing.GroupLayout( profileCard );
		profileCard.setLayout( profileCardLayout );
		profileCardLayout.setHorizontalGroup( profileCardLayout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGroup( javax.swing.GroupLayout.Alignment.TRAILING, profileCardLayout.createSequentialGroup().addComponent( machineCombo, 0, 176, Short.MAX_VALUE ).addPreferredGap( javax.swing.LayoutStyle.ComponentPlacement.RELATED ).addComponent( addMachineButton ) ) );
		profileCardLayout.setVerticalGroup( profileCardLayout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGroup( profileCardLayout.createParallelGroup( javax.swing.GroupLayout.Alignment.BASELINE ).addComponent( machineCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE ).addComponent( addMachineButton ) ) );

		jPanel1.add( profileCard, java.awt.BorderLayout.CENTER );

		profileBorder.add( jPanel1, java.awt.BorderLayout.NORTH );

		javax.swing.GroupLayout profilePanelLayout = new javax.swing.GroupLayout( profilePanel );
		profilePanel.setLayout( profilePanelLayout );
		profilePanelLayout.setHorizontalGroup( profilePanelLayout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGap( 0, 223, Short.MAX_VALUE ) );
		profilePanelLayout.setVerticalGroup( profilePanelLayout.createParallelGroup( javax.swing.GroupLayout.Alignment.LEADING ).addGap( 0, 486, Short.MAX_VALUE ) );

		profileBorder.add( profilePanel, java.awt.BorderLayout.CENTER );

		jPanel5.add( profileBorder );

		centralPanel.setPreferredSize( new java.awt.Dimension( 200, 800 ) );
		centralPanel.setLayout( new java.awt.GridLayout( 0, 1 ) );

		palettePanel.setLayout( new java.awt.GridLayout( 0, 1 ) );
		centralPanel.add( palettePanel );

		toolPanel.setBorder( javax.swing.BorderFactory.createLineBorder( new java.awt.Color( 0, 0, 0 ) ) );
		toolPanel.setLayout( new java.awt.GridLayout( 1, 1 ) );
		centralPanel.add( toolPanel );

		jPanel5.add( centralPanel );

		rootPanel.add( jPanel5, java.awt.BorderLayout.EAST );

//		rootPanel.add( preview.getPanel(), java.awt.BorderLayout.WEST );

		getContentPane().add( rootPanel, java.awt.BorderLayout.CENTER );

		fileMenu.setText( "file" );

		objDumpItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK ) );
		objDumpItem.setText( "output obj" );
		objDumpItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				objDumpItemActionPerformed( evt );
			}
		} );
		fileMenu.add( objDumpItem );

		meshItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_MASK ) );
		meshItem.setText( "add meshes" );
		meshItem.setEnabled( false );
		meshItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				meshItemActionPerformed( evt );
			}
		} );
		fileMenu.add( meshItem );

		jMenuBar1.add( fileMenu );

		jMenu1.setText( "view" );

		backgroundItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_MASK ) );
		backgroundItem.setText( "set background image" );
		backgroundItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				backgroundItemActionPerformed( evt );
			}
		} );
		jMenu1.add( backgroundItem );

		clearImageButton.setText( "clear background image" );
		clearImageButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				clearImageButtonActionPerformed( evt );
			}
		} );
		jMenu1.add( clearImageButton );

		jMenu3.setText( "grid" );

		gridGroup.add( gridOff );
		gridOff.setSelected( true );
		gridOff.setText( "off" );
		gridOff.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				gridOffActionPerformed( evt );
			}
		} );
		jMenu3.add( gridOff );

		gridGroup.add( gridSmall );
		gridSmall.setText( "small" );
		gridSmall.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				gridSmallActionPerformed( evt );
			}
		} );
		jMenu3.add( gridSmall );

		gridGroup.add( gridMedium );
		gridMedium.setText( "medium" );
		gridMedium.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				gridMediumActionPerformed( evt );
			}
		} );
		jMenu3.add( gridMedium );

		gridGroup.add( gridHuge );
		gridHuge.setText( "big" );
		gridHuge.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				gridHugeActionPerformed( evt );
			}
		} );
		jMenu3.add( gridHuge );

		jMenu1.add( jMenu3 );

		jMenuBar1.add( jMenu1 );

		jMenu2.setText( "plan" );

		jMenu4.setText( "new plan" );

		aPlanItem.setText( "animated plan" );
		aPlanItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				aPlanItemActionPerformed( evt );
			}
		} );
		jMenu4.add( aPlanItem );

		pioneersPlanItem.setText( "pioneers demo mode" );
		pioneersPlanItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				pioneersPlanItemActionPerformed( evt );
			}
		} );
		jMenu4.add( pioneersPlanItem );

		overhangPlanItem.setText( "overhang plan" );
		overhangPlanItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				overhangPlanItemActionPerformed( evt );
			}
		} );
		jMenu4.add( overhangPlanItem );

		jMenu2.add( jMenu4 );

		timeBackItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_COMMA, 0 ) );
		timeBackItem.setText( "time--" );
		timeBackItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				timeBackItemActionPerformed( evt );
			}
		} );
		jMenu2.add( timeBackItem );

		timeForwardItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_PERIOD, 0 ) );
		timeForwardItem.setText( "time++" );
		timeForwardItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				timeForwardItemActionPerformed( evt );
			}
		} );
		jMenu2.add( timeForwardItem );

		zeroTimeItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_0, 0 ) );
		zeroTimeItem.setText( "time to zero" );
		zeroTimeItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				zeroTimeItemActionPerformed( evt );
			}
		} );
		jMenu2.add( zeroTimeItem );

		editClassifier.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_K, java.awt.event.InputEvent.CTRL_MASK ) );
		editClassifier.setText( "edit edge classifier" );
		editClassifier.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				editClassifierActionPerformed( evt );
			}
		} );
		jMenu2.add( editClassifier );

		renderAnimItem.setText( "anim to .obj" );
		renderAnimItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				renderAnimItemActionPerformed( evt );
			}
		} );
		jMenu2.add( renderAnimItem );
		jMenu2.add( jSeparator1 );

		clearPlanItem.setText( "clear plan" );
		clearPlanItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				clearPlanItemActionPerformed( evt );
			}
		} );
		jMenu2.add( clearPlanItem );

		svgImport.setText( "import svg plan..." );
		svgImport.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				svgImportActionPerformed( evt );
			}
		} );
		jMenu2.add( svgImport );

		reversePlanItem.setText( "reverse plan direction" );
		reversePlanItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				reversePlanItemActionPerformed( evt );
			}
		} );
		jMenu2.add( reversePlanItem );

		jMenuBar1.add( jMenu2 );

		jMenu5.setText( "help" );
		jMenu5.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				jMenu5ActionPerformed( evt );
			}
		} );

		aboutItem.setAccelerator( javax.swing.KeyStroke.getKeyStroke( java.awt.event.KeyEvent.VK_F1, 0 ) );
		aboutItem.setText( "about" );
		aboutItem.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed( java.awt.event.ActionEvent evt ) {
				aboutItemActionPerformed( evt );
			}
		} );
		jMenu5.add( aboutItem );

		jMenuBar1.add( jMenu5 );

		setJMenuBar( jMenuBar1 );

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void machineComboItemStateChanged( java.awt.event.ItemEvent evt )//GEN-FIRST:event_machineComboItemStateChanged
	{//GEN-HEADEREND:event_machineComboItemStateChanged
		if ( !fireEvents )
			return;
		Profile p = (Profile) machineCombo.getSelectedItem();
		setProfile( p );

		if ( p != null )
			plan.profiles.put( selectedEdge, p );

		repaint();
	}//GEN-LAST:event_machineComboItemStateChanged

	private void addMachineButtonActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_addMachineButtonActionPerformed
	{//GEN-HEADEREND:event_addMachineButtonActionPerformed
		if ( selectedEdge == null ) {
			JOptionPane.showMessageDialog( this, "Please select an edge first, or middle click a plan to create one", "No selected edge", JOptionPane.ERROR_MESSAGE );
			return;
		}

		Profile p = plan.createNewProfile( plan.profiles.get( selectedEdge ) );

		plan.profiles.put( selectedEdge, p );

		setProfile( p );
		setupProfileCombo();

		repaint();
	}//GEN-LAST:event_addMachineButtonActionPerformed

	private void newFeatureButtonActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_newFeatureButtonActionPerformed
	{//GEN-HEADEREND:event_newFeatureButtonActionPerformed

	}//GEN-LAST:event_newFeatureButtonActionPerformed

	private void planComboActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_planComboActionPerformed
	{//GEN-HEADEREND:event_planComboActionPerformed
		if ( !fireEvents )
			return;
		Object o = planCombo.getSelectedItem();
		if ( o instanceof Tag ) {
			selectedTag = (Tag) o;

			if ( planUI != null )
				planUI.repaint();

			if ( o instanceof ForcedStep || o instanceof PillarFeature )
				setTool( Tool.Anchor );
			else
				setTool( Tool.Tag );
		}
	}//GEN-LAST:event_planComboActionPerformed

	private void editPlanButtonActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_editPlanButtonActionPerformed
	{//GEN-HEADEREND:event_editPlanButtonActionPerformed
		setPlan( (Plan) planCombo.getSelectedItem() );
	}//GEN-LAST:event_editPlanButtonActionPerformed

	private void meshItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_meshItemActionPerformed
	{//GEN-HEADEREND:event_meshItemActionPerformed

		if ( preview != null && output != null ) {
			preview.clear = true;

			show( output, (Skeleton) threadKey );

            preview.display( output, true, true, true );

			for ( PlanTag pt : plan.tags ) {
				pt.postProcess( output, preview, threadKey );
			}

			( (PlanSkeleton) output.skeleton ).addMeshesTo( preview );

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

	private void objDumpItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_objDumpItemActionPerformed
	{//GEN-HEADEREND:event_objDumpItemActionPerformed
		if ( preview != null )
			preview.outputObj( this );
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

	private void goButtonActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_goButtonActionPerformed
	{//GEN-HEADEREND:event_goButtonActionPerformed
		if ( !busy ) {
			busy = true;
			dirty = false;
			goButton.setText( "[working]" );

			new Thread() {
				@Override
				public void run() {
					try {
						DebugDevice.reset();
						Skeleton s = new PlanSkeleton( plan );
						s.skeleton();

						if ( s.output.faces != null ) {
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
					} finally {
						busy = false;
						goButton.setText( GO );
						// if something's changed since we started work, run again...
						if ( dirty )
							SwingUtilities.invokeLater( new Runnable() {

								public void run() {
									goButtonActionPerformed( null );
								}
							} );
					}

				}
			}.start();
		}
	}//GEN-LAST:event_goButtonActionPerformed
	// TODO add your handling code here:

	private void autoupdateButtomActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_autoupdateButtomActionPerformed
	{//GEN-HEADEREND:event_autoupdateButtomActionPerformed
		//        autoup = autoupdateButtom.isSelected();
		if ( autoupdateButtom.isSelected() )
			somethingChanged();
	}//GEN-LAST:event_autoupdateButtomActionPerformed

	private void newFeatureButtonMousePressed( java.awt.event.MouseEvent evt )//GEN-FIRST:event_newFeatureButtonMousePressed
	{//GEN-HEADEREND:event_newFeatureButtonMousePressed

		DefaultListModel dlm = new DefaultListModel();

		abstract class Clickable {
			String name;

			public Clickable( String name ) {
				this.name = name;
			}

			public abstract Tag makeFeature();

			@Override
			public String toString() {
				return name;
			}
		}

		dlm.addElement( new Clickable( "forced step" ) {
			@Override
			public Tag makeFeature() {
				return new ForcedStep( plan );
			}
		} );

		final JList list = new JList( dlm );

		Point pt = evt.getPoint();
		pt = SwingUtilities.convertPoint( newFeatureButton, pt, null );

		final Popup pop = PopupFactory.getSharedInstance().getPopup( this, list, pt.x + getX(), pt.y + getY() );
		pop.show();

		list.addMouseListener( new MouseAdapter() {
			@Override
			public void mouseExited( MouseEvent e ) {
				pop.hide();
			}
		} );

		list.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent e ) {
				Object o = list.getSelectedValue();
				if ( o != null && o instanceof Clickable ) {
					// adds to plan!
					( (Clickable) o ).makeFeature();
				}
				pop.hide();
			}
		} );
	}

	enum PlaybackStatus {
		PLAY, PAUSE
	}

	PlaybackStatus playbackStatus = PlaybackStatus.PAUSE;
	PlaybackThread playbackThread;

	public class PlaybackThread extends Thread {
		public boolean run = false;

		@Override
		public void run() {
			while ( true ) {
				try {
					if ( run && !busy )
						SwingUtilities.invokeLater( new Runnable() {

							@Override
							public void run() {
								int nF = frame + 1;
								if ( nF > 100 )
									nF = 0;

								if ( jToggleButton1.isSelected() ) {

									nF = (int) ( Math.random() * 100 );
								}

								update( nF );
							}
						} );
					Thread.sleep( 1000 );
				} catch ( Throwable th ) {
					th.printStackTrace();
				}
			}
		}

	}

	public void togglePlaybackStatus() {
		
		if (playbackThread == null) {
			playbackThread = new PlaybackThread();
			playbackThread.start();
		}
		
		if ( playbackStatus == PlaybackStatus.PLAY )
			playbackStatus = playbackStatus.PAUSE;
		else
			playbackStatus = playbackStatus.PLAY;

		playbackThread.run = playbackStatus == playbackStatus.PLAY;
	}//GEN-LAST:event_newFeatureButtonMousePressed

	private void showRootBurronActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_showRootBurronActionPerformed
		showRoot();
	}//GEN-LAST:event_showRootBurronActionPerformed

	File currentFolder = new File( "." );

	private void backgroundItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_backgroundItemActionPerformed
	{//GEN-HEADEREND:event_backgroundItemActionPerformed

		new SimpleFileChooser( this, false, "select background image" ) {
			@Override
			public void heresTheFile( File f ) throws Throwable {
				setImage( ImageIO.read( f ) );
			}
		};
	}//GEN-LAST:event_backgroundItemActionPerformed

	private void clearImageButtonActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_clearImageButtonActionPerformed
	{//GEN-HEADEREND:event_clearImageButtonActionPerformed
		setImage( null );
	}//GEN-LAST:event_clearImageButtonActionPerformed

	private void exportButtonActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_exportButtonActionPerformed
	{//GEN-HEADEREND:event_exportButtonActionPerformed
		new SimpleFileChooser( this, true, "save feature info" ) {
			@Override
			public void heresTheFile( File f ) {
				planUI.exportt( f );
			}
		};
	}//GEN-LAST:event_exportButtonActionPerformed

	private void importButtonActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_importButtonActionPerformed
	{//GEN-HEADEREND:event_importButtonActionPerformed
		new SimpleFileChooser( this, false, "load feature info" ) {
			@Override
			public void heresTheFile( File f ) {
				planUI.importt( f );
			}
		};
	}//GEN-LAST:event_importButtonActionPerformed

	private void gridOffActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_gridOffActionPerformed
	{//GEN-HEADEREND:event_gridOffActionPerformed
		setGrid( -1 );
	}//GEN-LAST:event_gridOffActionPerformed

	private void gridSmallActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_gridSmallActionPerformed
	{//GEN-HEADEREND:event_gridSmallActionPerformed
		setGrid( 1 );
	}//GEN-LAST:event_gridSmallActionPerformed

	private void gridMediumActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_gridMediumActionPerformed
	{//GEN-HEADEREND:event_gridMediumActionPerformed
		setGrid( 5 );
	}//GEN-LAST:event_gridMediumActionPerformed

	private void gridHugeActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_gridHugeActionPerformed
	{//GEN-HEADEREND:event_gridHugeActionPerformed
		setGrid( 10 );
	}//GEN-LAST:event_gridHugeActionPerformed

	private void setMachineButtonActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_setMachineButtonActionPerformed
	{//GEN-HEADEREND:event_setMachineButtonActionPerformed
		Profile p = (Profile) machineCombo.getSelectedItem();
		if ( p != null )
			plan.profiles.put( selectedEdge, p );
		repaint();
	}//GEN-LAST:event_setMachineButtonActionPerformed

	private void timeBackItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_timeBackItemActionPerformed
	{//GEN-HEADEREND:event_timeBackItemActionPerformed
		update( frame - 1 );
	}//GEN-LAST:event_timeBackItemActionPerformed

	private void zeroTimeItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_zeroTimeItemActionPerformed
	{//GEN-HEADEREND:event_zeroTimeItemActionPerformed
		update( 0 );
	}//GEN-LAST:event_zeroTimeItemActionPerformed

	private void timeForwardItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_timeForwardItemActionPerformed
	{//GEN-HEADEREND:event_timeForwardItemActionPerformed
		update( frame + ( evt.getModifiers() != 0 ? 10 : 1 ) );
	}//GEN-LAST:event_timeForwardItemActionPerformed

	private void renderAnimItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_renderAnimItemActionPerformed
	{//GEN-HEADEREND:event_renderAnimItemActionPerformed
		new RenderAnimFrame();
	}//GEN-LAST:event_renderAnimItemActionPerformed

	private void aPlanItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_aPlanItemActionPerformed
	{//GEN-HEADEREND:event_aPlanItemActionPerformed
		setPlan( new APlanBoxes() );
		frame = 0;
		update( 1 );
		update( 0 );
	}//GEN-LAST:event_aPlanItemActionPerformed

	private void svgImportActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_svgImportActionPerformed
	{//GEN-HEADEREND:event_svgImportActionPerformed
		new SimpleFileChooser( this, false, "select svg to import as plan" ) {
			@Override
			public void heresTheFile( File f ) throws Throwable {
				//                new ImportSVG( f, plan );
				planUI.repaint();
			}
		};
	}//GEN-LAST:event_svgImportActionPerformed

	private void reversePlanItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_reversePlanItemActionPerformed
	{//GEN-HEADEREND:event_reversePlanItemActionPerformed
		Bar.reverse( plan.points );
		planUI.repaint();
		somethingChanged();
	}//GEN-LAST:event_reversePlanItemActionPerformed

	private void clearPlanItemActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_clearPlanItemActionPerformed
	{//GEN-HEADEREND:event_clearPlanItemActionPerformed
		plan.points.clear();
		planUI.repaint();
		somethingChanged();
	}//GEN-LAST:event_clearPlanItemActionPerformed

	private void editClassifierActionPerformed( java.awt.event.ActionEvent evt )//GEN-FIRST:event_editClassifierActionPerformed
	{//GEN-HEADEREND:event_editClassifierActionPerformed
		//        if (plan.buildFromPlan != null)
		//            plan.buildFromPlan.showUI();
		//        else
		JOptionPane.showMessageDialog( this, "Must be using a procedural floorplan", "Null editor not available", JOptionPane.ERROR_MESSAGE );

	}//GEN-LAST:event_editClassifierActionPerformed

	private void pioneersPlanItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_pioneersPlanItemActionPerformed
		setPlan( new PioneerPlan() );
		frame = 0;
		update( 0 );
	}//GEN-LAST:event_pioneersPlanItemActionPerformed

	private void playButtonActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_playButtonActionPerformed
		togglePlaybackStatus();
		if ( playbackStatus == PlaybackStatus.PAUSE )
			playButton.setText( ">" );
		else
			playButton.setText( "||" );
	}//GEN-LAST:event_playButtonActionPerformed

	private void jToggleButton1ActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_jToggleButton1ActionPerformed
		// TODO add your handling code here:
	}//GEN-LAST:event_jToggleButton1ActionPerformed

	private void jMenu5ActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_jMenu5ActionPerformed
		// TODO add your handling code here:
	}//GEN-LAST:event_jMenu5ActionPerformed

	private void aboutItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_aboutItemActionPerformed
		new AboutBox();
	}//GEN-LAST:event_aboutItemActionPerformed

	private void overhangPlanItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_overhangPlanItemActionPerformed
		setPlan( new OverhangPlan() );
		frame = 0;
		update( 0 );
	}//GEN-LAST:event_overhangPlanItemActionPerformed

	private void frameSpinnerStateChanged( javax.swing.event.ChangeEvent evt ) {//GEN-FIRST:event_frameSpinnerStateChanged
		update( (Integer) frameSpinner.getValue() );
	}//GEN-LAST:event_frameSpinnerStateChanged

	public static void main( String[] args ) {
		WindowManager.init( "siteplan", "/org/twak/siteplan/resources/icon256.png" );
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch ( Throwable ex ) {
			ex.printStackTrace();
		}

		Siteplan cs = new Siteplan();
		cs.setVisible( true );
		WindowManager.register( cs );

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
	private javax.swing.JPanel rootPanel;
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
	private javax.swing.JButton showRootButton;
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
	public static LoopL<Bar> createCircularPoints( int count, int x, int y, int rad, Profile profile, Plan plan ) {
		double delta = Math.PI * 2 / count;

		LoopL<Bar> loopl = new LoopL();
		Loop<Bar> loop = new Loop();
		loopl.add( loop );

		Point2d prev = null, start = null;
		//        boolean odd = true;
		for ( int i = 0; i < count; i++ ) {
			Point2d c = new Point2d( x + (int) ( Math.cos( ( i - 0.5 ) * delta ) * rad ), y + (int) ( Math.sin( ( i - 0.5 ) * delta ) * rad ) );

			if ( prev != null ) {
				Bar e = new Bar( prev, c );
				loop.append( e );
				plan.profiles.put( e, profile );
			} else
				start = c;
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

	public static LoopL<Bar> createfourSqaures( Plan plan, Profile profile ) {
		LoopL<Bar> loopl = new LoopL();

		List<Point2d> pts = Arrays.asList(
				//            new Point2d (100, 250),
				//            new Point2d (250,250),
				//            new Point2d (250,100),
				//            new Point2d (350,100),
				//            new Point2d (400,600)

				new Point2d( 0, 0 ), new Point2d( -50, 0 ), new Point2d( -50, -50 ), new Point2d( 0, -50 ) );

		//         for (Point2d p : pts)
		//             p.scale( 0.3 );

		List<List<Point2d>> stp = new ArrayList();

		for ( Point2d offset : new Point2d[] { new Point2d( 0, 150 ), new Point2d( 0, -150 ), new Point2d( 150, 0 ), new Point2d( -150, 0 ) } ) {
			List<Point2d> t = new ArrayList();
			stp.add( t );

			for ( Point2d pt : pts )
				t.add( new Point2d( offset.x + pt.x, offset.y + pt.y ) );
		}

		for ( List<Point2d> pts2 : stp ) {
			Loop<Bar> loop = new Loop();
			loopl.add( loop );

			for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( pts2, true ) ) {
				Bar b;
				loop.append( b = new Bar( pair.first(), pair.second() ) );
				plan.profiles.put( b, profile );
			}
		}

		return loopl;
	}

	public static void createCross( Plan plan, Profile profile ) {
		LoopL<Bar> loopl = new LoopL();
		Loop<Bar> loop = new Loop();
		loopl.add( loop );

		List<Point2d> pts = Arrays.asList(
				//            new Point2d (100, 250),
				//            new Point2d (250,250),
				//            new Point2d (250,100),
				//            new Point2d (350,100),
				//            new Point2d (400,600)

				new Point2d( 250, 100 ), new Point2d( 350, 100 ), new Point2d( 350, 250 ), new Point2d( 500, 250 ), new Point2d( 500, 350 ), new Point2d( 350, 350 ), new Point2d( 350, 500 ), new Point2d( 250, 500 ), new Point2d( 250, 350 ), new Point2d( 100, 350 ), new Point2d( 100, 250 ), new Point2d( 250, 250 ) );

		for ( Point2d p : pts )
			p.scale( 0.3 );

		for ( Pair<Point2d, Point2d> pair : new ConsecutivePairs<Point2d>( pts, true ) ) {
			Bar b;
			loop.append( b = new Bar( pair.first(), pair.second() ) );
			plan.profiles.put( b, profile );
		}
		plan.points = loopl;
	}

	/**
	 * Debug locations, only use this method before the skeleton is complete
	 * 
	 * @param mat
	 */
	List<Spatial> markers = new ArrayList();

	public void addDebugMarker( Matrix4d mat ) {
		Point3d loc = Jme.convert( new Vector3d( mat.getM03(), mat.getM13(), mat.getM23() ) );
		System.out.println( mat );
		System.out.println( loc );
		Box box = new Box( new Vector3f( (float) loc.x, (float) loc.y, (float) loc.z ), 0.3f, 0.3f, 0.3f );
	}

	/**
	 * Thread key ensures that only additional meshes (tiles, pillars) that were
	 * designed for the current input are added
	 */
	Output output;

	public void show( Output output, Skeleton threadKey ) {
		this.threadKey = threadKey;
		meshItem.setEnabled( true );
		this.output = output;
		if ( preview != null ) // might take a while...
		{
			preview.clear = true;

			Point2d pt = getMiddle( plan.points );
			
			preview.display( output, true, true, true );
			
//			preview.setViewStats( pt.x / Jme.scale, threadKey.height / Jme.scale, pt.y / Jme.scale, threadKey );

//			if ( !markers.isEmpty() ) {
//				for ( Spatial s : markers )
//					preview.display( s, threadKey );
//			}
			
			markers.clear();
		}
	}

	public static Point2d getMiddle( LoopL<Bar> plan ) {
		Point2d middle = new Point2d();

		int count = 0;

		for ( Bar b : plan.eIterator() ) {
			middle.add( b.start );
			count++;
		}

		middle.scale( 1. / count );
		return middle;
	}

	public static void removeParallel( LoopL<Bar> loopL ) {
		Set<Bar> toRemove = new HashSet();
		for ( Loop<Bar> loop : loopL ) {
			Bar last = loop.start.getPrev().get();
			for ( Bar e : loop )
				if ( e.start.distance( e.end ) < 0.1 ) {
					// remove current
					toRemove.add( e );
					last.end = e.end;
					//                    last.end.nextC = e.end.nextC;
					//                    e.end.nextC.prevC = last.end;
					//                    last.end.nextL = e.end.nextL;
				} else
					last = e;

			for ( Bar e : toRemove )
				loop.remove( e );
		}
	}

	public void addedBar( Bar bar ) {
		
	}
}
