package net.caffeinemc.mods.sodium.client.render.chunk;

import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;

/** Stand-in for Sodium 0.8's ChunkRenderer (the render call has a sixth argument there); never packaged. */
public interface ChunkRenderer {
    void render(ChunkRenderMatrices matrices,CommandList commandList,ChunkRenderListIterable renderLists,TerrainRenderPass pass,CameraTransform camera,boolean sorted);
}
