import dev.fan4.compat.xaero.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class XaeroSyncFlowTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    static final String KEY="dim%minecraft$overworld/mw$default_1.txt";
    static String file(String name){return "sets:gui.xaero_default\nwaypoint:"+name+":H:1:64:1:0:false:0:gui.xaero_default:false:0:0:false\n";}
    static void write(Path root,String key,String text,long modified)throws IOException {
        Path p=root.resolve(key);Files.createDirectories(p.getParent());Files.writeString(p,text);Files.setLastModifiedTime(p,java.nio.file.attribute.FileTime.fromMillis(modified));
    }
    static boolean fails(IOExceptionRunnable r){try{r.run();return false;}catch(IOException expected){return true;}}
    interface IOExceptionRunnable{void run()throws IOException;}
    /** One simulated player machine. */
    record Pc(Path folder,WaypointClient client) {
        static Pc at(Path base,String name,long[] clock)throws IOException {
            Path root=base.resolve(name);Path folder=root.resolve("xaero/minimap/Multiplayer_host");Files.createDirectories(folder);
            return new Pc(folder,new WaypointClient(folder,root.resolve("state.properties"),root.resolve("backups"),64*1024,()->clock[0]));
        }
    }
    /** Server-side handling of a client's chunks, as the bridge does it. */
    static int deliver(WaypointServer server,UUID player,List<WaypointProtocol.Chunk> chunks,long now)throws IOException {
        int refused=0;
        for(WaypointProtocol.Chunk chunk:chunks)if(server.receive(player,WaypointProtocol.encode(chunk),now)!=null)refused++;
        return refused;
    }
    static WaypointProtocol.Snapshot snapshot(WaypointServer server,UUID player)throws IOException {
        return (WaypointProtocol.Snapshot)WaypointProtocol.decode(server.snapshot(player),WaypointStore.Limits.MAX_SNAPSHOT_BYTES);
    }
    public static void main(String[] args)throws Exception {
        // Wire format.
        var entry=new WaypointStore.Entry(7,1234,file("a"));
        var snap=new WaypointProtocol.Snapshot(List.of(new WaypointProtocol.Item(KEY,entry)));
        var back=(WaypointProtocol.Snapshot)WaypointProtocol.decode(WaypointProtocol.encode(snap),65536);
        check(back.items().size()==1&&back.items().get(0).entry().equals(entry)&&back.items().get(0).key().equals(KEY),"snapshot round trip");
        var chunk=new WaypointProtocol.Chunk(KEY,3,0,1,"hello".getBytes(StandardCharsets.UTF_8));
        var chunkBack=(WaypointProtocol.Chunk)WaypointProtocol.decode(WaypointProtocol.encode(chunk),65536);
        check(chunkBack.key().equals(KEY)&&chunkBack.baseVersion()==3&&new String(chunkBack.data(),StandardCharsets.UTF_8).equals("hello"),"chunk round trip");
        check(WaypointProtocol.decode(WaypointProtocol.encode(new WaypointProtocol.Done()),65536) instanceof WaypointProtocol.Done,"done round trip");
        byte[] good=WaypointProtocol.encode(chunk);
        check(fails(()->WaypointProtocol.decode(Arrays.copyOf(good,good.length-2),65536)),"truncated message rejected");
        check(fails(()->WaypointProtocol.decode(Arrays.copyOf(good,good.length+1),65536)),"trailing bytes rejected");
        check(fails(()->WaypointProtocol.decode(new byte[]{9},65536))&&fails(()->WaypointProtocol.decode(new byte[0],65536)),"unknown or empty message rejected");
        byte[] wrongVersion=WaypointProtocol.encode(snap);wrongVersion[1]=99;
        check(fails(()->WaypointProtocol.decode(wrongVersion,65536)),"other protocol version rejected");
        check(fails(()->WaypointProtocol.decode(WaypointProtocol.encode(new WaypointProtocol.Chunk("../x.txt",0,0,1,new byte[1])),65536)),"bad key rejected on decode");
        check(fails(()->WaypointProtocol.decode(WaypointProtocol.encode(new WaypointProtocol.Chunk(KEY,0,2,2,new byte[1])),65536)),"chunk index beyond total rejected");
        check(fails(()->WaypointProtocol.decode(WaypointProtocol.encode(new WaypointProtocol.Chunk(KEY,0,0,1000,new byte[1])),65536)),"absurd chunk count rejected");

        // Chunking and reassembly.
        String big="x".repeat(60_000);
        var parts=WaypointProtocol.split(KEY,4,big);
        check(parts.size()==3&&parts.get(0).data().length==WaypointProtocol.CHUNK_BYTES,"60 KB file needs three chunks");
        check(WaypointProtocol.split(KEY,0,"").size()==1,"an empty file is still one chunk");
        var assembler=new WaypointProtocol.Assembler(65536);
        check(assembler.accept(parts.get(0),0)==null&&assembler.accept(parts.get(1),1)==null,"partial uploads wait");
        var done=assembler.accept(parts.get(2),2);
        check(done!=null&&done.text().equals(big)&&done.baseVersion()==4&&done.key().equals(KEY),"chunks reassemble in order");
        assembler.accept(parts.get(0),10);
        check(fails(()->assembler.accept(parts.get(2),11)),"skipped chunk rejected");
        check(fails(()->assembler.accept(parts.get(1),12)),"the rejected upload is forgotten");
        assembler.accept(parts.get(0),20);
        check(fails(()->assembler.accept(parts.get(1),20+61_000)),"stale partials are dropped, not trusted");
        var limited=new WaypointProtocol.Assembler(30_000);
        limited.accept(parts.get(0),0);
        check(fails(()->limited.accept(parts.get(1),1)),"uploads over the file limit rejected mid-stream");
        var busy=new WaypointProtocol.Assembler(65536);
        for(int i=0;i<8;i++)busy.accept(new WaypointProtocol.Chunk("d"+i+"/f.txt",0,0,2,new byte[1]),0);
        check(fails(()->busy.accept(new WaypointProtocol.Chunk("d9/f.txt",0,0,2,new byte[1]),0)),"too many unfinished uploads rejected");

        // Xaero folder rules.
        check("Multiplayer_fantastic4.livenet.host".equals(XaeroFolders.node("fantastic4.livenet.host",true)),"plain address");
        check("Multiplayer_fantastic4.livenet.host".equals(XaeroFolders.node("fantastic4.livenet.host:25565",true)),"port stripped");
        check("Multiplayer_example.com".equals(XaeroFolders.node("example.com.",true)),"trailing dot stripped");
        check(XaeroFolders.ANY_ADDRESS.equals(XaeroFolders.node("example.com",false)),"any-address mode");
        for(String odd:new String[]{null,"","[::1]:25565","::1","a:b:c","bad/host","host\\x"})check(XaeroFolders.node(odd,true)==null,"unsupported address: "+odd);
        Path tmp=Files.createTempDirectory("fan4xaerofolders");
        Files.writeString(tmp.resolve("client.cfg"),"foo = 1\ndifferentiate_by_server_address = false\n");
        check(!XaeroFolders.differentiateByAddress(tmp.resolve("client.cfg"))&&XaeroFolders.differentiateByAddress(tmp.resolve("missing.cfg")),"config option read, default true");
        Files.createDirectories(tmp.resolve("minimap/Multiplayer_Host.Example"));
        check(XaeroFolders.serverFolder(tmp.resolve("minimap"),"Multiplayer_host.example").getFileName().toString().equals("Multiplayer_Host.Example"),"existing folder reused case-insensitively");
        check(XaeroFolders.serverFolder(tmp.resolve("minimap"),"Multiplayer_new").equals(tmp.resolve("minimap/Multiplayer_new")),"new folder path");
        check(XaeroFolders.resolve(tmp,KEY)!=null&&XaeroFolders.resolve(tmp,"../x.txt")==null&&XaeroFolders.resolve(tmp,"dim/../../x.txt")==null,"keys cannot leave the folder");
        Path server=tmp.resolve("server");
        write(server,KEY,file("a"),1000);write(server,"dim%minecraft$the_nether/mw$default_1.txt",file("n"),1000);
        write(server,"dim%minecraft$overworld/config.txt","cfg",1000);write(server,"config.txt","root",1000);
        write(server,"dim%x/huge.txt","x".repeat(70_000),1000);write(server,"dim%x/readme.md","no",1000);
        Files.writeString(server.resolve("dim%x/binary.txt"),"a\u0000b");
        var found=XaeroFolders.waypointFiles(server,64*1024);
        check(found.keySet().equals(Set.of(KEY,"dim%minecraft$the_nether/mw$default_1.txt")),"only waypoint files taking part: "+found.keySet());
        check(found.get(KEY).modified()==1000,"modification time carried");

        // Two PCs through one server, end to end.
        Path base=Files.createTempDirectory("fan4xaeroflow");long[] clock={50_000};
        UUID player=UUID.randomUUID();
        WaypointServer waypoints=new WaypointServer(new WaypointStore(base.resolve("store"),new WaypointStore.Limits(64*1024,256*1024,64,3,0)));
        Pc one=Pc.at(base,"pc1",clock),two=Pc.at(base,"pc2",clock);
        write(one.folder(),KEY,file("home"),10_000);
        check(deliver(waypoints,player,one.client().sync(snapshot(waypoints,player)),50_000)==0,"first PC uploads its waypoints on join");
        check(snapshot(waypoints,player).items().size()==1,"server now holds the file");
        check(two.client().sync(snapshot(waypoints,player)).isEmpty()&&Files.readString(two.folder().resolve(KEY)).equals(file("home")),"second PC receives the file before Xaero would read it");
        check(two.client().changes().isEmpty(),"nothing to upload after a sync");
        write(two.folder(),KEY,file("home2"),60_000);
        List<WaypointProtocol.Chunk> edit=two.client().changes();
        check(!edit.isEmpty()&&deliver(waypoints,player,edit,60_000)==0,"an edit made while playing is uploaded");
        check(two.client().changes().isEmpty(),"and not uploaded again");
        check(snapshot(waypoints,player).items().get(0).entry().text().equals(file("home2")),"server has the edit");
        clock[0]=70_000;
        check(one.client().sync(snapshot(waypoints,player)).isEmpty(),"first PC rejoins");
        check(Files.readString(one.folder().resolve(KEY)).equals(file("home2")),"first PC picks up the newer file");
        try(var backups=Files.walk(base.resolve("pc1/backups"))){check(backups.filter(Files::isRegularFile).count()<=1,"at most the superseded local copy is backed up");}
        // Both machines edit before syncing: the newer save wins and the loser is backed up.
        write(one.folder(),KEY,file("from-one"),80_000);
        write(two.folder(),KEY,file("from-two"),90_000);
        clock[0]=95_000;
        check(deliver(waypoints,player,two.client().changes(),95_000)==0,"second PC uploads");
        List<WaypointProtocol.Chunk> clash=one.client().sync(snapshot(waypoints,player));
        check(clash.isEmpty()&&Files.readString(one.folder().resolve(KEY)).equals(file("from-two")),"newer save wins on the PC that saved earlier");
        try(var backups=Files.walk(base.resolve("pc1/backups"))){check(backups.filter(Files::isRegularFile).anyMatch(p->{try{return Files.readString(p).equals(file("from-one"));}catch(IOException e){return false;}}),"the overwritten local copy is kept");}

        // The server refuses anything outside the rules without storing it.
        check(waypoints.receive(player,WaypointProtocol.encode(new WaypointProtocol.Chunk("dim/x.dat",0,0,1,new byte[1])),0)!=null,"invalid name refused");
        check(waypoints.receive(player,new byte[]{1,2,3},0)!=null,"garbage refused");
        check(WaypointServer.isDone(WaypointProtocol.encode(new WaypointProtocol.Done()))&&!WaypointServer.isDone(good)&&!WaypointServer.isDone(new byte[]{9}),"done detection");
        check(XaeroFolders.waypointFiles(base.resolve("missing"),1000).isEmpty(),"missing folder yields no files");
        System.out.println("PASS: Xaero sync wire format, chunk reassembly, folder rules and two-PC join/edit/conflict flow with backups");
    }
}
