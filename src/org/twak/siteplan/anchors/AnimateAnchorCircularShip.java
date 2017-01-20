
package org.twak.siteplan.anchors;

import java.util.Arrays;
import javax.swing.JComponent;
import javax.vecmath.Vector2d;

import org.twak.siteplan.anchors.AnchorHauler.AnchorHeightEvent;
import org.twak.siteplan.campskeleton.CampSkeleton;
import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.straightskeleton.Corner;
import org.twak.straightskeleton.Edge;
import org.twak.straightskeleton.ui.Bar;
import org.twak.straightskeleton.ui.Marker;
import org.twak.utils.LContext;
import org.twak.utils.LoopL.LoopLoopable;
import org.twak.utils.LoopL.LoopLoopable;
import org.twak.utils.Loopable;
import org.twak.utils.WeakListener;
import org.twak.utils.WeakListener.Changed;

/**
 * Hacky "feature" to animate the location of an anchor to demonstrate the effect of
 * animating a natural step around the exterior of an solid
 * 
 * @author twak
 */
public class AnimateAnchorCircularShip extends Ship
{
    double speed = 5;
    double radius = 20;

    public AnimateAnchorCircularShip() {
        setNewAnchorNames( Arrays.asList("center", "to move") );
    }

    @Override
    public JComponent getToolInterface( WeakListener refreshAnchors, Changed refreshFeatureListListener, Plan plan )
    {
        return new AnimateAnchorCircularShipUI(this);
    }

    @Override
    protected Instance createInstance()
    {
        return new AAInstance();
    }

    public class AAInstance extends Instance
    {
        // used to transfer marker between process and update, below!
        transient Marker planC, profileC, planM, profileM;

        @Override
        public LContext<Corner> process(
                Anchor anchor,
                LContext<Corner> toEdit,
                Marker planMarker,
                Marker profileMarker,
                Edge edge,
                AnchorHeightEvent hauler,
                Corner oldLeadingCorner )
        {
            for (int i = 0; i < anchors.length; i++) {
                if (anchors[i].matches(anchor.getPlanGen(), anchor.getProfileGen())) {
                    if (i == 0) {
                        planC = planMarker;
                        profileC = profileMarker;
                    }
                    else {
                        planM = planMarker;
                        profileM = profileMarker;
                    }

                    return toEdit;
                }
            }
            return toEdit;
        }

        @Override
        protected void update( int frame, int delta, Plan plan)
        {
            if (planC == null || profileC == null || profileM == null || planM == null)
                return;

//            double  angle = frame * speed / 200 ,
//                    xOffset = Math.cos( angle ) * radius,
//                    yOffset = Math.sin ( angle ) * radius;


//            setPlan ( planC, xOffset, planM );
//            setProfile ( profileC, yOffset, profileM, plan );
            setPlan ( planC, 0, planM );
//            setProfile ( profileC, 0.1 * 280 - 33.1, profileM, plan );
            setProfile ( profileC, 0.1 * frame - 33.1, profileM, plan );
        }

        private void setPlan(Marker planC, double distance, Marker planM) {
            
            // we always trail by 1 frame, but this can't really be helped.
            Bar bar = planC.bar;
//            Loop<Bar> loop = null;
//            Loopable<Bar> able = null;

            LoopLoopable<Bar>
                cAble = CampSkeleton.instance.plan.points.find( bar ),
                mAble = CampSkeleton.instance.plan.points.find( planM.bar );

            if ( cAble == null || mAble == null )
                return; // didn't find marker on main plan. give up.

            // remove plan marker from old location
            Object generator = planM.bar.mould.remove( planM );
            
            // anchors may have re-genereated. grab the most recent location
            planC = refresh (planC, cAble );
//            planM = refresh (planM, mAble);

//            Object generator = mAble.loopable.get().mould.remove( planM );

            // move plan marker - find correct bar by subtracting from desired distance

            double leftInBar;

            Loopable<Bar> able = cAble.loopable;

            if ( distance > 0 )
            {
                leftInBar = planC.distance( able.get().end );

                do
                    if ( leftInBar > distance )
                    {
                        leftInBar -= distance;
                        break;
                    }
                    else
                    {
                        distance -= leftInBar;
                        able = able.getNext();
                        leftInBar = able.get().length();
                    }
                while ( true );
            }
            else
            {
                distance = -distance;
                leftInBar = planC.distance( able.get().start );

                do
                    if ( leftInBar > distance )
                    {
                        leftInBar -= distance;
                        leftInBar = able.get().length() - leftInBar;
                        break;
                    }
                    else
                    {
                        distance -= leftInBar;
                        able = able.getPrev();
                        leftInBar = able.get().length();
                    }
                while ( true );
            }

            planM.bar = able.get();

            // calculate position as distance from end
            Vector2d dir = new Vector2d (planM.bar.end );
            dir.sub( planM.bar.start );
            dir.normalize();
            dir.scale( -leftInBar );
            dir.add( planM.bar.end );

            planM.set( dir );

            planM.bar.mould.create( planM, generator );
        }

        private LoopLoopable<Bar> findInProfiles (Plan plan, Bar bar){
            for (Profile p : plan.findProfiles())
            {
                 LoopLoopable<Bar> out = p.points.find(bar);
                 if (out != null)
                     return out;
            }
            return null;
        }

        private void setProfile(Marker profileC, double distance, Marker profileM, Plan plan ) {

            // we always trail by 1 frame, but this can't really be helped.
            LoopLoopable<Bar>
                cAble = findInProfiles(plan, profileC.bar ),
                mAble = findInProfiles(plan, profileM.bar );

            if ( cAble == null || mAble == null )
                return; // didn't find marker on main plan. give up.

            // remove plan marker from old location
            Object generator = profileM.bar.mould.remove( profileM );

            // anchors may have re-genereated. grab the most recent location
            profileC = refresh (profileC, cAble );
//            planM = refresh (planM, mAble);

//            Object generator = mAble.loopable.get().mould.remove( planM );

            // move plan marker - find correct bar by subtracting from desired distance

            double leftInBar;

            Loopable<Bar> able = cAble.loopable;

            if ( distance > 0 )
            {
                leftInBar =  profileC.y - able.get().end.y;

                do
                    if ( leftInBar > distance )
                    {
                        leftInBar -= distance;
                        double frac = leftInBar / (able.get().start.y - able.get().end.y);

                        leftInBar = frac * able.get().length();
                        break;
                    }
                    else
                    {
                        distance -= leftInBar;
                        able = able.getNext();
                        leftInBar = (able.get().start.y - able.get().end.y);
                    }
                while ( true );
            }
            else
            {
                distance = -distance;
                leftInBar = able.get().start.y - profileC.y;//.distance( able.get().start );

                do
                    if ( leftInBar > distance )
                    {
                        leftInBar -= distance;
                        double frac = leftInBar / (able.get().start.y - able.get().end.y);

                        leftInBar = (1-frac) * able.get().length();
                        break;
                    }
                    else
                    {
                        distance -= leftInBar;
                        able = able.getPrev();
                        leftInBar = able.get().start.y - able.get().end.y;//length();
                    }
                while ( true );
            }

            profileM.bar = able.get();

            // calculate position as distance from end
            Vector2d dir = new Vector2d (profileM.bar.end );
            dir.sub( profileM.bar.start );
            dir.normalize();
            dir.scale( -leftInBar );
            dir.add( profileM.bar.end );

            profileM.set( dir );

            profileM.bar.mould.create( profileM, generator );
        }

        private Marker refresh(Marker planC, LoopLoopable<Bar> cAble) {
            for (Marker m : cAble.loopable.get().mould.getAnchorsForEditing(
                    cAble.loopable.get(),
                    cAble.loopable.get().start,
                    cAble.loopable.get().end ))
            {
                if (m.generator == planC.generator)
                    return m;
            }
            return null;
        }
    }

    @Override
    public String getFeatureName()
    {
        return "animate anchors";
    }

    @Override
    public Ship clone( Plan plan )
    {
        throw new UnsupportedOperationException( "Not supported (evar)" );
    }

}
