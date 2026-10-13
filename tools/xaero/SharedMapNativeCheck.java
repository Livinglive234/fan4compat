import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Real jars for the shared explored map: the hook on Xaero's map writer must bind, and every Xaero World Map, Fabric networking
 * and lifecycle, and Minecraft member the server capture and client applier reach by reflection must exist.
 * Arguments: World Map jar, Fabric networking jar, Fabric lifecycle jar, intermediary-named Minecraft jar, generated classes directory.
 */
public final class SharedMapNativeCheck {
    static void method(String owner,String name,String parameters,boolean statik)throws Exception{XaeroMapNativeCheck.method(owner,name,parameters,statik);}
    static void field(String owner,String name,String desc,boolean statik)throws Exception{XaeroMapNativeCheck.field(owner,name,desc,statik);}
    public static void main(String[] args)throws Exception {
        XaeroMapNativeCheck.jars=Arrays.copyOf(args,4);
        String mc="net/minecraft/",map="xaero/map/",region=map+"region/",api="net/fabricmc/fabric/api/networking/v1/",client="net/fabricmc/fabric/api/client/networking/v1/",life="net/fabricmc/fabric/api/event/lifecycle/v1/";
        String type=mc+"class_8710$class_9154",pos="L"+mc+"class_2338;",key="L"+mc+"class_5321;",id="L"+mc+"class_2960;";
        // The hook binds to the real writer.
        ClassNode writer=XaeroMapNativeCheck.find(map+"MapWriter");
        ClassNode mixin=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[4],"dev/fan4/compat/mixin/xaero/client/SharedMapTickMixin.class"))).accept(mixin,0);
        VerifyAddon.classes.put(map+"MapWriter",writer);VerifyAddon.injectionTargets(mixin);
        method(map+"MapWriter","getMapProcessor","()",false);field(map+"MapWriter","blockStateShortShapeCache",null,false);
        // The applier's walk through Xaero's regions, tile chunks, tiles and blocks.
        String processor=map+"MapProcessor";
        method(processor,"getMapWorld","()",false);method(map+"world/MapWorld","getCurrentDimensionId","()",false);
        method(processor,"getLeafMapRegion","(IIIZ)",false);method(processor,"getTilePool","()",false);method(map+"pool/MapTilePool","get","(Ljava/lang/String;II)",false);
        method(processor,"getCurrentDimension","()",false);method(processor,"getWorldBlockTintProvider","()",false);method(processor,"getOverlayManager","()",false);
        String r=region+"MapRegion",chunk=region+"MapTileChunk",tile=region+"MapTile",block=region+"MapBlock";
        field(r,"writerThreadPauseSync",null,false);
        for(String name:new String[]{"isWritingPaused","getLoadState","registerVisit","isResting","isNormalMapData","canRequestReload_unsynced"})method(r,name,"()",false);
        // Water as Xaero stores it: an overlay built on the bed block, and read back through the block's overlay list.
        String builder=region+"OverlayBuilder";
        method(builder,"<init>","(L"+region+"OverlayManager;)",false);method(builder,"startBuilding","()",false);
        method(builder,"build","(L"+mc+"class_2680;IBL"+processor+";"+key+")",false);method(builder,"finishBuilding","(L"+region+"MapBlock;)",false);
        method(region+"MapBlock","getOverlays","()",false);
        // Asking Xaero to load a region the way its own writer does.
        method(processor,"getMapSaveLoad","()",false);method(map+"file/MapSaveLoad","requestLoad","(L"+r+";Ljava/lang/String;)",false);
        method(r,"setBeingWritten","(Z)",false);method(r,"getChunk","(II)",false);method(r,"setChunk","(IIL"+chunk+";)",false);method(r,"setAllCachePrepared","(Z)",false);
        if(XaeroMapNativeCheck.find(chunk).methods.stream().noneMatch(m->m.name.equals("<init>")&&m.desc.equals("(L"+r+";II)V")&&(m.access&Opcodes.ACC_PUBLIC)!=0))throw new AssertionError("MapTileChunk constructor changed");
        method(chunk,"setLoadState","(B)",false);method(chunk,"getLoadState","()",false);method(chunk,"getLeafTexture","()",false);method(region+"texture/LeafRegionTexture","shouldDownloadFromPBO","()",false);
        method(chunk,"getTile","(II)",false);method(chunk,"setChanged","(Z)",false);method(chunk,"wasChanged","()",false);method(chunk,"includeInSave","()",false);method(chunk,"setHasHadTerrain","()",false);
        method(chunk,"setTile","(IIL"+tile+";Lxaero/map/cache/BlockStateShortShapeCache;L"+processor+";)",false);
        method(chunk,"updateBuffers","(L"+processor+";Lxaero/map/biome/BlockTintProvider;L"+region+"OverlayManager;ZLxaero/map/cache/BlockStateShortShapeCache;L"+region+"MapUpdateFastConfig;)",false);
        if(XaeroMapNativeCheck.find(region+"MapUpdateFastConfig").methods.stream().noneMatch(m->m.name.equals("<init>")&&m.desc.equals("(L"+processor+";)V")))throw new AssertionError("MapUpdateFastConfig constructor changed");
        method(tile,"setBlock","(IIL"+block+";)",false);method(tile,"setWorldInterpretationVersion","(I)",false);method(tile,"setWrittenCave","(II)",false);method(tile,"setLoaded","(Z)",false);
        field(tile,"CURRENT_WORLD_INTERPRETATION_VERSION","I",true);
        method(block,"prepareForWriting","(I)",false);method(block,"write","(L"+mc+"class_2680;II"+key+"BZZ)",false);
        // Minecraft, for the registries, the server read of a chunk, and sending.
        // The uploader's reads: the same walk in reverse, plus the client's own position and send path.
        method(tile,"getBlock","(II)",false);method(tile,"isLoaded","()",false);method(region+"MapPixel","getState","()",false);
        for(String name:new String[]{"getBiome","getHeight","getTopHeight"})method(block,name,"()",false);
        method(client+"ClientPlayNetworking","canSend","(L"+type+";)",true);method(client+"ClientPlayNetworking","send","(L"+mc+"class_8710;)",true);
        field(mc+"class_310","field_1687",null,false);field(mc+"class_310","field_1724",null,false);method(mc+"class_310","method_1551","()",true);
        method(mc+"class_1937","method_27983","()",false);method(mc+"class_5321","method_29177","()",false);
        method(mc+"class_2378","method_10223","("+id+")",false);method(mc+"class_2378","method_10221","(Ljava/lang/Object;)",false);
        field(mc+"class_7923","field_41175",null,true);field(mc+"class_7924","field_41236",null,true);field(mc+"class_2902$class_2903","field_13202",null,true);
        method(mc+"class_2960","method_60654","(Ljava/lang/String;)",true);method(mc+"class_5321","method_29179","("+key+id+")",true);method(mc+"class_2248","method_9564","()",false);
        method(mc+"class_2791","method_12005","(L"+mc+"class_2902$class_2903;II)",false);method(mc+"class_2791","method_8320","("+pos+")",false);method(mc+"class_4538","method_23753","("+pos+")",false);
        method(mc+"class_6880","method_40230","()",false);method(mc+"class_4970$class_4971","method_26204","()",false);method(mc+"class_4970$class_4971","method_26227","()",false);method(mc+"class_3610","method_15769","()",false);
        method(mc+"class_1937","method_8402","(IIL"+mc+"class_2806;Z)",false);method(mc+"class_1937","method_8621","()",false);method(mc+"class_2784","method_11952","("+pos+")",false);
        method(mc+"class_3324","method_14571","()",false);method(mc+"class_3324","method_14568","()",false);method("net/minecraft/server/MinecraftServer","method_3760","()",false);
        method(mc+"class_3222","method_51469","()",false);method(mc+"class_1297","method_24515","()",false);method(mc+"class_1297","method_5845","()",false);
        // Fabric: registration, send/receive, tick and stop events, and the client's join/leave events.
        method(api+"PayloadTypeRegistry","playS2C","()",true);method(api+"PayloadTypeRegistry","playC2S","()",true);
        method(api+"ServerPlayNetworking","canSend","(L"+mc+"class_3222;L"+type+";)",true);method(api+"ServerPlayNetworking","send","(L"+mc+"class_3222;L"+mc+"class_8710;)",true);
        method(client+"ClientPlayNetworking","registerGlobalReceiver","(L"+type+";L"+client+"ClientPlayNetworking$PlayPayloadHandler;)",true);
        method(client+"ClientPlayNetworking$PlayPayloadHandler","receive","(L"+mc+"class_8710;L"+client+"ClientPlayNetworking$Context;)",false);
        field(life+"ServerTickEvents","END_SERVER_TICK",null,true);method(life+"ServerTickEvents$EndTick","onEndTick","(Lnet/minecraft/server/MinecraftServer;)",false);
        field(life+"ServerLifecycleEvents","SERVER_STOPPING",null,true);method(life+"ServerLifecycleEvents$ServerStopping","onServerStopping","(Lnet/minecraft/server/MinecraftServer;)",false);
        System.out.println("PASS: shared map hook binds to Xaero's map writer; every World Map, Fabric and Minecraft member used by the capture, relay and applier exists, and so do the client uploader's reads");
    }
}
