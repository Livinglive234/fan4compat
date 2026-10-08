import java.nio.file.*;
import org.objectweb.asm.*;

/** Generates standalone mixins; never writes to the supplied mod jars. */
public class GenerateAddon implements Opcodes {
    static final String ROOT = "dev/fan4/compat/";
    static final String CORE = "org/valkyrienskies/core/impl/shadow/Et";
    static final String CI = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    static final String QUEUE = ROOT+"valkyrienskies/DimensionQueue";
    static final Path OUT = Path.of(System.getProperty("fan4compat.generatedClasses", "build/generated/classes"));
    static ClassWriter writer(String name, String target, String... interfaces) {
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            protected String getCommonSuperClass(String a,String b) {return "java/lang/Object";}
        };
        w.visit(V17,ACC_PUBLIC | ACC_SUPER,name,null,"java/lang/Object",interfaces);
        AnnotationVisitor a=w.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;",false);
        AnnotationVisitor arr=a.visitArray("targets");arr.visit(null,target.replace('/','.'));arr.visitEnd();a.visit("remap",false);a.visitEnd();return w;
    }
    static void save(String name,ClassWriter w)throws Exception {
        w.visitEnd();Path p=OUT.resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,w.toByteArray());
    }
    static void inject(MethodVisitor m,String target,String at,boolean cancel) {
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;",true);
        AnnotationVisitor arr=a.visitArray("method");arr.visit(null,target);arr.visitEnd();a.visit("remap",false);a.visit("require",1);
        if(cancel)a.visit("cancellable",true);
        arr=a.visitArray("at");AnnotationVisitor point=arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/At;");point.visit("value",at);point.visitEnd();arr.visitEnd();a.visitEnd();
    }
    public static void main(String[] args)throws Exception {
        ImmersivePortalsGenerator.clientBridges();
        ValkyrienSkiesGenerator.dimensionMixin();
        SableGenerator.sableMixin();
        EurekaGenerator.debugMixin();
        EurekaGenerator.warningMixin();
        PointBlankGenerator.generate();
        DistantHorizonsGenerator.dhDimensionMixin();
        ImmersivePortalsGenerator.chunkCacheDuckMixin();
        TardisBridgeGenerator.generate();
        ShipTransitGenerator.transit();
        ShipLoadRecoveryGenerator.generate();
        MovementDiagnosticsGenerator.generate();
        PortalShipWatchGenerator.generate();
        PortalMotionGenerator.generate();
        TardisQolGenerator.generate();
        ShipPortalCreationGenerator.generate();
        LogCompatibilityGenerator.generate();
        ShipChunkPacketGenerator.generate();
        StackCodecGenerator.generate();
        WingTrailGenerator.generate();
        System.out.println("Generated standalone dimension, ship lifecycle, Sable and optional debug mixins.");
    }
}
