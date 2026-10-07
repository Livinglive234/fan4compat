import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

/** Scope checks plus a cross-border feature fixture; native DH selector verification. */
public final class WwooGenerationSmokeTest implements Opcodes {
    public enum Mode {FEATURES,SURFACE,INTERNAL_SERVER,PRE_EXISTING_ONLY}
    public enum Step {FEATURES,SURFACE,NOISE}
    public record ChunkPos(int x,int z) {public int getX(){return x;}public int getZ(){return z;}}
    public static class Event {public int widthInChunks=16;public ChunkPos minChunkPos=new ChunkPos(0,0);public Mode generatorMode=Mode.FEATURES;public Step targetGenerationStep=Step.FEATURES;}
    public record Key(String id) {public String method_29177(){return id;}}
    public static class World {public String dimension="minecraft:overworld";public Key method_27983(){return new Key(dimension);}}
    public static class Params {public World mcServerLevel=new World();}
    public static class Generator {public Params globalParams=new Params();}
    public static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    // Features originate in each generated chunk and may write into the next chunk.
    // Compare the retained interior with a larger reference generation area.
    static boolean[] disks(int minX,int minZ,int width,int border){
        int side=width*16;boolean[] output=new boolean[side*side];
        for(int cx=minX-border;cx<minX+width+border;cx++)for(int cz=minZ-border;cz<minZ+width+border;cz++){
            int centerX=cx*16+15,centerZ=cz*16+15;
            for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++)if(dx*dx+dz*dz<=49){
                int x=centerX+dx-minX*16,z=centerZ+dz-minZ*16;
                if(x>=0&&x<side&&z>=0&&z<side)output[z*side+x]=true;
            }
        }
        return output;
    }
    public static void main(String[] args)throws Exception {
        var border=Class.forName("dev.fan4.compat.wwoo.WwooGenerationCompat").getMethod("border",Object.class,Object.class,int.class);
        Generator g=new Generator();Event e=new Event();String previous=System.getProperty("fan4compat.wwooBorder");
        try {
            System.clearProperty("fan4compat.wwooBorder");int patched=(int)border.invoke(null,g,e,0);check(patched==1,"FEATURES gains one bounded neighbour ring");
            for(int min:new int[]{-32,-1,0,16})for(int width:new int[]{2,4,16}){
                boolean[] reference=disks(min,min-3,width,3);
                check(!Arrays.equals(disks(min,min-3,width,0),reference),"fixture reproduces ungenerated edge features");
                check(Arrays.equals(disks(min,min-3,width,patched),reference),"overlap repairs retained feature interior, including negative chunks");
                check(reference.length==width*width*256,"only requested interior is retained");
            }
            check((int)border.invoke(null,g,e,2)==2,"existing native padding preserved");
            for(Mode mode:new Mode[]{Mode.SURFACE,Mode.INTERNAL_SERVER,Mode.PRE_EXISTING_ONLY}){e.generatorMode=mode;check((int)border.invoke(null,g,e,0)==0,"other generation modes unchanged");}
            e.generatorMode=Mode.FEATURES;e.targetGenerationStep=Step.NOISE;check((int)border.invoke(null,g,e,0)==0,"non-feature target unchanged");e.targetGenerationStep=Step.FEATURES;
            g.globalParams.mcServerLevel.dimension="dwm:tardis";check((int)border.invoke(null,g,e,0)==0,"dynamic and remote dimensions unchanged");g.globalParams.mcServerLevel.dimension="minecraft:overworld";
            System.setProperty("fan4compat.wwooBorder","false");check((int)border.invoke(null,g,e,0)==0,"diagnostic opt-out restores original border");
        }finally{if(previous==null)System.clearProperty("fan4compat.wwooBorder");else System.setProperty("fan4compat.wwooBorder",previous);}
        if(args.length>1){
            VerifyAddon.readJar(args[1]);String owner="com/seibel/distanthorizons/common/wrappers/worldGeneration/DhChunkGenerator";
            ClassNode nativeClass=VerifyAddon.classes.get(owner);check(nativeClass!=null,"native DH generator exists");
            MethodNode nativeMethod=nativeClass.methods.stream().filter(m->m.name.equals("generateChunks")).findFirst().orElseThrow();
            int reads=0;for(var insn:nativeMethod.instructions)if(insn instanceof FieldInsnNode f&&f.getOpcode()==GETSTATIC&&f.owner.equals(owner)&&f.name.equals("MAX_WORLD_GEN_CHUNK_BORDER_NEEDED")&&f.desc.equals("I"))reads++;
            check(reads==1,"exactly one native batch-border field read");
            // Execute DH's actual allocation math, stopping before Minecraft chunk reads.
            ClassWriter math=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
            math.visit(V17,ACC_PUBLIC,"WwooNativeBatchMath",null,"java/lang/Object",null);
            MethodVisitor method=math.visitMethod(ACC_PUBLIC|ACC_STATIC,"bounds","(Ljava/lang/Object;LWwooGenerationSmokeTest$Event;I)[I",null,null);
            method.visitCode();Map<String,String> mapping=Map.of("com/seibel/distanthorizons/common/wrappers/worldGeneration/ChunkGenEvent","WwooGenerationSmokeTest$Event","com/seibel/distanthorizons/core/pos/DhChunkPos","WwooGenerationSmokeTest$ChunkPos","com/seibel/distanthorizons/core/util/LodUtil","WwooGenerationSmokeTest");
            MethodVisitor mapped=new MethodRemapper(method,new SimpleRemapper(mapping){public String mapMethodName(String owner,String name,String descriptor){return name.equals("assertTrue")?"check":name;}});
            boolean copying=false;
            for(var insn:nativeMethod.instructions){
                if(insn instanceof FieldInsnNode f&&f.name.equals("MAX_WORLD_GEN_CHUNK_BORDER_NEEDED"))copying=true;
                if(!copying)continue;
                if(insn instanceof TypeInsnNode t&&t.getOpcode()==NEW&&t.desc.endsWith("/LightGetterAdaptor"))break;
                if(insn instanceof FrameNode)continue;
                if(insn instanceof FieldInsnNode f&&f.name.equals("MAX_WORLD_GEN_CHUNK_BORDER_NEEDED"))method.visitVarInsn(ILOAD,2);
                else insn.accept(mapped);
            }
            method.visitInsn(ICONST_3);method.visitIntInsn(NEWARRAY,T_INT);
            for(int i=0;i<3;i++){method.visitInsn(DUP);method.visitInsn(ICONST_0+i);method.visitVarInsn(ILOAD,new int[]{4,5,3}[i]);method.visitInsn(IASTORE);}
            method.visitInsn(ARETURN);method.visitMaxs(0,0);method.visitEnd();math.visitEnd();byte[] bytecode=math.toByteArray();
            Class<?> nativeMath=new ClassLoader(WwooGenerationSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass("WwooNativeBatchMath",bytecode,0,bytecode.length);}}.define();
            Method bounds=nativeMath.getMethod("bounds",Object.class,Event.class,int.class);
            e.widthInChunks=16;e.minChunkPos=new ChunkPos(-20,30);
            int[] vanilla=(int[])bounds.invoke(null,g,e,0),overlap=(int[])bounds.invoke(null,g,e,1);
            check(Arrays.equals(vanilla,new int[]{-20,30,15}),"native baseline batch bounds");
            check(Arrays.equals(overlap,new int[]{-21,29,17}),"native math expands symmetrically and retains original event");
            check(e.widthInChunks==16&&e.minChunkPos.equals(new ChunkPos(-20,30)),"requested output coordinates never rewritten");

            ClassNode mixin=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/wwoo/common/WwooDhGenerationMixin.class"))).accept(mixin,0);VerifyAddon.injectionTargets(mixin);
            MethodNode hook=mixin.methods.stream().filter(m->m.name.equals("fan4$wwooNeighbourRing")).findFirst().orElseThrow();
            check(hook.desc.equals("(Lcom/seibel/distanthorizons/common/wrappers/worldGeneration/ChunkGenEvent;)I"),"static-field redirect captures original event argument");
            for(String[] field:new String[][]{{owner,"globalParams"},{"com/seibel/distanthorizons/common/wrappers/worldGeneration/params/GlobalWorldGenParams","mcServerLevel"},{"com/seibel/distanthorizons/common/wrappers/worldGeneration/ChunkGenEvent","generatorMode"},{"com/seibel/distanthorizons/common/wrappers/worldGeneration/ChunkGenEvent","targetGenerationStep"}})check(VerifyAddon.fieldExists(field[0],field[1]),"missing native field "+Arrays.toString(field));
        }
        System.out.println("PASS: bounded WWOO overlap, cross-edge disk fixture, negative coordinates, requested interior, generation/dimension scope, native padding preservation and exact DH field selector");
    }
}
