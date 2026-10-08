import dev.fan4.compat.freecam.*;
import net.xolt.freecam.Freecam;
import net.xolt.freecam.util.FreeCamera;
import org.lwjgl.opengl.GL20;
public final class FreecamShaderTest {
    public static class Program {
        public int lookups,location=42;
        public int tryGetUniformLocation2(CharSequence name){check(name.toString().equals("fan4compatFreecamUnloaded"),"uniform name");lookups++;return location;}
    }
    public static void main(String[] args) {
        for(String condition:new String[]{" && cloud < 0.5", ""})for(String overdraw:new String[]{"DH_OVERDRAW","1.0","9.0"}) {
            String original="#version 330\n#extension GL_ARB_shader_texture_lod : enable\nvoid main(){float minDist = (dither - "+overdraw+" - 0.75) * 16.0 + far; if (viewLength <= minDist"+condition+") {discard;}}";
            String fixed=FreecamShaderCompat.source(original);
            check(fixed.indexOf("uniform bool")>fixed.indexOf("#extension"),"directive order");
            check(fixed.contains("viewLength <= minDist && !fan4compatFreecamUnloaded"+condition),"BSL terrain/water discard guard");
            check(FreecamShaderCompat.source(fixed)==fixed,"idempotence");
        }
        String other="#version 330\nvoid main(){if(viewLength < far)discard;}";
        check(FreecamShaderCompat.source(other)==other,"unrelated shader unchanged");check(FreecamShaderCompat.source(null)==null,"missing stage unchanged");
        String comment="/* float minDist = (dither - 1.0 - 0.75) * 16.0 + far; if (viewLength <= minDist)discard; */";
        check(FreecamShaderCompat.source(comment)==comment,"comments do not activate patch");
        FreeCamera camera=new FreeCamera();Freecam.camera=camera;Freecam.enabled=true;
        net.minecraft.class_310.INSTANCE.camera=camera;net.minecraft.class_310.INSTANCE.field_1687=camera.world;
        Program program=new Program();FreecamShaderCompat.uniforms(program);check(GL20.value==1&&GL20.location==42,"unloaded camera guard");
        camera.world.loaded=true;FreecamShaderCompat.uniforms(program);check(GL20.value==0&&program.lookups==1,"loaded restoration and cached lookup");camera.world.loaded=false;
        qouteall.imm_ptl.core.render.context_management.PortalRendering.rendering=true;FreecamShaderCompat.uniforms(program);check(GL20.value==0,"portal view unchanged");qouteall.imm_ptl.core.render.context_management.PortalRendering.rendering=false;
        Freecam.enabled=false;FreecamShaderCompat.uniforms(program);check(GL20.value==0,"disabled restoration");
        Program unsupported=new Program();unsupported.location=-1;int calls=GL20.calls;FreecamShaderCompat.uniforms(unsupported);check(GL20.calls==calls,"unpatched shader gets no GL changes");
        System.out.println("PASS: targeted BSL terrain/water source, preprocessed options, directive order, active/loaded/portal/disabled uniform restoration and unrelated shader preservation (test GL backend)");
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
