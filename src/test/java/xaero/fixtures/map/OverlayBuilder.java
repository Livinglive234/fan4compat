package xaero.map.region;
import java.util.*;
/** Stand-in for Xaero's OverlayBuilder: collects the overlays built for one block and attaches them to it. */
public class OverlayBuilder {
    public final Object manager;final List<Overlay> building=new ArrayList<>();
    public OverlayBuilder(Object manager){this.manager=manager;}
    public void startBuilding(){building.clear();}
    public void build(Object state,int opacity,byte light,Object processor,Object biome){building.add(new Overlay(state,opacity,light));}
    public void finishBuilding(MapBlock block){for(Overlay overlay:building)block.addOverlay(overlay);building.clear();}
}
