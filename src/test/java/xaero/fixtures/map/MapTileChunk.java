package xaero.map.region;
import java.util.*;
public class MapTileChunk {
    public final Object region;public final int x,z;public int loadState;public final Map<List<Integer>,Object> tiles=new HashMap<>();
    public boolean changed,terrain,included=true,downloading;public int updates;public Object shapes,processor;
    public MapTileChunk(Object region,int x,int z){this.region=region;this.x=x;this.z=z;}
    public void setLoadState(byte state){loadState=state;}
    public int getLoadState(){return loadState;}
    public Object getLeafTexture(){return new Object(){public boolean shouldDownloadFromPBO(){return downloading;}};}
    public Object getTile(int x,int z){return tiles.get(List.of(x,z));}
    public void setChanged(boolean changed){this.changed=changed;}
    public boolean wasChanged(){return changed;}
    public void setTile(int x,int z,Object tile,Object shapes,Object processor){tiles.put(List.of(x,z),tile);this.shapes=shapes;this.processor=processor;}
    public boolean includeInSave(){return included;}
    public void setHasHadTerrain(){terrain=true;}
    public void updateBuffers(Object processor,Object tint,Object overlays,boolean flag,Object shapes,Object fast){updates++;}
}
