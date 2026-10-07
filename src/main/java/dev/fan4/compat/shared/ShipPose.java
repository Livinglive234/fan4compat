package dev.fan4.compat.shared;

/** Immutable affine transform; Minecraft and optional mod classes stay out of this math. */
public record ShipPose(double[] matrix) {
    public ShipPose { if (matrix.length != 16) throw new IllegalArgumentException("4x4 matrix required"); matrix = matrix.clone(); }
    @Override public double[] matrix() { return matrix.clone(); }
    public record Point(double x, double y, double z) {
        public Point normalized() { double n = length(); if (n < 1e-12) throw new IllegalArgumentException("Degenerate ship transform"); return new Point(x/n,y/n,z/n); }
        public double length() { return Math.sqrt(x*x+y*y+z*z); }
    }
    public Point position(Point p) { return transform(p, 1); }
    public Point direction(Point p) { return transform(p, 0); }
    private Point transform(Point p, double w) {
        return new Point(matrix[0]*p.x+matrix[4]*p.y+matrix[8]*p.z+matrix[12]*w,
            matrix[1]*p.x+matrix[5]*p.y+matrix[9]*p.z+matrix[13]*w,
            matrix[2]*p.x+matrix[6]*p.y+matrix[10]*p.z+matrix[14]*w);
    }
    public static Point door(Point block, Point facing) { return new Point(block.x+.5+facing.x*.5,block.y+1,block.z+.5+facing.z*.5); }
    public static String cardinal(Point p) { return Math.abs(p.x) > Math.abs(p.z) ? (p.x > 0 ? "east" : "west") : (p.z > 0 ? "south" : "north"); }
}
