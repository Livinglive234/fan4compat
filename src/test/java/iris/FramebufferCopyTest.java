import dev.fan4.compat.iris.FramebufferCopyCompat;
import java.util.*;
public final class FramebufferCopyTest {
    static final class GL implements FramebufferCopyCompat.Backend {
        int read=17,draw=18,next=100,mask,copies;boolean scissor=true,complete=true,fail;List<Integer> deleted=new ArrayList<>(),textures=new ArrayList<>(),attachments=new ArrayList<>();
        public boolean imageCopy(){return false;}
        public int integer(int key){return key==36010?read:draw;}
        public boolean scissor(){return scissor;}
        public void scissor(boolean enabled){scissor=enabled;}
        public int generate(){return ++next;}
        public void bind(int target,int fbo){if(target==36008)read=fbo;else draw=fbo;}
        public void attach(int target,int attachment,int texture){attachments.add(attachment);textures.add(texture);}
        public void buffer(int target,int buffer){check(buffer==0||buffer==36064,"buffer selection");}
        public boolean complete(int target){return complete;}
        public void blit(int width,int height,int bits){check(!scissor&&read==101&&draw==102,"copy scope");check(width==800&&height==600,"native extent");mask=bits;copies++;if(fail)throw new IllegalStateException("copy failed");}
        public void delete(int fbo){deleted.add(fbo);}
    }
    public static void main(String[] args){
        check(!FramebufferCopyCompat.fallback(new Object(),new Object(),false),"native-supported depth path skips fallback and framebuffer access");
        check(!FramebufferCopyCompat.fallback(new Object(),new Object(),true),"native-supported color path skips fallback");
        for(boolean color:new boolean[]{false,true})for(boolean stencil:new boolean[]{false,true}){
            GL gl=new GL();FramebufferCopyCompat.copy(gl,51,52,800,600,color,stencil);
            restored(gl);check(gl.mask==(color?16384:stencil?1280:256),"color or packed depth/stencil copy");check(gl.textures.equals(List.of(51,52)),"native texture IDs");check(gl.attachments.equals(List.of(color?36064:stencil?33306:36096,color?36064:stencil?33306:36096)),"matching attachments");
        }
        GL gl=new GL();gl.fail=true;try{FramebufferCopyCompat.copy(gl,51,52,800,600,false,false);throw new AssertionError();}catch(IllegalStateException expected){restored(gl);}
        gl=new GL();gl.complete=false;try{FramebufferCopyCompat.copy(gl,51,52,800,600,false,false);throw new AssertionError();}catch(IllegalStateException expected){restored(gl);check(gl.copies==0,"incomplete FBO never copied");}
        gl=new GL();gl.scissor=false;FramebufferCopyCompat.copy(gl,51,52,800,600,true,false);check(!gl.scissor&&gl.read==17&&gl.draw==18,"disabled scissor preserved");
        System.out.println("PASS: GL3 color/depth-stencil blit, native extent/texture attachments, framebuffer/scissor restoration and temporary resource cleanup on success/failure (test backend)");
    }
    static void restored(GL gl){check(gl.read==17&&gl.draw==18&&gl.scissor&&gl.deleted.equals(List.of(101,102)),"state and resources restored");}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
