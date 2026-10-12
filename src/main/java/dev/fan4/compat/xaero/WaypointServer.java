package dev.fan4.compat.xaero;

import java.io.IOException;
import java.util.*;

/** Server-side behavior, free of Minecraft types: answer a join with the player's files and store their uploads. */
public final class WaypointServer {
    private final WaypointStore store;private final Map<UUID,WaypointProtocol.Assembler> assemblers=new HashMap<>();
    public WaypointServer(WaypointStore store){this.store=store;}
    public WaypointStore.Limits limits(){return store.limits();}
    public byte[] snapshot(UUID player)throws IOException {
        List<WaypointProtocol.Item> items=new ArrayList<>();
        for(var entry:store.files(player).entrySet())items.add(new WaypointProtocol.Item(entry.getKey(),entry.getValue()));
        return WaypointProtocol.encode(new WaypointProtocol.Snapshot(items));
    }
    /** Decode and handle one client message; returns null on success or why it was refused. */
    public synchronized String receive(UUID player,byte[] payload,long now) {
        try {
            WaypointProtocol.Message message=WaypointProtocol.decode(payload,store.limits().maxFileBytes());
            if(message instanceof WaypointProtocol.Chunk chunk) {
                WaypointProtocol.Assembler.Complete done=assemblers.computeIfAbsent(player,id->new WaypointProtocol.Assembler(store.limits().maxFileBytes())).accept(chunk,now);
                if(done!=null&&store.put(player,done.key(),done.baseVersion(),done.text(),now) instanceof WaypointStore.Result.Rejected rejected)return rejected.reason();
            }
            return null;
        } catch(IOException invalid){return invalid.getMessage();}
    }
    public static boolean isDone(byte[] payload) {
        try{return WaypointProtocol.decode(payload,Integer.MAX_VALUE) instanceof WaypointProtocol.Done;}catch(IOException invalid){return false;}
    }
}
