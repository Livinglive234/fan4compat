package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import java.util.List;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;

/** Stand-in for Sodium's SortedRenderLists: the sections it was built from; never packaged. */
public class SortedRenderLists implements ChunkRenderListIterable {
    public final List<RenderSection> sections;
    public final Viewport viewport;
    public SortedRenderLists(List<RenderSection> sections,Viewport viewport){this.sections=sections;this.viewport=viewport;}
}
