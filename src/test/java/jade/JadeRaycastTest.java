import dev.fan4.compat.jade.ShipRaycastCompat;
import net.minecraft.*;
public final class JadeRaycastTest {
    public static void main(String[] args){
        class_1297 camera=new class_1297();class_243 hit=new class_243(20004,60,5);
        class_243 world=(class_243)ShipRaycastCompat.worldPosition(hit,camera);
        check(world.equals(new class_243(25,70,4)),"translated/rotated ship hit");
        check(hit.equals(new class_243(20004,60,5)),"original ship hit/block address preserved");
        class_243 eye=new class_243(25,70,0);check(ShipRaycastCompat.worldPosition(eye,camera)==eye,"world eye unchanged");
        check(Math.abs(world.z()-eye.z())==4,"world-space short ray");
        camera.world.ship=false;check(ShipRaycastCompat.worldPosition(hit,camera)==hit,"foreign world unchanged");
        System.out.println("PASS: Jade ship/world vector normalization, rotation, ordinary/foreign world preservation and exact overload (VS test fixture)");
    }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
