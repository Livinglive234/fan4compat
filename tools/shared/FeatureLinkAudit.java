import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/** Require each packaged addon class to be linked to an entrypoint or configured hook. */
public final class FeatureLinkAudit {
    static final Pattern CLASS=Pattern.compile("dev(?:/|\\.)fan4(?:/|\\.)compat(?:/|\\.)[A-Za-z0-9_/$\\.]+");
    static final Pattern MIXIN=Pattern.compile("\"([a-z]+\\.(?:common|client)\\.[A-Za-z0-9]+)\"");
    public static void main(String[] args)throws Exception {
        Map<String,String> classes=new TreeMap<>();Set<String> roots=new HashSet<>();
        try(ZipFile jar=new ZipFile(args[0])) {
            for(var entry:Collections.list(jar.entries()))if(entry.getName().startsWith("dev/fan4/compat/")&&entry.getName().endsWith(".class"))
                classes.put(entry.getName().substring(0,entry.getName().length()-6),new String(jar.getInputStream(entry).readAllBytes(),StandardCharsets.ISO_8859_1));
            String config=new String(jar.getInputStream(jar.getEntry("fan4compat.mixins.json")).readAllBytes(),StandardCharsets.UTF_8);
            Matcher mixins=MIXIN.matcher(config);while(mixins.find())roots.add("dev/fan4/compat/mixin/"+mixins.group(1).replace('.','/'));
            for(String resource:new String[]{"fabric.mod.json","fan4compat.mixins.json"}) {
                Matcher names=CLASS.matcher(new String(jar.getInputStream(jar.getEntry(resource)).readAllBytes(),StandardCharsets.UTF_8));
                while(names.find()){String name=names.group().replace('.','/');if(classes.containsKey(name))roots.add(name);}
            }
        }
        for(String name:classes.keySet())if(name.startsWith("dev/fan4/compat/mixin/")&&!roots.contains(name))throw new AssertionError("Unconfigured generated hook: "+name);
        Set<String> linked=new HashSet<>();Deque<String> pending=new ArrayDeque<>(roots);
        while(!pending.isEmpty()) {
            String name=pending.removeFirst();if(!linked.add(name))continue;
            String contents=classes.get(name);if(contents==null)throw new AssertionError("Missing feature root: "+name);
            Matcher references=CLASS.matcher(contents);while(references.find()) {
                String target=references.group().replace('.','/');if(classes.containsKey(target)&&!linked.contains(target))pending.add(target);
            }
        }
        Set<String> orphaned=new TreeSet<>(classes.keySet());orphaned.removeAll(linked);
        if(!orphaned.isEmpty())throw new AssertionError("Packaged classes without feature links: "+orphaned);
        for(String forbidden:new String[]{"MovementDiagnostics","MovementDiagnosticMixin","MovementTerrainDiagnosticMixin","ShipGuardDiagnosticMixin"})
            if(classes.keySet().stream().anyMatch(name->name.contains(forbidden)))throw new AssertionError("Retired diagnostic retained: "+forbidden);
        System.out.println("PASS: all "+classes.size()+" packaged addon classes link to configured hooks/entrypoints; retired diagnostics absent");
    }
}
