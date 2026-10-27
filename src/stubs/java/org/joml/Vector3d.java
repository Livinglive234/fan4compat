package org.joml;

/** Stand-in for JOML's Vector3d; never packaged. */
public class Vector3d implements Vector3dc {
    private final double x,y,z;
    public Vector3d(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
    public double x(){return x;}
    public double y(){return y;}
    public double z(){return z;}
}
