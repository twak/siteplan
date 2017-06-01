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
import org.twak.utils.Loop;
import org.twak.utils.MUtils;

/**
 * For the Sigg demo videos!
 * @author twak
 */
public class OverhangPlan extends Plan
{
//    Tween xTween, yTween, zTween, aTween, tTween, bTween;
    public List<Profile> lp = new ArrayList();
    NaturalStepShip ns; // users better not delete this!
    Map<Profile, Marker> profileMarkers = new HashMap();

    public OverhangPlan()
    {
        super ();

        for (int i = 0; i < 2; i++)
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



        TankTrack ltt = new TankTrack(15, 60, 115, -200.01, true),
                rtt = new TankTrack(15, 60, 185, -200.01, false);

        double frac = frame % length /(double)length;

        Point2d l = ltt.getPoint(frac), r = rtt.getPoint(frac);
        
        double[][] vals = new double[][] {
            {300, -170},
            {300, -40},
            {0,-40},
            {0, -170},
            {100, -170},
            {l.x, l.y},
            {r.x, r.y},
            {200, -170},
        };

        int sides = 0, front = 1;

        Profile[] profs = new Profile[]{
            lp.get(sides),
            lp.get(front),
            lp.get(sides),
            lp.get(front),
            lp.get(front),
            lp.get(front),
            lp.get(front),
            lp.get(front)
        };

        setFrom (vals, profs);

        // clear all instances of the natural step
        ns.clearInstances();
        profileMarkers.clear();

//        int left = 0, right = 1, top = 2, bottom = 3, gSides = 4, gFront = 5;
//        double[] profAnchorOffsets = new double[] { 0, 0, -10,-50, -60, -30};
//        for ( int i = 0; i < 6; i++ )
//            clearAndCreateProfileAnchors(lp.get(i), profAnchorOffsets[i]);
//        double[] nsSpacings = new double[] {50,50,40,60,40,0,50,60,0,30,0,0};
//        int i = 0;
//        for (Bar b : points.eIterator())
//        {
//            if (nsSpacings[i] > 0)
//                createDormerWindowsFor(b, nsSpacings[i] );
//            i++;
//        }

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
        return t.get( MUtils.clamp( (frame - min)/((double)max - min), 0., 1. ));
    }

    public static abstract class Tween
    {
        public abstract double get (double frac);
    }

    public static class TankTrack
    {
        double radius, height, x, y;
        boolean forwards;
        
        public TankTrack ( double radius, double height, double x, double y, boolean forwards ) {
            this.radius = radius;
            this.height = height;
            this.x = x;
            this.y = y;
            this.forwards = forwards;
        }
        
        public Point2d getPoint(double frac) {
            Point2d res = getPoint_(frac);
            if (!forwards)
                res.x = x - (res.x - x );
            return res;
        }
        public Point2d getPoint_(double frac) {

            double halfCirc = Math.PI * Math.abs ( radius );
            double totalLength = 2 * Math.PI * Math.abs ( radius ) + 2 * height;

            if (frac < height / totalLength) {
                double e1 = frac / (height/totalLength);
                return new Point2d (x-radius, y + e1 * height);
            } else if (frac < (height + halfCirc) / totalLength) {
                frac -= (height/totalLength);

                double f = frac / (halfCirc/totalLength);
                return new Point2d ( x - Math.cos(Math.PI*f) * Math.abs(radius),
                        y + height + Math.sin(Math.PI*f) * Math.abs(radius));

            } else if (frac < (2 * height + halfCirc) / totalLength) {
                frac -= (height + halfCirc) / totalLength;
                double f= frac / (height/totalLength);
                return new Point2d (x+radius, y+height - f *height);
            } else // < 1
            {
                frac -= (height*2 + halfCirc) / totalLength;
                double f = frac / (halfCirc/totalLength);
                return new Point2d ( x + Math.cos(Math.PI*-f) * Math.abs(radius),
                        y - Math.sin(Math.PI*f) * Math.abs(radius));
            }
        }

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