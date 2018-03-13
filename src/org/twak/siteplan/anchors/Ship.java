package org.twak.siteplan.anchors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;

import org.twak.siteplan.campskeleton.Siteplan;
import org.twak.camp.Corner;
import org.twak.camp.Edge;
import org.twak.camp.ui.Bar;
import org.twak.camp.ui.Marker;
import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.siteplan.jme.Preview;
import org.twak.utils.LContext;
import org.twak.utils.WeakListener;

/**
 * A ship is an operation on a set of anchor'd sites. So might be one NatrualStep shape, or a particualar mesh.
 * Each ship has anumber of instances which are different applications of the same rule.
 * They come from ship factories
 *
 * @author twak
 */
public abstract class Ship
{
    public String[] anchorNames = new String[] {"default"};
    private List<Instance> instances = new ArrayList();
    public String className; // what we were created from

    // interface to go in the wee panel
    public abstract JComponent getToolInterface(WeakListener refreshAnchors, WeakListener.Changed refreshFeatureListListener, Plan plan);
    // how many anchors would you like?
    protected abstract Instance createInstance();

    public int countAnchors()
    {
        return anchorNames.length;
    }

    public Instance newInstance()
    {
        Instance out = createInstance();
        instances.add(out);
        return out;
    }

    public void removeInstance(int instanceNo) {
        instances.remove(instanceNo);
    }

    public void clearInstances() {
        instances.clear();
    }

    public List<Instance> getInstances()
    {
        return instances;
    }

    // the descriptor's name in the ui (natural step)
    public abstract String getFeatureName();

    @Override
    public String toString()
    {
        return getFeatureName();
    }

    protected Anchor createNewAnchor()
    {
        // allows subclasses to create a profileanchor instaed
        return Siteplan.instance.plan.createAnchor( Plan.AnchorType.PROFILE_PLAN, null, null );
    }

    public void update( int frame, int delta, Plan plan )
    {
        for (Instance i : instances)
            i.update(frame, delta, plan );
    }

    public abstract class Instance
    {
        public Anchor[] anchors;
        // instance's name (pillar no 3)
        public String name = "another "+getFeatureName();

        public Instance()
        {
            int i = 0;
            anchors = new Anchor[ anchorNames.length ];
            for (String s : anchorNames)
            {
                Anchor a = createNewAnchor();
                a.name = s;
                anchors[i++] = a;
            }
        }

        public boolean isComplete()
        {
            return anchors.length == anchorNames.length;
        }

        public int matches (Object planGen, Object profileGen)
        {
            for (int i = 0; i < anchors.length; i++)
                if (anchors[i].matches (planGen, profileGen))
                    return i;
            return -1;
        }

        /**
         * Defines the update that happen as a ship's isntance matches an anchor
         *
         * a process may call-back to offset-skeleton to getOffset().registerProfile, above to register offset distances for any machines in first
         * @param planMarker  the position on the plan at this height (eg - the location of the bottom,left of the window
         * @param profileMarker the profile marker
         * @param edge the edge that this segment came from (for normals etc).
         * @param hauler callback for editing the profile
         * @param oldLeadingCorner the corner of the unedited loop that currently starts the edge
         * @param planAnchor plan anchor we're reporting to (eg window)
         * @param first  segment to edit (wall segment at the moment)
         * @return first the start of the remainder of the segment you've just editied. or toEdit if nothing has changed
         *
         */
        public abstract LContext<Corner> process(
                Anchor anchor,
                LContext<Corner> toEdit,
                Marker planMarker,
                Marker profileMarker,
                Edge edge,
                AnchorHauler.AnchorHeightEvent hauler,
                Corner oldLeadingCorner);

        public void addMeshes( Preview preview , Object key)
        {
            // default is to do nothing - override for post-process
        }

        // when an option change changes the number of anchors, this method gets called on new instances to let them keep/discard data as they see fit.
        public void upgrade(List<String> names)
        {
            Anchor[] oldA = anchors;
            anchors = new Anchor[names.size()];
            
            for (int i = 0; i < oldA.length; i++)
            {
                if (i < names.size())
                    anchors[i] = oldA[i];
            }

            for (int i = oldA.length; i < names.size(); i++)
            {
                anchors[i] = createNewAnchor();
                anchors[i].name = names.get(i);
            }
        }

        protected void update( int frame, int delta, Plan plan)
        {
            // some animated anchors may choose to update themselves here
        }

        public void addUsedBars(Set<Bar> out) {
            // if the system wants to know about all possible bars, it calls this
        }
    }

    public static Ship createAShip( String className )
    {
        try {
            Ship ship = (Ship) Class.forName(className).newInstance();
            ship.className = className;
            ship.newInstance(); // start with a default instance
            return ship;
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void setNewAnchorNames(List<String> names)
    {
        // ignore root bone (with name 'x')
        Iterator<String> it = names.iterator();
        while (it.hasNext())
            if (it.next().compareToIgnoreCase("x") == 0)
                it.remove();

        anchorNames = new String[names.size()];
        for (int i = 0; i < names.size(); i++)
            anchorNames[i] = names.get(i);

        for (Instance i : getInstances())
                i.upgrade(names);

    }

    /**
     * Called before each skeleton is run, allowing things like the profile cache to be cleared
     */
    public void clearCache()
    {
    }

    /**
     * A ship may insert markers (for example on a natural step), if they
     * are not counted in Plan.countMarkerMatches (eg: in plan.profiles),
     * they should be counted here.
     */
    public int countMarkerMatches (Object generator)
    {
        return 0;
    }

    public abstract Ship clone(Plan plan);


    protected void setupClone (Ship ship)
    {
        ship.anchorNames = Arrays.copyOf(anchorNames, anchorNames.length);
        ship.newInstance();
        ship.className = className;
    }

    /**
     * The user needs to knwo which profiles are in use. Each ship should add the profiles that they reference here.
     * @param vProfiles
     */
    public void addUsedProfiles(List<Profile> vProfiles) {
        // override
    }
}
