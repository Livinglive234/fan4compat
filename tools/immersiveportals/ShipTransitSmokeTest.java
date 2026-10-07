import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

public class ShipTransitSmokeTest {
    public static class World {public boolean field_9236;String dimension;World(String d){dimension=d;}public String method_27983(){return dimension;}}
    public static class ClientWorld extends World {ClientWorld(String d){super(d);field_9236=true;}public Object method_22338(int x,int z){return null;}}
    public static class ServerWorld extends World {ServerWorld(String d){super(d);}}
    public static class Client {static Client INSTANCE;public static Client method_1551(){return INSTANCE;}public ClientWorld field_1687;public Player field_1724;}
    public static class Ship {long id;String dimension;Ship(long i,String d){id=i;dimension=d;}public long getId(){return id;}public String getChunkClaimDimension(){return dimension;}public Matrix getWorldToShip(){return new Matrix();}public Active getActiveChunksSet(){return active;}public Active active=new Active();}
    public static class Ships extends ArrayList<Ship> {public Ship getById(long id){return stream().filter(s->s.id==id).findFirst().orElse(null);}}
    public static class ShipWorld {boolean synced=true;Ships ships=new Ships();public boolean isSyncedWithServer(){return synced;}public Ships getLoadedShips(){return ships;}public Ships getAllShips(){return ships;}}
    public static class Utils {static ShipWorld ships=new ShipWorld();public static ShipWorld getShipObjectWorld(ClientWorld w){return ships;}public static ShipWorld getShipObjectWorld(ServerWorld w){return ships;}public static String getDimensionId(World w){return w.dimension;}public static List<Ship> nearby=new ArrayList<>();public static Iterable<Ship> getShipsIntersecting(World w,Box b){return nearby;}}
    public static class Motion {long id;Motion(long i){id=i;}public long getShipID(){return id;}}
    public static class Wrapper {Player player;Wrapper(Player p){player=p;}public Player getPlayer(){return player;}}
    public static class Vector {public double field_1352=0,field_1351=64,field_1350=0;public Vector(){}}
    public static class Box {public Box(double a,double b,double c,double d,double e,double f){}}
    public static class Matrix {
        public double m00(){return 1;}
        public double m01(){return 0;}
        public double m02(){return 0;}
        public double m03(){return 0;}
        public double m10(){return 0;}
        public double m11(){return 1;}
        public double m12(){return 0;}
        public double m13(){return 0;}
        public double m20(){return 0;}
        public double m21(){return 0;}
        public double m22(){return 1;}
        public double m23(){return 0;}
        public double m30(){return 1600;}
        public double m31(){return 0;}
        public double m32(){return 3200;}
        public double m33(){return 1;}
    }
    public static class Active {Set<String> chunks=new HashSet<>();public boolean contains(int x,int z){return chunks.contains(x+":"+z);}}
    public static class Watch {public boolean isValid=true,isLoadedToPlayer=true;}
    public static class Loading {public int loadedChunks=99;List<Watch> pending=new ArrayList<>();public void markPendingLoading(Watch w){pending.add(w);}}
    public static class Tracking {
        static Map<String,Map<Player,Watch>> records=new HashMap<>();static Loading loading=new Loading();static int updates;
        public static void immediatelyUpdateForPlayer(Player p){updates++;}public static Loading getPlayerInfo(Player p){return loading;}
        public static Map<Player,Watch> getWatchRecordForChunk(String dimension,int x,int z){return records.get(dimension+":"+x+":"+z);}
    }

    public interface Duck {}
    public static class Drag {
        Map<String,Object> values=new HashMap<>();
        public void setLastShipStoodOn(Long v){values.put("LastShipStoodOn",v);}
        public void setLastShipStoodOnServerWriteOnly(Long v){values.put("LastShipStoodOnServerWriteOnly",v);}
        public void setAddedMovementLastTick(Vector v){values.put("AddedMovementLastTick",v);}
        public void setAddedYawRotLastTick(double v){values.put("AddedYawRotLastTick",v);}
        public void setLerpPositionOnShip(Object v){values.put("LerpPositionOnShip",v);}
        public void setRelativeVelocityOnShip(Object v){values.put("RelativeVelocityOnShip",v);}
        public void setRelativePositionOnShip(Object v){values.put("RelativePositionOnShip",v);}
        public void setPreviousRelativeVelocityOnShip(Object v){values.put("PreviousRelativeVelocityOnShip",v);}
        public void setCachedLastPosition(Object v){values.put("CachedLastPosition",v);}
        public void setServerRelativePlayerPosition(Object v){values.put("ServerRelativePlayerPosition",v);}
        public void setLerpYawOnShip(Object v){values.put("LerpYawOnShip",v);}
        public void setLerpHeadYawOnShip(Object v){values.put("LerpHeadYawOnShip",v);}
        public void setLerpPitchOnShip(Object v){values.put("LerpPitchOnShip",v);}
        public void setRelativeYawOnShip(Object v){values.put("RelativeYawOnShip",v);}
        public void setRelativeHeadYawOnShip(Object v){values.put("RelativeHeadYawOnShip",v);}
        public void setRelativePitchOnShip(Object v){values.put("RelativePitchOnShip",v);}
        public void setDraggedArmorStandRelYaw(Object v){values.put("DraggedArmorStandRelYaw",v);}
        public void setServerRelativePlayerYaw(Object v){values.put("ServerRelativePlayerYaw",v);}
        public void setRestoreCachedLastPosition(boolean v){values.put("RestoreCachedLastPosition",v);}
        public void setShouldImpulseMovement(boolean v){values.put("ShouldImpulseMovement",v);}
        public void setLerpSteps(int v){values.put("LerpSteps",v);}
        public void setHeadLerpSteps(int v){values.put("HeadLerpSteps",v);}
    }
    public static class Handler {public Object field_14119;}
    public static class Player implements Duck {
        public Handler field_13987=new Handler();
        World world;Drag drag=new Drag();Object queued=new Object();boolean handled=true;Set<Long> known=new HashSet<>();int acknowledgements;
        Player(World w){world=w;}
        public Vector method_19538(){return new Vector();}public World method_37908(){return world;}public Drag getDraggingInformation(){return drag;}
        public void vs_setQueuedPositionUpdate(Object pos){queued=pos;}public void vs_setHandledMovePacket(boolean value){handled=value;}
        public boolean vs_isKnownShip(long id){return known.contains(id);}public void vs_addKnownShip(long id){known.add(id);acknowledgements++;}
    }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        Map<String,String> mapping=new HashMap<>();String[][] names={{"org.valkyrienskies.mod.common.VSGameUtilsKt","Utils"},{"net.minecraft.class_638","ClientWorld"},{"net.minecraft.class_3218","ServerWorld"},{"net.minecraft.class_1937","World"},{"org.joml.Vector3d","Vector"},{"org.valkyrienskies.mod.mixinducks.world.entity.PlayerDuck","Duck"},{"net.minecraft.class_238","Box"},{"net.minecraft.class_310","Client"},{"qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking","Tracking"}};
        for(String[] n:names)mapping.put(n[0],"ShipTransitSmokeTest$"+n[1]);
        String name="dev.fan4.compat.immersiveportals.ShipTransitCompat";
        ClassWriter writer=new ClassWriter(0);new ClassReader(Files.readAllBytes(Path.of(args[0],name.replace('.','/')+".class"))).accept(new ClassRemapper(writer,new Remapper(){public Object mapValue(Object value){return value instanceof String s?mapping.getOrDefault(s,s):super.mapValue(value);}}),0);
        Class<?> helper=new ClassLoader(ShipTransitSmokeTest.class.getClassLoader()){Class<?> define(){byte[] b=writer.toByteArray();return defineClass(name,b,0,b.length);}}.define();
        var ack=helper.getMethod("acknowledgeLoadedShips",Object.class);var reject=helper.getMethod("wrongDimensionMotion",Object.class,Object.class);var reset=helper.getMethod("resetShipMotion",Object.class);
        Client client=new Client();Client.INSTANCE=client;ack.invoke(null,client);
        client.field_1687=new ClientWorld("overworld");client.field_1724=new Player(client.field_1687);
        Utils.ships.ships.addAll(List.of(new Ship(42,"overworld"),new Ship(43,"tardis")));
        Utils.ships.synced=false;ack.invoke(null,client);check(client.field_1724.known.isEmpty(),"no acknowledgement before synchronization");
        client.field_1724.known.add(42L);Utils.ships.synced=true;ack.invoke(null,client);ack.invoke(null,client);
        check(client.field_1724.known.equals(Set.of(42L,43L))&&client.field_1724.acknowledgements==2,"received ships in portal dimensions acknowledged once without visiting them");
        client.field_1687=new ClientWorld("tardis");ack.invoke(null,client);check(client.field_1724.known.equals(Set.of(42L,43L)),"portal-world arrival acknowledges loaded ships");check(client.field_1724.acknowledgements==4,"world transition resends acknowledgements even with known flags preserved");ack.invoke(null,client);check(client.field_1724.acknowledgements==4,"stable world does not resend every tick");
        Player server=new Player(new ServerWorld("tardis"));Wrapper wrapper=new Wrapper(server);
        check((Boolean)reject.invoke(null,new Motion(42),wrapper),"late overworld ship packet rejected in TARDIS");
        check(!(Boolean)reject.invoke(null,new Motion(43),wrapper),"current dimension ship packet retained");
        check(!(Boolean)reject.invoke(null,new Motion(-1),wrapper),"no-ship packet retained");
        wrapper.player.field_13987.field_14119=new Object();check((Boolean)reject.invoke(null,new Motion(43),wrapper),"ship motion waits for authoritative teleport acknowledgement");check((Boolean)reject.invoke(null,new Motion(-1),wrapper),"detach motion also waits for teleport acknowledgement");wrapper.player.field_13987.field_14119=null;check(!(Boolean)reject.invoke(null,new Motion(43),wrapper),"motion resumes after teleport ACK");
        check((Boolean)reject.invoke(null,new Motion(99),wrapper),"removed ship packet rejected");
        reset.invoke(null,server);reset.invoke(null,client.field_1724);
        check(server.queued==null&&!server.handled,"queued ship position cannot overwrite portal arrival");
        check(server.drag.values.get("LastShipStoodOn")==null&&server.drag.values.get("ServerRelativePlayerPosition")==null,"old ship attachment cleared");
        check(Boolean.FALSE.equals(server.drag.values.get("RestoreCachedLastPosition")),"render tail cannot restore pre-teleport position");
        check(server.drag.values.get("AddedMovementLastTick") instanceof Vector&&((Number)server.drag.values.get("AddedYawRotLastTick")).doubleValue()==0,"old drag delta cleared");
        check(client.field_1724.known.equals(Set.of(42L,43L)),"dimension transfer preserves known ship list");
        var finish=helper.getMethod("finishTransfer",Object.class);Player arrival=new Player(new ServerWorld("overworld")),otherPlayer=new Player(arrival.world);
        Ship near=new Ship(44,"overworld");near.active.chunks.addAll(List.of("100:200","101:200","100:201","101:201","150:200"));Utils.nearby.add(near);
        Watch delivered=new Watch(),pending=new Watch(),invalid=new Watch(),otherDimension=new Watch(),distant=new Watch(),otherOwner=new Watch();pending.isLoadedToPlayer=false;invalid.isValid=false;
        Tracking.records.put("overworld:100:200",Map.of(arrival,delivered,otherPlayer,otherOwner));Tracking.records.put("overworld:101:200",Map.of(arrival,pending));Tracking.records.put("overworld:100:201",Map.of(arrival,invalid));Tracking.records.put("overworld:150:200",Map.of(arrival,distant));Tracking.records.put("tardis:100:200",Map.of(arrival,otherDimension));
        finish.invoke(null,arrival);check(Tracking.updates==1&&!delivered.isLoadedToPlayer&&Tracking.loading.pending.equals(List.of(delivered)),"arrival requeues only valid delivered nearby ship watches");
        check(otherOwner.isLoadedToPlayer&&otherDimension.isLoadedToPlayer&&distant.isLoadedToPlayer&&invalid.isLoadedToPlayer,"other owners, dimensions, distant chunks and invalid watches stay unchanged");
        check(Tracking.loading.loadedChunks==99,"refresh preserves IP watch-count bookkeeping");
        finish.invoke(null,arrival);check(Tracking.loading.pending.size()==1,"already-pending watch is not duplicated");
        Utils.nearby.clear();int updates=Tracking.updates;finish.invoke(null,arrival);check(Tracking.updates==updates,"ground arrivals do not refresh ship watches");
        if(args.length>2) {
            VerifyAddon.readJar(args[1]);VerifyAddon.readJar(args[2]);VerifyAddon.transitContracts();
            for(String mixin:List.of("ShipPortalTransferMixin")) {
                ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/immersiveportals/common",mixin+".class"))).accept(node,0);VerifyAddon.injectionTargets(node);
            }
            ClassNode placement=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/immersiveportals/client/MixinShipRemotePlacement.class"))).accept(placement,0);VerifyAddon.injectionTargets(placement);
            for(String field:List.of("isLoadedToPlayer","isValid"))check(VerifyAddon.fieldExists("qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTracking$PlayerWatchRecord",field),"exact IP watch fields");
        }
        System.out.println("PASS: loaded-ship acknowledgements, late cross-dimension motion rejection, queued position and render/drag reset, selective arrival refresh and exact native contracts");
    }
}
