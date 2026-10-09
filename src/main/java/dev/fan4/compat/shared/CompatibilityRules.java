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
        return mods.containsKey(id) && (!VERSIONS.containsKey(id)||VERSIONS.get(id).equals(mods.get(id)));
    }
    public static boolean applies(Map<String,String> mods,boolean sable,String name) {
        String prefix="dev.fan4.compat.mixin.";
        if(!name.startsWith(prefix))return false;
        String path=name.substring(prefix.length()),simple=path.substring(path.lastIndexOf('.')+1);
        boolean vs=supported(mods,"valkyrienskies"),ip=supported(mods,"immersive_portals");
        if(path.startsWith("graves."))return supported(mods,"graves")&&supported(mods,"accessories")&&mods.containsKey("aether");
        if(path.startsWith("freecam."))return supported(mods,"freecam")&&supported(mods,"distanthorizons")&&(!simple.startsWith("FreecamBsl")||supported(mods,"iris"));
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
        if(path.startsWith("accessories."))return ip&&supported(mods,"accessories");
        if(simple.equals("DhPortalShaderMixin"))return ip&&supported(mods,"iris")&&supported(mods,"distanthorizons");
        if(path.startsWith("iris."))return ip&&supported(mods,"iris");
        if(simple.equals("PortalSoundSnapshotMixin"))return ip&&supported(mods,"sound_physics_remastered");
        if(simple.equals("PortalSoundRayMixin"))return ip&&supported(mods,"sound_physics_remastered")&&!mods.containsKey("valkyrienskies");
        if(path.startsWith("soundphysics."))return vs&&supported(mods,"sound_physics_remastered");
        if(path.startsWith("bclib."))return supported(mods,"bclib");
        if(path.startsWith("distanthorizons."))return supported(mods,"distanthorizons")&&(!path.startsWith("distanthorizons.client.")||ip);
        if(path.startsWith("pointblank."))return supported(mods,"pointblank");
        if(path.startsWith("elytratrails."))return supported(mods,"elytratrails");
        if(path.startsWith("jade."))return vs&&supported(mods,"jade")&&(simple.equals("JadeShipRaycastMixin")||supported(mods,"vs_eureka"));
        if(simple.equals("EurekaPortalWarningMixin")||simple.equals("EurekaPortalWarningInfoMixin"))return ip&&supported(mods,"vs_eureka");
        if(simple.equals("EurekaDebugMixin"))return supported(mods,"vs_eureka");
        if(simple.equals("ShipNetherPortalCreationMixin"))return vs;
        if(simple.equals("ShipAetherPortalCreationMixin"))return vs&&mods.containsKey("aether");
        if(simple.equals("OptionalRecipeDependenciesMixin"))return supported(mods,"bclib")||mods.containsKey("jeed")||mods.containsKey("supplementaries")||mods.containsKey("amendments")||mods.containsKey("moonlight");
        return false;
    }
}
