package dev.fan4.compat.iris;

import dev.fan4.compat.shared.WeakIdentityMap;
import java.util.*;
import java.util.function.BiConsumer;
import static dev.fan4.compat.shared.CompatCalls.*;

/** DH owns its depth attachment; Iris main-depth refresh must not overwrite it. */
public final class DhDepthAttachmentCompat {
    public static final class Ownership {
        private final Map<Object,Boolean> owned=Collections.synchronizedMap(new WeakIdentityMap<>());
        public void remember(Object framebuffer){if(framebuffer!=null)owned.put(framebuffer,true);}
        public void refresh(Object framebuffer,int texture,BiConsumer<Object,Integer> nativeAttach){if(!owned.containsKey(framebuffer))nativeAttach.accept(framebuffer,texture);}
    }
    private static final Ownership OWNERSHIP=new Ownership();
    private DhDepthAttachmentCompat(){}
    public static void remember(Object framebuffer){OWNERSHIP.remember(framebuffer);}
    public static void refresh(Object framebuffer,int texture){OWNERSHIP.refresh(framebuffer,texture,(f,t)->call(f,"addDepthAttachment",t));}
}
