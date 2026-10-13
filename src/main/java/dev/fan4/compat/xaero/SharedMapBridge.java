package dev.fan4.compat.xaero;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * Fabric glue for the shared explored map, by reflection. Server: every couple of ticks look at a few chunks around players
 * (those a real client holds), summarize new or changed ones into the store, and stream each Fan4Compat client the chunks it
 * lacks. Client: receive batches for {@link SharedMapClient}, and tell the server at join what it already has.
 */
public final class SharedMapBridge {
    private static final String API="net.fabricmc.fabric.api.networking.v1.",CLIENT_API="net.fabricmc.fabric.api.client.networking.v1.",
        LIFE="net.fabricmc.fabric.api.event.lifecycle.v1.",POS="net.minecraft.class_2338";
    private static final int BATCH_CHUNKS=100,SEND_EVERY_TICKS=10,FLUSH_EVERY_TICKS=6000,MAX_VIEW=16;
    private static PayloadChannel batches,hello,modeChannel,uploads;
    private static boolean warned;

    private SharedMapBridge() {}
    /** Registers the four payload types (batches and the mode message to clients; the hello and uploads to the server). Safe on every install. */
    public static void registerPayloads() {
        batches=new PayloadChannel("fan4compat","shared_map");hello=new PayloadChannel("fan4compat","shared_map_hello");
        modeChannel=new PayloadChannel("fan4compat","shared_map_mode");uploads=new PayloadChannel("fan4compat","shared_map_upload");
        Object registry=type(API+"PayloadTypeRegistry");
        call(call(registry,"playS2C"),"register",batches.type,batches.codec());
        call(call(registry,"playS2C"),"register",modeChannel.type,modeChannel.codec());
        call(call(registry,"playC2S"),"register",hello.type,hello.codec());
        call(call(registry,"playC2S"),"register",uploads.type,uploads.codec());
    }
    private static String id(Object key){return String.valueOf(call(key,"method_29177"));}
    private static UUID uuid(Object entity){return UUID.fromString((String)call(entity,"method_5845"));}
    private static int chunkX(Object position){return (Integer)call(position,"method_10263")>>4;}
    private static int chunkZ(Object position){return (Integer)call(position,"method_10260")>>4;}
    private static void warn(Throwable failure){if(!warned){warned=true;dev.fan4.compat.shared.GameLog.warn("Shared map problem (it keeps trying; this is logged once): "+failure);}}

    // ---- server ----------------------------------------------------------------------------------------------

    private static SharedMapStore store;
    private static final SharedMapCapturePlan PLAN=new SharedMapCapturePlan(1200);
    private static final Map<UUID,SharedMapRelay> RELAYS=new ConcurrentHashMap<>();
    private static Set<String> allowed=Set.of();
    private static SharedMapMode mode=SharedMapMode.BOTH;
    private static SharedMapIntake intake;
    private static int budget=2;
    private static long tick;

    public static void registerServer() {
        Properties config=XaeroPaths.config();
        Set<String> dimensions=new HashSet<>();
        for(String entry:config.getProperty("sharedMapDimensions","minecraft:overworld,minecraft:the_nether,minecraft:the_end,aether:the_aether").split(","))
            if(SharedMapChunk.validId(entry.trim()))dimensions.add(entry.trim());
        allowed=Set.copyOf(dimensions);
        mode=SharedMapMode.parse(config.getProperty("sharedMapMode"));
        try{budget=Math.max(1,Math.min(8,Integer.parseInt(config.getProperty("sharedMapCaptureChunksPerTick","2").trim())));}catch(NumberFormatException ignored){}
        dev.fan4.compat.shared.GameLog.info("Shared map mode: "+mode+(mode.active()?"":" (the server shares nothing and clients keep Xaero's default behavior)"));
        if(mode.active())call(field(type(LIFE+"ServerTickEvents"),"END_SERVER_TICK"),"register",PayloadChannel.listener(LIFE+"ServerTickEvents$EndTick",args->serverTick(args[0])));
        call(field(type(LIFE+"ServerLifecycleEvents"),"SERVER_STOPPING"),"register",PayloadChannel.listener(LIFE+"ServerLifecycleEvents$ServerStopping",args->{if(store!=null)store.flush();}));
        call(type(API+"ServerPlayNetworking"),"registerGlobalReceiver",hello.type,PayloadChannel.listener(API+"ServerPlayNetworking$PlayPayloadHandler",args->{
            try {
                UUID player=uuid(call(args[1],"player"));
                Object sender=call(args[1],"player");
                if(mode.active())RELAYS.put(player,new SharedMapRelay(storeFor(call(args[1],"server")),SharedMapCodec.decodeHello(hello.bytes(args[0]))));
                // Tell the client what this server does with uploads (and that it is a Fan4Compat server at all).
                call(type(API+"ServerPlayNetworking"),"send",sender,modeChannel.payload(SharedMapCodec.encodeMode(mode)));
            } catch(IOException|RuntimeException|LinkageError failure){warn(failure);}
        }));
        call(type(API+"ServerPlayNetworking"),"registerGlobalReceiver",uploads.type,PayloadChannel.listener(API+"ServerPlayNetworking$PlayPayloadHandler",args->{
            try {
                Object player=call(args[1],"player"),world=call(player,"method_51469");
                SharedMapStore map=storeFor(call(args[1],"server"));
                if(intake==null)intake=new SharedMapIntake(mode,allowed,map);
                Object border=call(world,"method_8621");
                List<SharedMapChunk> stored=intake.accept(uuid(player),id(call(world,"method_27983")),
                    SharedMapCodec.decode(uploads.bytes(args[0])),System.currentTimeMillis(),
                    chunk->(Boolean)call(border,"method_11952",create(POS,chunk.x*16+8,64,chunk.z*16+8)));
                for(SharedMapChunk chunk:stored)for(SharedMapRelay relay:RELAYS.values())relay.changed(chunk);
            } catch(IOException|RuntimeException|LinkageError failure){warn(failure);}
        }));
    }
    private static synchronized SharedMapStore storeFor(Object server)throws IOException {
        if(store==null)store=new SharedMapStore(XaeroPaths.worldRoot(server).resolve("fan4compat").resolve("shared-map"),32);
        return store;
    }
    private static void serverTick(Object server) {
        try {
            if(++tick%2!=0)return;
            Object manager=call(server,"method_3760");
            List<?> players=(List<?>)call(manager,"method_14571");
            if(players.isEmpty())return;
            SharedMapStore map=storeFor(server);
            int view=Math.min(MAX_VIEW,(Integer)call(manager,"method_14568"));
            List<SharedMapCapturePlan.Player> near=new ArrayList<>();Map<String,Object> worlds=new HashMap<>();
            for(Object player:players) {
                Object world=call(player,"method_51469");String dimension=id(call(world,"method_27983"));
                if(!allowed.contains(dimension))continue;
                Object position=call(player,"method_24515");
                near.add(new SharedMapCapturePlan.Player(dimension,chunkX(position),chunkZ(position),view));
                worlds.putIfAbsent(dimension,world);
            }
            Object full=field(type("net.minecraft.class_2806"),"field_12803");
            if(mode.captures)for(SharedMapCapturePlan.Target target:PLAN.next(near,tick,budget)) {
                Object world=worlds.get(target.dimension());
                Object chunk=call(world,"method_8402",target.x(),target.z(),full,false);
                if(chunk==null)continue;
                // Ship blocks live in far "shipyard" chunks outside the world border: never map those.
                if(!(Boolean)call(call(world,"method_8621"),"method_11952",create(POS,target.x()*16+8,64,target.z()*16+8)))continue;
                SharedMapChunk summary=SharedMapSummarizer.summarize(world,chunk,target.dimension(),target.x(),target.z(),System.currentTimeMillis());
                if(summary!=null&&map.put(summary))for(SharedMapRelay relay:RELAYS.values())relay.changed(summary);
            }
            if(tick%SEND_EVERY_TICKS==0)
                for(Object player:players) {
                    UUID uuid=uuid(player);
                    SharedMapRelay relay=RELAYS.get(uuid);
                    if(relay==null)continue;
                    String dimension=id(call(call(player,"method_51469"),"method_27983"));
                    if(!allowed.contains(dimension))continue;
                    Object position=call(player,"method_24515");
                    relay.moved(dimension,chunkX(position),chunkZ(position));
                    List<SharedMapChunk> batch=relay.next(BATCH_CHUNKS);
                    if(!batch.isEmpty()&&(Boolean)call(type(API+"ServerPlayNetworking"),"canSend",player,batches.type))
                        call(type(API+"ServerPlayNetworking"),"send",player,batches.payload(SharedMapCodec.encode(batch)));
                }
            if(tick%FLUSH_EVERY_TICKS==0)map.flush();
            Set<UUID> online=new HashSet<>();for(Object player:players)online.add(uuid(player));
            RELAYS.keySet().retainAll(online);
            if(intake!=null)intake.forget(online);
        } catch(IOException|RuntimeException|LinkageError failure){warn(failure);}
    }

    // ---- client ----------------------------------------------------------------------------------------------

    public static void registerClient() {
        call(type(CLIENT_API+"ClientPlayNetworking"),"registerGlobalReceiver",batches.type,PayloadChannel.listener(CLIENT_API+"ClientPlayNetworking$PlayPayloadHandler",args->{
            try{SharedMapClient.accept(SharedMapCodec.decode(batches.bytes(args[0])));}
            catch(IOException|RuntimeException|LinkageError failure){warn(failure);}
        }));
        call(type(CLIENT_API+"ClientPlayNetworking"),"registerGlobalReceiver",modeChannel.type,PayloadChannel.listener(CLIENT_API+"ClientPlayNetworking$PlayPayloadHandler",args->{
            try{SharedMapUploader.serverMode(SharedMapCodec.decodeMode(modeChannel.bytes(args[0])));}
            catch(IOException|RuntimeException|LinkageError failure){warn(failure);}
        }));
        call(field(type(CLIENT_API+"ClientPlayConnectionEvents"),"JOIN"),"register",PayloadChannel.listener(CLIENT_API+"ClientPlayConnectionEvents$Join",args->{
            try {
                String node=XaeroPaths.node(FabricLoader.getInstance().getConfigDir(),XaeroSyncBridge.address());
                SharedMapClient.join(node==null?null:XaeroPaths.clientData(FabricLoader.getInstance().getGameDir()).resolve("shared-map").resolve(node+".properties"));
                if((Boolean)call(type(CLIENT_API+"ClientPlayNetworking"),"canSend",hello.type))
                    call(type(CLIENT_API+"ClientPlayNetworking"),"send",hello.payload(SharedMapCodec.encodeHello(SharedMapClient.since(),SharedMapClient.owed())));
            } catch(IOException|RuntimeException|LinkageError failure){warn(failure);}
        }));
        call(field(type(CLIENT_API+"ClientPlayConnectionEvents"),"DISCONNECT"),"register",PayloadChannel.listener(CLIENT_API+"ClientPlayConnectionEvents$Disconnect",args->SharedMapClient.leave()));
    }

    /** Client to server: summaries of what Xaero has mapped, when the server's mode asks for them. */
    static void upload(List<SharedMapChunk> chunks) {
        try {
            if((Boolean)call(type(CLIENT_API+"ClientPlayNetworking"),"canSend",uploads.type))
                call(type(CLIENT_API+"ClientPlayNetworking"),"send",uploads.payload(SharedMapCodec.encode(chunks)));
        } catch(IOException|RuntimeException|LinkageError failure){warn(failure);}
    }
    /** The player's real dimension and chunk, or null before a world is loaded. */
    static SharedMapUploader.Where where() {
        Object client=call(type("net.minecraft.class_310"),"method_1551"),world=field(client,"field_1687"),player=field(client,"field_1724");
        if(world==null||player==null)return null;
        Object position=call(player,"method_24515");
        return new SharedMapUploader.Where(id(call(world,"method_27983")),chunkX(position),chunkZ(position));
    }
}
