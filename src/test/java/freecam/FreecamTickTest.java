import dev.fan4.compat.freecam.FreecamTickCompat;
import net.xolt.freecam.Freecam;
import net.xolt.freecam.util.FreeCamera;

public final class FreecamTickTest {
    public static final class Shader {
        public int uClipDistance=17,calls;public float distance=80;
        public void setUniform(int location,float value){check(location==17,"correct native uniform");distance=value;calls++;}
    }
    static void clip(boolean relax,String message) {
        check(FreecamTickCompat.nearClip(48f)==(relax?.5f:48f),message);
        Shader shader=new Shader();FreecamTickCompat.lodClip(shader);
        check(shader.calls==(relax?1:0)&&shader.distance==(relax?0:80),message+" uniform");
    }
    public static void main(String[] args) {
        Object player=new Object();FreeCamera camera=new FreeCamera(),inactive=new FreeCamera();
        Freecam.enabled=true;Freecam.camera=camera;
        check(FreecamTickCompat.allowTick(true,player),"loaded player tick retained");
        check(!FreecamTickCompat.allowTick(false,player),"real player still waits for terrain");
        check(FreecamTickCompat.allowTick(false,camera),"active camera ticks outside full chunks");
        check(!FreecamTickCompat.allowTick(false,inactive),"inactive camera keeps native check");
        Freecam.enabled=false;
        check(!FreecamTickCompat.allowTick(false,camera),"disabled freecam keeps native check");
        check(!FreecamTickCompat.allowTick(false,null),"null entity cannot bypass check");
        Freecam.enabled=true;Freecam.camera=camera;
        net.minecraft.class_310.INSTANCE.camera=camera;net.minecraft.class_310.INSTANCE.field_1687=camera.world;
        clip(true,"active unloaded camera retains nearby LODs");
        Shader reused=new Shader();FreecamTickCompat.lodClip(reused);check(reused.distance==0,"unloaded frame on reused shader");
        camera.world.loaded=true;reused.distance=80;FreecamTickCompat.lodClip(reused);
        check(reused.distance==80&&reused.calls==1,"native next-frame uniform retained when terrain returns");camera.world.loaded=false;
        check(FreecamTickCompat.nearClip(.1f)==.1f,"never increase a smaller near plane");
        camera.world.loaded=true;clip(false,"full terrain restores native clipping");camera.world.loaded=false;
        net.minecraft.class_310.INSTANCE.camera=player;clip(false,"real player view unchanged");net.minecraft.class_310.INSTANCE.camera=camera;
        qouteall.imm_ptl.core.render.context_management.PortalRendering.rendering=true;clip(false,"portal view retains guards");qouteall.imm_ptl.core.render.context_management.PortalRendering.rendering=false;
        net.minecraft.class_310.INSTANCE.field_1687=new Object();clip(false,"foreign view unchanged");net.minecraft.class_310.INSTANCE.field_1687=camera.world;
        Freecam.enabled=false;clip(false,"disabled camera unchanged");
        System.out.println("PASS: active camera unloaded ticking, loaded terrain, real-player and inactive/disabled camera boundaries; unloaded-camera LOD near/fragment clipping, full-terrain and portal restoration");
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
