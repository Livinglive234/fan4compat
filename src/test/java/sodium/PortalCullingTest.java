import dev.fan4.compat.sodium.PortalCulling;
import java.util.*;

public final class PortalCullingTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) {
        List<float[]> asked=new ArrayList<>();
        PortalCulling.BoxCuller hides=(a,b,c,d,e,f)->{asked.add(new float[]{a,b,c,d,e,f});return true;};
        PortalCulling.BoxCuller shows=(a,b,c,d,e,f)->{asked.add(new float[]{a,b,c,d,e,f});return false;};
        // A section is the centre plus or minus the radius on every axis, and is only culled when Sodium kept it and the portal hides it.
        check(!PortalCulling.section(true,hides,10,20,30,9.125f)&&Arrays.equals(asked.get(0),new float[]{0.875f,10.875f,20.875f,19.125f,29.125f,39.125f}),"section box is centre +/- radius: "+Arrays.toString(asked.get(0)));
        check(PortalCulling.section(true,shows,10,20,30,9.125f),"a section the portal shows stays");
        int before=asked.size();
        check(!PortalCulling.section(false,hides,0,0,0,9.125f)&&asked.size()==before,"a section Sodium already dropped is not asked about");
        check(PortalCulling.section(true,null,0,0,0,9.125f),"no culler (not in a portal view) leaves the answer alone");
        // Boxes pass through as given.
        asked.clear();
        check(!PortalCulling.box(true,hides,1,2,3,4,5,6)&&Arrays.equals(asked.get(0),new float[]{1,2,3,4,5,6}),"box is passed through unchanged");
        check(PortalCulling.box(true,shows,1,2,3,4,5,6)&&!PortalCulling.box(false,shows,1,2,3,4,5,6)&&PortalCulling.box(true,null,1,2,3,4,5,6),"box answers");
        // JOML intersection codes: negative means inside or intersecting, a plane index means outside. Only a negative result can be turned into outside.
        before=asked.size();
        check(PortalCulling.intersect(-1,hides,1,2,3,4,5,6)==0&&PortalCulling.intersect(-2,hides,1,2,3,4,5,6)==0,"a hidden box inside or crossing the frustum becomes outside");
        check(PortalCulling.intersect(-1,shows,1,2,3,4,5,6)==-1&&PortalCulling.intersect(-2,shows,1,2,3,4,5,6)==-2,"a shown box keeps its code");
        int asks=asked.size();
        check(PortalCulling.intersect(3,hides,1,2,3,4,5,6)==3&&asked.size()==asks,"a box already outside keeps its plane index and is not asked about");
        check(PortalCulling.intersect(-1,null,1,2,3,4,5,6)==-1,"no culler leaves the code alone");
        // Without Immersive Portals' classes the entry points leave Sodium's answers unchanged and never throw.
        check(PortalCulling.section(true,1,2,3,9.125f)&&!PortalCulling.section(false,1,2,3,9.125f),"entry: section unchanged without Immersive Portals");
        check(PortalCulling.box(true,1,2,3,4,5,6)&&!PortalCulling.box(false,1,2,3,4,5,6),"entry: box unchanged without Immersive Portals");
        check(PortalCulling.intersect(-1,1,2,3,4,5,6)==-1&&PortalCulling.intersect(2,1,2,3,4,5,6)==2,"entry: intersection unchanged without Immersive Portals");
        System.out.println("PASS: Sodium 0.8 portal culling (section radius, pass-through boxes, JOML codes, no culler outside a portal view, missing Immersive Portals)");
    }
}
