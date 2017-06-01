/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.twak.siteplan.campskeleton;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.twak.camp.Corner;
import org.twak.camp.Skeleton;
import org.twak.camp.SkeletonCapUpdate;
import org.twak.utils.DHash;
import org.twak.utils.Loop;
import org.twak.utils.LoopL;
import org.twak.utils.SetCorrespondence;

/**
 *
 * @author twak
 */
public class CapFeatureFactory extends FeatureFactory
{
    public CapFeatureFactory()
    {
        super (new CapFeature());
    }

    @Override
    public FactoryEvent createFactoryEvent( double height, Global g, int valency )
    {
        return new CapEvent( height, g, valency );
    }

    public class CapEvent extends FactoryEvent
    {
        public CapEvent( double height, Global global, int valency )
        {
            super( height, global, valency );
        }

        @Override
        public boolean process( Skeleton skel )
        {
            SkeletonCapUpdate capUpdate = new SkeletonCapUpdate(skel);
            LoopL<Corner> flatTop = capUpdate.getCap(height);

            LoopL<Point3d> pts = new LoopL();
            for (Loop<Corner> cLoop : flatTop)
            {
                Loop<Point3d> loop = new Loop();
                pts.add(loop);
                for ( Corner c : cLoop)
                    loop.append( new Point3d( c.x, c.y, height) );
            }

            skel.output.addNonSkeletonOutputFace2(pts , new Vector3d(0,0,1) );

            capUpdate.update(new LoopL(), new SetCorrespondence<Corner, Corner>(), new DHash<Corner, Corner>());
            skel.qu.clearFaceEvents();
            skel.qu.clearOtherEvents();
            return true;

        }
    }

}
