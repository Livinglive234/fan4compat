import dev.fan4.compat.tool.BridgeCheck;
import dev.fan4.compat.tool.BridgeTool;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class BridgeToolTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}

    static void jar(Path mods,String file,String json,String... extra)throws Exception {
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(mods.resolve(file)))) {
            out.putNextEntry(new ZipEntry("fabric.mod.json"));out.write(json.getBytes(StandardCharsets.UTF_8));out.closeEntry();
            for(int i=0;i+1<extra.length;i+=2){out.putNextEntry(new ZipEntry(extra[i]));out.write(extra[i+1].getBytes(StandardCharsets.UTF_8));out.closeEntry();}
        }
    }
    static String mod(String id,String version){return "{\"schemaVersion\":1,\"id\":\""+id+"\",\"version\":\""+version+"\"}";}
    static final String PORTALS="{\n  \"schemaVersion\": 1,\n  \"id\": \"immersive_portals\",\n  \"version\": \"6.0.6\",\n  \"depends\": {\"fabricloader\": \">=0.16\"},\n"
        +"  \"breaks\": {\"optifabric\": \"*\", \"pehkui\": \"<3.4.1\", \"iris\": [\"<1.8.1\", \">1.8.1\"], \"sodium\": [\"<0.6.7\", \">0.6.7\"], \"vmp\": \"*\"},\n  \"custom\": {\"note\": \"caf\\u00e9 \\\"quoted\\\"\"}\n}";

    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("bridge"),mods=Files.createDirectories(root.resolve("minecraft/mods"));
        // Nothing to fix: no Immersive Portals, or Sodium and Iris it is built for.
        check(!BridgeCheck.scan(mods).mismatch(),"an empty mods folder has no mismatch");
        jar(mods,"immersive-portals.jar",PORTALS);
        jar(mods,"sodium.jar",mod("sodium","0.6.7+mc1.21.1"));
        jar(mods,"iris.jar",mod("iris","1.8.1+mc1.21.1"));
        check(!BridgeCheck.scan(mods).mismatch(),"Sodium 0.6.7 and Iris 1.8.1 are what Immersive Portals is built for");
        Files.delete(mods.resolve("sodium.jar"));Files.delete(mods.resolve("iris.jar"));
        check(!BridgeCheck.scan(mods).mismatch(),"without Sodium and Iris there is nothing to violate");
        // Sodium 0.8 and Iris 1.8.14 with the unedited jar.
        jar(mods,"sodium.jar",mod("sodium","0.8.13+mc1.21.1"));
        jar(mods,"iris.jar",mod("iris","1.8.14-beta.1+mc1.21.1"));
        jar(mods,"sodium-extra.jar",mod("sodium-extra","0.9.5+mc1.21.1"));
        jar(mods,"culling.jar",mod("moreculling","1.0.10"));
        jar(mods,"notes.jar","not json");
        BridgeCheck.Report report=BridgeCheck.scan(mods);
        check(report.mismatch()&&report.blocked().size()==1,"Sodium 0.8.13 and Iris 1.8.14 violate the unedited jar");
        String message=BridgeCheck.message(report);
        check(message.startsWith("Immersive Portals must be modified to work with Sodium 0.8.13 and Iris 1.8.14-beta.1.")&&message.contains("0.6.7 and 1.8.1 respectively")&&message.contains("reinstall all Sodium related mods"),"the dialog text: "+message);
        String list=String.join("\n",BridgeCheck.downgradeList(report));
        check(list.contains("Sodium: 0.8.13+mc1.21.1  ->  0.6.7+mc1.21.1")&&list.contains("https://modrinth.com/mod/sodium/version/AxSdcDcP")&&list.contains("Iris: 1.8.14-beta.1+mc1.21.1  ->  1.8.1+mc1.21.1")
            &&list.contains("https://modrinth.com/mod/iris/version/IzngD3Cy")&&list.contains("Sodium Extra: 0.9.5+mc1.21.1  ->  0.6.0+mc1.21.1")&&list.contains("https://modrinth.com/mod/sodium-extra/version/anDafurZ")
            &&list.contains("More Culling: 1.0.10  ->  1.0.6")&&list.contains("https://modrinth.com/mod/moreculling/version/1V6UtDhN")&&list.contains("Sodium Options API: reinstall")&&list.contains("https://modrinth.com/mod/sodium-options-api/version/RR1lyQmh")&&list.contains("run on Fabric Loader 0.19.5"),"the downgrade list: "+list);
        check(!list.contains("Reese"),"only installed mods are listed");
        String html=BridgeCheck.downgradeHtml(report);
        check(html.contains("<a href=\"https://modrinth.com/mod/sodium/version/AxSdcDcP\">Sodium</a>")&&html.contains("sodium-options-api/version/RR1lyQmh")&&!html.contains("reeses-sodium-options"),"the window lists each installed mod as a link: "+html);
        // An agent or double-click run with the console does nothing but report when it is not told to patch.
        PrintStream out=System.err;ByteArrayOutputStream captured=new ByteArrayOutputStream();System.setErr(new PrintStream(captured));
        try{check(BridgeTool.run(new String[]{"nogui",mods.toString()},true)==0,"console run returns");}finally{System.setErr(out);}
        check(captured.toString().contains("Immersive Portals must be modified")&&BridgeCheck.scan(mods).mismatch(),"the console run only reports");
        // Patching.
        jar(mods,"immersive-portals.jar",PORTALS,"qouteall/Keep.class","class bytes","META-INF/x.txt","kept");
        PrintStream stdout=System.out;ByteArrayOutputStream said=new ByteArrayOutputStream();System.setOut(new PrintStream(said));
        try{check(BridgeTool.run(new String[]{"nogui","patch",mods.toString()},true)==0,"patch run returns");}finally{System.setOut(stdout);}
        check(said.toString().contains("was successfully patched.")&&said.toString().contains("The game will now continue starting.")&&!said.toString().contains("Start the game again"),"as an agent the game continues by itself: "+said);
        BridgeCheck.Report after=BridgeCheck.scan(mods);
        check(!after.mismatch()&&after.portals().size()==1,"after patching nothing conflicts");
        try(ZipFile zip=new ZipFile(mods.resolve("immersive-portals.jar").toFile())) {
            String json=new String(zip.getInputStream(zip.getEntry("fabric.mod.json")).readAllBytes(),StandardCharsets.UTF_8);
            check(!json.contains("\"iris\"")&&!json.contains("\"sodium\"")&&json.contains("optifabric")&&json.contains("pehkui")&&json.contains("vmp"),"only the iris and sodium entries are gone: "+json);
            check(json.contains("\"fabricloader\": \">=0.16\"")&&json.contains("caf\\u00e9")==false&&json.contains("café \\\"quoted\\\""),"everything else, including escapes, survives: "+json);
            check(new String(zip.getInputStream(zip.getEntry("qouteall/Keep.class")).readAllBytes(),StandardCharsets.UTF_8).equals("class bytes")&&zip.getEntry("META-INF/x.txt")!=null,"the other entries are copied");
        }
        Path backups=root.resolve("minecraft/.fan4compat-backup").getParent().resolve(".fan4compat-backup");
        check(BridgeCheck.backupDirectory(mods).equals(root.resolve("minecraft").resolve(".fan4compat-backup"))&&Files.exists(backups.resolve("immersive-portals.jar")),"the original is kept next to the mods folder, not in it");
        try(var files=Files.list(mods)){check(files.noneMatch(p->p.getFileName().toString().endsWith(".fan4compat-tmp")),"no temporary file is left");}
        check(BridgeCheck.scan(backups).portals().size()==1,"the backup is the unedited jar");
        // A second patch run does nothing; a second conflict gets its own backup name.
        check(BridgeTool.run(new String[]{"nogui","patch",mods.toString()},true)==0&&BridgeCheck.scan(mods).portals().size()==1,"patching again changes nothing");
        jar(mods,"immersive-portals.jar",PORTALS);
        said.reset();System.setOut(new PrintStream(said));
        try{BridgeTool.run(new String[]{"nogui","patch",mods.toString()},false);}finally{System.setOut(stdout);}
        check(said.toString().contains("Start the game again.")&&!said.toString().contains("continue starting"),"started by hand, the player starts the game: "+said);
        check(Files.exists(backups.resolve("immersive-portals.jar.2")),"a second backup does not overwrite the first");
        System.out.println("PASS: bridge tool (conflict detection with Sodium 0.8 / Iris 1.8.14, dialog text, downgrade list, no action without a conflict, patch keeps everything but the two entries, original backed up outside mods, no temp files)");
    }
}
