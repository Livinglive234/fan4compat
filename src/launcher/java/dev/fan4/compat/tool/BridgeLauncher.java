package dev.fan4.compat.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * The jar's Main-Class, compiled for Java 8. Double-clicking a .jar runs it with whatever Java the operating system has registered for .jar files, which is often
 * Java 8 on a machine that plays Minecraft with Java 21 (the registered Java is not the game's), and Java 8 cannot load the rest of Fan4Compat. This class only needs
 * Java 8: on Java 17 or newer it hands over to {@link BridgeTool}, otherwise it starts this jar again with a newer Java it finds on the machine, and failing that
 * says what is needed. It never touches the game; the game starts through the Premain-Class on its own Java.
 */
public final class BridgeLauncher {
    private static final int NEEDED=17;

    private BridgeLauncher() {}

    public static void main(String[] args)throws Exception {
        if(major(System.getProperty("java.specification.version",""))>=NEEDED) {
            Class.forName("dev.fan4.compat.tool.BridgeTool").getMethod("main",String[].class).invoke(null,(Object)args);
            return;
        }
        File java=findJava();
        File self=self();
        if(java!=null&&self!=null) {
            List<String> command=new ArrayList<String>();
            command.add(java.getPath());command.add("-jar");command.add(self.getPath());
            for(String arg:args)command.add(arg);
            System.exit(new ProcessBuilder(command).inheritIO().start().waitFor());
            return;
        }
        String message="Fan4Compat needs Java "+NEEDED+" or newer to check your mods, but this .jar file is set to open with Java "+System.getProperty("java.specification.version")+".\n\n"
            +"Run it with the Java your game uses instead, for example:\n    java -jar \""+(self==null?"Fan4Compat.jar":self.getPath())+"\"\n"
            +"(Prism: the Java folder is shown under Settings, Java.) Or add -javaagent:mods/"+(self==null?"Fan4Compat.jar":self.getName())+" to the instance's Java arguments.";
        System.err.println(message);
        try{JOptionPane.showMessageDialog(null,message,"Fan4Compat",JOptionPane.WARNING_MESSAGE);}catch(Throwable noDisplay){}
    }

    private static File self(){
        try{return new File(BridgeLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());}
        catch(URISyntaxException unusable){return null;}
        catch(RuntimeException unusable){return null;}
    }

    /** 1.8 is 8; 17 and 21 are themselves. */
    static int major(String specification){
        try{
            String[] parts=specification.split("\\.");
            int first=Integer.parseInt(parts[0]);
            return first==1&&parts.length>1?Integer.parseInt(parts[1]):first;
        } catch(RuntimeException unreadable) {
            return 0;
        }
    }

    /** The newest Java of version 17 or newer in the places Java usually is and where Prism keeps the ones it downloads; javaw on Windows, so no console opens. */
    private static File findJava(){
        boolean windows=System.getProperty("os.name","").toLowerCase().contains("win");
        List<File> roots=new ArrayList<File>();
        String home=System.getenv("JAVA_HOME");
        if(home!=null)roots.add(new File(home));
        String appData=System.getenv("APPDATA"),programFiles=System.getenv("ProgramFiles");
        if(appData!=null)addChildren(roots,new File(appData,"PrismLauncher/java"));
        if(programFiles!=null)for(String vendor:new String[]{"Java","Eclipse Adoptium","Microsoft","Zulu","Amazon Corretto","BellSoft"})addChildren(roots,new File(programFiles,vendor));
        String user=System.getProperty("user.home","");
        addChildren(roots,new File(user,"Library/Application Support/PrismLauncher/java"));
        addChildren(roots,new File("/Library/Java/JavaVirtualMachines"));
        addChildren(roots,new File("/usr/lib/jvm"));
        addChildren(roots,new File(user,".local/share/PrismLauncher/java"));
        File best=null;int bestVersion=0;
        for(File root:roots) {
            File[] candidates={root,new File(root,"Contents/Home")};
            for(File candidate:candidates) {
                File executable=new File(candidate,windows?"bin/javaw.exe":"bin/java");
                int version=releaseOf(candidate);
                if(executable.isFile()&&version>=NEEDED&&version>bestVersion){best=executable;bestVersion=version;}
            }
        }
        return best;
    }
    private static void addChildren(List<File> into,File folder){
        File[] children=folder.listFiles();
        if(children!=null)for(File child:children)if(child.isDirectory())into.add(child);
    }
    /** The major version in the JAVA_VERSION line of a Java home's release file, 0 when there is none. */
    private static int releaseOf(File javaHome){
        File release=new File(javaHome,"release");
        if(!release.isFile())return 0;
        BufferedReader reader=null;
        try {
            reader=new BufferedReader(new FileReader(release));
            for(String line=reader.readLine();line!=null;line=reader.readLine()) {
                if(line.startsWith("JAVA_VERSION=")) return major(line.substring("JAVA_VERSION=".length()).replace("\"","").trim());
            }
        } catch(IOException unreadable) {
            return 0;
        } finally {
            if(reader!=null)try{reader.close();}catch(IOException ignored){}
        }
        return 0;
    }
}
