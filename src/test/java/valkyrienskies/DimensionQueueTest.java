import dev.fan4.compat.valkyrienskies.DimensionQueue;
import java.util.*;

public class DimensionQueueTest {
    public static class PhysicsWorld {
        boolean validPhase = true;
        final Map<String, Object> dimensions = new HashMap<>();
        final List<String> calls = new ArrayList<>();
        public void addDimension(String name, Object range, Object gravity, double sea, double max) {
            checkPhase();if(dimensions.putIfAbsent(name, gravity)!=null)throw new IllegalStateException("duplicate");calls.add("add:"+name);
        }
        public void updateDimension(String name, Object gravity, Double sea, Double max) {
            checkPhase();if(!dimensions.containsKey(name))throw new IllegalStateException("missing");dimensions.put(name,gravity);calls.add("update:"+name);
        }
        public void removeDimension(String name) { checkPhase();dimensions.remove(name);calls.add("remove:"+name); }
        void checkPhase() { if(!validPhase)throw new IllegalStateException("Constraints failed"); }
    }
    static void check(boolean value) { if(!value)throw new AssertionError(); }
    public static void main(String[] args) {
        PhysicsWorld world = new PhysicsWorld();
        Object[] initial={"overworld",new Object(),"normal",62d,962d};
        check(!DimensionQueue.defer(world,"addDimension",initial));world.addDimension("overworld",initial[1],initial[2],62,962);
        DimensionQueue.openAndFlush(world);
        DimensionQueue.close(world);world.validPhase=false;
        check(DimensionQueue.defer(world,"addDimension",new Object[]{"tardis",new Object(),"normal",62d,962d}));
        check(DimensionQueue.defer(world,"updateDimension",new Object[]{"tardis","changed",null,null}));
        check(!world.dimensions.containsKey("tardis"));
        world.validPhase=true;DimensionQueue.openAndFlush(world);
        check(world.dimensions.get("tardis").equals("changed"));
        check(world.calls.equals(List.of("add:overworld","add:tardis","update:tardis")));
        check(!DimensionQueue.defer(world,"removeDimension",new Object[]{"tardis"}));
        world.removeDimension("tardis");DimensionQueue.close(world);world.validPhase=false;
        check(DimensionQueue.defer(world,"addDimension",new Object[]{"temporary",new Object(),"normal",62d,962d}));
        check(DimensionQueue.defer(world,"removeDimension",new Object[]{"temporary"}));
        PhysicsWorld independent = new PhysicsWorld();DimensionQueue.openAndFlush(independent);check(independent.calls.isEmpty());
        world.validPhase=true;DimensionQueue.openAndFlush(world);check(!world.dimensions.containsKey("temporary"));
        DimensionQueue.close(world);
        DimensionQueue.defer(world,"updateDimension",new Object[]{"missing","gravity",null,null});
        try { DimensionQueue.openAndFlush(world);throw new AssertionError("failure swallowed"); }
        catch(IllegalStateException expected) { check(expected.getMessage().equals("missing")); }
        DimensionQueue.forget(world);check(!DimensionQueue.defer(world,"removeDimension",new Object[]{"anything"}));
        System.out.println("PASS: startup, idle deferral, ordered add/update/remove, separate worlds, failure propagation, shutdown cleanup");
    }
}
