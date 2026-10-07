package dev.fan4.compat.soundphysics;

import dev.fan4.compat.shared.ShipPose.Point;

/** Bounds of blocks actually present in a published acoustic snapshot. */
public record AcousticBounds(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
    public AcousticBounds include(int x,int z) {return new AcousticBounds(Math.min(minX,x*16d),minY,Math.min(minZ,z*16d),Math.max(maxX,(x+1)*16d),maxY,Math.max(maxZ,(z+1)*16d));}
    public double[] interval(Point from,Point to) {
        double enter=0,exit=1;double[] a={from.x(),from.y(),from.z()},b={to.x(),to.y(),to.z()},lo={minX,minY,minZ},hi={maxX,maxY,maxZ};
        for(int i=0;i<3;i++) {
            if(!Double.isFinite(a[i])||!Double.isFinite(b[i]))return null;
            double delta=b[i]-a[i];
            if(Math.abs(delta)<1e-12){if(a[i]<lo[i]||a[i]>hi[i])return null;continue;}
            double first=(lo[i]-a[i])/delta,last=(hi[i]-a[i])/delta;
            enter=Math.max(enter,Math.min(first,last));exit=Math.min(exit,Math.max(first,last));if(enter>exit)return null;
        }
        return new double[]{enter,exit};
    }
    public static Point lerp(Point a,Point b,double t){return new Point(a.x()+(b.x()-a.x())*t,a.y()+(b.y()-a.y())*t,a.z()+(b.z()-a.z())*t);}
}
