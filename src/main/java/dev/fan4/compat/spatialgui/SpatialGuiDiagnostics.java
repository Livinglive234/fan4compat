package dev.fan4.compat.spatialgui;

import dev.fan4.compat.shared.Diagnostics;
import dev.fan4.compat.shared.GameLog;
import java.util.*;
import java.util.function.*;
import static dev.fan4.compat.shared.CompatCalls.*;
import static dev.fan4.compat.shared.Diagnostics.integer;

/**
 * Read-only trace of Spatial GUI next to Immersive Portals. Spatial GUI hooks the world render and draws its screen on a plane, and Immersive Portals renders each
 * portal view as a nested world render, so every Spatial GUI hook can fire more than once per frame. While a Spatial GUI screen is open this logs, for a
 * limited number of stages after the screen opens, how deep the world renders are nested, which portal layer is active, which framebuffers are bound and what
 * Spatial GUI's own target and flags look like. Never changes any state; one failure turns the whole trace off.
 */
public final class SpatialGuiDiagnostics {
    /** Counts nesting, notices when a Spatial GUI screen opens and spends a per-screen budget of lines. */
    public static final class Probe {
        private final Supplier<Map<String,String>> reader;private final Consumer<String> log;private final int perScreen,total;
        private String lastHooked="none";private int budget,reports,depth,frame;private boolean disabled,announcedEnd;
        public Probe(Supplier<Map<String,String>> reader,Consumer<String> log,int perScreen,int total){this.reader=reader;this.log=log;this.perScreen=perScreen;this.total=total;}
        public void observe(String stage) {
            if(disabled)return;
            if(stage.equals("level head"))depth++;
            try {
                Map<String,String> snapshot=reader.get();String hooked=snapshot.getOrDefault("hooked","none");
                if(!hooked.equals(lastHooked)) {
                    lastHooked=hooked;
                    if(!hooked.equals("none")){budget=perScreen;frame=0;announcedEnd=false;log.accept("--- Spatial GUI screen opened: "+snapshot.getOrDefault("screen","?")+" (tracing the next "+perScreen+" stages) ---");}
                }
                if(stage.equals("level head")&&depth==1)frame++;
                if(!hooked.equals("none")) {
                    if(budget>0&&reports<total) {
                        budget--;reports++;
                        StringBuilder line=new StringBuilder("#"+reports+" frame="+frame+" depth="+depth+" "+stage);
                        for(var entry:snapshot.entrySet())line.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
                        log.accept(line.toString());
                    } else if(budget==0&&!announcedEnd){announcedEnd=true;log.accept("--- trace budget for this screen used up ---");}
                }
            } catch(RuntimeException|LinkageError failure) {
                disabled=true;
                try{log.accept("Diagnostics disabled after observation failure: "+failure.getClass().getSimpleName()+": "+failure.getMessage());}catch(RuntimeException|LinkageError ignored){}
            } finally {
                if(stage.equals("level tail"))depth=Math.max(0,depth-1);
            }
        }
    }
    private static final String CLIENT="net.minecraft.class_310",SPATIAL="org.tastytrash.spatialGUI.client.SpatialGUIClient",RENDERER="org.tastytrash.spatialGUI.render.SpatialGUIRenderer";
    private static final Probe PROBE=new Probe(SpatialGuiDiagnostics::snapshot,message->GameLog.info("[Fan4Compat SpatialGuiDiag] "+message),160,1500);
    private SpatialGuiDiagnostics() {}
    /** Called from the generated hooks, which are only applied when the diagnostics switch is on. */
    public static void stage(String stage){PROBE.observe(stage);}

    private static String name(Object object){return object==null?"none":object.getClass().getName();}
    private static Map<String,String> snapshot() {
        Map<String,String> into=new LinkedHashMap<>();
        Object client=call(type(CLIENT),"method_1551"),renderer=call(type(SPATIAL),"renderer");
        Object hooked=renderer==null?null:call(renderer,"getHookedScreen");
        into.put("screen",name(field(client,"field_1755")));
        into.put("hooked",hooked==null?"none":hooked.getClass().getSimpleName()+"@"+Integer.toHexString(System.identityHashCode(hooked)));
        // Everything below is a plain read: the bound draw/read framebuffers, IP's portal layer and Spatial GUI's own target and flags.
        into.put("drawFbo",String.valueOf(integer(36006)));into.put("readFbo",String.valueOf(integer(36010)));
        Diagnostics.context(into);
        Object target=renderer==null?null:call(call(renderer,"getTargetManager"),"getInventoryTarget");
        into.put("spatialTarget",target==null?"none":field(target,"field_1476")+" "+field(target,"field_1480")+"x"+field(target,"field_1477"));
        // The camera the world render is using: a portal view should not be at the player's own position and angles.
        Object gameRenderer=client==null?null:field(client,"field_1773"),camera=gameRenderer==null?null:call(gameRenderer,"method_19418"),at=camera==null?null:field(camera,"field_18712");
        into.put("camera",at==null?"none":String.format(Locale.ROOT,"%.1f,%.1f,%.1f yaw=%.1f pitch=%.1f",field(at,"field_1352"),field(at,"field_1351"),field(at,"field_1350"),field(camera,"field_18718"),field(camera,"field_18717")));
        into.put("renderingLevel",String.valueOf(field(type(RENDERER),"renderingLevel")));
        into.put("extracting",String.valueOf(field(type(RENDERER),"isExtractingScreen")));
        into.put("skipWindowOverride",String.valueOf(field(type(RENDERER),"skipWindowOverride")));
        return into;
    }
}
