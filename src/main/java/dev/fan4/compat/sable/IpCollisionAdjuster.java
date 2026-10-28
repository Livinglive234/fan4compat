package dev.fan4.compat.sable;

import com.bawnorton.mixinsquared.adjuster.tools.AdjustableAnnotationNode;
import com.bawnorton.mixinsquared.adjuster.tools.AdjustableRedirectNode;
import com.bawnorton.mixinsquared.api.MixinAnnotationAdjuster;
import dev.fan4.compat.shared.GameLog;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Immersive Portals and Sable both @Redirect the same call in Entity.move: the call to Entity.collide. A call can be redirected by one mixin only, so Mixin keeps
 * Sable's (higher priority) and skips Immersive Portals', and because every Immersive Portals injector is required, the skip stops Entity from loading and the game
 * crashes on start. Immersive Portals' redirect (its checks, then portal-aware collision or the plain collide) stays where it is, and Sable's moves into it: Sable's
 * redirect now replaces the plain collide call inside Immersive Portals' {@code redirectHandleCollisions}. Entity.move calls Immersive Portals' handler, which does its
 * checks and portal collision and, for the plain collide, runs Sable's handler (Sable's collision against its sub-levels, then the plain collide). Immersive Portals'
 * mixin has the lower priority, so it is applied first and Sable's redirect can name its handler. Nothing is cancelled and neither mod's code is copied; only the
 * {@code method} of Sable's annotation changes, through MixinSquared's annotation adjuster, and a TargetHandler marker is added to its handler.
 */
public final class IpCollisionAdjuster implements MixinAnnotationAdjuster {
    static final String IP_MIXIN="qouteall.imm_ptl.core.mixin.common.collision.MixinEntity",IP_HANDLER="redirectHandleCollisions";
    static final String SABLE_MIXIN="dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision.EntityMixin",SABLE_HANDLER="sable$collideRedirect";
    static final String TARGET_HANDLER="Lcom/bawnorton/mixinsquared/TargetHandler;",SELECTOR="@MixinSquared:Handler";

    /**
     * MixinSquared walks a handler's visible annotations (it keeps hold of that list object), calls the adjusters, then clears that same list and adds back what they
     * returned. So an adjuster can neither add an annotation to the list it is being called from (the walk would fail) nor return two annotations, and a replacement
     * list would miss the answers MixinSquared puts back. This view is the replacement: it reads and writes through to the original list and always shows one
     * TargetHandler marker after it, whatever MixinSquared clears and adds back.
     */
    static final class MarkedList extends AbstractList<AnnotationNode> implements RandomAccess {
        private final List<AnnotationNode> original;
        private final AnnotationNode marker;
        MarkedList(List<AnnotationNode> original,AnnotationNode marker){this.original=original;this.marker=marker;}
        private boolean has(){return original.stream().anyMatch(a->TARGET_HANDLER.equals(a.desc));}
        @Override public int size(){return original.size()+(has()?0:1);}
        @Override public AnnotationNode get(int index){return index<original.size()?original.get(index):marker;}
        @Override public AnnotationNode set(int index,AnnotationNode node){if(index<original.size())return original.set(index,node);throw new UnsupportedOperationException();}
        @Override public void add(int index,AnnotationNode node){original.add(Math.min(index,original.size()),node);modCount++;}
        @Override public AnnotationNode remove(int index){if(index<original.size()){modCount++;return original.remove(index);}throw new UnsupportedOperationException();}
        @Override public void clear(){original.clear();modCount++;}
        @Override public boolean addAll(Collection<? extends AnnotationNode> nodes){modCount++;return original.addAll(nodes);}
    }

    @Override
    public AdjustableAnnotationNode adjust(List<String> targetClassNames,String mixinClassName,MethodNode handler,AdjustableAnnotationNode annotation){
        if(!SABLE_MIXIN.equals(mixinClassName)||!SABLE_HANDLER.equals(handler.name)||!annotation.is(Redirect.class))return annotation;
        GameLog.info("Sable collision hook: moving Sable's "+handler.name+" redirect into Immersive Portals' "+IP_HANDLER+" (targets "+targetClassNames+")");
        if(!(handler.visibleAnnotations instanceof MarkedList)) {
            AnnotationNode target=new AnnotationNode(TARGET_HANDLER);
            target.values=new ArrayList<>(List.of("mixin",IP_MIXIN,"name",IP_HANDLER));
            handler.visibleAnnotations=new MarkedList(handler.visibleAnnotations==null?new ArrayList<>():handler.visibleAnnotations,target);
        }
        // Changed in place and returned as it came. MixinSquared renames a handler whose annotation came back as a different node, and Mixin's conform step then
        // fails on the new name (it looks the handler up in a table built from the original name), so the answer must be the node it passed in.
        return annotation.as(AdjustableRedirectNode.class).withMethod(methods->new ArrayList<>(List.of(SELECTOR)));
    }
}
