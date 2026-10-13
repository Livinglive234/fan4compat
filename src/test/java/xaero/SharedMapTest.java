import dev.fan4.compat.xaero.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class SharedMapTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static SharedMapChunk chunk(String dim,int x,int z,long stamp,String top,int height){
        byte[] block=new byte[256],biome=new byte[256];short[] h=new short[256],t=new short[256];
        for(int i=0;i<256;i++){h[i]=(short)height;t[i]=(short)height;}
        return new SharedMapChunk(dim,x,z,stamp,new String[]{top,"minecraft:stone"},new String[]{"minecraft:plains"},block,biome,h,t);
    }
    static boolean fails(IOExceptionRunnable r){try{r.run();return false;}catch(IOException expected){return true;}}
    interface IOExceptionRunnable{void run()throws IOException;}
    public static void main(String[] args)throws Exception {
        String overworld="minecraft:overworld";
        SharedMapChunk a=chunk(overworld,3,-5,100,"minecraft:grass_block",64);
        check(a.valid()&&a.blockAt(2,3).equals("minecraft:grass_block")&&a.biomeAt(0,0).equals("minecraft:plains"),"a plain chunk is valid and readable");
        check(!chunk("Overworld",0,0,1,"minecraft:stone",64).valid()&&!chunk(overworld,0,0,1,"not an id",64).valid()&&!chunk(overworld,0,0,1,"minecraft:stone",9999).valid(),"bad dimension, block id or height rejected");
        byte[] outOfRange=new byte[256];outOfRange[7]=5;
        check(!new SharedMapChunk(overworld,0,0,1,a.blocks,a.biomes,outOfRange,a.biome,a.height,a.top).valid(),"palette index beyond the palette rejected");
        short[] belowSurface=new short[256];Arrays.fill(belowSurface,(short)10);
        check(!new SharedMapChunk(overworld,0,0,1,a.blocks,a.biomes,a.block,a.biome,a.height,belowSurface).valid(),"a top height under the surface height is rejected");
        check(a.sameTerrain(chunk(overworld,3,-5,999,"minecraft:grass_block",64))&&!a.sameTerrain(chunk(overworld,3,-5,100,"minecraft:sand",64))&&!a.sameTerrain(chunk(overworld,3,-5,100,"minecraft:grass_block",65)),"terrain comparison ignores the stamp only");
        check(SharedMapChunk.key(-1,-1)!=SharedMapChunk.key(-1,0)&&SharedMapChunk.key(1,2)!=SharedMapChunk.key(2,1),"chunk keys are distinct");

        // Fluid: the terrain block and the fluid on it travel with the chunk, in the wire format and in region files; older batches are still read.
        byte[] terrain=new byte[256],fluids=new byte[256];Arrays.fill(terrain,(byte)1);fluids[5]=1;   // stone everywhere; column 5 has palette index 0 (water) on it
        SharedMapChunk lake=new SharedMapChunk(overworld,1,1,7,new String[]{"minecraft:water","minecraft:stone"},new String[]{"minecraft:plains"},terrain,new byte[256],fluids,a.height,a.top);
        check(lake.valid()&&lake.blockAt(5,0).equals("minecraft:stone")&&lake.fluidAt(5,0).equals("minecraft:water")&&lake.fluidAt(6,0)==null,"a chunk with a fluid is valid and readable");
        SharedMapChunk lakeBack=SharedMapCodec.decode(SharedMapCodec.encode(List.of(lake))).get(0);
        check(lakeBack.fluidAt(5,0).equals("minecraft:water")&&lakeBack.sameTerrain(lake)&&lakeBack.contentHash()==lake.contentHash(),"the fluid survives the wire format");
        check(!lake.sameTerrain(a)&&lake.contentHash()!=a.contentHash(),"a different fluid is different terrain");
        byte[] outOfPalette=new byte[256];outOfPalette[0]=9;
        check(!new SharedMapChunk(overworld,1,1,7,lake.blocks,lake.biomes,lake.block,lake.biome,outOfPalette,lake.height,lake.top).valid(),"a fluid index beyond the palette is rejected");
        ByteArrayOutputStream oldBatch=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(new DeflaterOutputStream(oldBatch))) {
            out.writeByte(1);out.writeShort(1);out.writeUTF(overworld);out.writeInt(4);out.writeInt(5);out.writeLong(99);
            out.writeByte(1);out.writeUTF("minecraft:grass_block");out.writeByte(1);out.writeUTF("minecraft:plains");
            out.write(new byte[256]);out.write(new byte[256]);for(int i=0;i<256;i++)out.writeShort(64);for(int i=0;i<256;i++)out.writeShort(64);
        }
        SharedMapChunk legacy=SharedMapCodec.decode(oldBatch.toByteArray()).get(0);
        check(legacy.valid()&&legacy.x==4&&legacy.z==5&&legacy.fluidAt(0,0)==null,"a version 1 batch (no fluid) is still read");
        // Version 2 stored the water as the block and the block under it as a "bed" (1 + palette index); it is read as terrain plus fluid.
        ByteArrayOutputStream bedBatch=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(new DeflaterOutputStream(bedBatch))) {
            byte[] shown=new byte[256],bed=new byte[256];bed[0]=2;bed[1]=0;
            out.writeByte(2);out.writeShort(1);out.writeUTF(overworld);out.writeInt(6);out.writeInt(7);out.writeLong(99);
            out.writeByte(2);out.writeUTF("minecraft:water");out.writeUTF("minecraft:stone");out.writeByte(1);out.writeUTF("minecraft:plains");
            out.write(shown);out.write(new byte[256]);out.write(bed);for(int i=0;i<256;i++)out.writeShort(64);for(int i=0;i<256;i++)out.writeShort(66);
        }
        SharedMapChunk oldLake=SharedMapCodec.decode(bedBatch.toByteArray()).get(0);
        check(oldLake.valid()&&oldLake.blockAt(0,0).equals("minecraft:stone")&&oldLake.fluidAt(0,0).equals("minecraft:water")&&oldLake.blockAt(1,0).equals("minecraft:water")&&oldLake.fluidAt(1,0)==null,"a version 2 batch (water as block, bed beside it) is read as terrain plus fluid");

        // Codec: round trip, and every malformed shape refused.
        List<SharedMapChunk> batch=List.of(a,chunk(overworld,4,-5,101,"minecraft:sand",70));
        byte[] encoded=SharedMapCodec.encode(batch);
        List<SharedMapChunk> back=SharedMapCodec.decode(encoded);
        check(back.size()==2&&back.get(0).sameTerrain(a)&&back.get(0).stamp==100&&back.get(1).blockAt(0,0).equals("minecraft:sand"),"batch round trip");
        check(encoded.length<2*1500,"a flat chunk compresses well: "+encoded.length+" bytes for two");
        check(fails(()->SharedMapCodec.decode(Arrays.copyOf(encoded,encoded.length/2)))&&fails(()->SharedMapCodec.decode(new byte[]{1,2,3}))&&fails(()->SharedMapCodec.decode(new byte[0])),"truncated or garbage batches rejected");
        ByteArrayOutputStream wrongVersion=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new DeflaterOutputStream(wrongVersion))){out.writeByte(9);out.writeShort(0);}
        check(fails(()->SharedMapCodec.decode(wrongVersion.toByteArray())),"another version rejected");
        ByteArrayOutputStream bomb=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new DeflaterOutputStream(bomb))){out.writeByte(1);out.writeShort(1);out.write(new byte[8*1024*1024]);}
        check(bomb.size()<100_000&&fails(()->SharedMapCodec.decode(bomb.toByteArray())),"an inflation bomb stops at the size limit");
        check(fails(()->SharedMapCodec.encode(Collections.nCopies(SharedMapCodec.MAX_CHUNKS+1,a))),"oversized batch refused on encode");

        // Store: put/get, dedupe, newer wins, persistence, eviction, damaged files.
        Path dir=Files.createTempDirectory("fan4sharedmap");
        SharedMapStore store=new SharedMapStore(dir,4);
        check(store.put(a)&&store.get(overworld,3,-5)!=null,"a new chunk is stored");
        check(!store.put(chunk(overworld,3,-5,200,"minecraft:grass_block",64)),"identical terrain is not a change");
        check(store.put(chunk(overworld,3,-5,300,"minecraft:sand",64))&&store.get(overworld,3,-5).blockAt(0,0).equals("minecraft:sand"),"changed terrain replaces it");
        check(!store.put(chunk(overworld,3,-5,250,"minecraft:stone",64))&&store.get(overworld,3,-5).blockAt(0,0).equals("minecraft:sand"),"an older stamp never overwrites a newer one");
        check(!store.put(chunk("bad",0,0,1,"minecraft:stone",64)),"an invalid chunk is refused");
        store.put(chunk("minecraft:the_nether",3,-5,1,"minecraft:netherrack",60));
        check(store.get("minecraft:the_nether",3,-5).blockAt(0,0).equals("minecraft:netherrack")&&store.get(overworld,3,-5).blockAt(0,0).equals("minecraft:sand"),"dimensions are separate");
        for(int i=0;i<10;i++)store.put(chunk(overworld,i*16,0,1,"minecraft:stone",64));
        check(store.get(overworld,0,0)!=null&&store.get(overworld,144,0)!=null,"chunks in evicted regions come back from disk");
        store.flush();
        SharedMapStore reopened=new SharedMapStore(dir,4);
        check(reopened.get(overworld,3,-5).blockAt(0,0).equals("minecraft:sand")&&reopened.get("minecraft:the_nether",3,-5)!=null&&reopened.get(overworld,144,0)!=null,"everything survives a restart");
        List<int[]> regions=reopened.regions(overworld);
        check(regions.size()==11&&regions.stream().anyMatch(r->r[0]==0&&r[1]==-1),"regions listed from disk: "+regions.size());
        check(reopened.region(overworld,0,-1).size()==1&&reopened.region(overworld,0,-1).get(0).x==3,"a region lists its chunks");
        try(var files=Files.walk(dir)){check(files.noneMatch(p->p.toString().endsWith(".tmp")),"no temporary files left");}
        Path victim;try(var files=Files.walk(dir)){victim=files.filter(p->p.toString().endsWith(".smc")).findFirst().orElseThrow();}
        Files.write(victim,new byte[]{1,2,3,4});
        SharedMapStore damaged=new SharedMapStore(dir,4);
        damaged.regions(overworld).forEach(r->damaged.region(overworld,r[0],r[1]));
        try(var files=Files.walk(dir)){check(files.anyMatch(p->p.getFileName().toString().contains(".damaged-")),"a damaged region is kept aside, not overwritten");}
        System.out.println("PASS: shared map chunk validation, bounded batch codec (including an inflation bomb), and the server region store with dedupe, newest-wins, eviction, restart and damaged-file handling");
    }
}
