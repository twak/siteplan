
package camp.anchors;

import camp.anchors.AnchorHauler.AnchorHeightEvent;
import campskeleton.CampSkeleton;
import campskeleton.Plan;
import javax.swing.JComponent;
import javax.vecmath.Vector2d;
import straightskeleton.Corner;
import straightskeleton.Edge;
import straightskeleton.ui.Bar;
import straightskeleton.ui.Marker;
import org.twak.utils.LContext;
import org.twak.utils.Loop;
import org.twak.utils.Loopable;
import org.twak.utils.WeakListener;
import org.twak.utils.WeakListener.Changed;

/**
 * Hacky "feature" to animate the location of an anchor to demonstrate the effect of
 * animating a natural step around the exterior of an solid
 *
 * todo: should extends AnimateAnchorCircularShip?
 *
 * @author twak
 */
public class AnimateAnchorShip extends Ship
{
    double speed = 5;
    
    @Override
    public JComponent getToolInterface( WeakListener refreshAnchors, Changed refreshFeatureListListener, Plan plan )
    {
        return new AnimateAnchorShipUI(this);
    }

    @Override
    protected Instance createInstance()
    {
        return new AAInstance();
    }

    public class AAInstance extends Instance
    {
        // used to transfer marker between process and update, below!
        transient Marker planMarker, profileMarker;

        @Override
        public LContext<Corner> process( Anchor anchor, LContext<Corner> toEdit, Marker planMarker, Marker profileMarker, Edge edge, AnchorHeightEvent hauler, Corner oldLeadingCorner )
        {
            this.planMarker = planMarker;
            this.profileMarker = profileMarker;
            
            return toEdit;
        }

        @Override
        protected void update( int frame, int delta, Plan plan)
        {
            if (planMarker == null)
                return;
            // we always trail by 1 frame, but this can't really be helped.
            Bar bar = planMarker.bar;
            Loop<Bar> loop = null;

            for (Loop<Bar> lloop : CampSkeleton.instance.plan.points)
                for (Bar b : lloop)
                    if (b == bar)
                        loop = lloop;

            if ( loop == null )
                return; // didn't find marker on main plan. give up.

            Loopable<Bar> able = null;
            
            // locate plan marker
            for (Loopable<Bar> lb : loop.loopableIterator())
            {
                if (lb.get() == bar)
                    able = lb;
            }

            if (able == null)
                return;


            // anchors may have re-genereated. grab the most recent location
            for (Marker m : able.get().mould.getAnchorsForEditing( able.get(), able.get().start, able.get().end ))
            {
                if (m.generator == planMarker.generator)
                    planMarker = m;
            }
            
            // remove plan marker from old location
            Object generator = able.get().mould.remove( planMarker );

            // move plan marker - find correct bar by subtracting from desired distance

            double leftInBar;
            
            if ( delta > 0 )
            {
                double distance = speed * delta;

                leftInBar = planMarker.distance( able.get().end );
                able.get().distance( planMarker );
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
                double distance = speed * -delta;

                leftInBar = planMarker.distance( able.get().start );
                able.get().distance( planMarker );
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

            planMarker.bar = able.get();

            // calculate position as distance from end
            Vector2d dir = new Vector2d ( able.get().end );
            dir.sub( able.get().start );
            dir.normalize();
            dir.scale( -leftInBar );
            dir.add( able.get().end );

            planMarker.set( dir );
            
            able.get().mould.create( planMarker, generator );
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
