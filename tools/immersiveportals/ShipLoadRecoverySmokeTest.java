import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public final class ShipLoadRecoverySmokeTest {
    public static class Server {}
    public static class World {public boolean field_9236;public String dimension="overworld";Server server;public World(Server s){server=s;}public String method_27983(){return dimension;}public Server method_8503(){return server;}}
    public static class Client {static Client INSTANCE;public World field_1687;public Player field_1724;public static Client method_1551(){return INSTANCE;}}
    public static class Vec {public double field_1352=.5,field_1351=1,field_1350=.5;}
    public static class Box {public double field_1323=.2,field_1322=1,field_1321=.2,field_1320=.8,field_1325=2.8,field_1324=.8;}
    public static class Player {World world;int acks;boolean removed;Set<Long> known=new HashSet<>();Player(World w){world=w;}public World method_37908(){return world;}public Vec method_19538(){return new Vec();}public Box method_5829(){return new Box();}public boolean method_31481(){return removed;}public void vs_addKnownShip(long id){known.add(id);acks++;}}
    public static class Bounds {public double minX(){return 0;}public double minY(){return 0;}public double minZ(){return 0;}public double maxX(){return 16;}public double maxY(){return 16;}public double maxZ(){return 16;}}
    public static class Active {public boolean contains(int x,int z){return x==0&&z==0;}}
    public static class Matrix {
        public double m00(){return 1;}public double m01(){return 0;}public double m02(){return 0;}public double m03(){return 0;}
        public double m10(){return 0;}public double m11(){return 1;}public double m12(){return 0;}public double m13(){return 0;}
        public double m20(){return 0;}public double m21(){return 0;}public double m22(){return 1;}public double m23(){return 0;}
        public double m30(){return 0;}public double m31(){return 0;}public double m32(){return 0;}public double m33(){return 1;}
    }
    public static class Ship {long id;String dimension="overworld";Ship(long i){id=i;}public long getId(){return id;}public String getChunkClaimDimension(){return dimension;}public Bounds getWorldAABB(){return new Bounds();}public Active getActiveChunksSet(){return new Active();}public Matrix getWorldToShip(){return new Matrix();}}
    public static class Ships extends ArrayList<Ship> {public Ship getById(long id){return stream().filter(s->s.id==id).findFirst().orElse(null);}}
    public static class ShipWorld {public Ships all=new Ships(),loaded=new Ships();public boolean isSyncedWithServer(){return true;}public Ships getAllShips(){return all;}public Ships getLoadedShips(){return loaded;}}
    public static class Utils {static ShipWorld ships=new ShipWorld();public static ShipWorld getShipObjectWorld(World w){return ships;}public static ShipWorld getShipObjectWorld(Server s){return ships;}public static String getDimensionId(World w){return w.dimension;}}
    public record Packet(long id,boolean add){public long getShipID(){return id;}public boolean getAdd(){return add;}}
    public record Wrapper(Player player){public Player getPlayer(){return player;}}
    public static class Watch {public boolean isValid=true,isLoadedToPlayer=true;}
    public static class Loading {List<Watch> pending=new ArrayList<>();public void markPendingLoading(Watch w){pending.add(w);}}
    public static class Tracking {static int updates;static Map<String,Map<Player,Watch>> records=new HashMap<>();static Loading loading=new Loading();public static void immediatelyUpdateForPlayer(Player p){updates++;}public static Loading getPlayerInfo(Player p){return loading;}public static Map<Player,Watch> getWatchRecordForChunk(String d,int x,int z){return records.get(d+":"+x+":"+z);}}
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        String name="dev.fan4.compat.immersiveportals.ShipLoadRecovery";
        Map<String,String> names=new HashMap<>();names.put("org.valkyrienskies.mod.common.VSGameUtilsKt",Utils.class.getName());names.put("net.minecraft.class_638",World.class.getName());names.put("net.minecraft.class_3218",World.class.getName());names.put("net.minecraft.class_1937",World.class.getName());names.put("net.minecraft.server.MinecraftServer",Server.class.getName());names.put("net.minecraft.class_310",Client.class.getName());names.put("qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking",Tracking.class.getName());
        ClassWriter writer=new ClassWriter(0);new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main",name.replace('.','/')+".class"))).accept(new ClassRemapper(writer,new Remapper(){public Object mapValue(Object v){return v instanceof String s?names.getOrDefault(s,s):super.mapValue(v);}}),0);
        Class<?> helper=new ClassLoader(ShipLoadRecoverySmokeTest.class.getClassLoader()){Class<?> define(){byte[] bytes=writer.toByteArray();return defineClass(name,bytes,0,bytes.length);}}.define();
        var ack=helper.getMethod("acknowledged",Object.class,Object.class);var flush=helper.getMethod("flush",Object.class);var guard=helper.getMethod("guard",Object.class,boolean.class);var tick=helper.getMethod("tick",Object.class);
        Server server=new Server();World world=new World(server);Player player=new Player(world),other=new Player(world);Ship ship=new Ship(5);Utils.ships.all.add(ship);
        Watch delivered=new Watch(),otherWatch=new Watch();Tracking.records.put("overworld:0:0",Map.of(player,delivered,other,otherWatch));
        ack.invoke(null,new Packet(5,true),new Wrapper(player));flush.invoke(null,server);check(player.acks==0&&Tracking.updates==0,"early ACK waits for authoritative ship load");
        Utils.ships.loaded.add(ship);flush.invoke(null,server);check(player.known.contains(5L)&&player.acks==1,"early ACK recovered after server load");check(!delivered.isLoadedToPlayer&&Tracking.loading.pending.equals(List.of(delivered)),"delivered stale watch requeued through IP");check(otherWatch.isLoadedToPlayer,"another player's chunks untouched");
        ack.invoke(null,new Packet(5,true),new Wrapper(player));check(Tracking.loading.pending.size()==1,"repeated ACK throttles recovery and preserves pending watches");
        Ship second=new Ship(6);Utils.ships.all.add(second);ack.invoke(null,new Packet(6,true),new Wrapper(player));ack.invoke(null,new Packet(6,false),new Wrapper(player));Utils.ships.loaded.add(second);flush.invoke(null,server);check(!player.known.contains(6L),"remove ACK cancels deferred add");
        ack.invoke(null,new Packet(999,true),new Wrapper(player));flush.invoke(null,server);check(!player.known.contains(999L),"unknown authoritative ship never acknowledged");
        Ship expiring=new Ship(7);Utils.ships.all.add(expiring);ack.invoke(null,new Packet(7,true),new Wrapper(player));
        var pendingField=helper.getDeclaredField("PENDING");pendingField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Object,Map<Long,Long>> pending=(Map<Object,Map<Long,Long>>)pendingField.get(null);pending.get(player).put(7L,System.nanoTime()-1);
        flush.invoke(null,server);Utils.ships.loaded.add(expiring);flush.invoke(null,server);check(!player.known.contains(7L),"expired ACK cannot survive indefinitely");Utils.ships.loaded.remove(expiring);
        Ship disconnected=new Ship(8);Utils.ships.all.add(disconnected);Player gone=new Player(world);ack.invoke(null,new Packet(8,true),new Wrapper(gone));gone.removed=true;Utils.ships.loaded.add(disconnected);flush.invoke(null,server);check(gone.acks==0,"removed player does not receive deferred acknowledgements");Utils.ships.loaded.remove(disconnected);
        World interior=new World(server);interior.dimension="tardis";Player inside=new Player(interior);int updates=Tracking.updates;ack.invoke(null,new Packet(5,true),new Wrapper(inside));check(Tracking.updates==updates,"cross-dimension ACK never reloads current-world chunks");
        Client client=new Client();Client.INSTANCE=client;World clientWorld=new World(server);clientWorld.field_9236=true;client.field_1687=clientWorld;client.field_1724=new Player(clientWorld);guard.invoke(null,client.field_1724,true);tick.invoke(null,client);check(client.field_1724.acks==2,"blocked client retransmits native ACKs for nearby loaded ships");int count=client.field_1724.acks;guard.invoke(null,client.field_1724,true);tick.invoke(null,client);check(client.field_1724.acks==count,"recovery is rate limited");guard.invoke(null,player,true);check(player.acks==1,"server guard observation cannot emit client requests");
        @SuppressWarnings("unchecked") List<int[]> cells=(List<int[]>)helper.getMethod("region",double.class,double.class,double.class,double.class).invoke(null,-16.2,-.2,-15.8,.2);
        check(cells.stream().anyMatch(c->c[0]==-2&&c[1]==-1)&&cells.stream().anyMatch(c->c[0]==-1&&c[1]==0),"negative coordinates and boundary margin covered");
        check(((List<?>)helper.getMethod("region",double.class,double.class,double.class,double.class).invoke(null,0d,0d,10000d,10000d)).isEmpty(),"oversized recovery bounded");
        System.out.println("PASS: early/native ACK recovery, removal cancellation, unknown/dimension isolation, IP requeue ownership, pending-watch preservation, client throttling and negative chunk boundaries");
    }
}
