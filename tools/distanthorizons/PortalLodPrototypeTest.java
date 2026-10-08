import java.util.*;

/** CPU-only prototype: excluded from the addon jar and never touches Minecraft or GL. */
public final class PortalLodPrototypeTest {
    record Camera(double x,double y,double z) {}
    record Plane(double x,double y,double z,double w) {
        Plane relativeTo(Camera c){return new Plane(x,y,z,w+x*c.x+y*c.y+z*c.z);}
        boolean contains(double px,double py,double pz){return x*px+y*py+z*pz+w>=0;}
    }
    static final class State {
        String world;Camera camera;float[] matrix;int framebuffer,depthTexture,distance;
        State(String world,Camera camera,float[] matrix,int framebuffer,int depthTexture,int distance) {
            this.world=world;this.camera=camera;this.matrix=matrix.clone();this.framebuffer=framebuffer;this.depthTexture=depthTexture;this.distance=distance;
        }
        State copy(){return new State(world,camera,matrix,framebuffer,depthTexture,distance);}
        void restore(State s){world=s.world;camera=s.camera;matrix=s.matrix.clone();framebuffer=s.framebuffer;depthTexture=s.depthTexture;distance=s.distance;}
    }
    static boolean supported(boolean portal,int layer,boolean mirror,double scale,boolean shaders,boolean dataReady) {
        return portal&&layer==1&&!mirror&&scale==1&&!shaders&&dataReady;
    }
    static void view(State active,State destination,Runnable draw) {
        State previous=active.copy();try{active.restore(destination);active.distance=Math.min(previous.distance,5);draw.run();}finally{active.restore(previous);}
    }
    public static void main(String[] args) {
        check(supported(true,1,false,1,false,true),"basic destination view");
        check(!supported(true,1,false,1,true,true),"shader fallback");
        check(!supported(true,2,false,1,false,true),"nested fallback");
        check(!supported(true,1,true,1,false,true),"mirror fallback");
        check(!supported(true,1,false,2,false,true),"scale fallback");
        check(!supported(true,1,false,1,false,false),"missing-data terrain fallback");
        State main=new State("overworld",new Camera(0,70,0),new float[]{1,2,3},11,12,16);
        State aether=new State("aether",new Camera(1000,150,-2000),new float[]{4,5,6},21,22,16);
        State other=new State("tardis",new Camera(-99,64,88),new float[]{7,8,9},31,32,16);
        view(main,aether,()->{
            check(main.world.equals("aether")&&main.distance==5&&main.depthTexture==22,"destination and depth ownership");
            main.matrix[0]=99;
            view(main,other,()->check(main.world.equals("tardis")&&main.framebuffer==31,"nested ownership"));
            check(main.world.equals("aether")&&main.matrix[0]==99&&main.depthTexture==22,"nested restoration");
        });
        check(main.world.equals("overworld")&&main.matrix[0]==1&&main.depthTexture==12&&main.distance==16,"main restoration");
        check(aether.matrix[0]==4,"no matrix aliases");
        try{view(main,aether,()->{main.matrix[0]=98;throw new IllegalStateException("draw failed");});throw new AssertionError("failure lost");}catch(IllegalStateException expected){check(expected.getMessage().equals("draw failed"),"native failure preserved");}
        check(main.world.equals("overworld")&&main.matrix[0]==1&&main.framebuffer==11,"failure restoration");
        main.distance=3;view(main,aether,()->check(main.distance==3,"do not raise low distances"));check(main.distance==3,"low distance restoration");
        for(Plane world:new Plane[]{new Plane(1,0,0,-1000),new Plane(-1,0,0,1000),new Plane(0,1,0,-150),new Plane(0,0,-1,-2000)}) {
            Camera c=aether.camera;Plane relative=world.relativeTo(c);
            for(double[] p:new double[][]{{1001,151,-2001},{999,149,-1999},{1000,150,-2000}})
                check(world.contains(p[0],p[1],p[2])==relative.contains(p[0]-c.x,p[1]-c.y,p[2]-c.z),"portal-plane translation");
        }
        System.out.println("PASS: CPU-only destination ownership, copied matrices, nested/exception restoration, five-chunk policy, fallbacks and portal-plane translation; no GPU integration claimed");
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
