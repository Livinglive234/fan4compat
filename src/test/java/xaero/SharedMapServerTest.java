import dev.fan4.compat.xaero.*;
import java.nio.file.*;
import java.util.*;

public final class SharedMapServerTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    static SharedMapChunk chunk(String dim,int x,int z,long stamp){return SharedMapTest.chunk(dim,x,z,stamp,"minecraft:grass_block",64+(x&3));}
    public static void main(String[] args)throws Exception {
        String overworld="minecraft:overworld",nether="minecraft:the_nether";

        // Capture planning: nearest first, a budget, no repeats until the recheck delay, fair between players.
        SharedMapCapturePlan plan=new SharedMapCapturePlan(100);
        var alice=new SharedMapCapturePlan.Player(overworld,0,0,2);
        List<SharedMapCapturePlan.Target> first=plan.next(List.of(alice),0,9);
        check(first.size()==9&&first.get(0).x()==0&&first.get(0).z()==0,"the player's own chunk comes first");
        check(first.stream().allMatch(t->Math.abs(t.x())<=1&&Math.abs(t.z())<=1),"the first nine are the inner ring and centre");
        List<SharedMapCapturePlan.Target> second=plan.next(List.of(alice),1,100);
        check(second.size()==16&&second.stream().noneMatch(t->Math.abs(t.x())<=1&&Math.abs(t.z())<=1),"then the outer ring, never repeating a chunk");
        check(plan.next(List.of(alice),50,100).isEmpty(),"nothing is due again before the recheck delay");
        check(plan.next(List.of(alice),200,100).size()==25,"after the delay every chunk is looked at again");
        SharedMapCapturePlan fair=new SharedMapCapturePlan(1000);
        var bob=new SharedMapCapturePlan.Player(nether,500,500,8);
        List<SharedMapCapturePlan.Target> shared=fair.next(List.of(bob,new SharedMapCapturePlan.Player(overworld,0,0,8)),0,4);
        check(shared.stream().filter(t->t.dimension().equals(nether)).count()==2&&shared.stream().filter(t->t.dimension().equals(overworld)).count()==2,"a budget is shared between players");
        check(new SharedMapCapturePlan(10).next(List.of(),0,10).isEmpty(),"no players, nothing to do");

        // Relay: catch-up newer than the client's floor, nearest first; live changes first; dimension switches; overflow recovery.
        SharedMapStore store=new SharedMapStore(Files.createTempDirectory("fan4relay"),8);
        for(int x=0;x<40;x+=8)store.put(chunk(overworld,x,0,10+x));
        store.put(chunk(nether,1,1,5));
        SharedMapRelay relay=new SharedMapRelay(store,Map.of(overworld,12L));
        relay.moved(overworld,24,0);
        List<SharedMapChunk> batch=relay.next(100);
        check(batch.stream().map(c->c.x).toList().equals(List.of(24,16,8,32)),"only chunks newer than the client's floor (stamp 12; chunk 0 at stamp 10 is excluded), nearest region first and nearest chunk first within it: "+batch.stream().map(c->c.x).toList());
        check(relay.idle()&&relay.next(10).isEmpty(),"idle once caught up");
        relay.changed(chunk(overworld,100,0,500));relay.changed(chunk(nether,2,2,501));
        List<SharedMapChunk> live=relay.next(10);
        check(live.size()==1&&live.get(0).x==100,"a live change in the player's dimension is sent, another dimension's is not");
        relay.moved(nether,1,1);
        check(relay.next(10).stream().map(c->c.dimension+c.x).toList().equals(List.of(nether+1)),"entering another dimension starts that dimension's catch-up");
        SharedMapRelay everything=new SharedMapRelay(store,Map.of());
        everything.moved(overworld,0,0);
        check(everything.next(3).size()==3&&!everything.idle()&&everything.next(100).size()==2,"batches respect the limit and continue where they left off");
        SharedMapRelay burst=new SharedMapRelay(store,Map.of(overworld,0L));
        burst.moved(overworld,0,0);burst.next(100);
        for(int i=0;i<3000;i++)burst.changed(chunk(overworld,1000+i,0,1000+i));
        int received=0;List<SharedMapChunk> got;while(!(got=burst.next(500)).isEmpty())received+=got.size();
        check(received>=2048&&burst.idle(),"a flood of changes overflows the live queue without hanging or growing without bound ("+received+" sent)");
        System.out.println("PASS: shared map capture planning (nearest-first, budget, recheck, fairness) and per-player relay (since floor, nearest-first, live changes, dimension switch, overflow)");
    }
}
