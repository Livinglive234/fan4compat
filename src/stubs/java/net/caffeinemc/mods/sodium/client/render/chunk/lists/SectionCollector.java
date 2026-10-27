package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.TaskQueueType;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;

/** Stand-in for Sodium 0.8's SectionCollector (private fields named as there; createRenderLists is an interface default there); never packaged. */
public abstract class SectionCollector {
    private final TaskQueueType importantRebuildQueueType;
    private final TaskQueueType importantSortQueueType;
    public final List<RenderSection> visited=new ArrayList<>();
    public final EnumMap<TaskQueueType,ArrayDeque<RenderSection>> tasks=new EnumMap<>(TaskQueueType.class);
    /** For tests: every collector reports that it needs another pass. */
    public static boolean revisitAll;
    protected SectionCollector(int frame,TaskQueueType rebuild,TaskQueueType sort) {
        this.importantRebuildQueueType=rebuild;this.importantSortQueueType=sort;
        for(TaskQueueType type:TaskQueueType.values())tasks.put(type,new ArrayDeque<>());
    }
    public void visit(RenderSection section){visited.add(section);if(section.pending)tasks.get(importantRebuildQueueType).add(section);}
    public Map<TaskQueueType,ArrayDeque<RenderSection>> getTaskLists(){return tasks;}
    public boolean needsRevisitForPendingUpdates(){return revisitAll;}
    public SortedRenderLists createRenderLists(Viewport viewport){return new SortedRenderLists(new ArrayList<>(visited),viewport);}
}
