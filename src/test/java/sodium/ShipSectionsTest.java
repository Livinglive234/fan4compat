import dev.fan4.compat.sodium.ShipSections;
import java.util.*;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.TaskQueueType;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.*;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.*;
import org.joml.*;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.IShipActiveChunksSet;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.internal.ships.VsiQueryableShipData;
import org.valkyrienskies.core.internal.world.VsiClientShipWorld;
import org.valkyrienskies.mod.common.VSClientGameUtils;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class ShipSectionsTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static final class Ship implements ClientShip {
        final List<int[]> columns;final Vector3d center;
        Ship(Vector3d center,int[]... columns){this.center=center;this.columns=List.of(columns);}
        public ShipTransform getRenderTransform(){return ()->center;}
        public IShipActiveChunksSet getActiveChunksSet(){return consumer->{for(int[] c:columns)consumer.accept(c[0],c[1]);};}
    }
    record Call(ChunkRenderMatrices matrices,CommandList commands,ChunkRenderListIterable lists,TerrainRenderPass pass,CameraTransform camera,boolean sorted) {}
    public static void main(String[] args) {
        class_638 level=new class_638();
        // Two chunk columns of a ship: sections -4..19, of which only y=0 and y=1 hold blocks.
        for(int[] column:new int[][]{{100,200},{101,200}}) {
            class_2818 chunk=new class_2818();chunk.sections=new class_2826[level.maxSection-level.minSection];
            for(int i=0;i<chunk.sections.length;i++){chunk.sections[i]=new class_2826();chunk.sections[i].empty=!(i==4||i==5);}
            level.chunks.put(class_638.key(column[0],column[1]),chunk);
        }
        Map<String,RenderSection> sections=new HashMap<>();
        ShipSections.SectionLookup lookup=(x,y,z)->sections.get(x+","+y+","+z);
        for(int[] column:new int[][]{{100,200},{101,200}})for(int y=0;y<2;y++)sections.put(column[0]+","+y+","+column[1],new RenderSection(column[0],y,column[1]));
        sections.remove("101,1,200");                       // Sodium has no render section for this one yet
        sections.get("100,0,200").pending=true;             // and this one waits for a rebuild
        Ship ship=new Ship(new Vector3d(10,20,30),new int[]{100,200},new int[]{101,200}),other=new Ship(new Vector3d(1,2,3));
        VsiQueryableShipData<ClientShip> ships=()->List.<ClientShip>of(ship,other).iterator();
        VSGameUtilsKt.shipWorld=()->ships;
        Object manager=new Object();Viewport viewport=new Viewport();
        EnumMap<TaskQueueType,ArrayDeque<RenderSection>> taskLists=new EnumMap<>(TaskQueueType.class);
        for(TaskQueueType type:TaskQueueType.values())taskLists.put(type,new ArrayDeque<>());
        OcclusionSectionCollector worldCollector=new OcclusionSectionCollector(7,TaskQueueType.ONE_FRAME_DEFER,TaskQueueType.ALWAYS_DEFER);

        // Each ship gets a list of its non-empty sections that Sodium has a render section for; a pending rebuild goes to the queue Sodium's own collector uses.
        check(!ShipSections.afterCreate(lookup,level,taskLists,worldCollector,viewport,7,false,manager),"no revisit needed");
        WeakHashMap<ClientShip,SortedRenderLists> built=ShipSections.lists(manager);
        check(built.size()==2,"a list per loaded ship");
        List<RenderSection> visited=built.get(ship).sections;
        check(visited.size()==3&&!visited.contains(sections.get("101,1,200"))&&built.get(ship).viewport==viewport,"ship list: non-empty sections with a render section: "+visited.size());
        check(built.get(other).sections.isEmpty(),"a ship with no chunks gets an empty list");
        check(taskLists.get(TaskQueueType.ONE_FRAME_DEFER).size()==1&&taskLists.get(TaskQueueType.ONE_FRAME_DEFER).peek()==sections.get("100,0,200")&&taskLists.get(TaskQueueType.INITIAL_BUILD).isEmpty(),"the pending rebuild is queued where Sodium's collector puts them");
        // The answer is Sodium's own, or true when a ship section is waiting on a running job.
        check(ShipSections.afterCreate(lookup,level,taskLists,worldCollector,viewport,7,true,manager),"Sodium's own revisit answer is kept");
        SectionCollector.revisitAll=true;
        check(ShipSections.afterCreate(lookup,level,taskLists,worldCollector,viewport,7,false,manager),"a ship section waiting on a job asks for another pass");
        SectionCollector.revisitAll=false;
        // The visible renders tick the ship lists too: a snapshot of them, cleared when Sodium resets.
        check(ShipSections.shipLists(manager).length==2,"ship lists to tick");
        ShipSections.clear(manager);
        check(ShipSections.shipLists(manager).length==0&&ShipSections.shipLists(new Object()).length==0,"cleared lists, and none for an unknown manager");

        // Drawing: each ship with its transform on a copy of the model-view matrix, its position as the camera, then the command list is closed.
        ShipSections.afterCreate(lookup,level,taskLists,worldCollector,viewport,7,false,manager);
        List<Call> calls=new ArrayList<>();int[] closes={0};
        ChunkRenderer renderer=(m,c,l,p,t,s)->calls.add(new Call(m,c,l,p,t,s));
        CommandList commands=()->closes[0]++;
        Matrix4f projection=new Matrix4f(null),modelView=new Matrix4f(null);TerrainRenderPass pass=new TerrainRenderPass();
        ChunkRenderMatrices matrices=new ChunkRenderMatrices(projection,modelView);
        VSClientGameUtils.calls.clear();
        ShipSections.renderShips(manager,renderer,matrices,commands,pass,5,6,7,true);
        check(calls.size()==2&&closes[0]==2&&VSClientGameUtils.calls.size()==2,"every ship is drawn and its command list closed");
        Call first=calls.stream().filter(c->c.camera().x==10).findFirst().orElseThrow();
        check(first.camera().y==20&&first.camera().z==30&&first.sorted()&&first.pass()==pass&&first.commands()==commands&&first.lists()==built.get(ship),"ship drawn with its position as the camera, its own list and the sorting flag");
        check(first.matrices().projection()==projection&&first.matrices().modelView()!=modelView&&((Matrix4f)first.matrices().modelView()).source==modelView,"projection kept, model-view is a copy");
        Object[] transform=VSClientGameUtils.calls.stream().filter(c->(double)c[2]==10).findFirst().orElseThrow();
        check(transform[1]==first.matrices().modelView()&&(double)transform[3]==20&&(double)transform[4]==30&&(double)transform[5]==5&&(double)transform[6]==6&&(double)transform[7]==7,"ship transform applied to that copy with the ship centre and the camera");
        ShipSections.renderShips(manager,renderer,matrices,commands,pass,5,6,7,false);
        check(calls.get(calls.size()-1).sorted()==false,"the sorting flag is passed through");
        calls.clear();ShipSections.renderShips(new Object(),renderer,matrices,commands,pass,0,0,0,true);
        check(calls.isEmpty(),"nothing is drawn for a manager without ships");

        // One failure turns it off without throwing: a wrong level object breaks list building, and drawing stops with it.
        check(ShipSections.afterCreate(lookup,"not a level",taskLists,worldCollector,viewport,7,true,manager),"a failure returns Sodium's own answer");
        calls.clear();ShipSections.renderShips(manager,renderer,matrices,commands,pass,0,0,0,true);
        check(calls.isEmpty()&&!ShipSections.afterCreate(lookup,level,taskLists,worldCollector,viewport,7,false,manager),"after a failure nothing is built or drawn");
        ShipSections.reset();
        check(!ShipSections.afterCreate(lookup,level,taskLists,worldCollector,viewport,7,false,manager)&&ShipSections.lists(manager).size()==2,"reset turns it back on");
        System.out.println("PASS: Sodium 0.8 ship rendering (ship lists, rebuild queues, revisit, ticking, drawing with the ship transform, failure latch)");
    }
}
