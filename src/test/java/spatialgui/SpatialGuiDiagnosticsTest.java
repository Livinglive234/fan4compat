import dev.fan4.compat.spatialgui.SpatialGuiDiagnostics;
import dev.fan4.compat.spatialgui.SpatialGuiPortalFix;
import java.util.*;

public final class SpatialGuiDiagnosticsTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) {
        Map<String,String> state=new LinkedHashMap<>();state.put("hooked","none");state.put("screen","none");state.put("drawFbo","0");
        List<String> out=new ArrayList<>();
        var probe=new SpatialGuiDiagnostics.Probe(()->new LinkedHashMap<>(state),out::add,4,6);
        // Nothing is logged while no Spatial GUI screen is open, whatever the world renders do.
        probe.observe("level head");probe.observe("level tail");
        check(out.isEmpty(),"silent without a Spatial GUI screen");
        // Opening a screen announces it and spends that screen's budget; nested renders show as depth 2, and the frame counts outermost renders only.
        state.put("hooked","InventoryScreen@1");state.put("screen","net.minecraft.InventoryScreen");state.put("drawFbo","7");
        probe.observe("level head");probe.observe("level head");probe.observe("level tail");probe.observe("level tail");
        check(out.get(0).startsWith("--- Spatial GUI screen opened: net.minecraft.InventoryScreen"),"announces the screen: "+out);
        check(out.get(1).contains("frame=1 depth=1 level head")&&out.get(2).contains("frame=1 depth=2 level head")&&out.get(1).contains("drawFbo=7"),"outer render is depth 1, the nested one depth 2, same frame: "+out);
        check(out.get(3).contains("depth=2 level tail")&&out.get(4).contains("depth=1 level tail"),"depth falls back as renders finish: "+out);
        probe.observe("level head");
        check(out.get(out.size()-1).contains("budget for this screen used up")&&out.stream().filter(l->l.startsWith("#")).count()==4,"stops after the per-screen budget: "+out);
        probe.observe("level tail");
        int lines=out.size();probe.observe("level head");probe.observe("level tail");
        check(out.size()==lines,"nothing more once the budget is spent");
        // A new screen gets a fresh budget and a fresh frame count; the total cap holds across screens.
        state.put("hooked","none");probe.observe("level head");probe.observe("level tail");
        state.put("hooked","ChestScreen@2");state.put("screen","net.minecraft.ContainerScreen");
        probe.observe("level head");probe.observe("level tail");
        int announced=-1;for(int i=0;i<out.size();i++)if(out.get(i).startsWith("--- Spatial GUI screen opened: net.minecraft.ContainerScreen"))announced=i;
        check(announced>=0&&out.get(announced+1).contains("frame=1 depth=1 level head")&&out.get(announced+2).contains("depth=1 level tail"),"a new screen starts again with a fresh frame count: "+out);
        long numbered=out.stream().filter(l->l.startsWith("#")).count();
        probe.observe("x");probe.observe("x");probe.observe("x");probe.observe("x");probe.observe("x");
        check(out.stream().filter(l->l.startsWith("#")).count()<=6&&numbered<=6,"never more than the total cap (6 here)");
        // One failure turns the trace off for good, without throwing.
        List<String> out2=new ArrayList<>();int[] calls={0};
        var failing=new SpatialGuiDiagnostics.Probe(()->{if(calls[0]++>0)throw new IllegalStateException("snapshot broke");return Map.of("hooked","none");},out2::add,4,6);
        failing.observe("level head");failing.observe("level head");failing.observe("level head");
        check(out2.size()==1&&out2.get(0).contains("Diagnostics disabled after observation failure: IllegalStateException"),"a failure disables the trace once: "+out2);
        // The perspective fix skips the save only inside a portal view, and never when the portal layer cannot be read.
        check(!SpatialGuiPortalFix.skipCapture(()->0)&&SpatialGuiPortalFix.skipCapture(()->1)&&SpatialGuiPortalFix.skipCapture(()->2),"skips only while a portal view renders");
        check(!SpatialGuiPortalFix.skipCapture(()->{throw new IllegalStateException("no layer");})&&!SpatialGuiPortalFix.skipCapture(()->{throw new NoClassDefFoundError("gone");}),"an unreadable layer keeps the normal capture");
        System.out.println("PASS: Spatial GUI portal trace (silent without a screen, nesting depth, frame count, per-screen budget, total cap, failure latch, perspective fix)");
    }
}
