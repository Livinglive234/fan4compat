package dev.fan4.compat.iris;

import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Apple's depth-only float texture must blit into another depth-only float texture. */
public final class SeparateStencilCompat {
    public interface Backend {
        int integer(int key);void framebuffer(int target,int id);void texture(int id);void renderbuffer(int id);
        int createStencil();void stencilStorage(int width,int height);void depthStorage(int format,int width,int height);
        void attachTexture(int point,int texture);void attachStencil(int renderbuffer);boolean complete();void deleteStencil(int id);
    }
    public record Resource(int texture,int width,int height,int stencil) {}
    public static final class Resources {
        private final Map<Object,Resource> resources=new IdentityHashMap<>();
        public Resource get(Object target){return resources.get(target);}
        public void release(Backend gl,Object target){Resource old=resources.remove(target);if(old!=null&&old.stencil()!=0)gl.deleteStencil(old.stencil());}
        public boolean prepare(Backend gl,Object target,int framebuffer,int texture,int width,int height,int originalFormat) {
            Resource old=resources.get(target);
            if(old!=null&&old.texture()==texture&&old.width()==width&&old.height()==height)return old.stencil()!=0;
            release(gl,target);
            int read=gl.integer(36010),draw=gl.integer(36006),tex=gl.integer(32873),rb=gl.integer(36007),stencil=0;
            boolean committed=false,changed=false;
            try {
                gl.framebuffer(36160,framebuffer);gl.texture(texture);
                stencil=gl.createStencil();gl.renderbuffer(stencil);gl.stencilStorage(width,height);
                changed=true;gl.attachTexture(33306,0);gl.depthStorage(36012,width,height);
                gl.attachTexture(36096,texture);gl.attachStencil(stencil);
                if(!gl.complete())return false;
                resources.put(target,new Resource(texture,width,height,stencil));committed=true;return true;
            } finally {
                try {
                    if(!committed) {
                        if(changed){gl.attachStencil(0);gl.attachTexture(36096,0);gl.depthStorage(originalFormat,width,height);gl.attachTexture(33306,texture);}
                        if(stencil!=0)gl.deleteStencil(stencil);
                        // Do not retry a rejected layout every frame; native resize/delete clears this.
                        resources.put(target,new Resource(texture,width,height,0));
                    }
                } finally {
                    gl.texture(tex);gl.renderbuffer(rb);gl.framebuffer(36008,read);gl.framebuffer(36009,draw);
                }
            }
        }
    }
    private static final Resources RESOURCES=new Resources();
    private static final NativeBackend GL=new NativeBackend();
    private static final System.Logger LOGGER=System.getLogger("Fan4Compat PortalDepth");
    private static boolean reported;
    private SeparateStencilCompat() {}
    public static void enable(Object target,boolean enabled) {
        Object nativeCompat=type("qouteall.imm_ptl.core.compat.IPPortingLibCompat");
        call(nativeCompat,"setIsStencilEnabled",target,enabled);
        if(!enabled)return;
        String vendor=(String)exact("org.lwjgl.opengl.GL11","glGetString",new String[]{"int"},7936);
        if(vendor==null||!vendor.toLowerCase(Locale.ROOT).contains("apple"))return;
        Object main=call(call(type("net.minecraft.class_310"),"method_1551"),"method_1522");
        int mainTexture=(Integer)call(main,"method_30278"),texture=(Integer)call(target,"method_30278");
        int width=(Integer)field(target,"field_1480"),height=(Integer)field(target,"field_1477");
        int source=textureFormat(mainTexture);
        if(source!=36012) {
            if(RESOURCES.get(target)!=null)call(target,"method_1234",width,height,field(type("net.minecraft.class_310"),"field_1703"));
            return;
        }
        Resource existing=RESOURCES.get(target);
        if(existing!=null&&existing.texture()==texture&&existing.width()==width&&existing.height()==height)return;
        int original=textureFormat(texture);
        if(original!=36013&&original!=35056)return;
        boolean ready=RESOURCES.prepare(GL,target,(Integer)field(target,"field_1476"),texture,width,height,original);
        if(!reported){reported=true;LOGGER.log(System.Logger.Level.INFO,"[Fan4Compat PortalDepth] Separate float-depth texture and stencil renderbuffer "+(ready?"enabled":"rejected; restored native packed attachment"));}
    }
    public static void release(Object target){RESOURCES.release(GL,target);}
    private static int textureFormat(int texture) {
        if(texture<=0)return 0;
        int binding=GL.integer(32873);
        try {
            GL.texture(texture);int format=texParameter(4099);
            return format==6402?DepthFormatCompat.resolvedFormat(format,texParameter(34890),texParameter(35862)):format;
        } finally {GL.texture(binding);}
    }
    private static int texParameter(int key){return (Integer)exact("org.lwjgl.opengl.GL11","glGetTexLevelParameteri",new String[]{"int","int","int"},3553,0,key);}
    private static final class NativeBackend implements Backend {
        private static final String G11="org.lwjgl.opengl.GL11",G30="org.lwjgl.opengl.GL30";
        public int integer(int key){return (Integer)exact(G11,"glGetInteger",new String[]{"int"},key);}
        public void framebuffer(int target,int id){exact(G30,"glBindFramebuffer",new String[]{"int","int"},target,id);}
        public void texture(int id){exact(G11,"glBindTexture",new String[]{"int","int"},3553,id);}
        public void renderbuffer(int id){exact(G30,"glBindRenderbuffer",new String[]{"int","int"},36161,id);}
        public int createStencil(){return (Integer)exact(G30,"glGenRenderbuffers",new String[]{});}
        public void stencilStorage(int width,int height){exact(G30,"glRenderbufferStorage",new String[]{"int","int","int","int"},36161,36168,width,height);}
        public void depthStorage(int format,int width,int height){
            boolean packed=format==36013||format==35056;
            exact(G11,"glTexImage2D",new String[]{"int","int","int","int","int","int","int","int","java.nio.ByteBuffer"},3553,0,format,width,height,0,packed?34041:6402,format==35056?34042:packed?36269:5126,null);
        }
        public void attachTexture(int point,int texture){exact(G30,"glFramebufferTexture2D",new String[]{"int","int","int","int","int"},36160,point,3553,texture,0);}
        public void attachStencil(int rb){exact(G30,"glFramebufferRenderbuffer",new String[]{"int","int","int","int"},36160,36128,36161,rb);}
        public boolean complete(){return (Integer)exact(G30,"glCheckFramebufferStatus",new String[]{"int"},36160)==36053;}
        public void deleteStencil(int id){exact(G30,"glDeleteRenderbuffers",new String[]{"int"},id);}
    }
}
