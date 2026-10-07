package dev.fan4.compat.accessories;

import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Accessories IDs are server-global; portal-watched entities may be in another world. */
public final class PortalEntitySyncCompat {
    private record Pending(Object packet,Object player,int expires) {}
    private static final Deque<Pending> PENDING=new ArrayDeque<>();
    private static Object connectionPlayer;
    private static int tick;
    private static boolean replaying;
    public static Object entity(Object world,int id) {
        Object entity=call(world,"method_8469",id);
        if(entity!=null)return entity;
        Object loader=type("qouteall.imm_ptl.core.ClientWorldLoader");
        if(!(Boolean)call(loader,"getIsInitialized"))return null;
        for(Object other:(Iterable<?>)call(loader,"getClientWorlds")) {
            if(other==world)continue;
            entity=call(other,"method_8469",id);if(entity!=null)return entity;
        }
        return null;
    }
    public static boolean defer(Object packet,Object player) {
        if(replaying)return false;
        if(connectionPlayer!=player){PENDING.clear();connectionPlayer=player;}
        int id=((Number)call(packet,"entityId")).intValue();
        boolean ordered=PENDING.stream().anyMatch(p->((Number)call(p.packet,"entityId")).intValue()==id);
        if(!ordered&&entity(call(player,"method_37908"),id)!=null)return false;
        if(PENDING.size()==256) {
            int oldest=((Number)call(PENDING.peekFirst().packet,"entityId")).intValue();
            PENDING.removeIf(p->((Number)call(p.packet,"entityId")).intValue()==oldest);
        }
        PENDING.addLast(new Pending(packet,player,tick+100));return true;
    }
    public static void tick(Object client) {
        Object player=field(client,"field_1724");tick++;
        if(player==null||player!=connectionPlayer){PENDING.clear();connectionPlayer=player;return;}
        Set<Integer> waiting=new HashSet<>(),expired=new HashSet<>();
        for(Pending p:PENDING)if(p.expires<=tick)expired.add(((Number)call(p.packet,"entityId")).intValue());
        PENDING.removeIf(p->expired.contains(((Number)call(p.packet,"entityId")).intValue()));
        for(Iterator<Pending> it=PENDING.iterator();it.hasNext();) {
            Pending pending=it.next();int id=((Number)call(pending.packet,"entityId")).intValue();
            if(pending.expires<=tick){it.remove();continue;}
            if(waiting.contains(id))continue;
            if(entity(call(player,"method_37908"),id)==null){waiting.add(id);continue;}
            it.remove();replaying=true;
            try{call(pending.packet.getClass(),"handlePacket",pending.packet,pending.player);}finally{replaying=false;}
        }
    }
}
