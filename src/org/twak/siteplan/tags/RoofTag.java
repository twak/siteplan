/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.twak.siteplan.tags;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;

import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.jme.Preview;
import org.twak.straightskeleton.Output;
import org.twak.straightskeleton.Output.Face;
import org.twak.utils.WeakListener.Changed;

/**
 *
 * @author twak
 */
public class RoofTag extends PlanTag
{
    public double width = 0.5, height = 0.5;
    public double jitter = 1;
    public boolean stagger = true;

    public RoofTag()
    {
        super ("tile");
    }

    @Override
    public String toString() {
        return "tile ("+width+","+height+")";
    }

    @Override
    public JComponent getToolInterface(Changed rebuildFeatureList, Plan plan) {
        return new RoofUI(this, rebuildFeatureList);
    }

    @Override
    public void postProcess(Output output, Preview preview, Object threadKey) {

        Set<Face> allFaces = new HashSet();
        for (Face f : output.faces.values()) {
            if (f.profile.contains(this)) {
                allFaces.add(f);
            }
        }

//        new Tiler( preview, allFaces, threadKey, this );
    }


}
