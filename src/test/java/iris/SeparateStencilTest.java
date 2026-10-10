import dev.fan4.compat.iris.SeparateStencilCompat;
import java.util.*;

public final class SeparateStencilTest {
    static final class GL implements SeparateStencilCompat.Backend {
        int read=17,draw=18,texture=19,renderbuffer=20,format=36013,next=100,allocated,deleted;
        int depthAttachment=45,stencilAttachment=45;boolean accepted=true,throwDepth;
        final Set<Integer> live=new HashSet<>();
        public int integer(int key){return switch(key){case 36010->read;case 36006->draw;case 32873->texture;case 36007->renderbuffer;default->throw new AssertionError();};}
        public void framebuffer(int target,int id){if(target==36160){read=id;draw=id;}else if(target==36008)read=id;else if(target==36009)draw=id;else throw new AssertionError();}
        public void texture(int id){texture=id;}public void renderbuffer(int id){renderbuffer=id;}
        public int createStencil(){allocated++;live.add(++next);return next;}
        public void stencilStorage(int w,int h){check(w==854&&h==480,"stencil dimensions match copy");}
        public void depthStorage(int f,int w,int h){if(throwDepth&&f==36012)throw new IllegalStateException("allocation failed");format=f;}
        public void attachTexture(int point,int id){if(point==33306){depthAttachment=id;stencilAttachment=id;}else if(point==36096)depthAttachment=id;else throw new AssertionError();}
        public void attachStencil(int id){stencilAttachment=id;}
        public boolean complete(){check(format==36012&&depthAttachment==45&&stencilAttachment==next,"depth and stencil have distinct matching-size storage");return accepted;}
        public void deleteStencil(int id){check(live.remove(id),"no leaked or double-deleted renderbuffer");deleted++;}
    }
    public static void main(String[] args) {
        var resources=new SeparateStencilCompat.Resources();GL gl=new GL();Object target=new Object();
        check(resources.prepare(gl,target,22,45,854,480,36013),"supported layout commits");restored(gl);
        check(gl.format==36012&&gl.stencilAttachment!=gl.depthAttachment&&gl.live.size()==1,"depth-only float texture plus stencil storage");
        check(resources.prepare(gl,target,22,45,854,480,36013)&&gl.allocated==1,"per-frame reuse avoids allocation");
        resources.release(gl,target);check(gl.live.isEmpty()&&resources.get(target)==null,"native deletion releases owned stencil");resources.release(gl,target);
        gl=new GL();gl.accepted=false;resources=new SeparateStencilCompat.Resources();
        check(!resources.prepare(gl,target,22,45,854,480,36013),"driver rejection retains native path");restored(gl);
        check(gl.format==36013&&gl.depthAttachment==45&&gl.stencilAttachment==45&&gl.live.isEmpty(),"rejection rolls back packed texture/attachments and releases resource");
        check(!resources.prepare(gl,target,22,45,854,480,36013)&&gl.allocated==1,"rejected layout not retried every frame");
        resources.release(gl,target);gl.accepted=true;check(resources.prepare(gl,target,22,45,854,480,36013),"native resize clears rejection and permits retry");resources.release(gl,target);
        gl=new GL();gl.throwDepth=true;resources=new SeparateStencilCompat.Resources();
        try{resources.prepare(gl,target,22,45,854,480,36013);throw new AssertionError();}catch(IllegalStateException expected){}
        restored(gl);check(gl.format==36013&&gl.depthAttachment==45&&gl.stencilAttachment==45&&gl.live.isEmpty(),"allocation exception rolls back and restores bindings");
        System.out.println("PASS: matching depth/separate stencil allocation, reuse, delete/resize cleanup, driver rejection rollback and binding restoration");
    }
    static void restored(GL gl){check(gl.read==17&&gl.draw==18&&gl.texture==19&&gl.renderbuffer==20,"all GL bindings restored");}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
