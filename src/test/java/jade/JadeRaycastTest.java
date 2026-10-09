import dev.fan4.compat.jade.ShipRaycastCompat;
import net.minecraft.*;
public final class JadeRaycastTest {
    public static class Camera {public class_243 origin=new class_243(25,70,0);public class_243 method_19326(){return origin;}}
    public static class Renderer {public Camera camera=new Camera();public Camera method_19418(){return camera;}}
    public record Hit(class_243 position){public class_243 method_17784(){return position;}}
    public static void main(String[] args){
        class_1297 camera=new class_1297();class_243 hit=new class_243(20004,60,5);
        class_243 world=(class_243)ShipRaycastCompat.worldPosition(hit,camera);
        check(world.equals(new class_243(25,70,4)),"translated/rotated ship hit");
        check(hit.equals(new class_243(20004,60,5)),"original ship hit/block address preserved");
        class_243 eye=new class_243(25,70,0);check(ShipRaycastCompat.worldPosition(eye,camera)==eye,"world eye unchanged");
        check(Math.abs(world.z()-eye.z())==4,"world-space short ray");
        Renderer renderer=new Renderer();class_310.INSTANCE.field_1773=renderer;
        Hit current=new Hit(hit);check(ShipRaycastCompat.currentHit(current,camera,5,3)==current,"valid ship hit retained");
        Hit stale=new Hit(new class_243(-5729,107,257));check(ShipRaycastCompat.currentHit(stale,camera,5,3)==null,"cross-dimension cached hit ignored");
        check(ShipRaycastCompat.currentHit(null,camera,5,3)==null,"missing hit unchanged");
        renderer.camera.origin=new class_243(-5729,107,257);check(ShipRaycastCompat.currentHit(stale,camera,5,3)==null,"eye perspective rejects stale hit even before render camera updates");
        snownee.jade.api.config.IWailaConfig.CONFIG.mode=snownee.jade.api.config.IWailaConfig.Perspective.CAMERA;
        renderer.camera.origin=new class_243(25,70,-20);check(ShipRaycastCompat.currentHit(current,camera,5,3)==current,"third-person camera offset respected");
        check(ShipRaycastCompat.usableHit(200*200,0,256,3),"configured long reach preserved");
        check(!ShipRaycastCompat.usableHit(Double.NaN,0,5,3),"nonfinite hit rejected");
        camera.world.ship=false;check(ShipRaycastCompat.worldPosition(hit,camera)==hit,"foreign world unchanged");
        System.out.println("PASS: Jade ship/world vector normalization, rotation, ordinary/foreign world preservation and exact overload (VS test fixture)");
    }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
