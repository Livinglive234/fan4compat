package io.wispforest.accessories.pond;
import java.util.Collection;
public interface DroppedStacksExtension {
    Collection<Object> toBeDroppedStacks();
    void addToBeDroppedStacks(Collection<Object> stacks);
}
