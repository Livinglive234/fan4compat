import dev.fan4.compat.shared.CompatibilityRules;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public final class OptionalCompatibilityTest {
    static final String PREFIX="dev.fan4.compat.mixin.";
    static boolean applies(Map<String,String> mods,String name){return CompatibilityRules.applies(mods,true,PREFIX+name);}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        String config=Files.readString(Path.of("src/main/resources/fan4compat.mixins.json"));
        Matcher matcher=Pattern.compile("\"((?:[a-z]+)\\.(?:common|client)\\.[A-Za-z0-9]+)\"").matcher(config);
        List<String> mixins=new ArrayList<>();while(matcher.find())mixins.add(matcher.group(1));
        check(mixins.size()>60,"manifest coverage");
        Map<String,String> all=new HashMap<>(CompatibilityRules.VERSIONS);all.put("aether","1.5.11");all.put("spatial_gui","1.9.1");all.put("sodium","0.8.13+mc1.21.1");
        System.setProperty("fan4compat.renderDiagnostics","true");System.setProperty("fan4compat.portalDepthDiagnostics","true");System.setProperty("fan4compat.spatialGuiDiagnostics","true");
        for(String name:mixins){
            check(!applies(Map.of(),name),"empty setup applies "+name);
            check(applies(all,name)!=name.endsWith("PortalSoundRayMixin"),"full supported setup skips "+name);
        }
        System.clearProperty("fan4compat.renderDiagnostics");System.clearProperty("fan4compat.portalDepthDiagnostics");
        for(String version:List.of("1.0.38.4","1.0.38.4+build.2","1.0.38.5","1.0.39","1.0.39-dev.1","1.0.100","1.1.0","2.0.0")) {
            Map<String,String> newer=new HashMap<>(all);newer.put("dwm",version);
            check(CompatibilityRules.supported(newer,"dwm"),"accepted DWM minimum/newer version: "+version);
            for(String name:mixins)if(name.startsWith("doctorwho."))check(applies(newer,name),"newer DWM hook excluded: "+version+" / "+name);
            check(applies(Map.of("dwm",version),"doctorwho.client.TardisShipModelCache0Mixin"),"newer standalone model cache");
            check(!applies(Map.of("dwm",version),"doctorwho.common.TardisShipStateMixin"),"newer DWM still requires VS");
            newer.put("immersive_portals","unsupported");
            check(!applies(newer,"doctorwho.common.TardisShipStateMixin"),"newer DWM still rejects unsupported IP");
        }
        for(String version:List.of("1.0.38.3","1.0.38","1.0.9","1.0.38.4-beta.1","0.9.100","invalid","","1..39")) {
            Map<String,String> older=new HashMap<>(all);older.put("dwm",version);
            check(!CompatibilityRules.supported(older,"dwm"),"older/malformed DWM accepted: "+version);
            for(String name:mixins)if(name.startsWith("doctorwho."))check(!applies(older,name),"older DWM hook enabled: "+version+" / "+name);
        }
        check(CompatibilityRules.requirement("dwm").equals(">=1.0.38.4"),"startup warning states minimum");
        check(CompatibilityRules.requirement("pointblank").equals("2.2.0"),"other version pins remain exact");
        check(CompatibilityRules.requirement("xaerominimap").equals("26.6.0 (or 26.5.0)")&&CompatibilityRules.requirement("xaeroworldmap").equals("1.47.0 (or 1.46.0)"),"Xaero states the build it was written for and the older one it still accepts");
        for(String[] pair:new String[][]{{"xaerominimap","26.6.0"},{"xaerominimap","26.5.0"},{"xaeroworldmap","1.47.0"},{"xaeroworldmap","1.46.0"}})
            check(CompatibilityRules.supported(Map.of(pair[0],pair[1]),pair[0]),pair[0]+" "+pair[1]+" is supported");
        for(String[] pair:new String[][]{{"xaerominimap","26.4.0"},{"xaerominimap","26.7.0"},{"xaeroworldmap","1.45.0"},{"xaeroworldmap","1.48.0"},{"sound_physics_remastered","1.21.1-1.5.1"}})
            check(!CompatibilityRules.supported(Map.of(pair[0],pair[1]),pair[0]),pair[0]+" "+pair[1]+" is not supported");
        check(!applies(Map.of("pointblank","2.2.0"),"pointblank.client.PortalStencilCacheMixin"),"portal stencil bridge requires IP");
        check(!applies(Map.of("pointblank","2.2.0","immersive_portals","unsupported"),"pointblank.client.PortalStencilCacheMixin"),"portal stencil bridge rejects unsupported IP");
        check(applies(Map.of("pointblank","2.2.0","immersive_portals","6.0.6"),"pointblank.client.PortalStencilCacheMixin"),"portal stencil bridge needs no VS/Iris");
        check(!applies(Map.of("distanthorizons","3.3.3"),"distanthorizons.client.DhPortalRenderMixin"),"DH portal guard requires IP");
        check(applies(Map.of("distanthorizons","3.3.3","immersive_portals","6.0.6"),"distanthorizons.client.DhPortalRenderMixin"),"DH portal guard needs no VS");
        check(!applies(Map.of("iris","1.8.1+mc1.21.1","immersive_portals","6.0.6"),"iris.client.DhPortalShaderMixin"),"Iris DH guard requires DH");
        check(applies(Map.of("freecam","1.3.0+mc1.21","distanthorizons","3.3.3"),"freecam.client.FreecamUnloadedTickMixin"),"freecam fix needs no IP or VS");
        check(!applies(Map.of("freecam","1.3.0+mc1.21"),"freecam.client.FreecamUnloadedTickMixin"),"freecam fix requires DH");
        check(!applies(Map.of("pointblank","2.2.0"),"pointblank.client.PortalStencilCacheMixin"),"portal stencil bridge requires IP");
        check(!applies(Map.of("pointblank","2.2.0","immersive_portals","unsupported"),"pointblank.client.PortalStencilCacheMixin"),"portal stencil bridge rejects unsupported IP");
        check(applies(Map.of("pointblank","2.2.0","immersive_portals","6.0.6"),"pointblank.client.PortalStencilCacheMixin"),"portal stencil bridge needs no VS/Iris");
        check(!applies(Map.of("distanthorizons","3.3.3"),"freecam.client.FreecamUnloadedTickMixin"),"freecam fix requires freecam");
        check(!applies(Map.of("freecam","1.3.0+mc1.21","distanthorizons","3.3.3"),"freecam.client.FreecamBslShaderMixin"),"BSL source fix requires Iris");
        check(applies(Map.of("freecam","1.3.0+mc1.21","distanthorizons","3.3.3","iris","1.8.1+mc1.21.1"),"freecam.client.FreecamBslShaderMixin"),"BSL source fix needs no IP/VS");
        check(applies(Map.of("valkyrienskies",CompatibilityRules.VERSIONS.get("valkyrienskies"),"jade","15.10.6+fabric"),"jade.client.JadeShipRaycastMixin"),"Jade ray fix needs no Eureka or IP");
        check(applies(Map.of("immersive_portals","6.0.6","iris","1.8.1+mc1.21.1"),"iris.client.IrisFramebufferCopyMixin"),"GL fallback needs no DH or VS");
        Map<String,String> portalIris=Map.of("immersive_portals","6.0.6","iris","1.8.1+mc1.21.1");
        check(!applies(portalIris,"iris.client.IrisPortalDepthDiagnosticMixin"),"depth diagnostics are off by default");
        System.setProperty("fan4compat.portalDepthDiagnostics","true");
        check(applies(portalIris,"iris.client.IrisPortalDepthDiagnosticMixin"),"depth diagnostics enabled without VS/DH/Point Blank");
        System.setProperty("fan4compat.portalDepthDiagnostics","false");
        check(!applies(portalIris,"iris.client.IrisPortalDepthDiagnosticMixin"),"depth diagnostics can be disabled");
        check(applies(portalIris,"iris.client.IrisDepthFormatMixin"),"disabling diagnostics preserves depth fix");
        System.clearProperty("fan4compat.portalDepthDiagnostics");
        check(!applies(Map.of("pointblank","2.2.0"),"pointblank.client.IrisScopeStencilMixin"),"Iris stencil path requires Iris");
        check(applies(Map.of("pointblank","2.2.0","iris","1.8.1+mc1.21.1"),"pointblank.client.IrisScopeStencilMixin"),"Iris stencil repair requires no IP");
        Map<String,String> pair=Map.of("pointblank","2.2.0","dwm","1.0.38.4");
        Set<String> expected=new HashSet<>();expected.add("pointblank.client.StaleGunAnimationMixin");expected.add("pointblank.client.StaleGunDrawMixin");expected.add("pointblank.client.OffhandGunDrawMixin");
        expected.add("pointblank.client.GunStencilMixin");expected.add("pointblank.client.GunStencilClearMixin");expected.add("pointblank.client.GunFramebufferMixin");expected.add("pointblank.client.GunAuxFramebufferMixin");
        for(int i=0;i<5;i++)expected.add("doctorwho.client.TardisShipModelCache"+i+"Mixin");
        for(String name:mixins)check(applies(pair,name)==expected.contains(name),"Point Blank/TARDIS setup: "+name);
        for(String id:CompatibilityRules.VERSIONS.keySet()){
            Map<String,String> one=Map.of(id,CompatibilityRules.VERSIONS.get(id));
            for(String name:mixins){
                boolean active=applies(one,name);
                Map<String,String> wrong=Map.of(id,"unsupported-test-version");
                check(!applies(wrong,name),"unsupported lone mod applies "+id+" / "+name);
                if(active)check(!applies(Map.of(),name),"missing lone mod applies "+name);
            }
        }
        Map<String,String> ship=new HashMap<>(all);ship.remove("valkyrienskies");
        for(String name:mixins)if(name.startsWith("immersiveportals.")||name.contains("ShipAcoustic")||name.startsWith("sable.")||name.startsWith("jade.")||name.contains("ShipNetherPortal")||name.contains("ShipAetherPortal")||(name.startsWith("doctorwho.")&&!name.contains("ModelCache")))check(!applies(ship,name),"ship hook without VS: "+name);
        ship=new HashMap<>(all);ship.remove("immersive_portals");
        for(String name:mixins)if(name.startsWith("immersiveportals.")||name.startsWith("accessories.")||(name.startsWith("iris.")&&!name.equals("iris.client.DhDepthAttachmentMixin"))||name.contains("EurekaPortalWarning")||name.contains("TardisShipPortal")||name.contains("ExteriorShape"))check(!applies(ship,name),"portal hook without IP: "+name);
        Map<String,String> dhIris=Map.of("iris",CompatibilityRules.VERSIONS.get("iris"),"distanthorizons",CompatibilityRules.VERSIONS.get("distanthorizons"));
        check(applies(dhIris,"iris.client.DhDepthAttachmentMixin"),"DH depth ownership needs no IP or Point Blank");
        check(!applies(Map.of("iris",CompatibilityRules.VERSIONS.get("iris")),"iris.client.DhDepthAttachmentMixin"),"DH depth ownership requires DH");
        Map<String,String> wrongIp=new HashMap<>(all);wrongIp.put("immersive_portals","unsupported-test-version");
        for(String name:mixins)if(name.startsWith("doctorwho.")&&!name.contains("ModelCache"))check(!applies(wrongIp,name),"indirect ship portal update with unsupported IP: "+name);
        Map<String,String> audio=Map.of("immersive_portals","6.0.6","sound_physics_remastered","1.21.1-1.5.2");
        check(applies(audio,"soundphysics.client.PortalSoundSnapshotMixin")&&applies(audio,"soundphysics.client.PortalSoundRayMixin"),"standalone IP/Sound Physics hooks");
        check(!applies(audio,"soundphysics.client.ShipAcousticRaycastMixin"),"ship ray enabled without VS");
        check(!applies(Map.of("sound_physics_remastered","1.21.1-1.5.2"),"soundphysics.client.PortalSoundSnapshotMixin"),"portal scope without IP");
        check(!CompatibilityRules.applies(all,false,PREFIX+"sable.common.SableCompatMixin"),"missing Sable class");
        check(!CompatibilityRules.applies(all,true,PREFIX+"unknown.common.FutureMixin"),"unknown integration enabled");
        String metadata=Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        String depends=metadata.substring(metadata.indexOf("\"depends\""),metadata.indexOf("\"jars\""));
        check(!depends.contains("valkyrienskies")&&!depends.contains("immersive_portals")&&!depends.contains("pointblank")&&!depends.contains("dwm"),"gameplay mods declared mandatory");
        // The Spatial GUI trace needs Spatial GUI 1.9.1, Immersive Portals and its own switch.
        Map<String,String> spatial=Map.of("spatial_gui","1.9.1","immersive_portals","6.0.6");
        String trace="spatialgui.client.SpatialGuiLevelDiagnosticMixin";
        check(applies(spatial,trace),"trace applies with Spatial GUI 1.9.1 and Immersive Portals");
        check(!applies(Map.of("spatial_gui","1.9","immersive_portals","6.0.6"),trace)&&!applies(Map.of("spatial_gui","2.0","immersive_portals","6.0.6"),trace),"trace is exact to the Spatial GUI build it was checked against");
        check(!applies(Map.of("spatial_gui","1.9.1"),trace)&&!applies(Map.of("immersive_portals","6.0.6"),trace),"trace needs both mods");
        // Iris 1.8.14-beta.1 (the build that works with Sodium 0.8) is supported next to 1.8.1, so the Iris and Distant Horizons fixes apply to it; other builds are not.
        Map<String,String> iris14=new HashMap<>(all);iris14.put("iris","1.8.14-beta.1+mc1.21.1");
        check(CompatibilityRules.supported(iris14,"iris")&&applies(iris14,"iris.client.DhDepthAttachmentMixin")&&applies(iris14,"iris.client.DhPortalShaderMixin"),"Iris 1.8.14-beta.1 is supported and gets the Distant Horizons depth fixes");
        Map<String,String> bclib=new HashMap<>(all);
        for(String version:new String[]{"30.4.0","30.5.4","30.5.6"}) {bclib.put("bclib",version);check(CompatibilityRules.supported(bclib,"bclib")&&applies(bclib,"bclib.common.StackCodecAliasMixin"),"BCLib "+version+" gets the stack codec fix");}
        bclib.put("bclib","30.5.5");check(!CompatibilityRules.supported(bclib,"bclib")&&!applies(bclib,"bclib.common.StackCodecAliasMixin"),"an unchecked BCLib build is skipped");
        iris14.put("iris","1.8.13+mc1.21.1");
        check(!CompatibilityRules.supported(iris14,"iris")&&!applies(iris14,"iris.client.DhDepthAttachmentMixin"),"other Iris builds are still skipped");
        // The Sodium 0.8 portal culling needs the 0.8 series and Immersive Portals, and has its own feature switch.
        String cull="sodium.client.SodiumViewportCullingMixin";Map<String,String> sodium08=Map.of("sodium","0.8.13+mc1.21.1","immersive_portals","6.0.6");
        check(applies(sodium08,cull),"portal culling applies with Sodium 0.8 and Immersive Portals");
        check(!applies(Map.of("sodium","0.6.7+mc1.21.1","immersive_portals","6.0.6"),cull)&&!applies(Map.of("sodium","0.9.0","immersive_portals","6.0.6"),cull),"portal culling is only for the Sodium 0.8 series");
        check(!applies(Map.of("sodium","0.8.13+mc1.21.1"),cull)&&!applies(Map.of("immersive_portals","6.0.6"),cull),"portal culling needs both mods");
        String occlusion="sodium.client.SodiumPortalOcclusionMixin";
        check(applies(sodium08,occlusion)&&!applies(Map.of("sodium","0.6.7+mc1.21.1","immersive_portals","6.0.6"),occlusion)&&!applies(Map.of("sodium","0.8.13+mc1.21.1"),occlusion),"portal occlusion: Sodium 0.8 and Immersive Portals");
        // Ships on Sodium 0.8 need the 0.8 series and Valkyrien Skies at its supported build (Immersive Portals is not needed), and have their own switch.
        String ships="sodium.client.SodiumShipRenderingMixin";Map<String,String> shipsOn08=Map.of("sodium","0.8.13+mc1.21.1","valkyrienskies","2.4.12-td.9+66a13242ed");
        check(applies(shipsOn08,ships)&&!applies(Map.of("sodium","0.8.13+mc1.21.1"),ships),"ship rendering applies with Sodium 0.8 and Valkyrien Skies, not without VS");
        check(!applies(Map.of("sodium","0.6.7+mc1.21.1","valkyrienskies","2.4.12-td.9+66a13242ed"),ships)&&!applies(Map.of("sodium","0.8.13+mc1.21.1","valkyrienskies","3.0.0"),ships),"ship rendering is only for Sodium 0.8 and the supported VS build");
        System.setProperty("fan4compat.feature.sodiumShipRendering","false");
        check(!applies(shipsOn08,ships),"ship rendering can be turned off");
        System.clearProperty("fan4compat.feature.sodiumShipRendering");
        System.setProperty("fan4compat.feature.sodiumPortalCulling","false");
        check(!applies(sodium08,cull),"portal culling can be turned off");
        System.clearProperty("fan4compat.feature.sodiumPortalCulling");
        System.clearProperty("fan4compat.spatialGuiDiagnostics");
        check(!applies(spatial,trace),"trace is off unless the switch is on");
        // The perspective fix is a pack feature: it needs the same two mods at the same build but not the diagnostics switch, and its own feature switch turns it off.
        String fix="spatialgui.client.SpatialGuiPerspectiveMixin";
        check(applies(spatial,fix),"perspective fix applies without the diagnostics switch");
        check(!applies(Map.of("spatial_gui","1.9","immersive_portals","6.0.6"),fix)&&!applies(Map.of("spatial_gui","1.9.1"),fix)&&!applies(Map.of("immersive_portals","6.0.6"),fix),"perspective fix needs Spatial GUI 1.9.1 and Immersive Portals");
        System.setProperty("fan4compat.feature.spatialGuiPortalFix","false");
        check(!applies(spatial,fix),"perspective fix can be turned off");
        System.clearProperty("fan4compat.feature.spatialGuiPortalFix");
        System.setProperty("fan4compat.spatialGuiDiagnostics","true");
        System.out.println("PASS: all "+mixins.size()+" optional mixin gates; empty, target-only, Point Blank/TARDIS, missing VS/IP, unsupported versions and metadata");
    }
}
