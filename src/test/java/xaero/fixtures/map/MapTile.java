package xaero.map.region;
import java.util.*;
public class MapTile {
    public static final int CURRENT_WORLD_INTERPRETATION_VERSION=1;
    public final Map<List<Integer>,Object> blocks=new HashMap<>();public int version,caveStart,caveDepth;public boolean loaded;public final int x,z;
    public MapTile(int x,int z){this.x=x;this.z=z;}
    public Object getBlock(int x,int z){return blocks.get(List.of(x,z));}public boolean isLoaded(){return loaded;}
    public void setBlock(int x,int z,Object block){blocks.put(List.of(x,z),block);}
    public void setWorldInterpretationVersion(int version){this.version=version;}
    public void setWrittenCave(int start,int depth){caveStart=start;caveDepth=depth;}
    public void setLoaded(boolean loaded){this.loaded=loaded;}
}
