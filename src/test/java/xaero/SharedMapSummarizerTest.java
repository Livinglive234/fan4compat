import dev.fan4.compat.xaero.*;
import java.lang.reflect.*;
import java.util.*;

public final class SharedMapSummarizerTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static final class Fluid {final boolean empty;Fluid(boolean empty){this.empty=empty;}public boolean method_15769(){return empty;}}
    public static final class State {final String block;final boolean fluid;State(String block,boolean fluid){this.block=block;this.fluid=fluid;}
        public Object method_26204(){return new net.minecraft.class_7923.Block(block);}public Object method_26227(){return new Fluid(!fluid);}}
    /** A chunk with a column function: block id and whether it is fluid at each height. */
    public static final class Chunk {
        final int[] surface=new int[256];final Map<Integer,State> overrides=new HashMap<>();int blockReads,heightReads;
        public int method_12005(Object type,int x,int z){heightReads++;return surface[z*16+x];}
        public Object method_8320(net.minecraft.class_2338 pos){blockReads++;State s=overrides.get(key(pos.x,pos.y,pos.z));return s!=null?s:new State("minecraft:grass_block",false);}
    }
    static int key(int x,int y,int z){return (x*31+y)*31+z;}
    public static final class Entry {final Object key;Entry(Object key){this.key=key;}public Optional<Object> method_40230(){return Optional.of(key);}}
    public static final class World {int biomeReads;public Object method_23753(net.minecraft.class_2338 pos){biomeReads++;return new Entry(net.minecraft.class_5321.method_29179("biome",pos.x%32==0?"minecraft:desert":"minecraft:plains"));}}
    public static void main(String[] args)throws Exception {
        Method summarize=Class.forName("dev.fan4.compat.xaero.SharedMapSummarizer").getDeclaredMethod("summarize",Object.class,Object.class,String.class,int.class,int.class,long.class);summarize.setAccessible(true);
        World world=new World();Chunk chunk=new Chunk();Arrays.fill(chunk.surface,65); // top block at y=64
        // A lake in the corner: water on top at y=64, sand below it.
        for(int x=0;x<4;x++)for(int z=0;z<4;z++){
            chunk.overrides.put(key(32+x,64,48+z),new State("minecraft:water",true));
            chunk.overrides.put(key(32+x,63,48+z),new State("minecraft:water",true));
            chunk.overrides.put(key(32+x,62,48+z),new State("minecraft:sand",false));
        }
        SharedMapChunk summary=(SharedMapChunk)summarize.invoke(null,world,chunk,"minecraft:overworld",2,3,12345L);
        check(summary!=null&&summary.valid()&&summary.x==2&&summary.z==3&&summary.stamp==12345&&summary.dimension.equals("minecraft:overworld"),"a chunk is summarized and valid");
        check(summary.blockAt(8,8).equals("minecraft:grass_block")&&summary.height[summary.index(8,8)]==64&&summary.top[summary.index(8,8)]==64,"dry land: top and terrain heights agree");
        check(summary.blockAt(1,1).equals("minecraft:water")&&summary.top[summary.index(1,1)]==64&&summary.height[summary.index(1,1)]==62,"under water the terrain height is the bed (62) and the top is the surface (64)");
        check(summary.bedAt(1,1).equals("minecraft:sand")&&summary.bedAt(8,8)==null,"under water the bed block is recorded (sand); dry land has none");
        check(summary.blocks.length==3&&summary.biomes.length==2,"palettes hold only what is used (grass, water, and the sand bed): "+Arrays.toString(summary.blocks)+Arrays.toString(summary.biomes));
        check(world.biomeReads==16,"biome read once per 4x4 columns (16 reads), not per column: "+world.biomeReads);
        check(chunk.heightReads==256,"one heightmap sample per column");

        // Biomes follow the cells.
        World mixed=new World();
        SharedMapChunk edge=(SharedMapChunk)summarize.invoke(null,mixed,chunk,"minecraft:overworld",2,3,1L);
        check(edge.biomeAt(0,0).equals("minecraft:desert")&&edge.biomeAt(3,3).equals("minecraft:desert")&&edge.biomeAt(4,0).equals("minecraft:plains"),"the biome read for a 4x4 cell applies to all its columns, the next cell has its own");

        // A deep ocean does not cost a whole column scan per column.
        Chunk ocean=new Chunk();Arrays.fill(ocean.surface,65);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=64;y++)ocean.overrides.put(key(32+x,y,48+z),new State("minecraft:water",true));
        SharedMapChunk deep=(SharedMapChunk)summarize.invoke(null,world,ocean,"minecraft:overworld",2,3,1L);
        check(deep.bedAt(0,0)==null,"no bed is recorded when the scan gave up before finding one");
        check(deep.top[0]==64&&deep.height[0]==0,"a fluid column deeper than the scan limit stops at the limit (height "+deep.height[0]+")");
        check(ocean.blockReads<256*70,"bounded block reads for an ocean: "+ocean.blockReads);

        // Too many distinct blocks refuses the chunk instead of corrupting the palette.
        Chunk crowded=new Chunk();Arrays.fill(crowded.surface,65);
        for(int i=0;i<256;i++)crowded.overrides.put(key(32+(i%16),64,48+(i/16)),new State("mod:block_"+i,false));
        check(summarize.invoke(null,world,crowded,"minecraft:overworld",2,3,1L)==null,"more than 255 distinct top blocks: not summarized");

        // The hello message.
        Map<String,Long> since=Map.of("minecraft:overworld",12345L,"minecraft:the_nether",7L);
        check(SharedMapCodec.decodeHello(SharedMapCodec.encodeHello(since)).equals(since),"hello round trip");
        check(SharedMapCodec.decodeHello(SharedMapCodec.encodeHello(Map.of("bad id",5L,"minecraft:overworld",9L))).equals(Map.of("minecraft:overworld",9L)),"invalid dimension ids are not sent");
        for(byte[] bad:new byte[][]{{},{1},{9,0},{1,2},{1,1,0,3,'a','b','c',0,0,0,0,0,0,0,1}}) {
            try{SharedMapCodec.decodeHello(bad);throw new AssertionError("accepted a malformed hello");}catch(java.io.IOException expected){}
        }
        byte[] trailing=Arrays.copyOf(SharedMapCodec.encodeHello(since),SharedMapCodec.encodeHello(since).length+1);
        try{SharedMapCodec.decodeHello(trailing);throw new AssertionError("accepted trailing bytes");}catch(java.io.IOException expected){}
        System.out.println("PASS: shared map summarizer (water beds, palettes, per-cell biomes, bounded ocean scans, palette overflow) and the hello message");
    }
}
