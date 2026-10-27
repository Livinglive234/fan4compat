package net.caffeinemc.mods.sodium.client.render.chunk;

import org.joml.Matrix4fc;

/** Stand-in for Sodium's ChunkRenderMatrices (a record there); never packaged. */
public final class ChunkRenderMatrices {
    private final Matrix4fc projection,modelView;
    public ChunkRenderMatrices(Matrix4fc projection,Matrix4fc modelView){this.projection=projection;this.modelView=modelView;}
    public Matrix4fc projection(){return projection;}
    public Matrix4fc modelView(){return modelView;}
}
