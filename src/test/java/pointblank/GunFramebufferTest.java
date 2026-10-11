import dev.fan4.compat.pointblank.FramebufferCompat;

public final class GunFramebufferTest {
    static class GL implements FramebufferCompat.Backend {
        int read,draw;GL(int read,int draw){this.read=read;this.draw=draw;}
        public int integer(int key){return key==36010?read:draw;}
        public void bind(int target,int framebuffer){if(target==36008)read=framebuffer;else draw=framebuffer;}
    }
    public static void main(String[] args) {
        GL gl=new GL(1,1);final GL first=gl;
        FramebufferCompat.preserve(gl,1,()->2,()->{first.read=0;first.draw=0;});
        check(gl.read==2&&gl.draw==2,"reproduced main-to-zero leak restored to recreated main target");
        gl=new GL(211,213);final GL shader=gl;
        FramebufferCompat.preserve(gl,1,()->2,()->{shader.read=0;shader.draw=0;});
        check(gl.read==211&&gl.draw==213,"distinct shader read/draw bindings preserved");
        gl=new GL(1,213);final GL mixed=gl;
        RuntimeException failure=new IllegalStateException("resize failed");
        try{FramebufferCompat.preserve(gl,1,()->2,()->{mixed.read=0;mixed.draw=0;throw failure;});throw new AssertionError();}
        catch(RuntimeException e){check(e==failure,"original exception propagated");}
        check(gl.read==2&&gl.draw==213,"exception restores each binding independently");
        gl=new GL(0,0);FramebufferCompat.preserve(gl,1,()->2,()->{});check(gl.read==0&&gl.draw==0,"default framebuffer remains valid");
        gl=new GL(1,1);FramebufferCompat.preserve(gl,1,()->-1,()->{});check(gl.read==0&&gl.draw==0,"failed recreation never binds deleted or negative ID");
        System.out.println("PASS: lazy-stencil framebuffer leak, recreated IDs, distinct Iris targets, default target and exception restoration");
    }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
