package net.caffeinemc.mods.sodium.client.render.chunk;

/** Stand-in for Sodium's RenderSection: a position and whether it has a rebuild pending; never packaged. */
public class RenderSection {
    public final int x,y,z;
    public boolean pending;
    public RenderSection(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
}
