package net.drgmes.dwm.common.tardis;
import net.drgmes.dwm.common.tardis.systems.*;
public final class TardisStateManager {
    public final TardisSystemMaterialization materialization=new TardisSystemMaterialization();public final TardisSystemFlight flight=new TardisSystemFlight();public Object dimension,position,destination,facing;public final java.util.Map<Object,Object> doors=new java.util.LinkedHashMap<>();
    public Object getSystem(Class<?> type){return type==TardisSystemFlight.class?flight:materialization;}
    public Object getDestinationExteriorPosition(){return destination;}
    public Object getCurrentExteriorFacing(){return facing;}public java.util.Map<Object,Object> getInteriorDoorsTiles(){return doors;}
    public Object getCurrentExteriorDimension(){return dimension;}
    public Object getCurrentExteriorPosition(){return position;}
}
