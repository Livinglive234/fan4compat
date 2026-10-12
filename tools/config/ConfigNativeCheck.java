import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Pin every YACL and Mod Menu member the settings screen reaches by reflection or by compiled stubs. */
public final class ConfigNativeCheck implements Opcodes {
    static ClassNode read(String jar,String owner)throws Exception {
        try(ZipFile zip=new ZipFile(jar)) {
            var entry=zip.getEntry(owner+".class");if(entry==null)throw new AssertionError("Missing native class: "+owner);
            ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(entry)).accept(node,0);return node;
        }
    }
    /** Needs a public method of this name and parameter list (any return type), static as requested. */
    static void method(ClassNode node,String name,String parameters,boolean statik) {
        for(MethodNode m:node.methods)
            if(m.name.equals(name)&&m.desc.startsWith(parameters)&&(m.access&ACC_PUBLIC)!=0&&((m.access&ACC_STATIC)!=0)==statik&&(m.access&ACC_SYNTHETIC)==0)return;
        throw new AssertionError("Missing native method: "+node.name+"."+name+parameters+(statik?" (static)":""));
    }
    /** The reflective helper picks by name and argument types, so the other overloads must not accept our argument. */
    static void distinct(ClassNode node,String name,String first,String second) {
        method(node,name,first,false);method(node,name,second,false);
    }
    public static void main(String[] args)throws Exception {
        String yacl=args[0],menu=args[1],api="dev/isxander/yacl3/api/",text="Lnet/minecraft/class_2561;",screen="Lnet/minecraft/class_437;";
        ClassNode lib=read(yacl,api+"YetAnotherConfigLib");
        method(lib,"createBuilder","()",true);method(lib,"generateScreen","("+screen+")",false);
        ClassNode libBuilder=read(yacl,api+"YetAnotherConfigLib$Builder");
        method(libBuilder,"title","("+text+")",false);method(libBuilder,"category","(L"+api+"ConfigCategory;)",false);
        method(libBuilder,"save","(Ljava/lang/Runnable;)",false);method(libBuilder,"build","()",false);
        ClassNode category=read(yacl,api+"ConfigCategory");method(category,"createBuilder","()",true);
        ClassNode categoryBuilder=read(yacl,api+"ConfigCategory$Builder");
        method(categoryBuilder,"name","("+text+")",false);method(categoryBuilder,"option","(L"+api+"Option;)",false);
        method(categoryBuilder,"options","(Ljava/util/Collection;)",false);method(categoryBuilder,"build","()",false);
        method(read(yacl,api+"LabelOption"),"create","("+text+")",true);
        ClassNode option=read(yacl,api+"Option");method(option,"createBuilder","()",true);
        ClassNode optionBuilder=read(yacl,api+"Option$Builder");
        method(optionBuilder,"name","("+text+")",false);method(optionBuilder,"available","(Z)",false);method(optionBuilder,"build","()",false);
        method(optionBuilder,"binding","(Ljava/lang/Object;Ljava/util/function/Supplier;Ljava/util/function/Consumer;)",false);
        method(optionBuilder,"controller","(Ljava/util/function/Function;)",false);
        // description(OptionDescription) shares a name with description(Function); our argument is not a Function.
        distinct(optionBuilder,"description","(L"+api+"OptionDescription;)","(Ljava/util/function/Function;)");
        ClassNode button=read(yacl,api+"ButtonOption");method(button,"createBuilder","()",true);
        ClassNode buttonBuilder=read(yacl,api+"ButtonOption$Builder");
        method(buttonBuilder,"name","("+text+")",false);method(buttonBuilder,"text","("+text+")",false);method(buttonBuilder,"available","(Z)",false);method(buttonBuilder,"build","()",false);
        method(buttonBuilder,"description","(L"+api+"OptionDescription;)",false);
        // action(Consumer) shares a name with action(BiConsumer); a plain Consumer argument selects only the first.
        distinct(buttonBuilder,"action","(Ljava/util/function/Consumer;)","(Ljava/util/function/BiConsumer;)");
        method(read(yacl,api+"OptionDescription"),"of","([Lnet/minecraft/class_2561;)",true);
        method(read(yacl,api+"controller/TickBoxControllerBuilder"),"create","(L"+api+"Option;)",true);
        for(String type:new String[]{"OptionDescription","Option"})
            if((read(yacl,api+type).access&ACC_INTERFACE)==0)throw new AssertionError(type+" is no longer an interface");
        for(String name:new String[]{"Option$Builder","OptionDescription"})
            if(read(yacl,api+name).interfaces.contains("java/util/function/Function"))throw new AssertionError(name+" now implements Function; reflective overload choice becomes ambiguous");
        // The compile-time stubs must match the real Mod Menu signatures exactly.
        ClassNode mod=read(menu,"com/terraformersmc/modmenu/api/ModMenuApi");
        MethodNode factory=mod.methods.stream().filter(m->m.name.equals("getModConfigScreenFactory")).findFirst().orElseThrow(()->new AssertionError("Missing ModMenuApi.getModConfigScreenFactory"));
        if(!factory.desc.equals("()Lcom/terraformersmc/modmenu/api/ConfigScreenFactory;"))throw new AssertionError("ModMenuApi factory signature changed: "+factory.desc);
        ClassNode create=read(menu,"com/terraformersmc/modmenu/api/ConfigScreenFactory");
        MethodNode created=create.methods.stream().filter(m->m.name.equals("create")).findFirst().orElseThrow();
        if(!created.desc.equals("("+screen+")"+screen)&&!created.desc.equals("("+screen+")Lnet/minecraft/class_437;"))throw new AssertionError("ConfigScreenFactory.create changed: "+created.desc);
        // Our entry class must use exactly those descriptors.
        Path entry=Path.of(args[2],"dev/fan4/compat/config/ModMenuEntry.class");
        ClassNode ours=new ClassNode();new ClassReader(Files.readAllBytes(entry)).accept(ours,0);
        if(ours.methods.stream().noneMatch(m->m.name.equals("getModConfigScreenFactory")&&m.desc.equals(factory.desc)))throw new AssertionError("ModMenuEntry does not override the real factory method");
        if(!ours.interfaces.contains("com/terraformersmc/modmenu/api/ModMenuApi"))throw new AssertionError("ModMenuEntry lost its interface");
        System.out.println("PASS: YACL builder/category/option/controller members and Mod Menu entry descriptors verified against real jars");
    }
}
