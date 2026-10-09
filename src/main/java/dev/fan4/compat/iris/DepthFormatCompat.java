package dev.fan4.compat.iris;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Match Apple's main depth representation before IP allocates recursive stencil buffers. */
public final class DepthFormatCompat {
    private DepthFormatCompat() {}
    public static boolean nvidia(boolean original,Object renderer) {
        String vendor=(String)exact("org.lwjgl.opengl.GL11","glGetString",new String[]{"int"},7936);
        if(vendor==null||!vendor.toLowerCase(java.util.Locale.ROOT).contains("apple"))return original;
        Object client=call(type("net.minecraft.class_310"),"method_1551"),framebuffer=call(client,"method_1522");
        int texture=(Integer)call(framebuffer,"method_30278");
        if(texture<=0)return original;
        int binding=(Integer)exact("org.lwjgl.opengl.GL11","glGetInteger",new String[]{"int"},32873);
        int format;
        try {
            exact("org.lwjgl.opengl.GL11","glBindTexture",new String[]{"int","int"},3553,texture);
            format=(Integer)exact("org.lwjgl.opengl.GL11","glGetTexLevelParameteri",new String[]{"int","int","int"},3553,0,4099);
            if(format==6402) {
                int bits=(Integer)exact("org.lwjgl.opengl.GL11","glGetTexLevelParameteri",new String[]{"int","int","int"},3553,0,34890);
                int componentType=(Integer)exact("org.lwjgl.opengl.GL11","glGetTexLevelParameteri",new String[]{"int","int","int"},3553,0,35862);
                format=resolvedFormat(format,bits,componentType);
            }
        } finally {exact("org.lwjgl.opengl.GL11","glBindTexture",new String[]{"int","int"},3553,binding);}
        boolean selected=nvidiaForFormat(original,format),separated=!selected;
        if((format==33190||format==35056||format==36012||format==36013)
            && (Boolean)field(type("qouteall.imm_ptl.core.IPCGlobal"),"useSeparatedStencilFormat")!=separated) {
            // Existing layer buffers otherwise retain their old format at the same resolution.
            Object[] layers=(Object[])field(renderer,"deferredFbs");
            int expected=(Integer)call(type("qouteall.imm_ptl.core.render.context_management.PortalRendering"),"getMaxPortalLayer")+1;
            // Native prepare already destroys/recreates the array if its layer count changes.
            if(layers.length==expected)for(Object layer:layers) {
                Object fb=field(layer,"fb");
                if(fb!=null){call(fb,"method_1238");writeField(layer,"fb",null);}
            }
        }
        return selected;
    }
    public static int resolvedFormat(int format,int depthBits,int componentType) {
        if(format!=6402)return format;
        if(depthBits==24)return 33190;
        if(depthBits==32&&componentType==5126)return 36012;
        return format;
    }
    // IP negates this result to choose DEPTH32F_STENCIL8 instead of DEPTH24_STENCIL8.
    public static boolean nvidiaForFormat(boolean original,int format) {
        return switch(format){case 33190,35056 -> true;case 36012,36013 -> false;default -> original;};
    }
}
