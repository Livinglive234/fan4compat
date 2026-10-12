package dev.fan4.compat.xaero;

import java.lang.reflect.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * One Fabric-API custom payload type that carries a single byte array, built by reflection so Fan4Compat has no link-time
 * dependency on Fabric API. The payload object is a proxy of Minecraft's payload interface; the codec is a proxy of its
 * packet codec reading and writing the array.
 */
final class PayloadChannel {
    private static final String TYPE="net.minecraft.class_8710$class_9154",CODEC="net.minecraft.class_9139",PAYLOAD="net.minecraft.class_8710";
    private final ClassLoader loader=PayloadChannel.class.getClassLoader();
    final Object type;
    PayloadChannel(String namespace,String path){type=create(TYPE,call(type("net.minecraft.class_2960"),"method_60655",namespace,path));}
    private record Payload(Object type,byte[] bytes) implements InvocationHandler {
        public Object invoke(Object proxy,Method method,Object[] args)throws Throwable {
            if(method.getName().equals("method_56479"))return type;
            return plain(proxy,method,args,"Fan4Compat payload");
        }
    }
    private static Object plain(Object proxy,Method method,Object[] args,String name)throws Throwable {
        if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->name;};
        return InvocationHandler.invokeDefault(proxy,method,args);
    }
    /** A proxy of a Fabric callback interface that runs {@code body} with the call's arguments. */
    static Object listener(String iface,java.util.function.Consumer<Object[]> body) {
        return Proxy.newProxyInstance(PayloadChannel.class.getClassLoader(),new Class<?>[]{type(iface)},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class)return plain(proxy,method,args,"Fan4Compat listener");
            body.accept(args);return null;
        });
    }
    Object payload(byte[] bytes){return Proxy.newProxyInstance(loader,new Class<?>[]{type(PAYLOAD)},new Payload(type,bytes));}
    byte[] bytes(Object payload){return ((Payload)Proxy.getInvocationHandler(payload)).bytes;}
    Object codec() {
        return Proxy.newProxyInstance(loader,new Class<?>[]{type(CODEC)},(proxy,method,args)->switch(method.getName()) {
            case "decode" -> payload((byte[])call(args[0],"method_10795"));
            case "encode" -> {call(args[0],"method_10813",(Object)bytes(args[1]));yield null;}
            default -> plain(proxy,method,args,"Fan4Compat codec");
        });
    }
}
