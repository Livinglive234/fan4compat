package io.wispforest.accessories.menu;
import net.minecraft.class_1799;
import static dev.fan4.compat.shared.CompatCalls.*;
/** Fixture for native menu validation/setter delegation, not packaged in the addon. */
public final class AccessoriesInternalSlot {
    final Object container,contents;final int index;final boolean cosmetic;
    public AccessoriesInternalSlot(Object container,boolean cosmetic,int index,int x,int y) {
        this.container=container;this.cosmetic=cosmetic;this.index=index;contents=call(container,cosmetic?"getCosmeticAccessories":"getAccessories");
    }
    public boolean method_7680(class_1799 stack){return (boolean)call(container,"allowed",cosmetic);}
    public int method_7676(class_1799 stack){return 1;}
    public void method_7673(class_1799 stack){call(contents,"method_5447",index,stack);}
}
