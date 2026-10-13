package dev.fan4.compat.xaero;

import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * Reads one of Xaero's map tiles (a 16x16 chunk of its stored surface) into a {@link SharedMapChunk}; the reverse of
 * {@link SharedMapApplier}. Client side, by reflection. A tile that has any column Xaero has not filled is not summarized.
 */
final class SharedMapReader {
    private SharedMapReader() {}

    /** The tile's surface, or null when it is incomplete or does not fit the summary (for example too many distinct blocks). */
    static SharedMapChunk read(Object tile,String dimension,int chunkX,int chunkZ) {
        if(!(Boolean)call(tile,"isLoaded"))return null;
        Object registry=field(type("net.minecraft.class_7923"),"field_41175");
        // Most columns share their block and biome objects: resolve each to its id once per tile.
        Map<Object,String> blockIds=new IdentityHashMap<>(),biomeIds=new IdentityHashMap<>();
        Map<String,Integer> blockPalette=new LinkedHashMap<>(),biomePalette=new LinkedHashMap<>();
        byte[] block=new byte[SharedMapChunk.COLUMNS],biome=new byte[SharedMapChunk.COLUMNS],bed=new byte[SharedMapChunk.COLUMNS];
        short[] height=new short[SharedMapChunk.COLUMNS],top=new short[SharedMapChunk.COLUMNS];
        for(int z=0;z<16;z++)
            for(int x=0;x<16;x++) {
                Object pixel=call(tile,"getBlock",x,z);
                if(pixel==null)return null;
                Object state=call(pixel,"getState"),biomeKey=call(pixel,"getBiome");
                if(state==null||biomeKey==null)return null;
                int column=z*16+x,terrain=(Integer)call(pixel,"getHeight"),surface=(Integer)call(pixel,"getTopHeight");
                if(terrain<SharedMapChunk.MIN_Y||surface>SharedMapChunk.MAX_Y||surface<terrain)return null;
                // Xaero draws water as an overlay on the bed: what is seen from above is the (first) overlay, and the block under it is the bed.
                List<?> overlays=(List<?>)call(pixel,"getOverlays");
                boolean covered=overlays!=null&&!overlays.isEmpty();
                Object shown=covered?call(overlays.get(0),"getState"):state;
                int blockIndex=SharedMapChunk.paletteIndex(blockPalette,blockIds.computeIfAbsent(shown,s->String.valueOf(call(registry,"method_10221",call(s,"method_26204"))))),
                    bedIndex=covered?SharedMapChunk.paletteIndex(blockPalette,blockIds.computeIfAbsent(state,s->String.valueOf(call(registry,"method_10221",call(s,"method_26204"))))):-2,
                    biomeIndex=SharedMapChunk.paletteIndex(biomePalette,biomeIds.computeIfAbsent(biomeKey,k->String.valueOf(call(k,"method_29177"))));
                if(blockIndex<0||bedIndex==-1||biomeIndex<0)return null;
                block[column]=(byte)blockIndex;biome[column]=(byte)biomeIndex;if(covered)bed[column]=(byte)(bedIndex+1);height[column]=(short)terrain;top[column]=(short)surface;
            }
        SharedMapChunk summary=new SharedMapChunk(dimension,chunkX,chunkZ,0,blockPalette.keySet().toArray(String[]::new),biomePalette.keySet().toArray(String[]::new),block,biome,bed,height,top);
        return summary.valid()?summary:null;
    }
}
