package net.drgmes.dwm.common.tardis;
import net.drgmes.dwm.common.tardis.systems.TardisSystemMaterialization;
public final class TardisStateManager {
    public final TardisSystemMaterialization materialization=new TardisSystemMaterialization();public Object dimension,position;
    public Object getSystem(Class<?> type){return materialization;}
    public Object getCurrentExteriorDimension(){return dimension;}
    public Object getCurrentExteriorPosition(){return position;}
}
