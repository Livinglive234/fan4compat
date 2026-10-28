package dev.fan4.compat.tool;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

/**
 * Entry points for running Sodium 0.8 with Immersive Portals: the jar's Main-Class (double-click it, or {@code java -jar}) and its Premain-Class (add
 * {@code -javaagent:mods/Fan4Compat-….jar} to the Java arguments so it runs before Fabric Loader). Both look in the mods folder for Immersive Portals jars that the
 * installed Sodium or Iris violate and offer to edit them, or to list what a downgrade to Sodium 0.6.7 means. With nothing to fix they do nothing.
 * Run as an agent the game goes on starting afterwards, so its messages say that instead of "start the game again". Arguments: {@code patch} edits the jars without asking, {@code list} prints the downgrade list, {@code nogui} uses the console, and a folder path names the mods folder.
 */
public final class BridgeTool {
    private BridgeTool() {}

    /** How a question reaches the player. */
    interface Prompter {
        /** 0 for "patch", 1 for "downgrade list", -1 for closed. */
        int ask(String message);
        void show(String title,String text);
        /** The downgrade list: plain text, or a window with links. */
        default void showList(BridgeCheck.Report report){show("Mods to downgrade",String.join("\n\n",BridgeCheck.downgradeList(report)));}
    }

    public static void main(String[] args){
        try{System.exit(run(args,false));}
        catch(Throwable failure){System.err.println("Fan4Compat: "+failure);System.exit(1);}
    }

    /** Runs before Fabric Loader. Never stops the launch: every failure is reported on stderr and ignored. */
    public static void premain(String args,Instrumentation instrumentation){
        try{run(args==null?new String[0]:args.split(","),true);}
        catch(Throwable failure){System.err.println("Fan4Compat: Sodium/Immersive Portals check failed: "+failure);}
    }

    public static int run(String[] args,boolean agent)throws IOException {
        boolean patch=false,list=false,nogui=false;
        Path folder=null;
        for(String arg:args) {
            switch(arg.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "patch"->patch=true;
                case "list"->list=true;
                case "nogui"->nogui=true;
                case ""->{}
                default->folder=Path.of(arg.trim());
            }
        }
        BridgeCheck.Report report=BridgeCheck.scan(folder!=null?folder:findMods());
        if(!report.mismatch()) {
            if(!agent)System.out.println("Fan4Compat: no Immersive Portals jar in "+report.mods()+" conflicts with the installed Sodium and Iris.");
            return 0;
        }
        Prompter prompter=nogui||GraphicsEnvironment.isHeadless()?new Console():isMac()&&agent?new MacDialog():new SwingDialog();
        if(list){prompter.showList(report);return 0;}
        if(patch)return patchAll(report,prompter,agent);
        while(true) {
            int choice=prompter.ask(BridgeCheck.message(report));
            if(choice==0)return patchAll(report,prompter,agent);
            if(choice==1){prompter.showList(report);continue;}
            return 0;
        }
    }

    /** As an agent the game goes on starting once this returns, so the player is told that; started by hand there is no game to continue and the player starts it. */
    private static int patchAll(BridgeCheck.Report report,Prompter prompter,boolean agent)throws IOException {
        StringBuilder done=new StringBuilder();
        for(BridgeCheck.Mod portal:report.blocked()) {
            Path backup=BridgeCheck.patch(portal);
            done.append(portal.file().getFileName()).append(" was successfully patched. The original is in ").append(backup.getParent()).append(".\n");
        }
        done.append(agent?"\nThe game will now continue starting.":"\nStart the game again.");
        if(report.optionsApi())done.append(agent?"\nSodium Options API is installed and Sodium 0.8 cannot run with it: the game will stop with an error until you remove it from the mods folder.":"\nSodium Options API is installed and Sodium 0.8 cannot run with it: remove it from the mods folder.");
        done.append(agent?"\nSodium 0.8 also needs Fabric Loader 0.20.1 or newer; on an older loader the game will stop with an error.":"\nSodium 0.8 also needs Fabric Loader 0.20.1 or newer.");
        prompter.show("Immersive Portals patched",done.toString());
        return 0;
    }

    /** The mods folder next to this jar, or ./mods when this jar is run from somewhere else. */
    private static Path findMods()throws IOException {
        try {
            Path self=Path.of(BridgeTool.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path beside=Files.isRegularFile(self)?self.getParent():null;
            if(beside!=null&&BridgeCheck.scan(beside).portals().size()>0)return beside;
        } catch(URISyntaxException|RuntimeException ignored) {}
        return Path.of("mods").toAbsolutePath();
    }

    private static boolean isMac(){return System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT).contains("mac");}

    private static final class Console implements Prompter {
        public int ask(String message){System.err.println("Fan4Compat: "+message+"\n(run with the argument patch to edit the jar, or list for the downgrade list)");return -1;}
        public void show(String title,String text){System.out.println(title+":\n"+text);}
    }

    private static final class SwingDialog implements Prompter {
        SwingDialog(){try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}}
        public int ask(String message){
            return JOptionPane.showOptionDialog(null,label(message),"Fan4Compat",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,
                new Object[]{"Patch Immersive Portals","Show downgrade list"},"Patch Immersive Portals");
        }
        public void show(String title,String text){JOptionPane.showMessageDialog(null,label(text),title,JOptionPane.INFORMATION_MESSAGE);}
        public void showList(BridgeCheck.Report report){
            JEditorPane pane=new JEditorPane("text/html",BridgeCheck.downgradeHtml(report));
            pane.setEditable(false);pane.setOpaque(false);
            pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
            pane.setFont(font());
            pane.addHyperlinkListener(event->{
                if(event.getEventType()!=HyperlinkEvent.EventType.ACTIVATED||!Desktop.isDesktopSupported()||!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))return;
                try{Desktop.getDesktop().browse(event.getURL().toURI());}catch(Exception ignored){}
            });
            JScrollPane scroll=new JScrollPane(pane);
            scroll.setBorder(null);scroll.setPreferredSize(new Dimension(580,320));
            JOptionPane.showMessageDialog(null,scroll,"Mods to downgrade",JOptionPane.INFORMATION_MESSAGE);
        }
        private static Font font(){Font font=UIManager.getFont("Label.font");return font!=null?font:new Font(Font.SANS_SERIF,Font.PLAIN,13);}
        /** Wrapped text in the dialog font: an HTML label has a width of its own, so the dialog is sized to fit the text and the buttons. */
        private static JLabel label(String text){
            JLabel label=new JLabel("<html><body style=\"width:460px\">"+BridgeCheck.escape(text).replace("\n","<br>")+"</body></html>");
            label.setFont(font());
            return label;
        }
    }

    /** AWT cannot be used in the game's process on macOS (the main thread belongs to GLFW), so the agent asks through AppleScript. */
    private static final class MacDialog implements Prompter {
        public int ask(String message){
            String out=script("display dialog (item 1 of argv) with title \"Fan4Compat\" buttons {\"Close\",\"Show downgrade list\",\"Patch Immersive Portals\"} default button \"Patch Immersive Portals\" with icon caution",message);
            return out.contains("Patch Immersive Portals")?0:out.contains("Show downgrade list")?1:-1;
        }
        public void show(String title,String text){script("display dialog (item 1 of argv) with title (item 2 of argv) buttons {\"OK\"} default button \"OK\"",text,title);}
        private static String script(String body,String... arguments){
            try {
                List<String> command=new java.util.ArrayList<>(List.of("osascript","-e","on run argv","-e",body,"-e","end run","--"));
                command.addAll(List.of(arguments));
                Process process=new ProcessBuilder(command).redirectErrorStream(true).start();
                String out=new String(process.getInputStream().readAllBytes());
                process.waitFor(10,TimeUnit.MINUTES);
                return out;
            } catch(IOException|InterruptedException failure) {
                return "";
            }
        }
    }
}
