package dev.fan4.compat.config;

import dev.fan4.compat.shared.Features;
import dev.fan4.compat.updater.UpdaterSettings;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;
import net.fabricmc.loader.api.FabricLoader;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * Builds the settings screen with Yet Another Config Lib when it is installed. Everything goes
 * through reflection, so Fan4Compat has no dependency on it and simply has no screen without it.
 */
public final class ConfigScreen {
    private static final String YACL="dev.isxander.yacl3.api.",TEXT="net.minecraft.class_2561";
    private ConfigScreen() {}
    public static boolean available(){return present(YACL+"YetAnotherConfigLib");}
    /** The settings screen over {@code parent}, or {@code parent} itself when it cannot be built. */
    public static Object open(Object parent) {
        if(!available())return parent;
        try{return call(build(),"generateScreen",parent);}
        catch(RuntimeException|LinkageError failure) {
            System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING,"Could not open the Fan4Compat settings screen",failure);
            return parent;
        }
    }
    static Object build() {
        Object builder=call(type(YACL+"YetAnotherConfigLib"),"createBuilder");
        builder=call(builder,"title",text("Fan4Compat"));
        Map<String,List<Object>> byCategory=new LinkedHashMap<>();
        for(Features.Feature feature:Features.ALL)byCategory.computeIfAbsent(feature.category(),unused->new ArrayList<>()).add(option(feature));
        boolean first=true;
        for(var entry:byCategory.entrySet()) {
            Object category=call(type(YACL+"ConfigCategory"),"createBuilder");
            category=call(category,"name",text(entry.getKey()));
            if(first) {
                category=call(category,"option",label("Most changes apply after you restart the game. Options for mods you do not have installed are greyed out."));
                category=call(category,"option",updaterButton());
            }
            first=false;
            category=call(category,"options",entry.getValue());
            builder=call(builder,"category",call(category,"build"));
        }
        builder=call(builder,"save",(Runnable)Features::save);
        return call(builder,"build");
    }
    /** The updater has its own file and runs once at startup, so it gets its own screen. */
    static Object buildUpdater() {
        Object category=call(call(type(YACL+"ConfigCategory"),"createBuilder"),"name",text("Updates"));
        category=call(category,"option",label("Fan4Compat checks GitHub for new releases. Updates install when you quit the game; these settings apply at the next launch."));
        List<Object> options=new ArrayList<>();
        for(UpdaterSettings.Setting setting:UpdaterSettings.ALL)
            options.add(toggle(setting.label(),setting.description(),true,()->UpdaterSettings.get(setting.key()),value->UpdaterSettings.set(setting.key(),value),true));
        category=call(category,"options",options);
        Object builder=call(call(type(YACL+"YetAnotherConfigLib"),"createBuilder"),"title",text("Fan4Compat updates"));
        builder=call(builder,"category",call(category,"build"));
        builder=call(builder,"save",(Runnable)UpdaterSettings::save);
        return call(builder,"build");
    }
    private static Object updaterButton() {
        Object button=call(type(YACL+"ButtonOption"),"createBuilder");
        button=call(button,"name",text("Updater settings"));
        button=call(button,"description",call(type(YACL+"OptionDescription"),"of",texts("Open the automatic update settings.")));
        button=call(button,"text",text("Open"));
        button=call(button,"action",(Consumer<Object>)current->{
            try{call(call(type("net.minecraft.class_310"),"method_1551"),"method_1507",call(buildUpdater(),"generateScreen",current));}
            catch(RuntimeException|LinkageError failure){System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING,"Could not open the updater settings screen",failure);}
        });
        return call(button,"build");
    }
    private static Object label(String value){return call(type(YACL+"LabelOption"),"create",text(value));}
    private static Object option(Features.Feature feature) {
        List<String> missing=feature.requires().stream().filter(id->!FabricLoader.getInstance().isModLoaded(id)).toList();
        String description=feature.description()+(feature.needsRestart()?"\n\nTakes effect after restarting the game.":"\n\nTakes effect immediately.")
            +(missing.isEmpty()?"":"\n\nRequires: "+String.join(", ",missing)+" (not installed).");
        return toggle(feature.label(),description,feature.defaultValue(),()->Features.enabled(feature.id()),value->Features.set(feature.id(),value),missing.isEmpty());
    }
    private static Object toggle(String label,String description,boolean def,Supplier<Boolean> getter,Consumer<Boolean> setter,boolean available) {
        Object option=call(type(YACL+"Option"),"createBuilder");
        option=call(option,"name",text(label));
        option=call(option,"description",call(type(YACL+"OptionDescription"),"of",texts(description)));
        option=call(option,"binding",Boolean.valueOf(def),getter,setter);
        option=call(option,"controller",(Function<Object,Object>)built->call(type(YACL+"controller.TickBoxControllerBuilder"),"create",built));
        option=call(option,"available",available);
        return call(option,"build");
    }
    private static Object text(String value){return call(type(TEXT),"method_43470",value);}
    /** A Component[] holding one literal per paragraph, as varargs APIs expect. */
    private static Object texts(String value) {
        String[] paragraphs=value.split("\n");
        Object array=Array.newInstance(type(TEXT),paragraphs.length);
        for(int i=0;i<paragraphs.length;i++)Array.set(array,i,text(paragraphs[i]));
        return array;
    }
}
