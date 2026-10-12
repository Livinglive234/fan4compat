package dev.fan4.compat.xaero;

import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Reads the surface of one loaded server chunk into a {@link SharedMapChunk}. Server side, by reflection. */
final class SharedMapSummarizer {
    private static final String POS="net.minecraft.class_2338";
    private SharedMapSummarizer() {}

    /** The chunk's surface, or null when it cannot be summarized (for example more distinct blocks than the palette allows). */
    static SharedMapChunk summarize(Object world,Object chunk,String dimension,int chunkX,int chunkZ,long stamp) {
        Object surface=field(type("net.minecraft.class_2902$class_2903"),"field_13202"),blocks=field(type("net.minecraft.class_7923"),"field_41175");
        Map<String,Integer> blockPalette=new LinkedHashMap<>(),biomePalette=new LinkedHashMap<>();
        byte[] block=new byte[SharedMapChunk.COLUMNS],biome=new byte[SharedMapChunk.COLUMNS];
        short[] height=new short[SharedMapChunk.COLUMNS],top=new short[SharedMapChunk.COLUMNS];
        byte[] cellBiome=new byte[16];
        // Columns mostly share block states: resolve each state's block id and whether it holds fluid once per chunk.
        Map<Object,String> ids=new IdentityHashMap<>();Map<Object,Boolean> dry=new IdentityHashMap<>();
        for(int z=0;z<16;z++)
            for(int x=0;x<16;x++) {
                int topY=(Integer)call(chunk,"method_12005",surface,x,z)-1;
                Object state=call(chunk,"method_8320",create(POS,chunkX*16+x,topY,chunkZ*16+z));
                String id=ids.computeIfAbsent(state,t->String.valueOf(call(blocks,"method_10221",call(t,"method_26204"))));
                int terrainY=topY;
                // Under fluid, the terrain is the first non-fluid block below (bounded so a deep ocean costs little).
                if(!dry.computeIfAbsent(state,t->(Boolean)call(call(t,"method_26227"),"method_15769")))
                    for(int depth=1;depth<=64;depth++) {
                        Object below=call(chunk,"method_8320",create(POS,chunkX*16+x,topY-depth,chunkZ*16+z));
                        terrainY=topY-depth;
                        if(dry.computeIfAbsent(below,t->(Boolean)call(call(t,"method_26227"),"method_15769")))break;
                    }
                int column=z*16+x;
                Integer blockIndex=blockPalette.computeIfAbsent(id,key->blockPalette.size());
                if(blockIndex>SharedMapChunk.MAX_PALETTE-1)return null;
                block[column]=(byte)(int)blockIndex;
                // Biomes change slowly: read one per 4x4 cell of columns.
                int cell=(z>>2)*4+(x>>2);
                if((x&3)==0&&(z&3)==0) {
                    Object entry=call(world,"method_23753",create(POS,chunkX*16+x,topY,chunkZ*16+z));
                    Optional<?> key=(Optional<?>)call(entry,"method_40230");
                    String biomeId=key.isPresent()?String.valueOf(call(key.get(),"method_29177")):"minecraft:plains";
                    Integer biomeIndex=biomePalette.computeIfAbsent(biomeId,k->biomePalette.size());
                    if(biomeIndex>SharedMapChunk.MAX_PALETTE-1)return null;
                    cellBiome[cell]=(byte)(int)biomeIndex;
                }
                biome[column]=cellBiome[cell];
                top[column]=(short)topY;height[column]=(short)Math.min(terrainY,topY);
            }
        SharedMapChunk summary=new SharedMapChunk(dimension,chunkX,chunkZ,stamp,blockPalette.keySet().toArray(String[]::new),biomePalette.keySet().toArray(String[]::new),block,biome,height,top);
        return summary.valid()?summary:null;
    }
}
