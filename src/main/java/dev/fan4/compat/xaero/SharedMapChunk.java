package dev.fan4.compat.xaero;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * A 16x16 surface summary of one chunk: for every column the top block, its biome, the height of the terrain and the height
 * of the topmost non-air block (they differ under water). Blocks and biomes are registry ids through a small per-chunk
 * palette. Immutable by convention; {@link #stamp} orders updates and is not part of the content.
 */
public final class SharedMapChunk {
    public static final int COLUMNS=256,MAX_PALETTE=255,MIN_Y=-2048,MAX_Y=4096;
    private static final Pattern ID=Pattern.compile("[a-z0-9_.-]{1,64}:[a-z0-9_./-]{1,96}");
    public final String dimension;public final int x,z;public final long stamp;
    public final String[] blocks,biomes;
    public final byte[] block,biome;public final short[] height,top;
    public SharedMapChunk(String dimension,int x,int z,long stamp,String[] blocks,String[] biomes,byte[] block,byte[] biome,short[] height,short[] top) {
        this.dimension=dimension;this.x=x;this.z=z;this.stamp=stamp;this.blocks=blocks;this.biomes=biomes;this.block=block;this.biome=biome;this.height=height;this.top=top;
    }
    public static boolean validId(String id){return id!=null&&ID.matcher(id).matches();}
    /** Structural limits; decoders reject anything that fails so a hostile peer cannot smuggle odd data into the map. */
    public boolean valid() {
        if(!validId(dimension)||blocks.length==0||blocks.length>MAX_PALETTE||biomes.length==0||biomes.length>MAX_PALETTE)return false;
        if(block.length!=COLUMNS||biome.length!=COLUMNS||height.length!=COLUMNS||top.length!=COLUMNS)return false;
        for(String id:blocks)if(!validId(id))return false;
        for(String id:biomes)if(!validId(id))return false;
        for(int i=0;i<COLUMNS;i++) {
            if((block[i]&0xff)>=blocks.length||(biome[i]&0xff)>=biomes.length)return false;
            if(height[i]<MIN_Y||height[i]>MAX_Y||top[i]<MIN_Y||top[i]>MAX_Y||top[i]<height[i])return false;
        }
        return true;
    }
    public int index(int columnX,int columnZ){return columnZ*16+columnX;}
    public String blockAt(int columnX,int columnZ){return blocks[block[index(columnX,columnZ)]&0xff];}
    public String biomeAt(int columnX,int columnZ){return biomes[biome[index(columnX,columnZ)]&0xff];}
    public long key(){return key(x,z);}
    public static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    /** Same terrain, ignoring the update stamp and how the palette happens to be ordered. */
    public boolean sameTerrain(SharedMapChunk other) {
        if(other==null||x!=other.x||z!=other.z||!dimension.equals(other.dimension)||!Arrays.equals(height,other.height)||!Arrays.equals(top,other.top))return false;
        for(int i=0;i<COLUMNS;i++)if(!blocks[block[i]&0xff].equals(other.blocks[other.block[i]&0xff])||!biomes[biome[i]&0xff].equals(other.biomes[other.biome[i]&0xff]))return false;
        return true;
    }
    /** A fingerprint of the terrain that ignores the stamp and the palette order, to tell "already sent" from "changed". */
    public long contentHash() {
        long hash=1125899906842597L*31+Arrays.hashCode(height);hash=hash*31+Arrays.hashCode(top);
        for(int i=0;i<COLUMNS;i++)hash=(hash*31+blocks[block[i]&0xff].hashCode())*31+biomes[biome[i]&0xff].hashCode();
        return hash;
    }
    public String toString(){return "SharedMapChunk["+dimension+" "+x+","+z+"]";}
}
