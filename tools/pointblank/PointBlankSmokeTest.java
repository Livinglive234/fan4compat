import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;

public final class PointBlankSmokeTest implements Opcodes {
    public static class Entity {}
    public static class Player extends Entity {public Context context;}
    public record Context(Stack itemStack) {}
    public static class Item {}
    public static class Gun extends Item {
        public static Object getFireModeInstance(Stack stack){return stack.mode;}
        public static Context resolveOperableGunContext(Player player){return player.context;}
        public static UUID getItemStackId(Stack stack){return stack.id;}
    }
    public static class State {public Gun gun;public UUID id=UUID.randomUUID();public Stack lastDraw;public State(Gun gun){this.gun=gun;}public Gun getGunItem(){return gun;}public UUID getId(){return id;}public boolean tryDraw(Entity player,Stack stack){lastDraw=stack;return stack!=null&&stack.method_7909()==gun&&stack.mode!=null;}}
    public static class CustomGun extends Gun {}
    public static class Stack {
        public Object mode=new Object();
        public UUID id=UUID.randomUUID();
        private final Item item;
        public Stack(Item item){this.item=item;}
        public Item method_7909(){return item;}
    }
    static class FixtureLoader extends ClassLoader {
        FixtureLoader(){super(PointBlankSmokeTest.class.getClassLoader());}
        Class<?> define(byte[] bytes){return defineClass(null,bytes,0,bytes.length);}
    }
    public static class Callback {
        public boolean cancelled;public Object value;
        public void setReturnValue(Object value){cancelled=true;this.value=value;}
    }
    public static void main(String[] args)throws Exception {
        ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0],"dev/fan4/compat/mixin/pointblank/client/StaleGunAnimationMixin.class"))).accept(n,0);
        if(args.length>1){VerifyAddon.readJar(args[1]);VerifyAddon.injectionTargets(n);}
        for(MethodNode m:n.methods)m.access=ACC_PUBLIC|ACC_STATIC;
        Map<String,String> types=Map.of("com/vicmatskiv/pointblank/client/GunClientState","PointBlankSmokeTest$State","net/minecraft/class_1309","PointBlankSmokeTest$Entity","net/minecraft/class_1799","PointBlankSmokeTest$Stack","net/minecraft/class_1792","PointBlankSmokeTest$Item","com/vicmatskiv/pointblank/item/GunItem","PointBlankSmokeTest$Gun","org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable","PointBlankSmokeTest$Callback");
        ClassWriter w=new ClassWriter(0);n.accept(new ClassRemapper(w,new Remapper(){public String map(String s){return types.getOrDefault(s,s);}}));
        byte[] b=w.toByteArray();Class<?> c=new ClassLoader(PointBlankSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass(null,b,0,b.length);}}.define();
        var method=c.getMethod("fan4$skipNonGunControllers",Entity.class,Stack.class,Callback.class);
        for(Stack stack:new Stack[]{null,new Stack(new Item()),new Stack(null),new Stack(new Gun()),new Stack(new CustomGun())}){
            Callback cb=new Callback();method.invoke(null,new Entity(),stack,cb);
            boolean invalid=stack==null||!(stack.method_7909() instanceof Gun);
            if(cb.cancelled!=invalid||invalid&&(!(cb.value instanceof Map<?,?> map)||!map.isEmpty()))throw new AssertionError("wrong animation guard result");
            if(cb.cancelled)for(Object ignored:((Map<?,?>)cb.value).values())throw new AssertionError("non-gun controller invoked");
        }
        ClassWriter helper=new ClassWriter(0);
        new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/dev/fan4/compat/pointblank/PointBlankDrawCompat.class"))).accept(new ClassRemapper(helper,new Remapper(){public Object mapValue(Object value){return value instanceof String str ? switch(str){case "com.vicmatskiv.pointblank.item.GunItem" -> "PointBlankSmokeTest$Gun";case "net.minecraft.class_1657" -> "PointBlankSmokeTest$Player";default -> super.mapValue(value);} : super.mapValue(value);}}),0);
        byte[] hbytes=helper.toByteArray();FixtureLoader loader=new FixtureLoader();Class<?> h=loader.define(hbytes);
        var valid=h.getMethod("validDraw",Object.class,Object.class);Gun gun=new CustomGun();State state=new State(gun);Stack ready=new Stack(gun);
        if(!(Boolean)valid.invoke(null,state,ready))throw new AssertionError("valid custom gun draw skipped");
        for(Stack stale:new Stack[]{null,new Stack(new Item()),new Stack(new Gun())})if((Boolean)valid.invoke(null,state,stale))throw new AssertionError("stale draw permitted");
        ready.mode=null;if((Boolean)valid.invoke(null,state,ready))throw new AssertionError("missing fire mode permitted");
        ready.mode=new Object();if(!(Boolean)valid.invoke(null,state,ready))throw new AssertionError("resolved fire mode cannot draw on next attempt");
        ClassNode draw=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0],"dev/fan4/compat/mixin/pointblank/client/StaleGunDrawMixin.class"))).accept(draw,0);
        if(args.length>1){VerifyAddon.injectionTargets(draw);
            String[][] contracts={{"com/vicmatskiv/pointblank/client/GunClientState","getGunItem","()Lcom/vicmatskiv/pointblank/item/GunItem;"},{"com/vicmatskiv/pointblank/item/GunItem","getFireModeInstance","(Lnet/minecraft/class_1799;)Lcom/vicmatskiv/pointblank/item/FireModeInstance;"}};
            for(String[] contract:contracts)if(!VerifyAddon.exists(contract[0],contract[1],contract[2],new HashSet<>()))throw new AssertionError("missing native draw contract "+Arrays.toString(contract));
        }
        for(MethodNode m:draw.methods)m.access=ACC_PUBLIC;
        draw.visitField(ACC_PUBLIC,"gun","LPointBlankSmokeTest$Gun;",null,null).visitEnd();
        MethodVisitor getter=draw.visitMethod(ACC_PUBLIC,"getGunItem","()LPointBlankSmokeTest$Gun;",null,null);
        getter.visitCode();getter.visitVarInsn(ALOAD,0);getter.visitFieldInsn(GETFIELD,draw.name,"gun","LPointBlankSmokeTest$Gun;");getter.visitInsn(ARETURN);getter.visitMaxs(1,1);getter.visitEnd();
        MethodVisitor init=draw.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
        init.visitCode();init.visitVarInsn(ALOAD,0);init.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);init.visitInsn(RETURN);init.visitMaxs(1,1);init.visitEnd();
        ClassWriter dw=new ClassWriter(0);draw.accept(new ClassRemapper(dw,new Remapper(){public String map(String str){return types.getOrDefault(str,str);}}));
        Class<?> drawClass=loader.define(dw.toByteArray());Object drawState=drawClass.getConstructor().newInstance();drawClass.getField("gun").set(drawState,gun);
        var guard=drawClass.getMethod("fan4$rejectStaleDraw",Entity.class,Stack.class,Callback.class);
        for(Stack stack:new Stack[]{null,new Stack(new Item()),new Stack(new Gun()),ready}){
            Callback cb=new Callback();guard.invoke(drawState,new Entity(),stack,cb);
            if(cb.cancelled!=(stack!=ready)||cb.cancelled&&!Boolean.FALSE.equals(cb.value))throw new AssertionError("draw injection returns incorrect decision");
        }
        ready.mode=null;Callback pending=new Callback();guard.invoke(drawState,new Entity(),ready,pending);
        if(!pending.cancelled||!Boolean.FALSE.equals(pending.value))throw new AssertionError("pending draw transition not cancelled");
        ClassNode offhand=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0],"dev/fan4/compat/mixin/pointblank/client/OffhandGunDrawMixin.class"))).accept(offhand,0);
        if(args.length>1){VerifyAddon.injectionTargets(offhand);
            String[][] extra={{"com/vicmatskiv/pointblank/client/GunClientState","getId","()Ljava/util/UUID;"},{"com/vicmatskiv/pointblank/item/GunItem","getItemStackId","(Lnet/minecraft/class_1799;)Ljava/util/UUID;"},{"com/vicmatskiv/pointblank/item/GunItem","resolveOperableGunContext","(Lnet/minecraft/class_1657;)Lcom/vicmatskiv/pointblank/item/GunItem$OperableGunContext;"},{"com/vicmatskiv/pointblank/item/GunItem$OperableGunContext","itemStack","()Lnet/minecraft/class_1799;"}};
            for(String[] contract:extra)if(!VerifyAddon.exists(contract[0],contract[1],contract[2],new HashSet<>()))throw new AssertionError("missing native hand contract "+Arrays.toString(contract));
        }
        for(MethodNode m:offhand.methods)m.access=ACC_PUBLIC;
        MethodVisitor oi=offhand.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);oi.visitCode();oi.visitVarInsn(ALOAD,0);oi.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);oi.visitInsn(RETURN);oi.visitMaxs(1,1);oi.visitEnd();
        ClassWriter ow=new ClassWriter(0);offhand.accept(new ClassRemapper(ow,new Remapper(){public String map(String str){return types.getOrDefault(str,str);}}));
        Class<?> oc=loader.define(ow.toByteArray());Object handler=oc.getConstructor().newInstance();var redirect=oc.getMethod("fan4$drawOperableHand",State.class,Entity.class,Stack.class);
        Player player=new Player();Stack key=new Stack(new Item());ready.id=state.id;ready.mode=new Object();player.context=new Context(ready);
        if(!(Boolean)redirect.invoke(handler,state,player,key)||state.lastDraw!=ready)throw new AssertionError("offhand gun plus main-hand key was not drawn with gun stack");
        if(!(Boolean)redirect.invoke(handler,state,player,ready)||state.lastDraw!=ready)throw new AssertionError("main-hand draw changed");
        Stack replacement=new Stack(gun);player.context=new Context(replacement);
        if((Boolean)redirect.invoke(handler,state,player,key)||state.lastDraw!=key)throw new AssertionError("different instance of same gun rebound to stale state");
        player.context=null;if((Boolean)redirect.invoke(handler,state,player,key))throw new AssertionError("absent context invented a weapon");
        if(!(Boolean)redirect.invoke(handler,state,new Entity(),ready)||state.lastDraw!=ready)throw new AssertionError("nonplayer original call changed");
        System.out.println("PASS: executed offhand/main-hand draw forwarding, key preservation, weapon UUID isolation, absent context and native hand contracts");
        System.out.println("PASS: stale/non-gun/mismatched draw and unresolved fire mode rejected; resolved custom guns resume normally; exact draw contracts verified");
        System.out.println("PASS: stale empty/non-gun/null stacks have no animation controllers; valid guns/subclasses retain native handling; exact Point Blank selector verified");
    }
}
