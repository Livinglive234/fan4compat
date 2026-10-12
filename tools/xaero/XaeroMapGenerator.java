import org.objectweb.asm.*;

/**
 * Client hooks on Xaero's Minimap and World Map for TARDIS support: the map screen (open/close), and a "virtual location"
 * (exterior position, dimension and waypoint world) read through TardisLocation. Selectors and call sites come from the
 * real Xaero 26.5.0 / 1.46.0 classes and are re-verified by xaeroMapNativeCheck.
 */
public final class XaeroMapGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT+"mixin/xaero/client/",COMPAT=GenerateAddon.ROOT+"xaero/TardisMapCompat",LOCATION=GenerateAddon.ROOT+"xaero/TardisLocation",
        ENTITY="net/minecraft/class_1297",WORLD="net/minecraft/class_638",KEY="net/minecraft/class_5321",CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    static final String RENDER_MINIMAP="renderMinimap(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/class_332;Lxaero/common/minimap/MinimapProcessor;IIIIDIFLxaero/common/graphics/CustomVertexConsumers;)V";

    /**
     * Redirect of every {@code receiver.name(args)ret} call inside the given methods to {@code TardisLocation.helper(receiver, original)}: the
     * synthetic handler takes the receiver (and the call's arguments), makes the original call, and hands the helper the receiver and the
     * original result. A reference result goes through Object and is cast back to its own type.
     */
    static void redirectRead(ClassWriter w,String[] methods,String owner,String name,String args,String ret,String helper)throws Exception {
        Type result=Type.getType(ret);boolean reference=result.getSort()==Type.OBJECT;
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$"+helper+"$"+name,"(L"+owner+";"+args+")"+ret,null,null);
        redirect(m,methods,"L"+owner+";"+name+"("+args+")"+ret);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,0);
        int slot=1;for(Type argument:Type.getArgumentTypes("("+args+")V")){m.visitVarInsn(argument.getOpcode(ILOAD),slot);slot+=argument.getSize();}
        m.visitMethodInsn(INVOKEVIRTUAL,owner,name,"("+args+")"+ret,false);
        String carried=reference?"Ljava/lang/Object;":ret;
        m.visitMethodInsn(INVOKESTATIC,LOCATION,helper,"(Ljava/lang/Object;"+carried+")"+carried,false);
        if(reference)m.visitTypeInsn(CHECKCAST,result.getInternalName());
        m.visitInsn(result.getOpcode(IRETURN));m.visitMaxs(0,0);m.visitEnd();
    }
    /** Redirect of {@code entity.method_233xx()} inside the given methods to TardisLocation.x/y/z(entity, original). */
    static void positionRead(ClassWriter w,String[] methods,String name,String helper)throws Exception {redirectRead(w,methods,ENTITY,name,"","D",helper);}
    static void redirect(MethodVisitor m,String[] methods,String target) {
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true),arr=a.visitArray("method");
        for(String selector:methods)arr.visit(null,selector);arr.visitEnd();a.visit("remap",false);a.visit("require",1);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target",target);at.visit("remap",false);at.visitEnd();a.visitEnd();
    }
    /** Head injector on a static {@code getEntityX/Y/Z(entity, float)}: return the exterior's coordinate for the player. */
    static void entityOverride(ClassWriter w,String method,String helper)throws Exception {
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$"+helper,"(L"+ENTITY+";FL"+CIR+";)V",null,null);
        GenerateAddon.inject(m,method+"(L"+ENTITY+";F)D","HEAD",true);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,LOCATION,helper,"(Ljava/lang/Object;)Ljava/lang/Double;",false);m.visitVarInsn(ASTORE,3);
        Label skip=new Label();m.visitVarInsn(ALOAD,3);m.visitJumpInsn(IFNULL,skip);
        m.visitVarInsn(ALOAD,2);m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);
        m.visitLabel(skip);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    static void entityUtils(String simple,String target)throws Exception {
        String name=ROOT+simple;ClassWriter w=GenerateAddon.writer(name,target);
        entityOverride(w,"getEntityX","entityX");entityOverride(w,"getEntityY","entityY");entityOverride(w,"getEntityZ","entityZ");
        GenerateAddon.save(name,w);
    }
    /** Redirect the X, Y and Z reads, each inside the methods that actually contain it. */
    static void positionReads(String simple,String target,String[] xs,String[] ys,String[] zs)throws Exception {
        String name=ROOT+simple;ClassWriter w=GenerateAddon.writer(name,target);
        positionRead(w,xs,"method_23317","x");positionRead(w,ys,"method_23318","y");positionRead(w,zs,"method_23321","z");
        GenerateAddon.save(name,w);
    }
    static void positionReads(String simple,String target,String method)throws Exception {positionReads(simple,target,new String[]{method},new String[]{method},new String[]{method});}

    /** Head injector on an instance {@code getCaving(DDDLclass_1937;)I}: the virtual location supplies the cave start. */
    static void cavingOverride(String simple,String target)throws Exception {
        String name=ROOT+simple;ClassWriter w=GenerateAddon.writer(name,target);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$caving","(DDDLnet/minecraft/class_1937;L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"getCaving(DDDLnet/minecraft/class_1937;)I","HEAD",true);
        m.visitCode();m.visitMethodInsn(INVOKESTATIC,LOCATION,"caving","()Ljava/lang/Integer;",false);m.visitVarInsn(ASTORE,9);
        Label skip=new Label();m.visitVarInsn(ALOAD,9);m.visitJumpInsn(IFNULL,skip);
        m.visitVarInsn(ALOAD,8);m.visitVarInsn(ALOAD,9);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);
        m.visitLabel(skip);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(name,w);
    }
    /** The client world's dimension as read by a method, redirected to the virtual location's dimension (the minimap reads it through the base level class, the map through the client world). */
    static void dimensionRead(ClassWriter w,String owner,String... methods)throws Exception {redirectRead(w,methods,owner,"method_27983","","L"+KEY+";","dimension");}

    public static void generate()throws Exception {
        // World Map screen: choose what it shows when it opens, and undo that when it closes (unless the minimap mode owns the view).
        String name=ROOT+"TardisMapMixin";
        ClassWriter w=GenerateAddon.writer(name,"xaero/map/gui/GuiMap");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$tardisMapOpened","(L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"<init>(Lnet/minecraft/class_437;Lnet/minecraft/class_437;Lxaero/map/MapProcessor;Lnet/minecraft/class_1297;)V","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,COMPAT,"opened","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE,"fan4$tardisMapClosed","(L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"method_25432()V","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,COMPAT,"closed","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(name,w);

        // Virtual location. Minimap: the helper both Xaero renderers use for the player's position, the new-waypoint dialog,
        // the temporary-waypoint hotkey, and the renderer itself (a per-frame sync plus the dimension it draws).
        entityUtils("TardisMinimapEntityMixin","xaero/hud/entity/EntityUtils");
        positionReads("TardisMinimapWaypointMixin","xaero/common/gui/GuiAddWaypoint",new String[]{"getAutomaticX(D)I"},new String[]{"getAutomaticYInput(Lxaero/hud/minimap/world/MinimapWorld;)Ljava/lang/String;"},new String[]{"getAutomaticZ(D)I"});
        positionReads("TardisMinimapHotkeyMixin","xaero/hud/minimap/controls/key/function/TemporaryWaypointFunction","onPress()V");
        name=ROOT+"TardisMinimapRenderMixin";w=GenerateAddon.writer(name,"xaero/common/minimap/render/MinimapRenderer");
        m=w.visitMethod(ACC_PRIVATE,"fan4$tardisFrame","(Lxaero/hud/minimap/module/MinimapSession;Lnet/minecraft/class_332;Lxaero/common/minimap/MinimapProcessor;IIIIDIFLxaero/common/graphics/CustomVertexConsumers;L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,RENDER_MINIMAP,"HEAD",false);
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,LOCATION,"frame","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        dimensionRead(w,WORLD,RENDER_MINIMAP);
        // The coordinates the information display shows come straight from the player entity in this method; they also follow a flight.
        // The compass: the minimap turns with the camera's yaw (read in getActualAngle), which inside is the interior's.
        redirectRead(w,new String[]{"getActualAngle()D"},"net/minecraft/class_4184","method_19330","","F","cameraYaw");
        // With the minimap locked to north it does not turn; the player arrow turns instead, by the entity's interpolated yaw.
        redirectRead(w,new String[]{RENDER_MINIMAP},ENTITY,"method_5705","F","F","yaw");
        positionRead(w,new String[]{RENDER_MINIMAP},"method_23317","displayX");positionRead(w,new String[]{RENDER_MINIMAP},"method_23318","displayY");positionRead(w,new String[]{RENDER_MINIMAP},"method_23321","displayZ");
        GenerateAddon.save(name,w);
        // World Map: its own position helper (map marker, camera) and the position it loads regions around.
        entityUtils("TardisMapEntityMixin","xaero/map/entity/util/EntityUtil");
        // updateCaveStart treats a map dimension other than the real one as "viewing elsewhere" and falls back to the top view (the Nether's roof).
        name=ROOT+"TardisMapProcessorMixin";w=GenerateAddon.writer(name,"xaero/map/MapProcessor");
        positionRead(w,new String[]{"setMainValues()V"},"method_23317","x");positionRead(w,new String[]{"setMainValues()V"},"method_23318","y");positionRead(w,new String[]{"setMainValues()V"},"method_23321","z");
        dimensionRead(w,WORLD,"updateCaveStart()V");
        GenerateAddon.save(name,w);
        // The World Map's player arrow reads the player's yaw directly, in the screen's render method.
        name=ROOT+"TardisMapYawMixin";w=GenerateAddon.writer(name,"xaero/map/gui/GuiMap");
        redirectRead(w,new String[]{"method_25394(Lnet/minecraft/class_332;IIF)V"},ENTITY,"method_36454","","F","yaw");
        GenerateAddon.save(name,w);
        // Radar: it lists the client world's entities, which inside a TARDIS are the interior's; give it none.
        name=ROOT+"TardisMinimapRadarMixin";w=GenerateAddon.writer(name,"xaero/hud/minimap/radar/state/RadarStateUpdater");
        redirectRead(w,new String[]{"update(Lnet/minecraft/class_638;Lnet/minecraft/class_1297;Lnet/minecraft/class_1657;)V"},WORLD,"method_18112","","Ljava/lang/Iterable;","entities");
        GenerateAddon.save(name,w);
        // Shared map: apply a few received chunks each time Xaero's writer runs (same thread, same cadence as its own writes).
        name=ROOT+"SharedMapTickMixin";w=GenerateAddon.writer(name,"xaero/map/MapWriter");
        m=w.visitMethod(ACC_PRIVATE,"fan4$sharedMapTick","(Lxaero/map/biome/BiomeColorCalculator;Lxaero/map/region/OverlayManager;L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"onRender(Lxaero/map/biome/BiomeColorCalculator;Lxaero/map/region/OverlayManager;)V","HEAD",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"xaero/SharedMapClient","tick","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(name,w);
        // The cave start itself: both mods scan the real (interior) world for a roof, which tells them nothing about the exterior.
        cavingOverride("TardisMapCaveMixin","xaero/map/misc/CaveStartCalculator");
        cavingOverride("TardisMinimapCaveMixin","xaero/common/minimap/write/MinimapWriter");
        // In a ceiling dimension the minimap draws World Map chunks only while the World Map shows the world the player is in; the exterior's
        // dimension would fail that check and leave it drawing the (empty) interior. Both methods compare the map's dimension with the real one.
        name=ROOT+"TardisMinimapLayersMixin";w=GenerateAddon.writer(name,"xaero/common/mods/SupportXaeroWorldmap");
        dimensionRead(w,"net/minecraft/class_1937","caveLayersAreUsable()Z","shouldPreventAutoCaveMode(Lnet/minecraft/class_1937;)Z");
        GenerateAddon.save(name,w);
    }
}
