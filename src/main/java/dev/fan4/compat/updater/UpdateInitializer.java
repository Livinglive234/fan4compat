package dev.fan4.compat.updater;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Background release checks and staged installation after the current JVM exits. */
public final class UpdateInitializer implements ModInitializer {
    private static final System.Logger LOG=System.getLogger("Fan4Compat Updater");
    private final Object lock=new Object();
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Fan4Compat update check");t.setDaemon(true);return t;});
    private volatile String notice;
    private String announcedVersion;
    private volatile boolean stopping;
    private Path root,target,mods;private String current,previous;
    private Properties settings;
    public void onInitialize() {
        FabricLoader loader=FabricLoader.getInstance();
        try {
            Path config=loader.getConfigDir().resolve("fan4compat-updater.properties");settings=new Properties();
            settings.setProperty("enabled","true");settings.setProperty("autoDownload","true");settings.setProperty("allowPrereleases","true");
            if(Files.exists(config)){try(Reader reader=Files.newBufferedReader(config)){settings.load(reader);}}
            else{Files.createDirectories(config.getParent());try(Writer writer=Files.newBufferedWriter(config)){settings.store(writer,"Fan4Compat release updater; disable enabled to stop checks. Updates apply after exit. Backups are in .fan4compat-update/backups.");}}
            if(!Boolean.parseBoolean(settings.getProperty("enabled")))return;
            var container=loader.getModContainer("fan4compat").orElseThrow();current=container.getMetadata().getVersion().getFriendlyString();
            var origins=container.getOrigin().getPaths();mods=loader.getGameDir().resolve("mods").toRealPath();
            if(loader.isDevelopmentEnvironment()||origins.size()!=1||!Files.isRegularFile(origins.get(0),LinkOption.NOFOLLOW_LINKS))return;
            target=origins.get(0).toAbsolutePath().normalize();
            if(!target.getParent().toRealPath().equals(mods)||Files.isSymbolicLink(target)){
                LOG.log(System.Logger.Level.INFO,"Automatic updates require Fan4Compat to be a regular jar directly in mods");return;
            }
            target=target.getParent().toRealPath().resolve(target.getFileName());
            previous=UpdateInstaller.digest(target);root=loader.getGameDir().resolve(".fan4compat-update").toAbsolutePath();Files.createDirectories(root);
            prepareHelper();
            Runtime.getRuntime().addShutdownHook(new Thread(this::afterExit,"Fan4Compat update install launcher"));
            executor.scheduleWithFixedDelay(this::check,0,30,TimeUnit.MINUTES);
            if(loader.getEnvironmentType()==EnvType.CLIENT)executor.scheduleWithFixedDelay(this::notifyPlayer,5,10,TimeUnit.SECONDS);
        }catch(Exception error){LOG.log(System.Logger.Level.WARNING,"Updater unavailable; current mod remains installed: "+error.getMessage());executor.shutdownNow();}
    }
    private void prepareHelper()throws IOException {
        Path part=Files.createTempFile(root,"installer-",".part");
        try {
            try(JarOutputStream jar=new JarOutputStream(Files.newOutputStream(part));InputStream in=UpdateInstaller.class.getResourceAsStream("UpdateInstaller.class")) {
                if(in==null)throw new IOException("Installer class is missing");
                jar.putNextEntry(new JarEntry("dev/fan4/compat/updater/UpdateInstaller.class"));in.transferTo(jar);jar.closeEntry();
            }
            Files.move(part,root.resolve("installer.jar"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(part);}
    }
    private void check() {
        try {
            String baseline=current;
            Path pending=root.resolve("pending.properties");
            if(Files.exists(pending)){
                Properties p=new Properties();try(Reader in=Files.newBufferedReader(pending)){p.load(in);}
                if(ReleaseUpdates.compare(p.getProperty("version"),current)>0&&previous.equals(p.getProperty("previousSha256"))&&Files.exists(root.resolve("download.jar"))&&UpdateInstaller.digest(root.resolve("download.jar")).equals(p.getProperty("sha256"))){
                    if(!p.getProperty("version").equals(announcedVersion)){announcedVersion=p.getProperty("version");announce("Fan4Compat "+announcedVersion+" downloaded. Restart to apply the update.");}
                    baseline=p.getProperty("version");
                } else {
                    synchronized(lock){if(stopping)return;Files.deleteIfExists(pending);Files.deleteIfExists(root.resolve("download.jar"));}
                }
            }
            ReleaseUpdates service=new ReleaseUpdates();var release=service.check(baseline,Boolean.parseBoolean(settings.getProperty("allowPrereleases")));
            if(release==null||stopping)return;
            if(!Boolean.parseBoolean(settings.getProperty("autoDownload"))){announce("Fan4Compat "+release.version()+" is available on GitHub Releases.");return;}
            Path downloaded=service.download(release,root);
            try {
                synchronized(lock) {
                    if(stopping)return;
                    Files.move(downloaded,root.resolve("download.jar"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                    Properties p=new Properties();p.setProperty("version",release.version());p.setProperty("target",target.toString());p.setProperty("mods",mods.toString());p.setProperty("previousSha256",previous);p.setProperty("sha256",UpdateInstaller.digest(root.resolve("download.jar")));
                    Path part=Files.createTempFile(root,"pending-",".part");
                    try{try(Writer out=Files.newBufferedWriter(part)){p.store(out,"Verified Fan4Compat update for installation after exit");}Files.move(part,pending,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                    finally{Files.deleteIfExists(part);}
                }
                announcedVersion=release.version();
                announce("Fan4Compat "+release.version()+" downloaded. Restart to apply the update.");
            }finally{Files.deleteIfExists(downloaded);}
        }catch(InterruptedException stop){Thread.currentThread().interrupt();}
        catch(Exception error){if(!stopping)LOG.log(System.Logger.Level.WARNING,"Update check failed; current mod remains installed: "+error.getMessage());}
    }
    private void announce(String message){LOG.log(System.Logger.Level.INFO,message);notice=message;}
    private void notifyPlayer() {
        String message=notice;if(message==null||stopping)return;
        try {
            Object client=call(type("net.minecraft.class_310"),"method_1551");
            ((Executor)client).execute(()->{
                try{Object player=field(client,"field_1724");if(player==null||!message.equals(notice))return;
                    call(player,"method_7353",call(type("net.minecraft.class_2561"),"method_43470",message),false);notice=null;
                }catch(RuntimeException|LinkageError failure){notice=null;LOG.log(System.Logger.Level.DEBUG,"Update chat notice unavailable",failure);}
            });
        }catch(RuntimeException|LinkageError failure){notice=null;}
    }
    private void afterExit() {
        synchronized(lock){stopping=true;executor.shutdownNow();if(!Files.exists(root.resolve("pending.properties")))return;}
        try {
            String executable=System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")?"java.exe":"java";
            Path java=Path.of(System.getProperty("java.home"),"bin",executable);
            new ProcessBuilder(java.toString(),"-cp",root.resolve("installer.jar").toString(),UpdateInstaller.class.getName(),Long.toString(ProcessHandle.current().pid()),root.resolve("pending.properties").toString())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(root.resolve("installer.log").toFile())).start();
        }catch(IOException error){LOG.log(System.Logger.Level.WARNING,"Could not launch update installer; update remains pending: "+error.getMessage());}
    }
}
