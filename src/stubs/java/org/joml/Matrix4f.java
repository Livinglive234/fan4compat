package org.joml;

/** Stand-in for JOML's Matrix4f: remembers the matrix it was copied from; never packaged. */
public class Matrix4f implements Matrix4fc {
    public final Matrix4fc source;
    public Matrix4f(Matrix4fc source){this.source=source;}
}
