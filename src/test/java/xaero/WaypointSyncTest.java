import dev.fan4.compat.xaero.*;
import java.nio.file.*;
import java.util.*;

public final class WaypointSyncTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    static final UUID ALICE=UUID.fromString("00000000-0000-0000-0000-00000000000a"),BOB=UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final String KEY="dim%minecraft$overworld/mw$default_1.txt";
    static String file(String name){return "sets:gui.xaero_default\nwaypoint:"+name+":H:1:64:1:0:false:0:gui.xaero_default:false:0:0:false\n";}
    static WaypointStore.Entry accepted(WaypointStore.Result r){return ((WaypointStore.Result.Accepted)r).entry();}
    static boolean rejected(WaypointStore.Result r,String why){return r instanceof WaypointStore.Result.Rejected x&&x.reason().contains(why);}
    public static void main(String[] args)throws Exception {
        // File names and text are validated before anything touches disk.
        for(String good:List.of(KEY,"dim%minecraft$the_nether/mw$default_1.txt","Server Name/dim%minecraft$overworld/mw$default_1.txt","dim§§x/a.txt"))check(WaypointKeys.valid(good),"valid: "+good);
        for(String bad:new String[]{null,"","a.txt","../x.txt","dim/../x.txt","/abs/x.txt","dim\\x.txt","dim/x.dat","a/b/c/d.txt","dim//x.txt","dim/.hidden.txt","dim/ x.txt","dim/x:y.txt","dim/x\u0000.txt","dim/"+"x".repeat(130)+".txt","dim/."+".txt"})check(!WaypointKeys.valid(bad),"invalid: "+bad);
        check(WaypointKeys.validText(file("a"),1000)&&!WaypointKeys.validText("a\u0000b",1000)&&!WaypointKeys.validText("x".repeat(11),10),"text rules");
        check(!WaypointKeys.validText("lone \ud800 surrogate",1000),"malformed UTF-16 rejected");

        Path dir=Files.createTempDirectory("fan4xaero");
        var limits=new WaypointStore.Limits(200,1000,3,2,1000);
        WaypointStore store=new WaypointStore(dir,limits);
        var first=accepted(store.put(ALICE,KEY,0,file("home"),10_000));
        check(first.version()==1&&store.files(ALICE).get(KEY).text().equals(file("home")),"first upload stored as version 1");
        check(store.files(BOB).isEmpty(),"players are separate");
        check(accepted(store.put(ALICE,KEY,1,file("home"),10_001)).version()==1,"identical text changes nothing, even inside the rate window");
        check(rejected(store.put(ALICE,KEY,1,file("home2"),10_500),"too fast"),"rate limit per file");
        check(store.put(ALICE,"dim%minecraft$the_nether/mw$default_1.txt",0,file("n"),10_500) instanceof WaypointStore.Result.Accepted,"rate limit is per file");
        var up=(WaypointStore.Result.Accepted)store.put(ALICE,KEY,1,file("home2"),12_000);
        check(up.entry().version()==2&&!up.displaced()&&store.backups(ALICE,KEY).isEmpty(),"informed upload bumps the version without a backup");

        // A stale uploader still wins (last writer), and what it replaced is kept.
        var stale=(WaypointStore.Result.Accepted)store.put(ALICE,KEY,1,file("pc2"),14_000);
        check(stale.displaced()&&stale.entry().version()==3&&store.files(ALICE).get(KEY).text().equals(file("pc2")),"stale upload wins");
        check(store.backups(ALICE,KEY).size()==1&&store.backups(ALICE,KEY).get(0).text().equals(file("home2")),"displaced copy kept as a backup");
        store.put(ALICE,KEY,1,file("pc3"),16_000);store.put(ALICE,KEY,1,file("pc4"),18_000);
        check(store.backups(ALICE,KEY).size()==2,"backups bounded per file");

        // Limits.
        check(rejected(store.put(ALICE,"dim%x/a.txt",0,"x".repeat(201),20_000),"too large"),"per-file size cap");
        check(rejected(store.put(ALICE,"../x.txt",0,"x",20_000),"invalid"),"invalid key rejected by the store");
        store.put(ALICE,"dim%a/a.txt",0,"a",20_000);
        check(rejected(store.put(ALICE,"dim%a/b.txt",0,"b",20_000),"too many"),"file count cap (3)");
        WaypointStore tight=new WaypointStore(Files.createTempDirectory("fan4tight"),new WaypointStore.Limits(200,300,10,3,0));
        for(int i=0;i<5;i++)tight.put(ALICE,"d/f.txt",i==0?0:i,"x".repeat(100+i),1000+i);
        check(tight.files(ALICE).get("d/f.txt").text().length()==104&&tight.backups(ALICE,"d/f.txt").size()<=1,"over budget sheds the oldest backups first");
        check(rejected(tight.put(ALICE,"d/g.txt",0,"y".repeat(200),9000),"storage limit")&&tight.files(ALICE).size()==1&&tight.files(ALICE).get("d/f.txt").text().length()==104,"live files are never evicted and a rejected upload changes nothing");

        // Survives a restart; damaged data is kept aside and does not block the player.
        WaypointStore reopened=new WaypointStore(dir,limits);
        check(reopened.files(ALICE).get(KEY).text().equals(file("pc4"))&&reopened.files(ALICE).get(KEY).version()==5,"state survives restart");
        check(reopened.backups(ALICE,KEY).size()==2,"backups survive restart");
        Files.writeString(dir.resolve(BOB+".json"),"{not json");
        check(new WaypointStore(dir,limits).files(BOB).isEmpty()&&Files.list(dir).anyMatch(p->p.getFileName().toString().startsWith(BOB+".json.damaged-")),"damaged file kept aside");
        check(Files.list(dir).noneMatch(p->p.toString().endsWith(".tmp")),"no temporary files left");
        check(WaypointStore.Limits.from(new Properties()).maxPlayerBytes()==256*1024,"default player cap");
        Properties props=new Properties();props.setProperty("waypointMaxBytesPerPlayer","500000");
        check(WaypointStore.Limits.from(props).maxPlayerBytes()==500_000,"configured player cap");
        props.setProperty("waypointMaxBytesPerPlayer","5000000");
        check(WaypointStore.Limits.from(props).maxPlayerBytes()==WaypointStore.Limits.MAX_SNAPSHOT_BYTES,"cap clamped so a snapshot fits one payload");
        props.setProperty("waypointMaxBytesPerPlayer","oops");check(WaypointStore.Limits.from(props).maxPlayerBytes()==256*1024,"bad cap value ignored");

        // Client decisions.
        var s1=new WaypointStore.Entry(3,5_000,file("server"));
        Map<String,WaypointSyncPlan.Local> noLocal=Map.of();
        var plan=WaypointSyncPlan.plan(Map.of("a.txt",s1),noLocal,Map.of());
        check(plan.size()==1&&plan.get(0) instanceof WaypointSyncPlan.Action.Apply a&&!a.backupLocal()&&a.version()==3,"server-only file is written locally");
        plan=WaypointSyncPlan.plan(Map.of(),Map.of("a.txt",new WaypointSyncPlan.Local(file("mine"),1)),Map.of());
        check(plan.get(0) instanceof WaypointSyncPlan.Action.Upload u&&u.baseVersion()==0,"local-only file is uploaded");
        plan=WaypointSyncPlan.plan(Map.of("a.txt",s1),Map.of("a.txt",new WaypointSyncPlan.Local(file("server"),1)),Map.of());
        check(plan.get(0) instanceof WaypointSyncPlan.Action.Adopt d&&d.version()==3,"equal text just records the version");
        var seen=new WaypointSyncPlan.State(2,WaypointSyncPlan.hash(file("old")));
        plan=WaypointSyncPlan.plan(Map.of("a.txt",s1),Map.of("a.txt",new WaypointSyncPlan.Local(file("old"),1)),Map.of("a.txt",seen));
        check(plan.get(0) instanceof WaypointSyncPlan.Action.Apply b&&!b.backupLocal(),"only the server changed: take it, nothing to back up");
        plan=WaypointSyncPlan.plan(Map.of("a.txt",new WaypointStore.Entry(2,5_000,file("old"))),Map.of("a.txt",new WaypointSyncPlan.Local(file("edited"),9_000)),Map.of("a.txt",seen));
        check(plan.get(0) instanceof WaypointSyncPlan.Action.Upload c&&c.baseVersion()==2,"only the client changed: upload on top of the version it saw");
        plan=WaypointSyncPlan.plan(Map.of("a.txt",s1),Map.of("a.txt",new WaypointSyncPlan.Local(file("edited"),9_000)),Map.of("a.txt",seen));
        check(plan.get(0) instanceof WaypointSyncPlan.Action.Upload e&&e.baseVersion()==3,"both changed, local saved later: local wins against the current server version");
        plan=WaypointSyncPlan.plan(Map.of("a.txt",s1),Map.of("a.txt",new WaypointSyncPlan.Local(file("edited"),1_000)),Map.of("a.txt",seen));
        check(plan.get(0) instanceof WaypointSyncPlan.Action.Apply f&&f.backupLocal(),"both changed, server saved later: server wins and the local copy is backed up");
        plan=WaypointSyncPlan.plan(Map.of("a.txt",s1),Map.of("a.txt",new WaypointSyncPlan.Local(file("edited"),1_000)),Map.of());
        check(plan.get(0) instanceof WaypointSyncPlan.Action.Apply g&&g.backupLocal(),"no sync history and different text is treated as both changed");
        plan=WaypointSyncPlan.plan(Map.of("b.txt",s1,"a.txt",s1),Map.of("c.txt",new WaypointSyncPlan.Local("c",1)),Map.of());
        check(plan.stream().map(WaypointSyncPlan.Action::key).toList().equals(List.of("a.txt","b.txt","c.txt")),"one action per file in a stable order");
        System.out.println("PASS: waypoint file/key validation, per-player store limits, last-writer-wins with bounded backups, persistence and client sync decisions");
    }
}
