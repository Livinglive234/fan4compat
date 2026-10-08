import dev.fan4.compat.freecam.FreecamTickCompat;
import net.xolt.freecam.Freecam;
import net.xolt.freecam.util.FreeCamera;

public final class FreecamTickTest {
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
        System.out.println("PASS: active camera unloaded ticking, loaded terrain, real-player and inactive/disabled camera boundaries");
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
