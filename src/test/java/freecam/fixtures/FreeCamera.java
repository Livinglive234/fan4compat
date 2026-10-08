package net.xolt.freecam.util;
public final class FreeCamera {
    public World world=new World();
    public World method_37908(){return world;}
    public int method_31477(){return 100;}
    public int method_31479(){return -20;}
    public static final class World {
        public boolean loaded;
        public boolean method_33598(int x,int z){if(x!=100||z!=-20)throw new AssertionError("wrong camera chunk");return loaded;}
    }
}
