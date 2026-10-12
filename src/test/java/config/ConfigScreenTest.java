import dev.fan4.compat.config.ConfigScreen;
import dev.fan4.compat.shared.Features;
import dev.fan4.compat.updater.UpdaterSettings;
import dev.isxander.yacl3.api.*;
import java.nio.file.*;
import java.util.*;

public final class ConfigScreenTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("fan4screen"),file=dir.resolve("fan4compat.properties");
        Features.reset(file);UpdaterSettings.reset(dir.resolve("fan4compat-updater.properties"));
        check(ConfigScreen.available(),"fixture library detected");
        Object parent=new Object();
        YetAnotherConfigLib lib=(YetAnotherConfigLib)ConfigScreen.open(parent);
        check(lib.parent==parent&&lib.title.text.equals("Fan4Compat"),"screen built over the parent with a title");
        List<String> categories=lib.categories.stream().map(c->c.name.text).toList();
        check(categories.equals(List.of("Rendering","Ships & Portals","Pack integrations","Troubleshooting")),"categories in registry order: "+categories);
        check(lib.categories.get(0).options.get(0) instanceof LabelOption&&lib.categories.get(0).options.get(1) instanceof ButtonOption,"restart notice and updater button lead the first category");
        List<Option<?>> options=new ArrayList<>();
        for(var category:lib.categories)for(Object o:category.options)if(!(o instanceof LabelOption)&&!(o instanceof ButtonOption))options.add((Option<?>)o);
        check(options.size()==Features.ALL.size(),"one tick box per feature");
        for(int i=0;i<options.size();i++) {
            @SuppressWarnings("unchecked") Option<Boolean> option=(Option<Boolean>)options.get(i);
            Features.Feature feature=Features.ALL.get(i);
            check(option.name.text.equals(feature.label()),"label for "+feature.id());
            check(option.description.paragraphs.get(0).equals(feature.description()),"description for "+feature.id());
            check(option.description.paragraphs.stream().anyMatch(p->p.contains(feature.needsRestart()?"restarting":"immediately")),"restart note for "+feature.id());
            check(option.defaultValue==feature.defaultValue()&&option.getter.get()==feature.defaultValue(),"default and current value for "+feature.id());
            check(option.controller instanceof dev.isxander.yacl3.api.controller.TickBoxControllerBuilder,"tick box for "+feature.id());
        }
        // Without Fabric's loader every mod looks absent: the toggle is greyed with its requirement spelled out.
        @SuppressWarnings("unchecked") Option<Boolean> first=(Option<Boolean>)options.get(0);
        check(!first.available&&first.description.paragraphs.stream().anyMatch(p->p.startsWith("Requires: iris")),"missing mods grey out the option and name what is needed");

        // Saving writes through to the properties file.
        @SuppressWarnings("unchecked") Option<Boolean> wings=(Option<Boolean>)options.stream().filter(o->o.name.text.startsWith("Elytra")).findFirst().orElseThrow();
        wings.setter.accept(false);check(!Features.enabled("wingTrails"),"setter stages the value");
        check(!Files.exists(file),"nothing written before save");
        lib.save.run();
        check(Files.readString(file).contains("wingTrails=false"),"save persists the value");

        // The updater has its own screen, opened from a button, layered over the current screen.
        ButtonOption button=(ButtonOption)lib.categories.get(0).options.get(1);
        check(button.name.text.equals("Updater settings")&&button.text.text.equals("Open"),"updater button labelled");
        Object current=new Object();button.action.accept(current);
        YetAnotherConfigLib updater=(YetAnotherConfigLib)net.minecraft.class_310.INSTANCE.screen;
        check(updater!=null&&updater.parent==current&&updater.title.text.equals("Fan4Compat updates"),"button opens the updater screen over the current one");
        check(updater.categories.size()==1&&updater.categories.get(0).name.text.equals("Updates")&&updater.categories.get(0).options.get(0) instanceof LabelOption,"one Updates category with a notice");
        List<Option<Boolean>> toggles=new ArrayList<>();
        for(Object o:updater.categories.get(0).options)if(!(o instanceof LabelOption)){@SuppressWarnings("unchecked") Option<Boolean> t=(Option<Boolean>)o;toggles.add(t);}
        check(toggles.size()==UpdaterSettings.ALL.size()&&toggles.stream().allMatch(t->t.available&&t.getter.get()&&t.defaultValue),"every updater switch present, available and on by default");
        check(toggles.get(1).name.text.equals("Download updates automatically"),"switch labels follow the registry");
        Path updaterFile=dir.resolve("fan4compat-updater.properties");
        Files.writeString(updaterFile,"enabled=true\nextra=keep\n");UpdaterSettings.reset(updaterFile);
        button.action.accept(current);updater=(YetAnotherConfigLib)net.minecraft.class_310.INSTANCE.screen;
        @SuppressWarnings("unchecked") Option<Boolean> auto=(Option<Boolean>)updater.categories.get(0).options.get(2);
        auto.setter.accept(false);check(!Files.readString(updaterFile).contains("autoDownload=false"),"nothing written before save");
        updater.save.run();
        String written=Files.readString(updaterFile);
        check(written.contains("autoDownload=false")&&written.contains("extra=keep")&&written.contains("enabled=true")&&written.contains("allowPrereleases=true"),"updater save keeps other keys and the remaining switches");
        UpdaterSettings.reset(null);Features.reset(null);
        System.out.println("PASS: settings screens (features and updater), reflective overload choice, defaults, requirement hints and saves (YACL fixtures)");
    }
}
