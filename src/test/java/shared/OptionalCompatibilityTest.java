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
        Map<String,String> all=new HashMap<>(CompatibilityRules.VERSIONS);all.put("aether","1.5.11");
        for(String name:mixins){
            check(!applies(Map.of(),name),"empty setup applies "+name);
            check(applies(all,name),"full supported setup skips "+name);
        }
        Map<String,String> pair=Map.of("pointblank","2.2.0","dwm","1.0.38.4");
        Set<String> expected=new HashSet<>();expected.add("pointblank.client.StaleGunAnimationMixin");expected.add("pointblank.client.StaleGunDrawMixin");expected.add("pointblank.client.OffhandGunDrawMixin");
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
        for(String name:mixins)if(name.startsWith("immersiveportals.")||name.startsWith("soundphysics.")||name.startsWith("sable.")||name.startsWith("jade.")||name.contains("ShipNetherPortal")||name.contains("ShipAetherPortal")||(name.startsWith("doctorwho.")&&!name.contains("ModelCache")))check(!applies(ship,name),"ship hook without VS: "+name);
        ship=new HashMap<>(all);ship.remove("immersive_portals");
        for(String name:mixins)if(name.startsWith("immersiveportals.")||name.startsWith("accessories.")||name.startsWith("iris.")||name.contains("EurekaPortalWarning")||name.contains("TardisShipPortal")||name.contains("ExteriorShape"))check(!applies(ship,name),"portal hook without IP: "+name);
        Map<String,String> wrongIp=new HashMap<>(all);wrongIp.put("immersive_portals","unsupported-test-version");
        for(String name:mixins)if(name.startsWith("doctorwho.")&&!name.contains("ModelCache"))check(!applies(wrongIp,name),"indirect ship portal update with unsupported IP: "+name);
        check(!CompatibilityRules.applies(all,false,PREFIX+"sable.common.SableCompatMixin"),"missing Sable class");
        check(!CompatibilityRules.applies(all,true,PREFIX+"unknown.common.FutureMixin"),"unknown integration enabled");
        String metadata=Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        String depends=metadata.substring(metadata.indexOf("\"depends\""),metadata.indexOf("\"jars\""));
        check(!depends.contains("valkyrienskies")&&!depends.contains("immersive_portals")&&!depends.contains("pointblank")&&!depends.contains("dwm"),"gameplay mods declared mandatory");
        System.out.println("PASS: all "+mixins.size()+" optional mixin gates; empty, target-only, Point Blank/TARDIS, missing VS/IP, unsupported versions and metadata");
    }
}
