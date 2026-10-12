package xaero.map;
public final class WorldMapSession {
    public static WorldMapSession current;public Object processor;
    public static WorldMapSession getCurrentSession(){return current;}
    public Object getMapProcessor(){return processor;}
}
