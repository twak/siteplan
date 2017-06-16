package org.twak.siteplan.anim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.twak.camp.ui.Bar;
import org.twak.camp.ui.Marker;
import org.twak.siteplan.anchors.Anchor;
import org.twak.siteplan.anchors.NaturalStepShip;
import org.twak.siteplan.anchors.Ship;
import org.twak.siteplan.campskeleton.Global;
import org.twak.siteplan.campskeleton.Plan;
import org.twak.siteplan.campskeleton.Profile;
import org.twak.utils.Mathz;
import org.twak.utils.collections.Loop;

/**
 *
 * todo:
 *
 * fix natural steps
 * build nice profiles
 * add a door
 * add butress on each side
 *
 * @author twak
 */
public class PioneerPlan extends Plan
{
    Tween xTween, yTween, zTween, aTween, tTween, bTween;
    public List<Profile> lp = new ArrayList();
    NaturalStepShip ns; // users better not delete this!
    Map<Profile, Marker> profileMarkers = new HashMap();

    public PioneerPlan()
    {
        super ();
        xTween = new TwoTween(new RampTween(-100, 100), new RampTween(100, -100));
        yTween = new TwoTween(new RampTween(0, 100), new RampTween(100, 0));
        zTween = new TwoTween(new RampTween(20, 220), new RampTween(220, 20));
        aTween = new TwoTween(new RampTween(60, 180), new RampTween(180, 60));

        tTween = new TwoTween(new RampTween(-180, -140), new RampTween(-140, -180));
        bTween = new TwoTween(new RampTween(-80, -120), new RampTween(-120, -80));

        profiles.clear();

        for (int i = 0; i < 6; i++)
        {
            Profile p = new Profile (100);
            Loop<Bar> loop = p.points.get(0);
            loop.append (new Bar (loop.getFirst().end, new Point2d (50, -140)));
            p.getGlobalProfile(root).chainStarts.add(loop);
            profiles.put(null, p);
            lp.add(p);
        }

        ships.add(ns = new NaturalStepShip(this));
    }

    @Override
    public void update(int frame, int delta)
    {
        int length = 200;

        double xt = getValue(xTween, 0, length, frame);
        double yt = getValue(yTween, 0, length, frame);
        double zt = getValue(zTween, 0, length, frame);
        double at = getValue(aTween, 0, length, frame);
        double bt = getValue(bTween, 0, length, frame);
        double tt = getValue(tTween, 0, length, frame);
        
        double[][] vals = new double[][] {
            {0, -250},
            {at+120, -250},
            {at+120, -110},
            {at+zt+0.1, -110},
            {at+zt+0.1, -10},
            {at+120, -10},
            {at+120, yt},
            {0, yt},
            {0, -80},
            {xt+0.1, bt},
            {xt+0.1, tt},
            {0, -180}
        };

        int left = 0, right = 1, top = 2, bottom = 3, gSides = 4, gFront = 5;

        Profile[] profs = new Profile[]{
            lp.get(top),
            lp.get(right),
            lp.get(gSides),
            lp.get(gFront),
            lp.get(gSides),
            lp.get(right),
            lp.get(bottom),
            lp.get(left),
            lp.get(gSides),
            lp.get(gFront),
            lp.get(gSides),
            lp.get(left)
        };

        setFrom (vals, profs);

        // clear all instances of the natural step
        ns.clearInstances();
        profileMarkers.clear();

        //        int left = 0, right = 1, top = 2, bottom = 3, gSides = 4, gFront = 5;


        double[] profAnchorOffsets = new double[] { 0, 0, -10,-50, -60, -30};

        for ( int i = 0; i < 6; i++ )
            clearAndCreateProfileAnchors(lp.get(i), profAnchorOffsets[i]);


        double[] nsSpacings = new double[] {50,50,40,60,40,0,50,60,0,30,0,0};
        int i = 0;
        for (Bar b : points.eIterator())
        {
            if (nsSpacings[i] > 0)
                createDormerWindowsFor(b, nsSpacings[i] );
            i++;
        }

        super.update(frame, delta);
    }

    public void clearAndCreateProfileAnchors(Profile p, double d) {
        for (Global g : globals) {
            for (Loop<Bar> lb : p.getGlobalProfile(g).chainStarts) {
                for (Bar b : lb) {
                    b.mould.clear();
                }
            }
        }

        double dist = d + 111; // offset height for ns
        for (Bar b : p.points.get(0)) {
            double delta = b.start.y - b.end.y;
            if (dist < delta) {
                Vector2d d2 = new Vector2d(b.end);
                d2.sub(b.start);
                d2.scale( dist/delta );
                d2.add(b.start);

                Marker m = new Marker();
                m.set(d2);
                m.bar = b;

                b.mould.create(m, null);

                profileMarkers.put(p, m);
                break;
            } else {
                dist -= delta;
            }

        }
    }

    public void createDormerWindowsFor(Bar b, double desiredSpacing )
    {
        b.mould.clear();
        
        double length = b.length();
        int count = (int)(length /desiredSpacing);
        double delta = length / (count+1.);
        Vector2d v = new Vector2d (b.end);
        v.sub(b.start);
        v.normalize();
        v.scale(delta);
        
        for (int i = 1; i <= count; i++)
        {
            Vector2d loc = new Vector2d (v);
            loc.scale(i);
            loc.add(b.start);
            Marker m = new Marker();
            m.set(loc);
            m.bar = b;
            
            b.mould.create(m, null);

            Marker profM =  profileMarkers.get( profiles.get(b));


            if (profM != null) {
                Ship.Instance instance = ns.newInstance();
                instance.anchors = new Anchor[]{new Anchor(m.generator, profM.generator)};
            }
        }
    }

    public double getValue (Tween t, int min, int max, int frame)
    {
        return t.get( Mathz.clamp( (frame - min)/((double)max - min), 0., 1. ));
    }

    public static abstract class Tween
    {
        public abstract double get (double frac);
    }

    public static class SetIntevealTween extends Tween
    {
        Tween[] tweens;
        int erval;
        public SetIntevealTween( Tween ... tweens)
        {
            this.tweens = tweens;
        }

        @Override
        public double get(double frac)
        {
            double l = 1/tweens.length;
            int t = (int) (frac * tweens.length);
            return tweens[t].get( (frac - (t*l)) / l);
        }
    }

    public static class TwoTween extends Tween
    {
        public Tween a,b;
        public double swap;

        public TwoTween (Tween a, Tween b)
        {
            this(a, b, 0.5);
        }
        
        public TwoTween (Tween a, Tween b, double swap )
        {
            this.a = a;
            this.b = b;
            this.swap = swap;
        }

        @Override
        public double get(double frac)
        {
            if (frac < swap)
                return a.get(frac / swap);
            else
                return b.get( ( (frac-swap) / (swap)));
        }
    }

    public static class RampTween extends Tween
    {
        double start, end;
        public RampTween(double start, double end)
        {
            this.start= start;
            this.end = end;
        }
        
        @Override
        public double get(double frac)
        {
            return frac * (end-start) + start;
        }
    }

    @Override
    public List<Profile> findProfiles() {
        List<Profile> out = super.findProfiles();
        out.addAll (lp);
        return out;
    }


}
