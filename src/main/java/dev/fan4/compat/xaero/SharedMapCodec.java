package dev.fan4.compat.xaero;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Batches of chunk summaries as a deflated stream, used on the wire and for the server's region files. Decoding is bounded. */
public final class SharedMapCodec {
    /** VERSION: the small messages (hello, mode). BATCH_VERSION: chunk batches, which also carry the block under water (version 1 batches, written by older builds, are still read). */
    public static final int VERSION=1,LEGACY_BATCH_VERSION=1,BATCH_VERSION=2,MAX_CHUNKS=512,MAX_INFLATED_BYTES=4*1024*1024,MAX_HELLO_DIMENSIONS=16;
    private SharedMapCodec() {}

    public static byte[] encode(List<SharedMapChunk> chunks)throws IOException {
        if(chunks.size()>MAX_CHUNKS)throw new IOException("too many chunks in one batch");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(new DeflaterOutputStream(bytes,new Deflater(Deflater.BEST_SPEED)))) {
            out.writeByte(BATCH_VERSION);out.writeShort(chunks.size());
            for(SharedMapChunk chunk:chunks)write(out,chunk);
        }
        return bytes.toByteArray();
    }
    private static void write(DataOutputStream out,SharedMapChunk chunk)throws IOException {
        if(!chunk.valid())throw new IOException("invalid chunk "+chunk);
        out.writeUTF(chunk.dimension);out.writeInt(chunk.x);out.writeInt(chunk.z);out.writeLong(chunk.stamp);
        out.writeByte(chunk.blocks.length);for(String id:chunk.blocks)out.writeUTF(id);
        out.writeByte(chunk.biomes.length);for(String id:chunk.biomes)out.writeUTF(id);
        out.write(chunk.block);out.write(chunk.biome);out.write(chunk.bed);
        for(short h:chunk.height)out.writeShort(h);
        for(short t:chunk.top)out.writeShort(t);
    }
    /** Throws IOException for anything malformed, oversized once inflated, or from another version. */
    public static List<SharedMapChunk> decode(byte[] data)throws IOException {
        try(DataInputStream in=new DataInputStream(new BoundedInputStream(new InflaterInputStream(new ByteArrayInputStream(data)),MAX_INFLATED_BYTES))) {
            int version=in.readUnsignedByte();
            if(version<LEGACY_BATCH_VERSION||version>BATCH_VERSION)throw new IOException("unsupported version");
            int count=in.readUnsignedShort();
            if(count>MAX_CHUNKS)throw new IOException("too many chunks");
            List<SharedMapChunk> chunks=new ArrayList<>(count);
            for(int i=0;i<count;i++)chunks.add(read(in,version));
            if(in.read()!=-1)throw new IOException("trailing bytes");
            return chunks;
        } catch(ZipException|EOFException broken){throw new IOException("damaged batch: "+broken.getMessage(),broken);}
    }
    private static SharedMapChunk read(DataInputStream in,int version)throws IOException {
        String dimension=in.readUTF();int x=in.readInt(),z=in.readInt();long stamp=in.readLong();
        String[] blocks=new String[in.readUnsignedByte()];for(int i=0;i<blocks.length;i++)blocks[i]=in.readUTF();
        String[] biomes=new String[in.readUnsignedByte()];for(int i=0;i<biomes.length;i++)biomes[i]=in.readUTF();
        byte[] block=new byte[SharedMapChunk.COLUMNS],biome=new byte[SharedMapChunk.COLUMNS],bed=new byte[SharedMapChunk.COLUMNS];in.readFully(block);in.readFully(biome);if(version>=BATCH_VERSION)in.readFully(bed);
        short[] height=new short[SharedMapChunk.COLUMNS],top=new short[SharedMapChunk.COLUMNS];
        for(int i=0;i<height.length;i++)height[i]=in.readShort();
        for(int i=0;i<top.length;i++)top[i]=in.readShort();
        SharedMapChunk chunk=new SharedMapChunk(dimension,x,z,stamp,blocks,biomes,block,biome,bed,height,top);
        if(!chunk.valid())throw new IOException("invalid chunk data");
        return chunk;
    }
    /** Client to server: for each dimension, the newest stamp the client has already applied. */
    public static byte[] encodeHello(Map<String,Long> since)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeByte(VERSION);
            List<Map.Entry<String,Long>> entries=new ArrayList<>(since.entrySet());
            entries.removeIf(e->!SharedMapChunk.validId(e.getKey()));
            if(entries.size()>MAX_HELLO_DIMENSIONS)entries=entries.subList(0,MAX_HELLO_DIMENSIONS);
            out.writeByte(entries.size());
            for(var e:entries){out.writeUTF(e.getKey());out.writeLong(e.getValue());}
        }
        return bytes.toByteArray();
    }
    public static Map<String,Long> decodeHello(byte[] data)throws IOException {
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(data))) {
            if(in.readUnsignedByte()!=VERSION)throw new IOException("unsupported version");
            int count=in.readUnsignedByte();if(count>MAX_HELLO_DIMENSIONS)throw new IOException("too many dimensions");
            Map<String,Long> since=new HashMap<>();
            for(int i=0;i<count;i++){String dimension=in.readUTF();long stamp=in.readLong();if(!SharedMapChunk.validId(dimension))throw new IOException("bad dimension");since.put(dimension,Math.max(0,stamp));}
            if(in.read()!=-1)throw new IOException("trailing bytes");
            return since;
        } catch(EOFException truncated){throw new IOException("truncated hello",truncated);}
    }
    /** Server to client: which mode the server runs, so the client knows whether to upload. */
    public static byte[] encodeMode(SharedMapMode mode){return new byte[]{VERSION,(byte)mode.ordinal()};}
    public static SharedMapMode decodeMode(byte[] data)throws IOException {
        if(data.length!=2||data[0]!=VERSION)throw new IOException("bad mode message");
        return SharedMapMode.fromOrdinal(data[1]);
    }
    /** Stops reading after a byte limit: a small deflated bomb must not become memory. */
    private static final class BoundedInputStream extends FilterInputStream {
        private long left;
        BoundedInputStream(InputStream in,long limit){super(in);left=limit;}
        public int read()throws IOException{if(left<=0)throw new IOException("batch too large when inflated");int b=super.read();if(b>=0)left--;return b;}
        public int read(byte[] b,int off,int len)throws IOException{
            if(left<=0)throw new IOException("batch too large when inflated");
            int n=super.read(b,off,(int)Math.min(len,left));if(n>0)left-=n;return n;
        }
    }
}
