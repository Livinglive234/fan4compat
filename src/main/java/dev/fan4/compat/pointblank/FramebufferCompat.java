package dev.fan4.compat.pointblank;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Preserve the caller's framebuffer bindings around Point Blank's lazy stencil resize. */
public final class FramebufferCompat {
    public interface Backend {int integer(int key);void bind(int target,int framebuffer);}
    public static void preserve(Backend gl,int oldTarget,java.util.function.IntSupplier currentTarget,Runnable enable) {
        int read=gl.integer(36010),draw=gl.integer(36006);
        try{enable.run();}
        finally {
            // Native resize deletes/recreates the target; never rebind its deleted old ID.
            int replacement=Math.max(0,currentTarget.getAsInt());
            gl.bind(36008,read==oldTarget?replacement:read);
            gl.bind(36009,draw==oldTarget?replacement:draw);
        }
    }
    private static final Backend GL=new Backend() {
        public int integer(int key){return (Integer)exact("org.lwjgl.opengl.GL11","glGetInteger",new String[]{"int"},key);}
        public void bind(int target,int framebuffer){exact("com.mojang.blaze3d.platform.GlStateManager","_glBindFramebuffer",new String[]{"int","int"},target,framebuffer);}
    };
    private FramebufferCompat() {}
    public static void enable(Object framebuffer) {
        int original=(Integer)field(framebuffer,"field_1476");
        preserve(GL,original,()->(Integer)field(framebuffer,"field_1476"),()->call(framebuffer,"enablePointblankStencil"));
    }
}
