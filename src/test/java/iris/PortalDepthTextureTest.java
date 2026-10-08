import dev.fan4.compat.iris.PortalDepthTexture;
import org.lwjgl.opengl.GL11;

public final class PortalDepthTextureTest {
    public static void main(String[] args){
        int texture=PortalDepthTexture.create();
        check(texture==101&&GL11.binding==77&&GL11.images==1&&GL11.deletions==0);
        PortalDepthTexture.delete(0);check(GL11.deletions==0);
        PortalDepthTexture.delete(texture);check(GL11.deletions==1);
        GL11.fail=true;
        try{PortalDepthTexture.create();throw new AssertionError();}catch(IllegalStateException expected){check(expected.getMessage().equals("allocation failed"));}
        check(GL11.binding==77&&GL11.deletions==2);
        System.out.println("PASS: complete far-depth sampler, native buffer, binding restoration, zero delete and failed-allocation cleanup (test GL backend)");
    }
    static void check(boolean value){if(!value)throw new AssertionError("Portal depth texture lifecycle");}
}
