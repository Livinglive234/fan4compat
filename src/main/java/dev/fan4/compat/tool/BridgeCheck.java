package dev.fan4.compat.tool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Immersive Portals declares in its fabric.mod.json that it breaks with every Sodium except 0.6.7 and every Iris except 1.8.1, so Fabric Loader refuses to start with
 * Sodium 0.8 and Iris 1.8.14. Fan4Compat's hooks for Sodium 0.8 only help once the game starts, so this finds that situation in a mods folder and can remove those two
 * entries from the Immersive Portals jar. It reads only jar files and uses nothing but the JDK, so it also runs before Fabric Loader (as a Java agent) and from the command line.
 */
public final class BridgeCheck {
    static final String STOCK_SODIUM="0.6.7",STOCK_IRIS="1.8.1";
    private static final String PORTALS="immersive_portals";

    /** A mod from the Sodium family with the version the stock (Sodium 0.6.7) pack uses. */
    public record Family(String id,String name,String stock,String url){}
    private static final String MODRINTH="https://modrinth.com/mod/";
    public static final List<Family> FAMILY=List.of(
        new Family("sodium","Sodium","0.6.7+mc1.21.1",MODRINTH+"sodium/version/AxSdcDcP"),new Family("iris","Iris","1.8.1+mc1.21.1",MODRINTH+"iris/version/IzngD3Cy"),
        new Family("sodium-extra","Sodium Extra","0.6.0+mc1.21.1",MODRINTH+"sodium-extra/version/anDafurZ"),
        new Family("reeses-sodium-options","Reese's Sodium Options","1.8.3+mc1.21.4",MODRINTH+"reeses-sodium-options/version/KoUrx3jJ"),
        new Family("moreculling","More Culling","1.0.6",MODRINTH+"moreculling/version/1V6UtDhN"));
    /** Sodium Options API is not installed with Sodium 0.8 (it breaks with it) and has to come back with Sodium 0.6.7. */
    public static final Family OPTIONS_API=new Family("sodiumoptionsapi","Sodium Options API","1.0.10",MODRINTH+"sodium-options-api/version/RR1lyQmh");

    public record Mod(Path file,String id,String version,Map<String,Object> breaks){}
    public record Report(Path mods,List<Mod> portals,Map<String,Mod> installed,boolean optionsApi) {
        /** Immersive Portals jars whose breaks entries the installed Sodium or Iris violate. */
        public List<Mod> blocked(){
            List<Mod> out=new ArrayList<>();
            for(Mod portal:portals)
                if(violates(portal.breaks().get("sodium"),installed.get("sodium"))||violates(portal.breaks().get("iris"),installed.get("iris")))out.add(portal);
            return out;
        }
        public boolean mismatch(){return !blocked().isEmpty();}
        public String version(String id){Mod mod=installed.get(id);return mod==null?"?":mod.version();}
    }

    private BridgeCheck() {}

    /** The part of a version before the first '+': 0.8.13+mc1.21.1 is 0.8.13. */
    static String core(String version){int plus=version.indexOf('+');return plus<0?version:version.substring(0,plus);}

    /** Immersive Portals pins one version: it breaks with "&lt;V" and "&gt;V". Anything but that version violates it. */
    private static boolean violates(Object breaks,Mod installed){
        if(breaks==null||installed==null)return false;
        if("*".equals(breaks))return true;
        String pinned=null;
        if(breaks instanceof List<?> list)for(Object entry:list)if(entry instanceof String s&&s.startsWith(">"))pinned=s.substring(1);
        return pinned!=null&&!core(installed.version()).equals(pinned);
    }

    public static Report scan(Path mods)throws IOException {
        List<Mod> portals=new ArrayList<>();
        Map<String,Mod> installed=new java.util.LinkedHashMap<>();
        boolean optionsApi=false;
        if(!Files.isDirectory(mods))return new Report(mods,portals,installed,false);
        List<Path> jars;
        try(Stream<Path> files=Files.list(mods)){jars=files.filter(p->p.getFileName().toString().endsWith(".jar")).sorted().toList();}
        for(Path jar:jars) {
            Mod mod=read(jar);
            if(mod==null)continue;
            if(mod.id().equals(PORTALS))portals.add(mod);
            else if(mod.id().equals("sodiumoptionsapi"))optionsApi=true;
            else for(Family family:FAMILY)if(family.id().equals(mod.id()))installed.put(mod.id(),mod);
        }
        return new Report(mods,portals,installed,optionsApi);
    }

    @SuppressWarnings("unchecked")
    private static Mod read(Path jar){
        try(ZipFile zip=new ZipFile(jar.toFile())) {
            ZipEntry entry=zip.getEntry("fabric.mod.json");
            if(entry==null)return null;
            Map<String,Object> json=(Map<String,Object>)MiniJson.parse(new String(zip.getInputStream(entry).readAllBytes(),StandardCharsets.UTF_8));
            Object id=json.get("id"),version=json.get("version"),breaks=json.get("breaks");
            if(!(id instanceof String)||!(version instanceof String))return null;
            return new Mod(jar,(String)id,(String)version,breaks instanceof Map<?,?> map?(Map<String,Object>)map:Map.of());
        } catch(IOException|RuntimeException unreadable) {
            return null;
        }
    }

    /** Where originals go: next to the mods folder, never inside it (Fabric Loader would load them). */
    public static Path backupDirectory(Path mods){Path parent=mods.toAbsolutePath().getParent();return (parent==null?mods:parent).resolve(".fan4compat-backup");}

    /**
     * Writes the Immersive Portals jar again without its Sodium and Iris breaks entries, checks the copy, moves the original to the backup folder and puts the copy
     * in its place under the same name. Returns the backup of the original.
     */
    @SuppressWarnings("unchecked")
    public static Path patch(Mod portal)throws IOException {
        Path jar=portal.file(),temp=jar.resolveSibling(jar.getFileName()+".fan4compat-tmp");
        try {
            try(ZipFile zip=new ZipFile(jar.toFile());OutputStream file=Files.newOutputStream(temp);ZipOutputStream out=new ZipOutputStream(file)) {
                for(var entries=zip.entries();entries.hasMoreElements();) {
                    ZipEntry source=entries.nextElement();
                    ZipEntry copy=new ZipEntry(source.getName());
                    if(source.getTime()!=-1)copy.setTime(source.getTime());
                    out.putNextEntry(copy);
                    if(!source.isDirectory()) {
                        try(InputStream in=zip.getInputStream(source)) {
                            if(source.getName().equals("fabric.mod.json")) {
                                Map<String,Object> json=(Map<String,Object>)MiniJson.parse(new String(in.readAllBytes(),StandardCharsets.UTF_8));
                                if(json.get("breaks") instanceof Map<?,?> breaks){breaks.remove("sodium");breaks.remove("iris");}
                                out.write(MiniJson.write(json).getBytes(StandardCharsets.UTF_8));
                            } else in.transferTo(out);
                        }
                    }
                    out.closeEntry();
                }
            }
            Mod patched=read(temp);
            if(patched==null||!patched.id().equals(PORTALS)||patched.breaks().containsKey("sodium")||patched.breaks().containsKey("iris"))
                throw new IOException("The patched copy of "+jar.getFileName()+" did not check out; nothing was changed");
            Path backups=backupDirectory(jar.getParent());
            Files.createDirectories(backups);
            Path backup=backups.resolve(jar.getFileName().toString());
            for(int n=2;Files.exists(backup);n++)backup=backups.resolve(jar.getFileName()+"."+n);
            Files.move(jar,backup);
            Files.move(temp,jar,StandardCopyOption.ATOMIC_MOVE);
            return backup;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // Texts.

    public static String message(Report report){
        return "Immersive Portals must be modified to work with Sodium "+core(report.version("sodium"))+" and Iris "+core(report.version("iris"))+". "
            +"Fan4Compat can modify your Immersive Portals jar for you, or you must downgrade your Sodium and Iris versions to "+STOCK_SODIUM+" and "+STOCK_IRIS+" respectively.\n\n"
            +"NOTE: Downgrading versions will require you to reinstall all Sodium related mods to a supported version.";
    }

    /** One line of the downgrade list: what to do, the file it concerns and where to get the right version. */
    public record Line(String name,String text,String url){}

    /** Every Sodium-family mod that is installed with the version it needs on Sodium 0.6.7, and the Sodium Options API that has to come back. */
    public static List<Line> downgradeLines(Report report){
        List<Line> lines=new ArrayList<>();
        for(Family family:FAMILY) {
            Mod mod=report.installed().get(family.id());
            if(mod!=null)lines.add(new Line(family.name(),mod.version()+"  ->  "+family.stock()+"   ("+mod.file().getFileName()+")",family.url()));
        }
        lines.add(new Line(OPTIONS_API.name(),report.optionsApi()?"installed, keep it ("+OPTIONS_API.stock()+")":"reinstall it ("+OPTIONS_API.stock()+"): it was removed for Sodium 0.8, which cannot run with it, and the old Sodium needs it",OPTIONS_API.url()));
        return lines;
    }
    static final List<String> NOTES=List.of("Any other mod that needs Sodium or Iris must be a build for Sodium 0.6.7 and Iris 1.8.1.",
        "Fabric Loader 0.20.1 is only needed for the new versions; Sodium 0.6.7 and Iris 1.8.1 run on Fabric Loader 0.19.5.");

    /** The downgrade list as plain text, with the Modrinth address after each entry. */
    public static List<String> downgradeList(Report report){
        List<String> out=new ArrayList<>();
        for(Line line:downgradeLines(report))out.add(line.name()+": "+line.text()+"\n    "+line.url());
        out.addAll(NOTES);
        return out;
    }

    /** The downgrade list as HTML for the window: each mod name links to the Modrinth page of the version it needs. */
    public static String downgradeHtml(Report report){
        StringBuilder out=new StringBuilder("<html><body style=\"width:520px\"><p><b>Mods to downgrade</b> (click a name to open the right version on Modrinth)</p><ul>");
        for(Line line:downgradeLines(report))out.append("<li><a href=\"").append(escape(line.url())).append("\">").append(escape(line.name())).append("</a>: ").append(escape(line.text())).append("</li>");
        out.append("</ul>");
        for(String note:NOTES)out.append("<p>").append(escape(note)).append("</p>");
        return out.append("</body></html>").toString();
    }
    static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
