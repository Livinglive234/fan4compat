package dev.fan4.compat.xaero;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The single payload both sides exchange is one byte array; this class defines and bounds its contents.
 * Client-to-server custom payloads are limited to about 32 KiB, so files travel as numbered chunks.
 */
public final class WaypointProtocol {
    public static final int VERSION=1,CHUNK_BYTES=24_000,MAX_ITEMS=64;
    private static final int SNAPSHOT=1,CHUNK=2,DONE=3;
    public sealed interface Message {}
    public record Item(String key,WaypointStore.Entry entry) {}
    /** Server to client: every stored file for this player. */
    public record Snapshot(List<Item> items) implements Message {}
    /** Client to server: one piece of an uploaded file; {@code baseVersion} is the server version the client last saw. */
    public record Chunk(String key,long baseVersion,int index,int total,byte[] data) implements Message {}
    /** Client to server: nothing more to send in this phase. */
    public record Done() implements Message {}
    private WaypointProtocol() {}

    public static byte[] encode(Message message)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        if(message instanceof Snapshot s) {
            out.writeByte(SNAPSHOT);out.writeByte(VERSION);out.writeShort(s.items.size());
            for(Item item:s.items) {
                out.writeUTF(item.key);out.writeLong(item.entry.version());out.writeLong(item.entry.savedAt());
                byte[] text=item.entry.text().getBytes(StandardCharsets.UTF_8);out.writeInt(text.length);out.write(text);
            }
        } else if(message instanceof Chunk c) {
            out.writeByte(CHUNK);out.writeUTF(c.key);out.writeLong(c.baseVersion);out.writeShort(c.index);out.writeShort(c.total);out.writeInt(c.data.length);out.write(c.data);
        } else out.writeByte(DONE);
        return bytes.toByteArray();
    }
    /** Throws IOException for anything malformed, oversized or from another protocol version. */
    public static Message decode(byte[] payload,int maxFileBytes)throws IOException {
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload));
        int tag=in.readUnsignedByte();
        switch(tag) {
            case SNAPSHOT -> {
                if(in.readUnsignedByte()!=VERSION)throw new IOException("unsupported protocol version");
                int count=in.readUnsignedShort();if(count>MAX_ITEMS)throw new IOException("too many files");
                List<Item> items=new ArrayList<>();
                for(int i=0;i<count;i++) {
                    String key=in.readUTF();long version=in.readLong(),savedAt=in.readLong();
                    int length=in.readInt();if(length<0||length>maxFileBytes||length>in.available())throw new IOException("bad file length");
                    byte[] text=new byte[length];in.readFully(text);
                    if(!WaypointKeys.valid(key))throw new IOException("bad file name");
                    String decoded=new String(text,StandardCharsets.UTF_8);
                    if(!WaypointKeys.validText(decoded,maxFileBytes))throw new IOException("bad file text");
                    items.add(new Item(key,new WaypointStore.Entry(version,savedAt,decoded)));
                }
                finish(in);return new Snapshot(items);
            }
            case CHUNK -> {
                String key=in.readUTF();long base=in.readLong();int index=in.readUnsignedShort(),total=in.readUnsignedShort(),length=in.readInt();
                if(length<0||length>CHUNK_BYTES||length>in.available())throw new IOException("bad chunk length");
                if(total<1||total>maxChunks(maxFileBytes)||index>=total)throw new IOException("bad chunk numbering");
                if(!WaypointKeys.valid(key))throw new IOException("bad file name");
                byte[] data=new byte[length];in.readFully(data);finish(in);return new Chunk(key,base,index,total,data);
            }
            case DONE -> {finish(in);return new Done();}
            default -> throw new IOException("unknown message");
        }
    }
    private static void finish(DataInputStream in)throws IOException{if(in.available()!=0)throw new IOException("trailing bytes");}
    public static int maxChunks(int maxFileBytes){return (maxFileBytes+CHUNK_BYTES-1)/CHUNK_BYTES;}
    /** The chunks that carry one file; empty text is still one chunk so the server can store an emptied file. */
    public static List<Chunk> split(String key,long baseVersion,String text) {
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);
        int total=Math.max(1,(bytes.length+CHUNK_BYTES-1)/CHUNK_BYTES);
        List<Chunk> chunks=new ArrayList<>();
        for(int i=0;i<total;i++)chunks.add(new Chunk(key,baseVersion,i,total,Arrays.copyOfRange(bytes,i*CHUNK_BYTES,Math.min(bytes.length,(i+1)*CHUNK_BYTES))));
        return chunks;
    }

    /** Server side: puts chunks of one connection back together, in order, with bounded memory and age. */
    public static final class Assembler {
        public record Complete(String key,long baseVersion,String text) {}
        private static final int MAX_PARTIAL=8;private static final long STALE_MILLIS=60_000;
        private record Partial(long base,int total,int next,ByteArrayOutputStream bytes,long started) {}
        private final Map<String,Partial> partial=new HashMap<>();private final int maxFileBytes;
        public Assembler(int maxFileBytes){this.maxFileBytes=maxFileBytes;}
        /** The finished file when this was its last chunk; null while more are expected; throws on any inconsistency. */
        public Complete accept(Chunk chunk,long now)throws IOException {
            partial.values().removeIf(p->now-p.started>STALE_MILLIS);
            Partial current=partial.get(chunk.key);
            if(chunk.index==0) {
                if(current==null&&partial.size()>=MAX_PARTIAL)throw new IOException("too many unfinished uploads");
                current=new Partial(chunk.baseVersion,chunk.total,0,new ByteArrayOutputStream(),now);
            } else if(current==null||current.next!=chunk.index||current.total!=chunk.total||current.base!=chunk.baseVersion) {
                partial.remove(chunk.key);throw new IOException("chunk out of order");
            }
            if(current.bytes.size()+chunk.data.length>maxFileBytes){partial.remove(chunk.key);throw new IOException("file too large");}
            current.bytes.writeBytes(chunk.data);
            if(chunk.index+1<chunk.total){partial.put(chunk.key,new Partial(current.base,current.total,chunk.index+1,current.bytes,current.started));return null;}
            partial.remove(chunk.key);
            return new Complete(chunk.key,current.base,new String(current.bytes.toByteArray(),StandardCharsets.UTF_8));
        }
    }
}
