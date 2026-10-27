package dev.fan4.compat.sodium;

import static dev.fan4.compat.shared.CompatCalls.field;
import dev.fan4.compat.shared.FailureLatch;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.TaskQueueType;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.OcclusionSectionCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_310;
import net.minecraft.class_638;
import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.internal.world.VsiClientShipWorld;
import org.valkyrienskies.mod.common.VSClientGameUtils;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

/**
 * Valkyrien Skies draws a ship through Sodium by giving each loaded ship its own render lists: the sections of the ship's chunks, collected the same way Sodium
 * collects the world's, drawn again with the ship's transform. VS's hooks for this were written for Sodium 0.6 and do not fit Sodium 0.8 (the section collector, the
 * type of the rebuild queues and the render call all changed), so the generated mixin on RenderSectionManager hands these four steps to this class. It is compiled
 * against compile-only stand-ins for the Sodium, Valkyrien Skies and Minecraft classes it calls, and the real-jar check verifies every call it makes. One failure turns
 * ship drawing off for the session (ships stop being drawn) and logs a warning.
 */
public final class ShipSections {
    /** Sodium's private section lookup by section coordinates, exposed by the generated mixin. */
    public interface SectionLookup {Object fan4$section(int x,int y,int z);}

    /** Ship render lists by render section manager, then by ship; read through VS's RenderSectionManagerDuck. */
    private static final Map<Object,WeakHashMap<ClientShip,SortedRenderLists>> LISTS=new WeakHashMap<>();
    private static final FailureLatch LATCH=new FailureLatch("Sodium ship rendering");
    private static final Object[] NO_LISTS=new Object[0];

    private ShipSections() {}

    public static WeakHashMap<ClientShip,SortedRenderLists> lists(Object manager){return LISTS.computeIfAbsent(manager,key->new WeakHashMap<>());}
    /** A snapshot of the ship lists, in the order the map holds them. */
    public static Object[] shipLists(Object manager){
        WeakHashMap<ClientShip,SortedRenderLists> ships=LISTS.get(manager);
        return ships==null?NO_LISTS:ships.values().toArray();
    }
    public static void clear(Object manager){
        WeakHashMap<ClientShip,SortedRenderLists> ships=LISTS.get(manager);
        if(ships!=null)ships.clear();
    }

    /**
     * After Sodium has built the world's render lists (createTerrainRenderList): builds a list for every loaded ship from the ship's chunk sections, adds the ships'
     * pending rebuilds to the manager's queues and returns whether the graph must be built again, which is Sodium's own answer or a ship section waiting on a running job.
     * The queue types the ship collectors need are the ones Sodium just gave its own collector.
     */
    public static boolean afterCreate(SectionLookup lookup,Object levelObject,Object taskListsObject,Object worldCollector,Object viewportObject,int frame,boolean original,Object manager){
        if(LATCH.tripped()||worldCollector==null)return original;
        try {
            class_638 level=(class_638)levelObject;Viewport viewport=(Viewport)viewportObject;
            @SuppressWarnings("unchecked") Map<TaskQueueType,ArrayDeque<RenderSection>> taskLists=(Map<TaskQueueType,ArrayDeque<RenderSection>>)taskListsObject;
            TaskQueueType rebuild=(TaskQueueType)field(worldCollector,"importantRebuildQueueType"),sort=(TaskQueueType)field(worldCollector,"importantSortQueueType");
            VsiClientShipWorld world=VSGameUtilsKt.getShipObjectWorld(class_310.method_1551());
            WeakHashMap<ClientShip,SortedRenderLists> out=lists(manager);
            boolean revisit=original;
            for(ClientShip ship:world.getLoadedShips()) {
                OcclusionSectionCollector collector=new OcclusionSectionCollector(frame,rebuild,sort);
                ship.getActiveChunksSet().forEach((x,z)->collect(lookup,level,collector,x,z));
                out.put(ship,collector.createRenderLists(viewport));
                for(Map.Entry<TaskQueueType,ArrayDeque<RenderSection>> entry:collector.getTaskLists().entrySet())taskLists.get(entry.getKey()).addAll(entry.getValue());
                revisit|=collector.needsRevisitForPendingUpdates();
            }
            return revisit;
        } catch(Throwable failure) {
            LATCH.trip(failure);
            return original;
        }
    }
    /** Visits every non-empty section of one chunk column of a ship. */
    private static void collect(SectionLookup lookup,class_638 level,OcclusionSectionCollector collector,int x,int z){
        class_2818 chunk=level.method_8497(x,z);
        int min=level.method_32891(),max=level.method_31597();
        for(int y=min;y<max;y++) {
            class_2826 section=chunk.method_38259(y-min);
            if(section.method_38292())continue;
            Object renderSection=lookup.fan4$section(x,y,z);
            if(renderSection!=null)collector.visit((RenderSection)renderSection);
        }
    }
    /**
     * Draws each ship's lists in Sodium's renderLayer, with the ship's transform on the model-view matrix and the ship's position as the camera, as VS did for Sodium 0.6.
     * {@code sorted} is Sodium's own flag for translucency sorting, the sixth argument of the render call.
     */
    public static void renderShips(Object manager,Object chunkRendererObject,Object matricesObject,Object commandListObject,Object passObject,double camX,double camY,double camZ,boolean sorted){
        if(LATCH.tripped())return;
        WeakHashMap<ClientShip,SortedRenderLists> ships=LISTS.get(manager);
        if(ships==null||ships.isEmpty())return;
        try {
            ChunkRenderer renderer=(ChunkRenderer)chunkRendererObject;ChunkRenderMatrices matrices=(ChunkRenderMatrices)matricesObject;
            CommandList commandList=(CommandList)commandListObject;TerrainRenderPass pass=(TerrainRenderPass)passObject;
            for(Map.Entry<ClientShip,SortedRenderLists> entry:ships.entrySet()) {
                ClientShip ship=entry.getKey();
                Matrix4f modelView=new Matrix4f(matrices.modelView());
                ShipTransform transform=ship.getRenderTransform();
                Vector3dc center=transform.getPositionInShip();
                VSClientGameUtils.transformRenderWithShip(transform,modelView,center.x(),center.y(),center.z(),camX,camY,camZ);
                renderer.render(new ChunkRenderMatrices(matrices.projection(),modelView),commandList,entry.getValue(),pass,new CameraTransform(center.x(),center.y(),center.z()),sorted);
                commandList.close();
            }
        } catch(Throwable failure) {
            LATCH.trip(failure);
        }
    }

    /** For tests: clears the failure latch and all stored lists. */
    public static void reset(){LATCH.reset();LISTS.clear();}
}
