import dev.fan4.compat.shared.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public final class FeaturesTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    static final Map<String,String> ALL=new HashMap<>(CompatibilityRules.VERSIONS);
    static boolean applies(String mixin){return CompatibilityRules.applies(ALL,true,"dev.fan4.compat.mixin."+mixin);}
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("fan4features"),file=dir.resolve("fan4compat.properties");
        Features.reset(file);
        for(Features.Feature feature:Features.ALL)check(Features.enabled(feature.id())==feature.defaultValue(),"default for "+feature.id());
        check(!Files.exists(file),"reading settings never creates the file");

        // Saved values load; unknown keys survive a save; bad values fall back to the default.
        Files.writeString(file,"wingTrails=false\nrenderDiagnostics=maybe\nupdaterThing=keep\n");
        Features.reset(file);
        check(!Features.enabled("wingTrails")&&!Features.enabled("renderDiagnostics"),"saved value read; invalid value uses default");
        Features.set("shipAcoustics",false);Features.save();
        Properties saved=new Properties();try(var in=Files.newBufferedReader(file)){saved.load(in);}
        check("keep".equals(saved.getProperty("updaterThing")),"unknown keys preserved");
        check("false".equals(saved.getProperty("shipAcoustics"))&&"false".equals(saved.getProperty("wingTrails"))&&"true".equals(saved.getProperty("jadeHelm")),"every switch written explicitly");
        check(Files.list(dir).count()==1,"temporary file replaced");
        Features.reset(file);check(!Features.enabled("shipAcoustics"),"saved value survives a restart");

        // JVM override beats the file; unknown ids are programming errors.
        System.setProperty("fan4compat.feature.wingTrails","true");
        check(Features.enabled("wingTrails"),"JVM override wins");
        System.clearProperty("fan4compat.feature.wingTrails");
        try{Features.enabled("nope");throw new AssertionError("unknown id accepted");}catch(IllegalArgumentException expected){}

        // Each switch controls exactly its hooks; everything else keeps installing.
        Features.reset(dir.resolve("fresh.properties"));
        String config=Files.readString(Path.of("src/main/resources/fan4compat.mixins.json"));
        Matcher matcher=Pattern.compile("\"((?:[a-z]+)\\.(?:common|client)\\.[A-Za-z0-9]+)\"").matcher(config);
        List<String> mixins=new ArrayList<>();while(matcher.find())mixins.add(matcher.group(1));
        Set<String> ids=new HashSet<>();for(Features.Feature feature:Features.ALL)ids.add(feature.id());
        for(var hook:Features.hookedMixins().entrySet()) {
            check(ids.contains(hook.getValue()),"hook points at a listed feature: "+hook);
            check(mixins.stream().anyMatch(name->name.endsWith("."+hook.getKey())),"hooked mixin still exists: "+hook.getKey());
        }
        ALL.put("aether","1.5.11");
        for(Features.Feature feature:Features.ALL) {
            List<String> controlled=mixins.stream().filter(name->feature.id().equals(Features.hookedMixins().get(name.substring(name.lastIndexOf('.')+1)))
                ||(feature.id().equals("freecamFixes")&&name.startsWith("freecam."))||(feature.id().equals("tardisModelCache")&&name.contains("TardisShipModelCache"))).toList();
            Features.reset(dir.resolve("none.properties"));
            Features.set(feature.id(),false);
            for(String name:mixins) {
                boolean off=controlled.contains(name);
                if(off)check(!applies(name),feature.id()+" off removes "+name);
                else if(!name.endsWith("DiagnosticMixin")&&!name.endsWith("PortalSoundRayMixin"))check(applies(name),feature.id()+" off must not touch "+name);
            }
            check(feature.id().equals("trinketGraves")||feature.id().equals("xaeroWaypointSync")||feature.id().equals("eurekaDebugLogging")||feature.id().endsWith("iagnostics")||!controlled.isEmpty(),feature.id()+" controls at least one hook");
        }
        Features.reset(dir.resolve("clean.properties"));

        // Diagnostics read the switches, with JVM arguments still winning.
        check(!Diagnostics.render()&&!Diagnostics.dh()&&!Diagnostics.portalDepth(),"diagnostics are off by default");
        Features.set("renderDiagnostics",true);
        check(Diagnostics.render()&&Diagnostics.dh(),"render switch also drives DH diagnostics");
        System.setProperty("fan4compat.dhRenderDiagnostics","false");
        check(Diagnostics.render()&&!Diagnostics.dh(),"DH JVM argument wins");
        System.clearProperty("fan4compat.dhRenderDiagnostics");
        Features.set("renderDiagnostics",false);Features.set("dhRenderDiagnostics",true);
        check(!Diagnostics.render()&&Diagnostics.dh(),"DH switch works alone");
        Features.set("portalDepthDiagnostics",true);
        check(Diagnostics.portalDepth(),"portal depth switch");
        System.setProperty("fan4compat.portalDepthDiagnostics","false");
        check(!Diagnostics.portalDepth(),"portal depth JVM argument wins");
        System.clearProperty("fan4compat.portalDepthDiagnostics");
        Features.reset(null);
        System.out.println("PASS: feature defaults, file round trip with unknown keys, JVM overrides, per-feature hook gating and diagnostic switches");
    }
}
