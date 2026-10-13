import dev.fan4.compat.xaero.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import xaero.map.region.*;

public final class SharedMapUploadTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    static final String OVERWORLD="minecraft:overworld";
    public static void main(String[] args)throws Exception {
        // Modes: NORMAL leaves Xaero alone; unknown text falls back to BOTH.
        check(SharedMapMode.parse("server")==SharedMapMode.SERVER&&SharedMapMode.parse(" Client ")==SharedMapMode.CLIENT&&SharedMapMode.parse("BOTH")==SharedMapMode.BOTH&&SharedMapMode.parse("normal")==SharedMapMode.NORMAL,"modes parse case-insensitively");
        check(SharedMapMode.parse(null)==SharedMapMode.BOTH&&SharedMapMode.parse("everything")==SharedMapMode.BOTH,"missing or unknown mode is BOTH");
        check(SharedMapMode.SERVER.captures&&!SharedMapMode.SERVER.acceptsUploads&&!SharedMapMode.CLIENT.captures&&SharedMapMode.CLIENT.acceptsUploads
            &&SharedMapMode.BOTH.captures&&SharedMapMode.BOTH.acceptsUploads&&!SharedMapMode.NORMAL.active()&&SharedMapMode.BOTH.active(),"what each mode does");
        for(SharedMapMode mode:SharedMapMode.values())check(SharedMapCodec.decodeMode(SharedMapCodec.encodeMode(mode))==mode,"mode message round trip "+mode);
        check(SharedMapMode.fromOrdinal(99)==SharedMapMode.NORMAL,"an unknown mode number from the wire means do nothing");
        boolean refused=false;try{SharedMapCodec.decodeMode(new byte[]{9,1});}catch(java.io.IOException expected){refused=true;}
        check(refused,"a mode message of another version is refused");

        // Read Xaero tiles back: whatever the applier writes, the reader returns.
        Class<?> applier=Class.forName("dev.fan4.compat.xaero.SharedMapApplier");Method apply=applier.getDeclaredMethod("apply",Object.class,SharedMapChunk.class);apply.setAccessible(true);
        Class<?> reader=Class.forName("dev.fan4.compat.xaero.SharedMapReader");Method read=reader.getDeclaredMethod("read",Object.class,String.class,int.class,int.class);read.setAccessible(true);
        SharedMapClientTest.Writer writer=new SharedMapClientTest.Writer();writer.processor.world.dimension=net.minecraft.class_5321.method_29179("dimension",OVERWORLD);
        SharedMapClientTest.Region region=SharedMapClientTest.loaded(writer,0,0);
        SharedMapChunk original=SharedMapTest.chunk(OVERWORLD,5,6,500,"minecraft:grass_block",64);
        apply.invoke(null,writer,original);
        MapTile tile=(MapTile)region.chunks.get(List.of(1,1)).getTile(1,2);
        SharedMapChunk readBack=(SharedMapChunk)read.invoke(null,tile,OVERWORLD,5,6);
        check(readBack!=null&&readBack.sameTerrain(original)&&readBack.x==5&&readBack.z==6&&readBack.contentHash()==original.contentHash(),"a tile read back equals the chunk it was written from");
        // Water read back from Xaero's overlay model: the block is the terrain, the (first) overlay is the fluid.
        byte[] lakeFluid=new byte[256],lakeSand=new byte[256];Arrays.fill(lakeFluid,(byte)1);Arrays.fill(lakeSand,(byte)1);short[] lakeBed=new short[256],lakeTop=new short[256];Arrays.fill(lakeBed,(short)60);Arrays.fill(lakeTop,(short)64);
        SharedMapChunk lake=new SharedMapChunk(OVERWORLD,9,9,1,new String[]{"minecraft:water","minecraft:sand"},new String[]{"minecraft:plains"},lakeSand,new byte[256],lakeFluid,lakeBed,lakeTop);
        apply.invoke(null,writer,lake);
        MapTile lakeTile=(MapTile)region.chunks.get(List.of(2,2)).getTile(1,1);
        SharedMapChunk lakeRead=(SharedMapChunk)read.invoke(null,lakeTile,OVERWORLD,9,9);
        check(lakeRead!=null&&lakeRead.sameTerrain(lake)&&lakeRead.blockAt(3,3).equals("minecraft:sand")&&lakeRead.fluidAt(3,3).equals("minecraft:water"),"a lake written to Xaero is read back as water over its terrain, so an upload keeps its water");
        tile.loaded=false;check(read.invoke(null,tile,OVERWORLD,5,6)==null,"a tile that is not loaded is not read");
        tile.loaded=true;tile.blocks.remove(List.of(3,3));check(read.invoke(null,tile,OVERWORLD,5,6)==null,"a tile with a missing column is not read");
        check(original.contentHash()==SharedMapTest.chunk(OVERWORLD,5,6,900,"minecraft:grass_block",64).contentHash()&&original.contentHash()!=SharedMapTest.chunk(OVERWORLD,5,6,500,"minecraft:sand",64).contentHash(),"the content hash ignores the stamp and sees terrain changes");

        // Server intake.
        Path root=Files.createTempDirectory("fan4intake");
        UUID alice=UUID.randomUUID(),bob=UUID.randomUUID();Set<String> shared=Set.of(OVERWORLD,"minecraft:the_nether");
        SharedMapStore store=new SharedMapStore(root,8);
        SharedMapIntake both=new SharedMapIntake(SharedMapMode.BOTH,shared,store);
        SharedMapChunk upload=SharedMapTest.chunk(OVERWORLD,40,41,1,"minecraft:sand",70);
        List<SharedMapChunk> stored=both.accept(alice,OVERWORLD,List.of(upload),123456,c->true);
        check(stored.size()==1&&stored.get(0).stamp==123456&&store.get(OVERWORLD,40,41).stamp==123456,"an upload is stored with the server's stamp, not the client's");
        byte[] wet=new byte[256],wetBlock=new byte[256];wet[0]=1;wetBlock[0]=2;
        SharedMapChunk wetUpload=new SharedMapChunk(OVERWORLD,42,43,1,new String[]{"minecraft:water","minecraft:stone","minecraft:sand"},new String[]{"minecraft:plains"},wetBlock,new byte[256],wet,upload.height,upload.top);
        both.accept(alice,OVERWORLD,List.of(wetUpload),123457,c->true);
        check(store.get(OVERWORLD,42,43)!=null&&store.get(OVERWORLD,42,43).blockAt(0,0).equals("minecraft:sand")&&store.get(OVERWORLD,42,43).fluidAt(0,0).equals("minecraft:water"),"the terrain and fluid survive the server's intake");
        check(both.accept(alice,OVERWORLD,List.of(SharedMapTest.chunk(OVERWORLD,40,41,2,"minecraft:snow",90)),123500,c->true).isEmpty()&&store.get(OVERWORLD,40,41).blockAt(0,0).equals("minecraft:sand"),"in BOTH an upload never replaces a held chunk");
        check(both.accept(alice,OVERWORLD,List.of(SharedMapTest.chunk("minecraft:the_nether",1,1,1,"minecraft:netherrack",70)),1,c->true).isEmpty(),"a chunk of a dimension the player is not in is refused");
        check(both.accept(alice,"minecraft:the_end",List.of(SharedMapTest.chunk("minecraft:the_end",1,1,1,"minecraft:end_stone",70)),1,c->true).isEmpty(),"a dimension that is not shared is refused");
        check(both.accept(alice,OVERWORLD,List.of(SharedMapTest.chunk(OVERWORLD,9999999,9,1,"minecraft:stone",70)),1,c->c.x<1000).isEmpty(),"a chunk outside the world is refused");
        SharedMapChunk broken=new SharedMapChunk(OVERWORLD,50,50,1,new String[]{"x"},new String[]{"y"},new byte[256],new byte[256],new short[256],new short[256]);
        check(both.accept(alice,OVERWORLD,List.of(broken),1,c->true).isEmpty(),"a structurally invalid chunk is refused");
        List<SharedMapChunk> flood=new ArrayList<>();for(int i=0;i<SharedMapIntake.PER_WINDOW+50;i++)flood.add(SharedMapTest.chunk(OVERWORLD,100+i,0,1,"minecraft:stone",70));
        int first=both.accept(bob,OVERWORLD,flood,1000,c->true).size();
        check(first==SharedMapIntake.PER_WINDOW,"a player is held to the rate limit: "+first);
        check(both.accept(bob,OVERWORLD,List.of(SharedMapTest.chunk(OVERWORLD,5000,0,1,"minecraft:stone",70)),1500,c->true).isEmpty(),"still limited within the window");
        check(both.accept(bob,OVERWORLD,List.of(SharedMapTest.chunk(OVERWORLD,5000,0,1,"minecraft:stone",70)),1000+SharedMapIntake.WINDOW_MS,c->true).size()==1,"and allowed again in the next window");
        check(both.accept(alice,OVERWORLD,List.of(SharedMapTest.chunk(OVERWORLD,6000,0,1,"minecraft:stone",70)),1,c->true).size()==1,"another player's budget is separate");
        SharedMapIntake client=new SharedMapIntake(SharedMapMode.CLIENT,shared,store);
        check(client.accept(alice,OVERWORLD,List.of(SharedMapTest.chunk(OVERWORLD,40,41,1,"minecraft:snow",90)),200000,c->true).size()==1&&store.get(OVERWORLD,40,41).blockAt(0,0).equals("minecraft:snow"),"in CLIENT mode a changed upload replaces the stored chunk");
        check(client.accept(alice,OVERWORLD,List.of(SharedMapTest.chunk(OVERWORLD,40,41,1,"minecraft:snow",90)),300000,c->true).isEmpty(),"and the same terrain again changes nothing");
        for(SharedMapMode off:new SharedMapMode[]{SharedMapMode.SERVER,SharedMapMode.NORMAL})
            check(new SharedMapIntake(off,shared,store).accept(alice,OVERWORLD,List.of(SharedMapTest.chunk(OVERWORLD,7000,0,1,"minecraft:stone",70)),1,c->true).isEmpty(),off+" takes no uploads");

        // Client sweep: finds tiles in loaded regions, batches them, does not repeat, skips what the server gave us, rests.
        Method reset=Class.forName("dev.fan4.compat.xaero.SharedMapUploader").getDeclaredMethod("reset");reset.setAccessible(true);
        Class<?> uploaderType=Class.forName("dev.fan4.compat.xaero.SharedMapUploader");
        Class<?> whereType=Class.forName("dev.fan4.compat.xaero.SharedMapUploader$Where");
        Constructor<?> whereNew=whereType.getDeclaredConstructors()[0];whereNew.setAccessible(true);
        Method tick=uploaderType.getDeclaredMethod("tick",Object.class,Consumer.class,whereType);tick.setAccessible(true);
        Method mode=uploaderType.getDeclaredMethod("serverMode",SharedMapMode.class);mode.setAccessible(true);
        Method known=uploaderType.getDeclaredMethod("known",SharedMapChunk.class);known.setAccessible(true);
        reset.invoke(null);
        SharedMapClientTest.Writer w=new SharedMapClientTest.Writer();w.processor.world.dimension=net.minecraft.class_5321.method_29179("dimension",OVERWORLD);
        SharedMapClientTest.Region r=SharedMapClientTest.loaded(w,0,0);
        SharedMapClientTest.Region far=SharedMapClientTest.loaded(w,5,-5);
        for(int i=0;i<10;i++)apply.invoke(null,w,SharedMapTest.chunk(OVERWORLD,i,0,10,"minecraft:grass_block",64+i));
        apply.invoke(null,w,SharedMapTest.chunk(OVERWORLD,160,-160,10,"minecraft:sand",70));
        Object here=whereNew.newInstance(OVERWORLD,3,3);
        List<List<SharedMapChunk>> sent=new ArrayList<>();Consumer<List<SharedMapChunk>> sender=sent::add;
        for(int f=0;f<200;f++)tick.invoke(null,w,sender,here);
        check(sent.isEmpty(),"before the server asks for uploads nothing is read or sent");
        mode.invoke(null,SharedMapMode.SERVER);for(int f=0;f<200;f++)tick.invoke(null,w,sender,here);
        check(sent.isEmpty(),"a SERVER-mode server asks for none");
        mode.invoke(null,SharedMapMode.BOTH);
        for(int f=0;f<400;f++)tick.invoke(null,w,sender,here);
        Set<String> got=new TreeSet<>();for(List<SharedMapChunk> b:sent)for(SharedMapChunk c:b)got.add(c.x+","+c.z+":"+c.blockAt(0,0));
        check(got.size()==11&&got.contains("0,0:minecraft:grass_block")&&got.contains("9,0:minecraft:grass_block")&&got.contains("160,-160:minecraft:sand"),"every mapped tile near the player is found once: "+got);
        for(List<SharedMapChunk> b:sent)check(b.size()<=64,"batches stay small");
        int after=sent.size();for(int f=0;f<2000;f++)tick.invoke(null,w,sender,here);
        check(sent.size()==after,"unchanged tiles are not sent again");
        MapTile changed=(MapTile)r.chunks.get(List.of(0,0)).getTile(0,0);((MapBlock)changed.blocks.get(List.of(0,0))).state=((MapBlock)changed.blocks.get(List.of(1,1))).state;
        ((MapBlock)changed.blocks.get(List.of(0,0))).height=((MapBlock)changed.blocks.get(List.of(0,0))).top=99;
        for(int f=0;f<2000;f++)tick.invoke(null,w,sender,here);
        check(sent.size()==after+1&&sent.get(sent.size()-1).size()==1&&sent.get(sent.size()-1).get(0).height[0]==99,"a tile that changed is sent again");
        // Chunks the server gave us are not echoed back.
        reset.invoke(null);mode.invoke(null,SharedMapMode.CLIENT);sent.clear();
        SharedMapChunk fromServer=SharedMapTest.chunk(OVERWORLD,70,70,10,"minecraft:stone",60);
        apply.invoke(null,w,fromServer);
        SharedMapClientTest.loaded(w,2,2);apply.invoke(null,w,fromServer);known.invoke(null,fromServer);
        for(int f=0;f<400;f++)tick.invoke(null,w,sender,here);
        boolean echoed=false;for(List<SharedMapChunk> b:sent)for(SharedMapChunk c:b)if(c.x==70&&c.z==70)echoed=true;
        check(!echoed,"a chunk the server supplied is not uploaded back");
        // Another dimension's map (a TARDIS exterior) is not uploaded as this world's terrain.
        reset.invoke(null);mode.invoke(null,SharedMapMode.BOTH);sent.clear();
        Object inTardis=whereNew.newInstance("minecraft:the_nether",3,3);
        for(int f=0;f<400;f++)tick.invoke(null,w,sender,inTardis);
        check(sent.isEmpty(),"when the map shows a different dimension than the world the player is in, nothing is uploaded");
        // A failure inside Xaero turns the uploader off quietly.
        reset.invoke(null);mode.invoke(null,SharedMapMode.BOTH);
        tick.invoke(null,new Object(),sender,here);for(int f=0;f<5;f++)tick.invoke(null,w,sender,here);
        check(sent.isEmpty(),"an Xaero change disables uploads for the session without affecting the game");
        reset.invoke(null);
        System.out.println("PASS: shared map modes, tile reader round trip, upload intake (stamp, rate limit, dimension/world checks, BOTH never overwrites) and client sweep (batching, no repeats, no echo, TARDIS, failure containment)");
    }
}
