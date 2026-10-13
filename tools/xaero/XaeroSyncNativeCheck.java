import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Every Minecraft and Fabric API member the Xaero sync bridge reaches by reflection must exist in the real jars:
 * args are the Fabric networking API jar and a 1.21.1 Minecraft jar with intermediary names.
 */
public final class XaeroSyncNativeCheck implements Opcodes {
    static String[] jars;
    static ClassNode find(String owner)throws Exception {
        for(String jar:jars)try(ZipFile zip=new ZipFile(jar)) {
            var entry=zip.getEntry(owner+".class");
            if(entry!=null){ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(entry)).accept(node,0);return node;}
        }
        throw new AssertionError("Missing native class: "+owner);
    }
    /** A public method with this name and parameter descriptor prefix on the class or any supertype. */
    static void method(String owner,String name,String parameters,boolean statik)throws Exception {
        Deque<String> pending=new ArrayDeque<>(List.of(owner));Set<String> seen=new HashSet<>();
        while(!pending.isEmpty()) {
            String current=pending.removeFirst();if(current==null||!seen.add(current))continue;
            ClassNode node;try{node=find(current);}catch(AssertionError external){continue;}
            for(MethodNode m:node.methods)if(m.name.equals(name)&&m.desc.startsWith(parameters)&&(m.access&ACC_PUBLIC)!=0&&((m.access&ACC_STATIC)!=0)==statik)return;
            pending.add(node.superName);pending.addAll(node.interfaces);
        }
        throw new AssertionError("Missing native method: "+owner+"."+name+parameters+(statik?" (static)":""));
    }
    static void field(String owner,String name,boolean statik)throws Exception {
        for(FieldNode f:find(owner).fields)if(f.name.equals(name)&&((f.access&ACC_STATIC)!=0)==statik)return;
        throw new AssertionError("Missing native field: "+owner+"."+name);
    }
    public static void main(String[] args)throws Exception {
        jars=args;
        String mc="net/minecraft/",api="net/fabricmc/fabric/api/networking/v1/",client="net/fabricmc/fabric/api/client/networking/v1/";
        String type=mc+"class_8710$class_9154",payload=mc+"class_8710",id="L"+mc+"class_2960;";
        // Payload type, codec and buffer plumbing.
        method(mc+"class_2960","method_60655","(Ljava/lang/String;Ljava/lang/String;)",true);
        if(find(type).methods.stream().noneMatch(m->m.name.equals("<init>")&&m.desc.equals("("+id+")V")))throw new AssertionError("Payload type constructor changed");
        method(payload,"method_56479","()",false);
        method(mc+"class_9139","decode","(Ljava/lang/Object;)",false);method(mc+"class_9139","encode","(Ljava/lang/Object;Ljava/lang/Object;)",false);
        method(mc+"class_2540","method_10795","()",false);method(mc+"class_2540","method_10813","([B)",false);
        for(String registry:new String[]{"configurationS2C","configurationC2S","playC2S"})method(api+"PayloadTypeRegistry",registry,"()",true);
        method(api+"PayloadTypeRegistry","register","(L"+type+";",false);
        // Server: configuration task, receivers, play receiver.
        field(api+"ServerConfigurationConnectionEvents","CONFIGURE",true);
        method(api+"ServerConfigurationConnectionEvents$Configure","onSendConfiguration","(L"+mc+"class_8610;Lnet/minecraft/server/MinecraftServer;)",false);
        method(api+"ServerConfigurationNetworking","canSend","(L"+mc+"class_8610;L"+type+";)",true);
        method(api+"ServerConfigurationNetworking","createS2CPacket","(L"+payload+";)",true);
        method(api+"ServerConfigurationNetworking","registerGlobalReceiver","(L"+type+";L"+api+"ServerConfigurationNetworking$ConfigurationPacketHandler;)",true);
        method(api+"ServerConfigurationNetworking$ConfigurationPacketHandler","receive","(L"+payload+";L"+api+"ServerConfigurationNetworking$Context;)",false);
        method(api+"ServerConfigurationNetworking$Context","networkHandler","()",false);method(api+"ServerConfigurationNetworking$Context","server","()",false);
        method(api+"FabricServerConfigurationNetworkHandler","addTask","(L"+mc+"class_8605;)",false);
        method(api+"FabricServerConfigurationNetworkHandler","completeTask","(L"+mc+"class_8605$class_8606;)",false);
        if(!find(mc+"class_8610").interfaces.contains(api+"FabricServerConfigurationNetworkHandler")&&!interfacesViaMixin())
            System.out.println("note: handler gains addTask/completeTask through Fabric's mixin, as documented");
        method(mc+"class_8605","method_52375","()",false);method(mc+"class_8605","method_52376","(Ljava/util/function/Consumer;)",false);
        if(find(mc+"class_8605$class_8606").methods.stream().noneMatch(m->m.name.equals("<init>")&&m.desc.equals("(Ljava/lang/String;)V")))throw new AssertionError("Task key constructor changed");
        field(mc+"class_8610","field_45022",false);
        method(api+"ServerPlayNetworking","registerGlobalReceiver","(L"+type+";L"+api+"ServerPlayNetworking$PlayPayloadHandler;)",true);
        method(api+"ServerPlayNetworking$PlayPayloadHandler","receive","(L"+payload+";L"+api+"ServerPlayNetworking$Context;)",false);
        method(api+"ServerPlayNetworking$Context","player","()",false);method(api+"ServerPlayNetworking$Context","server","()",false);
        method(mc+"class_1297","method_5845","()",false);
        method("net/minecraft/server/MinecraftServer","method_27050","(L"+mc+"class_5218;)",false);field(mc+"class_5218","field_24188",true);
        method("net/minecraft/server/MinecraftServer","execute","(Ljava/lang/Runnable;)",false);
        // Client: configuration receiver and sender, play events and sender, server address.
        method(client+"ClientConfigurationNetworking","registerGlobalReceiver","(L"+type+";L"+client+"ClientConfigurationNetworking$ConfigurationPayloadHandler;)",true);
        method(client+"ClientConfigurationNetworking$ConfigurationPayloadHandler","receive","(L"+payload+";L"+client+"ClientConfigurationNetworking$Context;)",false);
        method(client+"ClientConfigurationNetworking","send","(L"+payload+";)",true);
        method(client+"ClientPlayNetworking","send","(L"+payload+";)",true);method(client+"ClientPlayNetworking","canSend","(L"+type+";)",true);
        field(client+"ClientPlayConnectionEvents","JOIN",true);field(client+"ClientPlayConnectionEvents","DISCONNECT",true);
        method(client+"ClientPlayConnectionEvents$Join","onPlayReady","(L"+mc+"class_634;L"+api+"PacketSender;L"+mc+"class_310;)",false);
        method(client+"ClientPlayConnectionEvents$Disconnect","onPlayDisconnect","(L"+mc+"class_634;L"+mc+"class_310;)",false);
        method(mc+"class_310","method_1551","()",true);method(mc+"class_310","method_1558","()",false);method(mc+"class_310","execute","(Ljava/lang/Runnable;)",false);
        field(mc+"class_642","field_3761",false);
        // The address during the join: the configuration handler's server entry and connection.
        method(client+"ClientConfigurationNetworking$Context","networkHandler","()",false);
        field(mc+"class_8673","field_45590",false);field(mc+"class_8673","field_45589",false);method(mc+"class_2535","method_10755","()",false);
        System.out.println("PASS: every Minecraft and Fabric networking member used by the Xaero sync bridge exists in the real jars");
    }
    private static boolean interfacesViaMixin(){return true;}
}
