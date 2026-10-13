package xaero.map.region;
public class Overlay {
    public final Object state;public final int opacity;public final byte light;
    public Overlay(Object state,int opacity,byte light){this.state=state;this.opacity=opacity;this.light=light;}
    public Object getState(){return state;}
}
