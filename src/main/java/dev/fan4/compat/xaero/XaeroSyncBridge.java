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
    private static final String TASK="net.minecraft.class_8605",KEY="net.minecraft.class_8605$class_8606",ID="fan4compat:xaero_waypoints",
        API="net.fabricmc.fabric.api.networking.v1.",CLIENT_API="net.fabricmc.fabric.api.client.networking.v1.";
    private static final long CONFIG_TIMEOUT_SECONDS=20,UPLOAD_PERIOD_SECONDS=30;
    private static Object type,key;
    private XaeroSyncBridge() {}

    // ---- payload plumbing: one type carrying one byte array --------------------------------------------------

    private static PayloadChannel channel;
    private static Object payload(byte[] bytes){return channel.payload(bytes);}
    private static byte[] bytes(Object payload){return channel.bytes(payload);}

    /** Registers the payload type for configuration and play traffic. Safe on every install. */
    public static void registerPayloads() {
        channel=new PayloadChannel("fan4compat","xaero_waypoints");type=channel.type;
        key=create(KEY,ID);
        Object codec=channel.codec(),registry=type(API+"PayloadTypeRegistry");
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
            server=new WaypointServer(new WaypointStore(XaeroPaths.worldRoot(minecraftServer).resolve("fan4compat").resolve("xaero-waypoints"),WaypointStore.Limits.from(XaeroPaths.config())));
        }
        return server;
    }
    /** Hold each joining player's configuration until their waypoint files have been exchanged. */
    public static void registerServer() {
        call(field(type(API+"ServerConfigurationConnectionEvents"),"CONFIGURE"),"register",PayloadChannel.listener(API+"ServerConfigurationConnectionEvents$Configure",args->{
            Object handler=args[0],minecraft=args[1];
            try {
                boolean receives=(Boolean)call(type(API+"ServerConfigurationNetworking"),"canSend",handler,type);
                trace("server",()->"a player is joining; their client can take part in the exchange: "+receives);
                if(!receives)return;
                UUID player=(UUID)call(field(handler,"field_45022"),"getId");
                WaypointServer waypoints=server(minecraft);
                Join join=new Join(player);JOINS.put(handler,join);
                Object task=Proxy.newProxyInstance(XaeroSyncBridge.class.getClassLoader(),new Class<?>[]{type(TASK)},(proxy,method,taskArgs)->switch(method.getName()) {
                    case "method_52375" -> key;
                    case "method_52376" -> {
                        @SuppressWarnings("unchecked") Consumer<Object> sender=(Consumer<Object>)taskArgs[0];
                        try {
                            byte[] snapshot=waypoints.snapshot(player);
                            trace("server",()->"sending "+snapshot.length+" bytes of stored waypoints to "+player);
                            sender.accept(call(type(API+"ServerConfigurationNetworking"),"createS2CPacket",payload(snapshot)));
                            TIMER.schedule(()->release(handler,minecraft,join),CONFIG_TIMEOUT_SECONDS,TimeUnit.SECONDS);
                        } catch(IOException|RuntimeException|LinkageError failure) {
                            // Nothing was sent, so the client will never answer: let the join continue now.
                            dev.fan4.compat.shared.GameLog.warn("Xaero waypoint snapshot failed; not holding the join",failure);
                            release(handler,minecraft,join);
                        }
                        yield null;
                    }
                    default -> method.getDeclaringClass()==Object.class?(method.getName().equals("hashCode")?System.identityHashCode(proxy):method.getName().equals("equals")?(Object)(proxy==taskArgs[0]):"Fan4Compat task"):InvocationHandler.invokeDefault(proxy,method,taskArgs);
                });
                call(handler,"addTask",task);
            } catch(IOException|RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Xaero waypoint sync skipped for a joining player",failure);}
        }));
        call(type(API+"ServerConfigurationNetworking"),"registerGlobalReceiver",type,PayloadChannel.listener(API+"ServerConfigurationNetworking$ConfigurationPacketHandler",args->{
            Object context=args[1],handler=call(context,"networkHandler"),minecraft=call(context,"server");
            Join join=JOINS.get(handler);
            if(join==null)return;
            byte[] bytes=bytes(args[0]);
            try {
                String refused=server(minecraft).receive(join.player,bytes,System.currentTimeMillis());
                trace("server",()->"message of "+bytes.length+" bytes from "+join.player+" during the join: "+(refused==null?"accepted":"refused ("+refused+")")+(WaypointServer.isDone(bytes)?", the client says it is done":""));
                if(refused!=null)dev.fan4.compat.shared.GameLog.debug("Xaero waypoint upload refused: "+refused);
                if(WaypointServer.isDone(bytes))release(handler,minecraft,join);
            } catch(IOException|RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Xaero waypoint upload failed",failure);release(handler,minecraft,join);}
        }));
        call(type(API+"ServerPlayNetworking"),"registerGlobalReceiver",type,PayloadChannel.listener(API+"ServerPlayNetworking$PlayPayloadHandler",args->{
            Object context=args[1],player=call(context,"player");
            try {
                UUID id=UUID.fromString((String)call(player,"method_5845"));
                String refused=server(call(context,"server")).receive(id,bytes(args[0]),System.currentTimeMillis());
                trace("server",()->"upload from "+id+" while playing: "+(refused==null?"accepted":"refused ("+refused+")"));
                if(refused!=null)dev.fan4.compat.shared.GameLog.debug("Xaero waypoint upload refused: "+refused);
            } catch(IOException|RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Xaero waypoint upload failed",failure);}
        }));
    }
    /** Let the held join continue, once, on the server thread. */
    private static void release(Object handler,Object minecraft,Join join) {
        if(!join.finish())return;
        try{call(minecraft,"execute",(Runnable)()->{try{call(handler,"completeTask",key);}catch(RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Could not release a held join",failure);}});}
        catch(RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Could not release a held join",failure);}
    }

    // ---- client ----------------------------------------------------------------------------------------------

    private static volatile WaypointClient client;
    private static ScheduledFuture<?> periodic;
    /** The folder this connection's Xaero waypoints live in, or null when the address is not one we can map. */
    static WaypointClient clientFor(Path gameDir,Path configDir,String address) {
        String node=XaeroPaths.node(configDir,address);
        if(node==null)return null;
        Path serverFolder=XaeroFolders.serverFolder(gameDir.resolve("xaero").resolve("minimap"),node);
        Path data=XaeroPaths.clientData(gameDir);
        return new WaypointClient(serverFolder,data.resolve("state").resolve(node+".properties"),data.resolve("backups").resolve(node),WaypointStore.Limits.defaults().maxFileBytes(),System::currentTimeMillis);
    }
    /**
     * The server address while the join is still being configured: the game has not recorded its "current server" yet then, but the
     * network handler already carries the server entry the player connected with (the address Xaero names its folder after); failing
     * that, the host of the connection itself.
     */
    private static void trace(String side,java.util.function.Supplier<String> message){XaeroTrace.event(()->"waypoint sync, "+side+": "+message.get());}
    private static String addressOf(Object serverInfo){return (String)field(serverInfo,"field_3761");}
    static String address(Object handler) {
        Object info=field(handler,"field_45590");
        if(info!=null)return addressOf(info);
        Object remote=call(field(handler,"field_45589"),"method_10755");
        if(remote instanceof java.net.InetSocketAddress socket)return socket.getHostString();
        return address();
    }
    static String address() {
        Object info=call(call(type("net.minecraft.class_310"),"method_1551"),"method_1558");
        return info==null?null:addressOf(info);
    }
    /** Receive the server's files, write them before Xaero looks, and answer; always ends by telling the server we are done. */
    public static void registerClient() {
        call(type(CLIENT_API+"ClientConfigurationNetworking"),"registerGlobalReceiver",type,PayloadChannel.listener(CLIENT_API+"ClientConfigurationNetworking$ConfigurationPayloadHandler",args->{
            try {
                WaypointProtocol.Message message=WaypointProtocol.decode(bytes(args[0]),WaypointStore.Limits.MAX_SNAPSHOT_BYTES);
                if(message instanceof WaypointProtocol.Snapshot snapshot) {
                    String address=address(call(args[1],"networkHandler"));
                    WaypointClient local=clientFor(FabricLoader.getInstance().getGameDir(),FabricLoader.getInstance().getConfigDir(),address);
                    client=local;
                    trace("client",()->"the server sent "+snapshot.items().size()+" waypoint file(s); server address "+address+", Xaero folder "+(local==null?"unknown, so nothing is synced":"found"));
                    if(local!=null) {
                        List<WaypointProtocol.Chunk> answers=local.sync(snapshot);
                        trace("client",()->"sending the server "+answers.size()+" message(s) with local waypoint files");
                        for(WaypointProtocol.Chunk chunk:answers)sendConfig(chunk);
                    }
                }
            } catch(IOException|RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Xaero waypoint sync failed; continuing the join",failure);}
            finally{sendConfig(new WaypointProtocol.Done());}
        }));
        call(field(type(CLIENT_API+"ClientPlayConnectionEvents"),"JOIN"),"register",PayloadChannel.listener(CLIENT_API+"ClientPlayConnectionEvents$Join",args->startUploads(args[2])));
        call(field(type(CLIENT_API+"ClientPlayConnectionEvents"),"DISCONNECT"),"register",PayloadChannel.listener(CLIENT_API+"ClientPlayConnectionEvents$Disconnect",args->stopUploads()));
    }
    private static void sendConfig(WaypointProtocol.Message message) {
        try{call(type(CLIENT_API+"ClientConfigurationNetworking"),"send",payload(WaypointProtocol.encode(message)));}
        catch(IOException|RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Could not send Xaero sync message",failure);}
    }
    private static synchronized void startUploads(Object minecraft) {
        cancelUploads();   // not stopUploads(): the sync client was set up during the join and must survive into play
        trace("client",()->"play started; uploads while playing are "+(client==null?"off (no folder was found during the join)":"on"));
        if(client==null)return;
        periodic=TIMER.scheduleWithFixedDelay(()->{
            try {
                WaypointClient local=client;if(local==null)return;
                List<WaypointProtocol.Chunk> chunks=local.changes();
                if(chunks.isEmpty())return;
                trace("client",()->"uploading "+chunks.size()+" changed waypoint message(s)");
                call(minecraft,"execute",(Runnable)()->{
                    try {
                        if(!(Boolean)call(type(CLIENT_API+"ClientPlayNetworking"),"canSend",type))return;
                        for(WaypointProtocol.Chunk chunk:chunks)call(type(CLIENT_API+"ClientPlayNetworking"),"send",payload(WaypointProtocol.encode(chunk)));
                    } catch(IOException|RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Could not upload Xaero waypoints",failure);}
                });
            } catch(RuntimeException|LinkageError failure){dev.fan4.compat.shared.GameLog.warn("Xaero waypoint scan failed",failure);}
        },UPLOAD_PERIOD_SECONDS,UPLOAD_PERIOD_SECONDS,TimeUnit.SECONDS);
    }
    private static void cancelUploads(){if(periodic!=null){periodic.cancel(false);periodic=null;}}
    private static synchronized void stopUploads(){cancelUploads();client=null;}
}
