import dev.fan4.compat.sodium.PortalOcclusion;
import java.util.*;
import net.minecraft.class_243;
import net.minecraft.class_4076;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

public final class PortalOcclusionTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static final class Host implements PortalOcclusion.Host {
        Object start="never set";final Map<String,Object> sections=new HashMap<>();boolean within=true;final List<String> asked=new ArrayList<>();boolean throwing;
        public Object fan4$section(int x,int y,int z){asked.add(x+","+y+","+z);if(throwing)throw new IllegalStateException("section lookup broke");return sections.get(x+","+y+","+z);}
        public boolean fan4$withinFrustum(Object viewport,Object section){return within;}
        public void fan4$startPoint(Object sectionPos){start=sectionPos;}
    }
    static Portal portalShowing(class_4076 start){Portal portal=new Portal();portal.shape=(p,camera)->start;return portal;}
    public static void main(String[] args) {
        Host host=new Host();Object viewport=new Object();
        // Outside a portal view: cave culling is Immersive Portals' answer, the search starts at the camera.
        PortalRendering.rendering=false;PortalRendering.caveCulling=true;
        check(PortalOcclusion.begin(host,viewport,false)&&host.start==null&&host.asked.isEmpty(),"outside a portal: IP's cave culling answer, no start section");
        PortalRendering.caveCulling=false;
        check(!PortalOcclusion.begin(host,viewport,true),"outside a portal the original value is not used, IP's answer is");
        // In a portal view whose shape gives no start section: same, and the search still starts at the camera.
        PortalRendering.rendering=true;PortalRendering.caveCulling=true;PortalRendering.portal=portalShowing(null);CHelper.camera=new class_243(1,2,3);
        check(PortalOcclusion.begin(host,viewport,false)&&host.start==null&&host.asked.isEmpty(),"portal without a start section keeps cave culling");
        // With a start section: it is handed to the host, cave culling is off, and a start section outside the frustum is tolerated once.
        class_4076 start=new class_4076(10,-2,30);PortalRendering.portal=portalShowing(start);
        host.sections.put("10,-2,30",new Object());host.within=true;
        check(!PortalOcclusion.begin(host,viewport,true)&&host.start==start&&host.asked.equals(List.of("10,-2,30")),"start section handed over and cave culling turned off");
        check(!PortalOcclusion.tolerate(false),"a start section inside the frustum needs no tolerance");
        host.within=false;
        check(!PortalOcclusion.begin(host,viewport,true),"start outside the frustum: still no cave culling");
        check(PortalOcclusion.tolerate(false)&&PortalOcclusion.tolerate(false),"tolerated: every answer is yes while Sodium says no");
        check(PortalOcclusion.tolerate(true),"Sodium's first yes is yes");
        check(!PortalOcclusion.tolerate(false),"and the tolerance ends there");
        // A start section Sodium has no render section for is not tolerated.
        host.sections.clear();
        PortalOcclusion.begin(host,viewport,true);
        check(!PortalOcclusion.tolerate(false),"no render section, no tolerance");
        // Starting a new search clears any tolerance left over.
        host.sections.put("10,-2,30",new Object());PortalOcclusion.begin(host,viewport,true);
        PortalRendering.rendering=false;PortalOcclusion.begin(host,viewport,true);
        check(host.start==null&&!PortalOcclusion.tolerate(false),"a search outside a portal clears the start section and the tolerance");
        // One failure turns it off without throwing, and leaves the original value and no start section.
        PortalRendering.rendering=true;host.throwing=true;host.start=start;
        check(PortalOcclusion.begin(host,viewport,true)&&host.start==null&&!PortalOcclusion.tolerate(false),"a failure returns the original value and clears the state");
        host.throwing=false;PortalRendering.portal=portalShowing(start);
        check(PortalOcclusion.begin(host,viewport,false)==false&&host.start==null,"after a failure the original value is always used");
        PortalOcclusion.reset();
        check(!PortalOcclusion.begin(host,viewport,true)&&host.start==start,"reset turns it back on");
        System.out.println("PASS: Sodium 0.8 portal occlusion (start section, cave culling, tolerated frustum test, state cleared per search, failure latch)");
    }
}
