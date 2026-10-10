package dev.fan4.compat.shared;

import java.util.Map;
import java.util.Set;

/** Dependency decisions use metadata only, never loading optional mod classes. */
public final class CompatibilityRules {
    public static final Map<String,String> VERSIONS=Map.ofEntries(
        Map.entry("valkyrienskies","2.4.12-td.9+66a13242ed"),
        Map.entry("immersive_portals","6.0.6"),Map.entry("dwm","1.0.38.4"),
        Map.entry("vs_eureka","1.5.3-beta.4-td.4+b0d9511582"),
        Map.entry("freecam","1.3.0+mc1.21"),Map.entry("distanthorizons","3.3.3"),Map.entry("pointblank","2.2.0"),
        Map.entry("iris","1.8.1+mc1.21.1"),Map.entry("accessories","1.1.0-beta.48+1.21.1"),
        Map.entry("sound_physics_remastered","1.21.1-1.5.1"),Map.entry("bclib","30.4.0"),
        Map.entry("graves","1.0.0"),Map.entry("jade","15.10.6+fabric"),Map.entry("elytratrails","1.4.7.5-1.21.1"));
    private static final Set<String> PORTAL_TARDIS=Set.of("TardisShipPortalMixin","TardisShipPortalDataMixin","TardisShipExteriorShapeMixin");
    private CompatibilityRules() {}
    public static boolean supported(Map<String,String> mods,String id) {
        String version=mods.get(id);
        return version!=null && version.equals(VERSIONS.getOrDefault(id,version));
    }
    /** VS and IP are both installed at their supported builds. */
    public static boolean portalsOnVs(Map<String,String> mods) {
        return supported(mods,"valkyrienskies")&&supported(mods,"immersive_portals");
    }
    public static boolean applies(Map<String,String> mods,boolean sable,String name) {
        String prefix="dev.fan4.compat.mixin.";
        if(!name.startsWith(prefix))return false;
        String path=name.substring(prefix.length()),simple=path.substring(path.lastIndexOf('.')+1);
        boolean vs=supported(mods,"valkyrienskies"),ip=supported(mods,"immersive_portals");
        boolean iris=supported(mods,"iris"),dh=supported(mods,"distanthorizons"),spr=supported(mods,"sound_physics_remastered"),eureka=supported(mods,"vs_eureka");
        boolean bclib=supported(mods,"bclib"),accessories=supported(mods,"accessories");
        if(path.startsWith("graves."))return supported(mods,"graves")&&accessories&&mods.containsKey("aether");
        if(path.startsWith("freecam."))return supported(mods,"freecam")&&dh&&(!simple.startsWith("FreecamBsl")||iris);
        if(path.startsWith("valkyrienskies."))return vs;
        if(path.startsWith("sable."))return vs&&sable;
        if(path.startsWith("immersiveportals."))return vs&&ip;
        if(path.startsWith("doctorwho.")) {
            if(!supported(mods,"dwm"))return false;
            if(simple.startsWith("TardisShipModelCache"))return true;
            // Shared ship ticks also update DWM portal state when IP is present.
            // An unsupported IP build must not reach those helpers indirectly.
            return vs&&(!mods.containsKey("immersive_portals")||ip)&&(!PORTAL_TARDIS.contains(simple)||ip);
        }
        if(path.startsWith("accessories."))return ip&&accessories;
        if(simple.equals("DhPortalShaderMixin"))return ip&&iris&&dh;
        if(path.startsWith("iris."))return ip&&iris;
        if(simple.equals("PortalSoundSnapshotMixin"))return ip&&spr;
        if(simple.equals("PortalSoundRayMixin"))return ip&&spr&&!mods.containsKey("valkyrienskies");
        if(path.startsWith("soundphysics."))return vs&&spr;
        if(path.startsWith("bclib."))return bclib;
        if(path.startsWith("distanthorizons."))return dh&&(!path.startsWith("distanthorizons.client.")||ip);
        if(path.startsWith("pointblank."))return supported(mods,"pointblank")&&(!simple.equals("PortalStencilCacheMixin")||ip)&&(!simple.endsWith("DiagnosticMixin")||Boolean.parseBoolean(System.getProperty("fan4compat.renderDiagnostics","true")));
        if(path.startsWith("elytratrails."))return supported(mods,"elytratrails");
        if(path.startsWith("jade."))return vs&&supported(mods,"jade")&&(simple.equals("JadeShipRaycastMixin")||eureka);
        if(simple.equals("EurekaPortalWarningMixin")||simple.equals("EurekaPortalWarningInfoMixin"))return ip&&eureka;
        if(simple.equals("EurekaDebugMixin"))return eureka;
        if(simple.equals("ShipNetherPortalCreationMixin"))return vs;
        if(simple.equals("ShipAetherPortalCreationMixin"))return vs&&mods.containsKey("aether");
        if(simple.equals("OptionalRecipeDependenciesMixin"))return bclib||mods.containsKey("jeed")||mods.containsKey("supplementaries")||mods.containsKey("amendments")||mods.containsKey("moonlight");
        return false;
    }
}
