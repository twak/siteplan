/*
 * AtlantisEditor2.java
 *
 * Created on May 6, 2010, 2:39:53 AM
 */

package camp.bigoldataset;

import camp.bigoldataset.AtlantisPointEditor.Mode;
import camp.jme.Jme;
import camp.jme.JmeFace;
import camp.jme.JmeLoopL;
import camp.jme.JmeObjDump;
import camp.jme.Preview;
import campskeleton.CampSkeleton;
import campskeleton.Plan;
import campskeleton.PlanSkeleton;
import campskeleton.Profile;
import campskeleton.ProfileUI;
import campskeleton.Tool;
import com.jme.scene.Node;
import java.awt.AWTEvent;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.vecmath.Point2d;
import straightskeleton.Output;
import straightskeleton.Output.Face;
import straightskeleton.Output.LoopNormal;
import straightskeleton.Skeleton;
import straightskeleton.debug.DebugDevice;
import straightskeleton.ui.Bar;
import utils.LContext;
import utils.ListDownLayout;
import utils.ui.SaveLoad;

/**
 * to do while waiting for partay to end:
 *
 * move/delete a set of plots
 * ensure plot direction is sensible
 * identify edges based on orientation of nearest street. (eg 4 profiles, auto assigned based on distance to nearest street-waypoint).
 *
 * @author twak
 */
public class AtlantisEditor extends javax.swing.JFrame {

    public static AtlantisEditor instance;
    public AtlantisPointEditor ape;
    public Atlantis at;
    public boolean shiftDown = false;
    List<Plot> selected = new ArrayList();
    private LContext<Bar> selectedBar;
    
    Preview preview;
        // what we're currently working on
    public Object threadKey;


    // this is required by a few classes... :(
    static CampSkeleton cs = new CampSkeleton(false);

    static
    {
        CampSkeleton.instance = cs;
    }

    /** Creates new form AtlantisEditor */
    public AtlantisEditor() {
        instance = this;
        initComponents();

        ButtonGroup paletteGroup = new ButtonGroup();

        modePanel.removeAll();
        modePanel.setLayout( new ListDownLayout() );

        new Thread()
        {

            @Override
            public void run()
            {
                preview = new Preview();
                preview.start();
            }
        }.start();

        SaveLoad saveLoad = new SaveLoad()
        {
            public void afterLoad()
            {
                // rebuild plot kd tree
                at.updateAll();
                setAtlantis( at );
            }
        };
        saveLoad.addSaveLoadMenuItems(jMenu1, "at", this);

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            public void eventDispatched(AWTEvent event) {
                if (event instanceof KeyEvent)
                {
                    KeyEvent ke = (KeyEvent)event;
                    if (ke.getKeyCode() == KeyEvent.VK_SHIFT)
                    {
                        shiftDown = ke.isShiftDown();
                    }
                }
            }
        }, -1);


        for (Mode tl : AtlantisPointEditor.Mode.values())
        {
                final Mode ft = tl;
                final JToggleButton tb = new JToggleButton( ft.name().toLowerCase().replace("_", " ") );
                tb.putClientProperty( this, ft );
                paletteGroup.add( tb );
                tb.addActionListener( new ActionListener()
                {

                    public void actionPerformed( ActionEvent e )
                    {
                        if ( tb.isSelected() )
                            setMode( ft );
                    }

                } );

                if (tl == Mode.PLOTS) //inital mode
                {
                    tb.doClick();
                }

            modePanel.add ( tb );
        }
    }

    public void setAtlantis (Atlantis at)
    {
        this.at = at;
        ape = new AtlantisPointEditor(this );
        editorFrame.removeAll();
        editorFrame.setLayout(new GridLayout(1,1) );
        editorFrame.add(ape);
        ape.setData( at );
        ape.revalidate();
        ape.repaint();
        setupProfiles();
        setMode( mode );
    }

    Mode mode;
    private void setMode( Mode mode )
    {
        this.mode = mode;
        if (ape != null)
            ape.setMode( mode );
    }

    private void setupProfiles()
    {
        DefaultListModel lm = new DefaultListModel();
        for (Profile p : at.profiles)
            lm.addElement( p );
        profileList.setModel( lm );
    }

    void plotsSelected(Set<Plot> lookupPlot) {
        if (!shiftDown)
            selected.clear();
        selected.addAll(lookupPlot);



        System.out.println( selected.size() +" selected" + lookupPlot.iterator().next().area);
    }

    void plotSelected(Plot get)
    {
        if (!shiftDown)
            selected.clear();

        selected.add(get);
        System.out.println( selected.size() +" selected");

    }

    void barSelected( LContext<Bar> ctx, Plot plot )
    {
        selectedBar = ctx;
        profileList.setSelectedValue( plot.profiles.get( ctx.get()), true);
    }

    private void editProfile( Profile profile )
    {
        ProfileUI ui = new ProfileUI( profile, selected.get( 0 ), null );
        profilePanel.removeAll();
        profilePanel.setLayout( new GridLayout( 1, 1 ) );
        profilePanel.add( ui );
        ui.revalidate();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        editorFrame = new javax.swing.JPanel();
        infoPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        newProfileButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        profileList = new javax.swing.JList();
        modePanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        vertexModeButton = new javax.swing.JButton();
        doorButton = new javax.swing.JButton();
        windowButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        profileButton = new javax.swing.JButton();
        dumpTo3D = new javax.swing.JButton();
        profilePanel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        parseMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        classifyMenuItem = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        findRoadsMenuItem = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));

        javax.swing.GroupLayout editorFrameLayout = new javax.swing.GroupLayout(editorFrame);
        editorFrame.setLayout(editorFrameLayout);
        editorFrameLayout.setHorizontalGroup(
            editorFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 368, Short.MAX_VALUE)
        );
        editorFrameLayout.setVerticalGroup(
            editorFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 460, Short.MAX_VALUE)
        );

        getContentPane().add(editorFrame);

        infoPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        infoPanel.setLayout(new java.awt.BorderLayout());

        newProfileButton.setText("+");
        newProfileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newProfileButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("profiles:");

        profileList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                profileListMouseClicked(evt);
            }
        });
        profileList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                profileListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(profileList);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(newProfileButton)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newProfileButton)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE)
                .addContainerGap())
        );

        infoPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout modePanelLayout = new javax.swing.GroupLayout(modePanel);
        modePanel.setLayout(modePanelLayout);
        modePanelLayout.setHorizontalGroup(
            modePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 366, Short.MAX_VALUE)
        );
        modePanelLayout.setVerticalGroup(
            modePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 216, Short.MAX_VALUE)
        );

        infoPanel.add(modePanel, java.awt.BorderLayout.NORTH);

        jPanel3.setLayout(new java.awt.GridLayout(0, 2));

        vertexModeButton.setText("profile vertex mode");
        vertexModeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vertexModeButtonActionPerformed(evt);
            }
        });
        jPanel3.add(vertexModeButton);

        doorButton.setText("profile door mode");
        doorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doorButtonActionPerformed(evt);
            }
        });
        jPanel3.add(doorButton);

        windowButton.setText("profile window mode");
        windowButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                windowButtonActionPerformed(evt);
            }
        });
        jPanel3.add(windowButton);

        jButton1.setText("profile add ground win");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton1);

        profileButton.setText("profile height mode");
        profileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                profileButtonActionPerformed(evt);
            }
        });
        jPanel3.add(profileButton);

        dumpTo3D.setText("3Derize");
        dumpTo3D.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dumpTo3DActionPerformed(evt);
            }
        });
        jPanel3.add(dumpTo3D);

        infoPanel.add(jPanel3, java.awt.BorderLayout.SOUTH);

        getContentPane().add(infoPanel);

        javax.swing.GroupLayout profilePanelLayout = new javax.swing.GroupLayout(profilePanel);
        profilePanel.setLayout(profilePanelLayout);
        profilePanelLayout.setHorizontalGroup(
            profilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 368, Short.MAX_VALUE)
        );
        profilePanelLayout.setVerticalGroup(
            profilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 460, Short.MAX_VALUE)
        );

        getContentPane().add(profilePanel);

        jMenu1.setText("File");

        parseMenuItem.setText("parse data file");
        parseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parseMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(parseMenuItem);

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText("merge loops");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        classifyMenuItem.setText("classify using Saturday");
        classifyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classifyMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(classifyMenuItem);

        jMenuItem8.setText("classify using 4prof");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem8);

        jMenuItem3.setText("select big plots");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuItem7.setText("select small plots");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem7);

        jMenuItem4.setText("select all plots");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem4);

        jMenuItem9.setText("deselect 1/3");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem9);

        findRoadsMenuItem.setText("find closest roads to plots");
        findRoadsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findRoadsMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(findRoadsMenuItem);

        jMenuItem2.setText("delete selected plots");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem6.setText("fix border directions");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem6);

        jMenuItem5.setText("all to obj");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem5);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed

        Plot only = selected.get(0);

        for (int i = 1; i < selected.size(); i++)
        {
            Plot p = selected.get(i);
            Bar.reverse(p.points);
            only.points.addLoopL(p.points);
            for (Map.Entry<Bar,Profile> entry : only.profiles.entrySet())
                only.profiles.put (entry.getKey(), entry.getValue());
            at.plots.remove(p);
        }



    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void newProfileButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_newProfileButtonActionPerformed
    {//GEN-HEADEREND:event_newProfileButtonActionPerformed
        if (selected.size() == 0) {
            JOptionPane.showMessageDialog(this, "no", "classify plots first!", 1);
            return;
        }

        String response = JOptionPane.showInputDialog(null,
                "Name?",
                "New profile name",
                JOptionPane.QUESTION_MESSAGE);

        Profile p = selected.get(0).createNewProfile(null);
        p.name = response;
        at.profiles.add(p);
        setupProfiles();
    }//GEN-LAST:event_newProfileButtonActionPerformed

    private void profileListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_profileListValueChanged
    {//GEN-HEADEREND:event_profileListValueChanged
        Object o = profileList.getSelectedValue();
        if (o == null)
        {

        }
        else
        {
            Profile p = (Profile)o;
            if (selectedBar != null)
            {
                for (Plot plot : selected)
                {
                    plot.profiles.put( selectedBar.get(), p);
                }
            }
            ape.repaint();
        }
    }//GEN-LAST:event_profileListValueChanged


        Output output;
    public void show( Plan plan, Output output, Skeleton threadKey )
    {
        this.threadKey = threadKey;
//        meshItem.setEnabled( true );
        this.output = output;
        if (preview != null) // might take a while...
        {
            preview.clear = true;
            preview.threadKey = threadKey;
            preview.display( output, true, true );

            Point2d pt = CampSkeleton.getMiddle( plan.points );
            preview.setViewStats( pt.x / Jme.scale, threadKey.height / Jme.scale, pt.y / Jme.scale, threadKey );
        }
    }

    private void dumpTo3DActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_dumpTo3DActionPerformed
    {//GEN-HEADEREND:event_dumpTo3DActionPerformed
        if (selected.size() == 0)
            return;

        Plot plot = selected.get( 0 );

        DebugDevice.reset();
        PlanSkeleton s = new PlanSkeleton( plot );
        s.skeleton();

        if ( s.output.faces != null )
        {
            output = s.output;
            show( plot, output, s );
        }

        // "this would be an eccumenical matter"
        if (s.pillarFactory != null) {
            s.pillarFactory.addTo(preview, s);
        }
        if (s.windowFactory != null) {
            s.windowFactory.addTo(preview, s);
        }
        if (s.meshFactory != null) {
            s.meshFactory.addTo(preview, s);
        }

//        for (Bar b : plot.profiles.keySet())
//            plan.profiles.put( b, plot.profiles.get)

    }//GEN-LAST:event_dumpTo3DActionPerformed

    private void profileListMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_profileListMouseClicked
    {//GEN-HEADEREND:event_profileListMouseClicked
        if (evt.getClickCount() == 2)
        {
            Object o = profileList.getSelectedValue();
            if (o != null)
            {
                editProfile((Profile)o);
            }
            else
                editProfile(null);
        }
    }//GEN-LAST:event_profileListMouseClicked

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        at.plots.removeAll(selected);
        at.updateAll();
        ape.updateLoops();
        ape.repaint();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void findRoadsMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_findRoadsMenuItemActionPerformed
    {//GEN-HEADEREND:event_findRoadsMenuItemActionPerformed
        at.findRoadsForPlots( at.plots );
    }//GEN-LAST:event_findRoadsMenuItemActionPerformed

    private void classifyMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_classifyMenuItemActionPerformed
    {//GEN-HEADEREND:event_classifyMenuItemActionPerformed
        SaturdayAlgo algo = new SaturdayAlgo();
//        new ProfileAlgo( algo.getProperties(), at.profiles, algo, selected );
        ape.repaint();
    }//GEN-LAST:event_classifyMenuItemActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem3ActionPerformed
    {//GEN-HEADEREND:event_jMenuItem3ActionPerformed
        if (at.plots.iterator().next().area == 0)
            {
                JOptionPane.showMessageDialog( this, "no", "classify plots first!", 1);
                return;
            }

        selected.clear();
        for (Plot p : at.plots)
        {
            if (p.area > 400)
                selected.add(p);
        }

        ape.repaint();
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void parseMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_parseMenuItemActionPerformed
    {//GEN-HEADEREND:event_parseMenuItemActionPerformed
//        setAtlantis( BigOlDataSet2int.floatAtlantis() );
        SVG2Int svg2Int = new SVG2Int();

        setAtlantis( svg2Int.get() );
    }//GEN-LAST:event_parseMenuItemActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        plotsSelected(new HashSet ( at.plots ) );
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void vertexModeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_vertexModeButtonActionPerformed
        cs.mode = Tool.Vertex;
        
    }//GEN-LAST:event_vertexModeButtonActionPerformed

    private void doorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doorButtonActionPerformed
        cs.mode = Tool.Anchor;
        cs.selectedTag = at.doorFeature;
    }//GEN-LAST:event_doorButtonActionPerformed

    private void windowButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_windowButtonActionPerformed
                cs.mode = Tool.Anchor;
        cs.selectedTag = at.windowFeature;
    }//GEN-LAST:event_windowButtonActionPerformed

    private void profileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profileButtonActionPerformed
        cs.mode = Tool.Anchor;
        cs.selectedTag = at.heightFeature;
    }//GEN-LAST:event_profileButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        cs.mode = Tool.Anchor;
        cs.selectedTag = at.groundWindowFeature;
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        JmeObjDump dump = new JmeObjDump();
        JmeObjDump doorsDump = new JmeObjDump();

        int i = 0;
        for (Plot p : selected) {

//            if (i > 1000) // delme!
//                break;
            try {
                System.out.println("on plot " + i + " of " + selected.size());
                i++;

                DebugDevice.reset();
                PlanSkeleton s = new PlanSkeleton(p);
                Output output = s.output;
                s.skeleton();

                Node solid = new Node();
                for (Face f : output.faces.values()) {
                    try {
                        JmeFace spatial = new JmeFace(f);
                        if (spatial.valid) {
                            solid.attachChild(spatial);
                        }
                        
                    } catch (Throwable e) {
                        if (f != null) {
                            System.err.println("point count is " + f.pointCount());
                        }

//                        e.printStackTrace();
//                        if (e.getCause() != null) {
//                            e.getCause().printStackTrace();
//                        }
                    }
                }

                for (LoopNormal ln : output.nonSkelFaces) {
                    try {
                        JmeLoopL spatial = new JmeLoopL(ln.loopl, ln.norm, true);
                        if (spatial.valid) {
                            solid.attachChild(spatial);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        if (e.getCause() != null) {
                            e.getCause().printStackTrace();
                        }
                    }
                }

                Node doors = new Node();
                if (s.windowFactory != null) {
                    s.windowFactory.addTo(doors);
                }
                if (s.meshFactory != null) {
                    s.meshFactory.addTo(doors);
                }

                if (doors != null)
                doorsDump.add(doors);
                dump.add(solid);
            } catch (Throwable th) {
                th.printStackTrace();
            }
//            break;
        }

        dump.allDone(new File("atlantis.obj"));
        doorsDump.allDone(new File("atlantis_doors.obj"));
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        int i = 0;
        for (Plot p : at.plots)
        {
            if (p.area < 0)
            {
//                System.out.println("That's wrong");
                Bar.reverse(p.points);
                i++;
                p.area = -p.area;
            }
        }
        System.out.println("fixed border directions on "+i);
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
                if (at.plots.iterator().next().area == 0)
            {
                JOptionPane.showMessageDialog( this, "no", "classify plots first!", 1);
                return;
            }

        selected.clear();
        for (Plot p : at.plots)
        {
            if (p.area < 50)
                selected.add(p);
        }

        ape.repaint();
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        FourSidesAlgo algo = new FourSidesAlgo();
//        new ProfileAlgo( algo.getProperties(), at.profiles, algo, selected );
        ape.repaint();
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        Set<Plot> nS = new HashSet();
        for (Plot p : selected)
        {
            if (Math.random() > 0.33)
                nS.add(p);
        }
        plotsSelected( nS);
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AtlantisEditor().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem classifyMenuItem;
    private javax.swing.JButton doorButton;
    private javax.swing.JButton dumpTo3D;
    private javax.swing.JPanel editorFrame;
    private javax.swing.JMenuItem findRoadsMenuItem;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel modePanel;
    private javax.swing.JButton newProfileButton;
    private javax.swing.JMenuItem parseMenuItem;
    private javax.swing.JButton profileButton;
    private javax.swing.JList profileList;
    private javax.swing.JPanel profilePanel;
    private javax.swing.JButton vertexModeButton;
    private javax.swing.JButton windowButton;
    // End of variables declaration//GEN-END:variables

//    void dragProfiles(Tuple2d offset) {
//        for (Plot p : selected)
//            for (Bar b : p.points.eIterator())
//                b.start.add(offset);
//    }
}
