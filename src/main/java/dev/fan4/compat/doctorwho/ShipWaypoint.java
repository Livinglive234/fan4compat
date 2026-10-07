package dev.fan4.compat.doctorwho;

/** Immutable ship identity; native DWM fields retain dimension, local block and facing. */
public record ShipWaypoint(long shipId,String waypointId) {
    private static final String PREFIX="fan4compat_ship:";
    public String encode(){return PREFIX+shipId+":"+waypointId;}
    public static ShipWaypoint decode(String id){
        if(id==null||!id.startsWith(PREFIX))return null;
        int end=id.indexOf(':',PREFIX.length());if(end<0||end==id.length()-1)return null;
        try{return new ShipWaypoint(Long.parseLong(id.substring(PREFIX.length(),end)),id.substring(end+1));}
        catch(NumberFormatException e){return null;}
    }
}
