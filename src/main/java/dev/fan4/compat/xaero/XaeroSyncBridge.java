package dev.fan4.compat.xaero;

import dev.fan4.compat.shared.WeakIdentityMap;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * Fabric API networking glue, by reflection so Fan4Compat has no link-time dependency on it. The join is held in
 * the configuration phase (before the world, and therefore before Xaero builds its session and reads its files):
 * the server sends the player's stored waypoint files, the client writes the newer ones and answers with any
 * local files the server should have, then both sides let the join continue. Later edits upload during play.
 * Anything that goes wrong lets the join continue; waypoints are never worth blocking a login.
 */
public final class XaeroSyncBridge {
    private static final System.Logger LOG=System.getLogger("Fan4Compat");
    private static final String TYPE="net.minecraft.class_8710$class_9154",CODEC="net.minecraft.class_9139",PAYLOAD="net.minecraft.class_8710",
        TASK="net.minecraft.class_8605",KEY="net.minecraft.class_8605$class_8606",ID="fan4compat:xaero_waypoints",
        API="net.fabricmc.fabric.api.networking.v1.",CLIENT_API="net.fabricmc.fabric.api.client.networking.v1.";
    private static final long CONFIG_TIMEOUT_SECONDS=20,UPLOAD_PERIOD_SECONDS=30;
    private static Object type,key;
    private static ClassLoader loader;
    private XaeroSyncBridge() {}

    // ---- payload plumbing: one type carrying one byte array --------------------------------------------------

    private record Payload(Object type,byte[] bytes) implements InvocationHandler {
        public Object invoke(Object proxy,Method method,Object[] args)throws Throwable {
            if(method.getName().equals("method_56479"))return type;
            if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->"Fan4Compat payload";};
            return InvocationHandler.invokeDefault(proxy,method,args);
        }
    }
    private static Object payload(byte[] bytes){return Proxy.newProxyInstance(loader,new Class<?>[]{type(PAYLOAD)},new Payload(type,bytes));}
    private static byte[] bytes(Object payload){return ((Payload)Proxy.getInvocationHandler(payload)).bytes;}
    private static Object codec() {
        return Proxy.newProxyInstance(loader,new Class<?>[]{type(CODEC)},(proxy,method,args)->switch(method.getName()) {
            case "decode" -> payload((byte[])call(args[0],"method_10795"));
            case "encode" -> {call(args[0],"method_10813",(Object)bytes(args[1]));yield null;}
            default -> method.getDeclaringClass()==Object.class?(method.getName().equals("hashCode")?System.identityHashCode(proxy):method.getName().equals("equals")?(Object)(proxy==args[0]):"Fan4Compat codec"):InvocationHandler.invokeDefault(proxy,method,args);
        });
    }
    private static Object listener(String iface,Consumer<Object[]> body) {
        return Proxy.newProxyInstance(loader,new Class<?>[]{type(iface)},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class)return method.getName().equals("hashCode")?System.identityHashCode(proxy):method.getName().equals("equals")?(Object)(proxy==args[0]):"Fan4Compat listener";
            body.accept(args);return null;
        });
    }

    /** Registers the payload type for configuration and play traffic. Safe on every install. */
    public static void registerPayloads() {
        loader=XaeroSyncBridge.class.getClassLoader();
        type=create(TYPE,call(type("net.minecraft.class_2960"),"method_60655","fan4compat","xaero_waypoints"));
        key=create(KEY,ID);
        Object codec=codec(),registry=type(API+"PayloadTypeRegistry");
        call(call(registry,"configurationS2C"),"register",type,codec);
        call(call(registry,"configurationC2S"),"register",type,codec);
        call(call(registry,"playC2S"),"register",type,codec);
    }
    // ---- server ----------------------------------------------------------------------------------------------

    private static WaypointServer server;
    private static final Map<Object,Join> JOINS=Collections.synchronizedMap(new WeakIdentityMap<>());
    private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Fan4Compat Xaero sync");t.setDaemon(true);return t;});
    private static final class Join {
        final UUID player;private boolean finished;
        Join(UUID player){this.player=player;}
        synchronized boolean finish(){if(finished)return false;finished=true;return true;}
    }
    private static synchronized WaypointServer server(Object minecraftServer)throws IOException {
        if(server==null) {
            Path root=(Path)call(minecraftServer,"method_27050",field(type("net.minecraft.class_5218"),"field_24188"));
            Properties config=new Properties();
            Path file=FabricLoader.getInstance().getConfigDir().resolve("fan4compat-xaero.properties");
            if(Files.isRegularFile(file))try(var in=Files.newBufferedReader(file)){config.load(in);}
            server=new WaypointServer(new WaypointStore(root.resolve("fan4compat").resolve("xaero-waypoints"),WaypointStore.Limits.from(config)));
        }
        return server;
    }
    /** Hold each joining player's configuration until their waypoint files have been exchanged. */
    public static void registerServer() {
        call(field(type(API+"ServerConfigurationConnectionEvents"),"CONFIGURE"),"register",listener(API+"ServerConfigurationConnectionEvents$Configure",args->{
            Object handler=args[0],minecraft=args[1];
            try {
                if(!(Boolean)call(type(API+"ServerConfigurationNetworking"),"canSend",handler,type))return;
                UUID player=(UUID)call(field(handler,"field_45022"),"getId");
                WaypointServer waypoints=server(minecraft);
                Join join=new Join(player);JOINS.put(handler,join);
                Object task=Proxy.newProxyInstance(loader,new Class<?>[]{type(TASK)},(proxy,method,taskArgs)->switch(method.getName()) {
                    case "method_52375" -> key;
                    case "method_52376" -> {
                        @SuppressWarnings("unchecked") Consumer<Object> sender=(Consumer<Object>)taskArgs[0];
                        try {
                            sender.accept(call(type(API+"ServerConfigurationNetworking"),"createS2CPacket",payload(waypoints.snapshot(player))));
                            TIMER.schedule(()->release(handler,minecraft,join),CONFIG_TIMEOUT_SECONDS,TimeUnit.SECONDS);
                        } catch(IOException|RuntimeException|LinkageError failure) {
                            // Nothing was sent, so the client will never answer: let the join continue now.
                            LOG.log(System.Logger.Level.WARNING,"Xaero waypoint snapshot failed; not holding the join",failure);
                            release(handler,minecraft,join);
                        }
                        yield null;
                    }
                    default -> method.getDeclaringClass()==Object.class?(method.getName().equals("hashCode")?System.identityHashCode(proxy):method.getName().equals("equals")?(Object)(proxy==taskArgs[0]):"Fan4Compat task"):InvocationHandler.invokeDefault(proxy,method,taskArgs);
                });
                call(handler,"addTask",task);
            } catch(IOException|RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Xaero waypoint sync skipped for a joining player",failure);}
        }));
        call(type(API+"ServerConfigurationNetworking"),"registerGlobalReceiver",type,listener(API+"ServerConfigurationNetworking$ConfigurationPacketHandler",args->{
            Object context=args[1],handler=call(context,"networkHandler"),minecraft=call(context,"server");
            Join join=JOINS.get(handler);
            if(join==null)return;
            byte[] bytes=bytes(args[0]);
            try {
                String refused=server(minecraft).receive(join.player,bytes,System.currentTimeMillis());
                if(refused!=null)LOG.log(System.Logger.Level.DEBUG,"Xaero waypoint upload refused: "+refused);
                if(WaypointServer.isDone(bytes))release(handler,minecraft,join);
            } catch(IOException|RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Xaero waypoint upload failed",failure);release(handler,minecraft,join);}
        }));
        call(type(API+"ServerPlayNetworking"),"registerGlobalReceiver",type,listener(API+"ServerPlayNetworking$PlayPayloadHandler",args->{
            Object context=args[1],player=call(context,"player");
            try {
                UUID id=UUID.fromString((String)call(player,"method_5845"));
                String refused=server(call(context,"server")).receive(id,bytes(args[0]),System.currentTimeMillis());
                if(refused!=null)LOG.log(System.Logger.Level.DEBUG,"Xaero waypoint upload refused: "+refused);
            } catch(IOException|RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Xaero waypoint upload failed",failure);}
        }));
    }
    /** Let the held join continue, once, on the server thread. */
    private static void release(Object handler,Object minecraft,Join join) {
        if(!join.finish())return;
        try{call(minecraft,"execute",(Runnable)()->{try{call(handler,"completeTask",key);}catch(RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Could not release a held join",failure);}});}
        catch(RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Could not release a held join",failure);}
    }

    // ---- client ----------------------------------------------------------------------------------------------

    private static volatile WaypointClient client;
    private static ScheduledFuture<?> periodic;
    /** The folder this connection's Xaero waypoints live in, or null when the address is not one we can map. */
    static WaypointClient clientFor(Path gameDir,Path configDir,String address) {
        String node=XaeroFolders.node(address,XaeroFolders.differentiateByAddress(configDir.resolve("xaero").resolve("minimap").resolve("client.cfg")));
        if(node==null)return null;
        Path serverFolder=XaeroFolders.serverFolder(gameDir.resolve("xaero").resolve("minimap"),node);
        Path data=gameDir.resolve(".fan4compat-xaero");
        return new WaypointClient(serverFolder,data.resolve("state").resolve(node+".properties"),data.resolve("backups").resolve(node),WaypointStore.Limits.defaults().maxFileBytes(),System::currentTimeMillis);
    }
    private static String address() {
        Object info=call(call(type("net.minecraft.class_310"),"method_1551"),"method_1558");
        return info==null?null:(String)field(info,"field_3761");
    }
    /** Receive the server's files, write them before Xaero looks, and answer; always ends by telling the server we are done. */
    public static void registerClient() {
        call(type(CLIENT_API+"ClientConfigurationNetworking"),"registerGlobalReceiver",type,listener(CLIENT_API+"ClientConfigurationNetworking$ConfigurationPayloadHandler",args->{
            try {
                WaypointProtocol.Message message=WaypointProtocol.decode(bytes(args[0]),WaypointStore.Limits.MAX_SNAPSHOT_BYTES);
                if(message instanceof WaypointProtocol.Snapshot snapshot) {
                    WaypointClient local=clientFor(FabricLoader.getInstance().getGameDir(),FabricLoader.getInstance().getConfigDir(),address());
                    client=local;
                    if(local!=null)for(WaypointProtocol.Chunk chunk:local.sync(snapshot))sendConfig(chunk);
                }
            } catch(IOException|RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Xaero waypoint sync failed; continuing the join",failure);}
            finally{sendConfig(new WaypointProtocol.Done());}
        }));
        call(field(type(CLIENT_API+"ClientPlayConnectionEvents"),"JOIN"),"register",listener(CLIENT_API+"ClientPlayConnectionEvents$Join",args->startUploads(args[2])));
        call(field(type(CLIENT_API+"ClientPlayConnectionEvents"),"DISCONNECT"),"register",listener(CLIENT_API+"ClientPlayConnectionEvents$Disconnect",args->stopUploads()));
    }
    private static void sendConfig(WaypointProtocol.Message message) {
        try{call(type(CLIENT_API+"ClientConfigurationNetworking"),"send",payload(WaypointProtocol.encode(message)));}
        catch(IOException|RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Could not send Xaero sync message",failure);}
    }
    private static synchronized void startUploads(Object minecraft) {
        stopUploads();
        if(client==null)return;
        periodic=TIMER.scheduleWithFixedDelay(()->{
            try {
                WaypointClient local=client;if(local==null)return;
                List<WaypointProtocol.Chunk> chunks=local.changes();
                if(chunks.isEmpty())return;
                call(minecraft,"execute",(Runnable)()->{
                    try {
                        if(!(Boolean)call(type(CLIENT_API+"ClientPlayNetworking"),"canSend",type))return;
                        for(WaypointProtocol.Chunk chunk:chunks)call(type(CLIENT_API+"ClientPlayNetworking"),"send",payload(WaypointProtocol.encode(chunk)));
                    } catch(IOException|RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Could not upload Xaero waypoints",failure);}
                });
            } catch(RuntimeException|LinkageError failure){LOG.log(System.Logger.Level.WARNING,"Xaero waypoint scan failed",failure);}
        },UPLOAD_PERIOD_SECONDS,UPLOAD_PERIOD_SECONDS,TimeUnit.SECONDS);
    }
    private static synchronized void stopUploads() {
        if(periodic!=null){periodic.cancel(false);periodic=null;}
        client=null;
    }
}
