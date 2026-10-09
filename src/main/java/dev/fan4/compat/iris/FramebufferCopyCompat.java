package dev.fan4.compat.iris;

import static dev.fan4.compat.shared.CompatCalls.*;

/** GL 3 framebuffer blits for contexts without glCopyImageSubData (notably macOS). */
public final class FramebufferCopyCompat {
    private static final int READ=36008,DRAW=36009,DEPTH_STENCIL=33306,COLOR=36064;
    private FramebufferCopyCompat() {}
    public interface Backend {
        boolean imageCopy();
        int integer(int key);
        boolean scissor();
        void scissor(boolean enabled);
        int generate();
        void bind(int target,int framebuffer);
        void attach(int target,int attachment,int texture);
        void buffer(int target,int attachment);
        boolean complete(int target);
        void blit(int width,int height,int mask);
        void delete(int framebuffer);
    }
    public static boolean fallback(Object source,Object destination,boolean color) {
        Backend gl=new NativeBackend();
        if(gl.imageCopy())return false;
        int texture=(Integer)call(source,color?"method_30277":"method_30278");
        int target=(Integer)call(destination,color?"method_30277":"method_30278");
        boolean stencil=!color&&(Boolean)exact("qouteall.imm_ptl.core.compat.IPPortingLibCompat","getIsStencilEnabled",new String[]{"net.minecraft.class_276"},source);
        copy(gl,texture,target,(Integer)field(source,"field_1482"),(Integer)field(source,"field_1481"),color,stencil);
        return true;
    }
    public static void copy(Backend gl,int source,int destination,int width,int height,boolean color,boolean stencil) {
        int oldRead=gl.integer(36010),oldDraw=gl.integer(36006),read=0,draw=0;
        boolean scissor=gl.scissor();
        try {
            read=gl.generate();draw=gl.generate();
            gl.bind(READ,read);gl.attach(READ,color?COLOR:stencil?DEPTH_STENCIL:36096,source);gl.buffer(READ,color?COLOR:0);
            gl.bind(DRAW,draw);gl.attach(DRAW,color?COLOR:stencil?DEPTH_STENCIL:36096,destination);gl.buffer(DRAW,color?COLOR:0);
            if(!gl.complete(READ)||!gl.complete(DRAW))throw new IllegalStateException("Incomplete portal copy framebuffer");
            if(scissor)gl.scissor(false);
            gl.blit(width,height,color?16384:stencil?1280:256);
        } finally {
            gl.bind(READ,oldRead);gl.bind(DRAW,oldDraw);
            if(scissor)gl.scissor(true);
            if(read!=0)gl.delete(read);if(draw!=0)gl.delete(draw);
        }
    }
    private static final class NativeBackend implements Backend {
        private static final String GL11="org.lwjgl.opengl.GL11",GL30="org.lwjgl.opengl.GL30",STATE="com.mojang.blaze3d.platform.GlStateManager";
        public boolean imageCopy(){return ((Number)field(call(type("org.lwjgl.opengl.GL"),"getCapabilities"),"glCopyImageSubData")).longValue()!=0;}
        public int integer(int key){return (Integer)exact(GL11,"glGetInteger",new String[]{"int"},key);}
        public boolean scissor(){return (Boolean)exact(GL11,"glIsEnabled",new String[]{"int"},3089);}
        public void scissor(boolean enabled){exact(STATE,enabled?"_enableScissorTest":"_disableScissorTest",new String[]{});}
        public int generate(){return (Integer)exact(GL30,"glGenFramebuffers",new String[]{});}
        public void bind(int target,int framebuffer){exact(STATE,"_glBindFramebuffer",new String[]{"int","int"},target,framebuffer);}
        public void attach(int target,int attachment,int texture){exact(GL30,"glFramebufferTexture2D",new String[]{"int","int","int","int","int"},target,attachment,3553,texture,0);}
        public void buffer(int target,int attachment){exact(GL11,target==READ?"glReadBuffer":"glDrawBuffer",new String[]{"int"},attachment);}
        public boolean complete(int target){return (Integer)exact(GL30,"glCheckFramebufferStatus",new String[]{"int"},target)==36053;}
        public void blit(int width,int height,int mask){exact(GL30,"glBlitFramebuffer",new String[]{"int","int","int","int","int","int","int","int","int","int"},0,0,width,height,0,0,width,height,mask,9728);}
        public void delete(int framebuffer){exact(GL30,"glDeleteFramebuffers",new String[]{"int"},framebuffer);}
    }
}
