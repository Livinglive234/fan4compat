package xaero.map.region;
public class MapBlock {
    public int prepared=-1,height,top;public Object state,biome;public byte light;public boolean glowing,cave;
    public Object getState(){return state;}public Object getBiome(){return biome;}public int getHeight(){return height;}public int getTopHeight(){return top;}
    public void prepareForWriting(int height){prepared=height;}
    public void write(Object state,int height,int top,Object biome,byte light,boolean glowing,boolean cave){this.state=state;this.height=height;this.top=top;this.biome=biome;this.light=light;this.glowing=glowing;this.cave=cave;}
}
