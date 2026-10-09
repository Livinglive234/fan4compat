import dev.fan4.compat.updater.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.concurrent.*;

public final class UpdateTest {
    static String asset(String version,String filename){return "{\"name\":\""+filename+"\",\"browser_download_url\":\"https://github.com/Livinglive234/fan4compat/releases/download/v"+version+"/"+filename+"\"}";}
    static String release(String version,boolean prerelease,boolean draft){String name="Fan4Compat-"+version+".jar";return "{\"tag_name\":\"v"+version+"\",\"prerelease\":"+prerelease+",\"draft\":"+draft+",\"assets\":["+asset(version,name)+","+asset(version,name+".sha256")+"]}";}
    static void jar(Path path,String id,String version,String minecraft)throws IOException {
        try(JarOutputStream out=new JarOutputStream(Files.newOutputStream(path))){out.putNextEntry(new JarEntry("fabric.mod.json"));out.write(("{\"id\":\""+id+"\",\"version\":\""+version+"\",\"depends\":{\"minecraft\":\""+minecraft+"\"}}").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();}
    }
    record Fixture(Path root,Path mods,Path target,Path staged,Path pending,String oldHash,String newHash) {}
    static Fixture fixture(Path base)throws IOException {
        Path mods=Files.createDirectories(base.resolve("mods")),root=Files.createDirectories(base.resolve(".fan4compat-update"));
        Path old=mods.resolve("Fan4Compat-0.1.0-beta.12.jar"),staged=root.resolve("download.jar"),pending=root.resolve("pending.properties");
        jar(old,"fan4compat","0.1.0-beta.12","1.21.1");jar(staged,"fan4compat","0.1.0-beta.13","1.21.1");String previous=UpdateInstaller.digest(old),next=UpdateInstaller.digest(staged);
        Properties p=new Properties();p.setProperty("target",old.toString());p.setProperty("mods",mods.toString());p.setProperty("previousSha256",previous);p.setProperty("sha256",next);p.setProperty("version","0.1.0-beta.13");try(Writer w=Files.newBufferedWriter(pending)){p.store(w,"");}
        return new Fixture(root,mods,old,staged,pending,previous,next);
    }
    public static void main(String[] args)throws Exception {
        check(ReleaseUpdates.compare("0.1.0-beta.12","0.1.0-beta.9")>0,"numeric beta version");check(ReleaseUpdates.compare("0.1.0","0.1.0-rc.99")>0,"stable ordering");check(ReleaseUpdates.compare("0.2.0-alpha.1","0.1.0")>0,"core version first");
        String json="["+release("0.1.0-beta.13",true,false)+","+release("0.1.0-beta.99",true,true)+","+release("0.1.0",false,false)+"]";
        check(ReleaseUpdates.select(json,"0.1.0-beta.12",true).version().equals("0.1.0"),"highest usable release wins");
        check(ReleaseUpdates.select("["+release("0.1.0-beta.13",true,false)+"]","0.1.0-beta.12",false)==null,"prerelease opt-out");
        check(ReleaseUpdates.select("["+release("0.1.0-beta.13",true,false)+","+release("0.1.0-beta.14",true,false)+"]","0.1.0-beta.13",true).version().equals("0.1.0-beta.14"),"newer release supersedes staged baseline");
        check(ReleaseUpdates.select("["+release("0.1.0-beta.12",true,false)+"]","0.1.0-beta.12",true)==null,"no downgrade/reinstall");
        check(ReleaseUpdates.select("["+release("0.1.0-beta.13",true,false).replace(".jar.sha256",".txt")+"]","0.1.0-beta.12",true)==null,"unverified asset skipped");
        try{ReleaseUpdates.select(json.replace("https://github.com/","https://elsewhere.example/"),"0.1.0-beta.12",true);throw new AssertionError();}catch(IllegalArgumentException expected){}
        Path temp=Files.createTempDirectory("fan4compat-update-test-");
        try {
            Fixture f=fixture(temp.resolve("valid"));ReleaseUpdates.validate(f.staged,"0.1.0-beta.13",f.newHash);
            try{ReleaseUpdates.validate(f.staged,"0.1.0-beta.14",f.newHash);throw new AssertionError();}catch(IOException expected){}
            try{ReleaseUpdates.validate(f.staged,"0.1.0-beta.13","0".repeat(64));throw new AssertionError();}catch(IOException expected){}
            Path wrong=temp.resolve("wrong.jar");jar(wrong,"other_mod","0.1.0-beta.13","1.21.1");expectInvalid(wrong);jar(wrong,"fan4compat","0.1.0-beta.13","1.22");expectInvalid(wrong);
            check(UpdateInstaller.install(f.pending),"verified install");check(UpdateInstaller.digest(f.target).equals(f.newHash),"replacement bytes");check(UpdateInstaller.digest(f.root.resolve("backups").resolve(f.oldHash+".jar")).equals(f.oldHash),"original backup");check(!Files.exists(f.pending)&&!Files.exists(f.staged),"pending cleanup");
            try(var files=Files.list(f.mods)){check(files.count()==1,"no duplicate mods jar");}
            Fixture corrupt=fixture(temp.resolve("corrupt"));Files.writeString(corrupt.staged,"broken");expectRefused(corrupt);
            Fixture changed=fixture(temp.resolve("changed"));jar(changed.target,"fan4compat","0.1.0-beta.14","1.21.1");String hash=UpdateInstaller.digest(changed.target);expectRefused(changed);check(UpdateInstaller.digest(changed.target).equals(hash),"manual update not overwritten");
            Fixture linked=fixture(temp.resolve("linked"));Path real=temp.resolve("real.jar");Files.move(linked.target,real);Files.createSymbolicLink(linked.target,real);expectRefused(linked);check(UpdateInstaller.digest(real).equals(linked.oldHash),"symlink target untouched");
            Path helper=temp.resolve("installer.jar");try(JarOutputStream out=new JarOutputStream(Files.newOutputStream(helper));InputStream in=UpdateInstaller.class.getResourceAsStream("UpdateInstaller.class")){out.putNextEntry(new JarEntry("dev/fan4/compat/updater/UpdateInstaller.class"));in.transferTo(out);out.closeEntry();}
            Fixture isolated=fixture(temp.resolve("isolated"));String java=Path.of(System.getProperty("java.home"),"bin","java").toString();
            Process child=new ProcessBuilder(java,"-cp",helper.toString(),UpdateInstaller.class.getName(),Long.toString(ProcessHandle.current().pid()),isolated.pending.toString()).redirectErrorStream(true).start();
            try {
                try(var reader=new BufferedReader(new InputStreamReader(child.getInputStream()))){String line=CompletableFuture.supplyAsync(()->{try{return reader.readLine();}catch(IOException e){throw new UncheckedIOException(e);}}).get(10,TimeUnit.SECONDS);check(line!=null&&line.contains("Waiting for"),"standalone helper starts without Fabric/Gson/current mod jar");}
                check(child.isAlive()&&UpdateInstaller.digest(isolated.target).equals(isolated.oldHash),"helper waits while game process lives");
            }finally{child.destroyForcibly();child.waitFor(10,TimeUnit.SECONDS);}
            child=new ProcessBuilder(java,"-cp",helper.toString(),UpdateInstaller.class.getName(),"99999999",isolated.pending.toString()).redirectErrorStream(true).start();
            check(child.waitFor(10,TimeUnit.SECONDS)&&child.exitValue()==0,"standalone post-exit installer succeeds");check(UpdateInstaller.digest(isolated.target).equals(isolated.newHash),"post-exit replacement installed");
            Fixture prestart=fixture(temp.resolve("prestart"));child=new ProcessBuilder(java,"-cp",helper.toString(),UpdateInstaller.class.getName(),"--apply",prestart.pending.toString()).redirectErrorStream(true).start();
            check(child.waitFor(10,TimeUnit.SECONDS)&&child.exitValue()==0&&UpdateInstaller.digest(prestart.target).equals(prestart.newHash),"managed-host pre-start command installs standalone");
        } finally {try(var paths=Files.walk(temp)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
        System.out.println("PASS: release channels/version ordering/trusted URLs, checksum and mod metadata, atomic install/backup, no duplicates, corruption/manual/symlink refusal and standalone helper waiting until exit");
    }
    static void expectInvalid(Path jar)throws IOException{try{ReleaseUpdates.validate(jar,"0.1.0-beta.13",UpdateInstaller.digest(jar));throw new AssertionError();}catch(IOException expected){}}
    static void expectRefused(Fixture f)throws IOException{try{UpdateInstaller.install(f.pending);throw new AssertionError();}catch(IOException expected){check(Files.exists(f.pending),"failed install remains pending");}}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
