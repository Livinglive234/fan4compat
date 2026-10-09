package dev.fan4.compat.updater;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Standalone shutdown helper: JDK classes only, never loads the running mod jar. */
public final class UpdateInstaller {
    private UpdateInstaller() {}
    public static String digest(Path file)throws IOException {
        try {
            MessageDigest hash=MessageDigest.getInstance("SHA-256");
            try(InputStream in=Files.newInputStream(file)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)hash.update(buffer,0,n);}
            return HexFormat.of().formatHex(hash.digest());
        }catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    public static boolean install(Path pending)throws IOException {
        Properties p=new Properties();try(Reader in=Files.newBufferedReader(pending)){p.load(in);}
        Path root=pending.toAbsolutePath().getParent().toRealPath();
        Path mods=Path.of(p.getProperty("mods")).toRealPath();
        Path target=Path.of(p.getProperty("target")).toAbsolutePath().normalize();
        Path staged=root.resolve("download.jar");
        if(!target.getParent().equals(mods)||Files.isSymbolicLink(target)||!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))throw new IOException("Update target is not the original mods jar");
        String wanted=p.getProperty("sha256"),previous=p.getProperty("previousSha256");
        if(!digest(staged).equals(wanted))throw new IOException("Staged update checksum mismatch");
        if(digest(target).equals(wanted)){Files.deleteIfExists(pending);Files.deleteIfExists(staged);return false;}
        if(!digest(target).equals(previous))throw new IOException("Installed jar changed since staging; update left pending");
        Path backup=root.resolve("backups").resolve(previous+".jar");Files.createDirectories(backup.getParent());
        if(!Files.exists(backup))Files.copy(target,backup);
        if(!digest(backup).equals(previous))throw new IOException("Backup checksum mismatch");
        Path replacement=Files.createTempFile(mods,".fan4compat-update-",".part");
        try {
            Files.copy(staged,replacement,StandardCopyOption.REPLACE_EXISTING);
            if(!digest(replacement).equals(wanted))throw new IOException("Replacement checksum mismatch");
            // Never delete the current jar first, and never fall back to a non-atomic replacement.
            Files.move(replacement,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(pending);Files.deleteIfExists(staged);return true;
        }finally{Files.deleteIfExists(replacement);}
    }
    public static void main(String[] args)throws Exception {
        if(args.length==2&&args[0].equals("--apply")){System.out.println(install(Path.of(args[1]))?"Fan4Compat update installed":"Fan4Compat update already installed");return;}
        long parent=Long.parseLong(args[0]);Path pending=Path.of(args[1]);
        System.out.println("Waiting for running game process "+parent+" to exit");
        // The parent may still hold zip handles while its shutdown hooks are running.
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.MINUTES.toNanos(3);
        while(ProcessHandle.of(parent).map(ProcessHandle::isAlive).orElse(false)){
            if(System.nanoTime()>deadline)throw new IOException("Game has not exited; update remains pending");
            Thread.sleep(250);
        }
        IOException last=null;
        for(int attempt=0;attempt<10;attempt++){
            try{System.out.println(install(pending)?"Fan4Compat update installed; backup saved":"Fan4Compat update already installed");return;}
            catch(IOException failure){last=failure;Thread.sleep(1000);}
        }
        throw last;
    }
}
