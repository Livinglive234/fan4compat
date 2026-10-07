import org.objectweb.asm.*;

/** Exact intermediary selectors for DWM 1.0.38.4. */
public final class TardisBridgeGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,HELPER=ROOT+"doctorwho/TardisShipCompat",STATE="net/drgmes/dwm/common/tardis/TardisStateManager";
    static final String CI=GenerateAddon.CI,CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    static final String NBT="net/minecraft/class_2487",LOOKUP="net/minecraft/class_7225$class_7874",POS="net/minecraft/class_2338",DIR="net/minecraft/class_2350";
    static void end(MethodVisitor m) { m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd(); }
    static void redirect(MethodVisitor m,String method,String target) {
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true);
        AnnotationVisitor arr=a.visitArray("method");arr.visit(null,method);arr.visitEnd();a.visit("remap",false);a.visit("require",1);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target",target);at.visit("remap",false);at.visitEnd();a.visitEnd();
    }
    static void state() throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipStateMixin";ClassWriter w=GenerateAddon.writer(name,STATE);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$shipTick","(L"+CI+";)V",null,null);
        GenerateAddon.inject(m,"tick()V","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,HELPER,"tick","(Ljava/lang/Object;)V",false);end(m);
        m=w.visitMethod(ACC_PRIVATE,"fan4$writeWorldPosition","(L"+NBT+";L"+LOOKUP+";L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"method_75(L"+NBT+";L"+LOOKUP+";)L"+NBT+";","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValue","()Ljava/lang/Object;",false);m.visitMethodInsn(INVOKESTATIC,HELPER,"write","(Ljava/lang/Object;Ljava/lang/Object;)V",false);end(m);
        m=w.visitMethod(ACC_PRIVATE,"fan4$readWorldPosition","(L"+NBT+";L"+LOOKUP+";L"+CI+";)V",null,null);
        GenerateAddon.inject(m,"readNbt(L"+NBT+";L"+LOOKUP+";)V","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,"read","(Ljava/lang/Object;Ljava/lang/Object;)V",false);end(m);
        GenerateAddon.save(name,w);
    }
    static void portal() throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipPortalMixin";ClassWriter w=GenerateAddon.writer(name,"net/drgmes/dwm/compat/immersiveportals/ImmersivePortals$TardisPortalsState");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$placeShipDoorPortal","(L"+CI+";)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;",true);
        AnnotationVisitor arr=a.visitArray("method");arr.visit(null,"createEntrancePortals()V");arr.visitEnd();a.visit("remap",false);a.visit("require",1);
        arr=a.visitArray("at");AnnotationVisitor at=arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lnet/minecraft/class_1937;method_8649(Lnet/minecraft/class_1297;)Z");at.visit("ordinal",0);at.visit("remap",false);at.visitEnd();arr.visitEnd();a.visitEnd();m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,HELPER,"updatePortals","(Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
    }
    static void recall() throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipRecallMixin";ClassWriter w=GenerateAddon.writer(name,"net/drgmes/dwm/items/tardis/keys/TardisKeyItem");
        String params="Lnet/minecraft/class_1799;Lnet/minecraft/class_1657;Lnet/minecraft/class_1937;L"+STATE+";";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$shipRecall","("+params+"L"+CI+";)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;",true);
        AnnotationVisitor arr=a.visitArray("method");arr.visit(null,"lambda$use$4("+params+")V");arr.visitEnd();a.visit("remap",false);a.visit("require",1);
        arr=a.visitArray("at");AnnotationVisitor at=arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lnet/drgmes/dwm/common/tardis/systems/TardisSystemFlight;init(ZLjava/util/UUID;)Z");at.visit("remap",false);at.visitEnd();arr.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,3);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,HELPER,"prepareRecall","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
        name=ROOT+"mixin/doctorwho/common/TardisShipLandingMixin";w=GenerateAddon.writer(name,"net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization");
        params="Lnet/minecraft/class_3218;L"+POS+";L"+DIR+";Lnet/drgmes/dwm/enums/TardisVerticalScanning;";
        m=w.visitMethod(ACC_PRIVATE,"fan4$validateRecallShip","("+params+"L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"findLandingSpot("+params+")Lnet/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization$LandingSpot;","HEAD",true);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,HELPER,"invalidRecall","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",false);
        Label ok=new Label();m.visitJumpInsn(IFEQ,ok);m.visitVarInsn(ALOAD,5);m.visitInsn(ACONST_NULL);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);m.visitLabel(ok);end(m);GenerateAddon.save(name,w);
    }
    static void coordinateScan()throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipCoordinateScanMixin";ClassWriter w=GenerateAddon.writer(name,"net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization");
        String args="Lnet/minecraft/class_3218;L"+POS+";L"+DIR+";Lnet/drgmes/dwm/enums/TardisVerticalScanning;";
        String landing="net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization$LandingSpot",operation="com/llamalad7/mixinextras/injector/wrapoperation/Operation";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$scanShipSurface","("+args+"L"+operation+";)L"+landing+";",null,null);
        AnnotationVisitor annotation=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",true);
        AnnotationVisitor selectors=annotation.visitArray("method");selectors.visit(null,"getSafeSpot("+args+")L"+landing+";");selectors.visitEnd();annotation.visit("remap",false);annotation.visitEnd();
        // DWM overwrites its BlockPos parameter while scanning. Keep the entry
        // arguments in this separate frame rather than capturing them at RETURN.
        m.visitCode();m.visitVarInsn(ALOAD,5);m.visitInsn(ICONST_4);m.visitTypeInsn(ANEWARRAY,"java/lang/Object");
        for(int i=1;i<=4;i++) {m.visitInsn(DUP);m.visitInsn(ICONST_0+i-1);m.visitVarInsn(ALOAD,i);m.visitInsn(AASTORE);}
        m.visitMethodInsn(INVOKEINTERFACE,operation,"call","([Ljava/lang/Object;)Ljava/lang/Object;",true);m.visitVarInsn(ASTORE,6);
        for(int i=0;i<=4;i++)m.visitVarInsn(ALOAD,i);m.visitVarInsn(ALOAD,6);
        m.visitMethodInsn(INVOKESTATIC,HELPER,"scanShipLanding","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false);
        m.visitTypeInsn(CHECKCAST,landing);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
    static void flight() throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipFlightMixin";ClassWriter w=GenerateAddon.writer(name,"net/drgmes/dwm/common/tardis/systems/TardisSystemFlight");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$worldFlightDistance","(L"+POS+";Lnet/minecraft/class_2382;)I",null,null);
        redirect(m,"getFlightDuration(II)I","L"+POS+";method_19455(Lnet/minecraft/class_2382;)I");
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,HELPER,"flightDistance","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=ROOT+"mixin/doctorwho/common/TardisShipFlyoverMixin";w=GenerateAddon.writer(name,"net/drgmes/dwm/common/tardis/systems/flight/TardisFlyoverPlanner");
        m=w.visitMethod(ACC_PRIVATE,"fan4$shipFlyover","(L"+STATE+";)Z",null,null);
        // One redirect applies to all four exact call sites.
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true);
        AnnotationVisitor arr=a.visitArray("method");for(String method:new String[]{"affectsTrip()Z","mode()Lnet/drgmes/dwm/common/tardis/systems/flight/TardisFlyoverPlanner$Mode;","shouldTakeOffInstantly()Z","findInstantLandingSpot(Z)Lnet/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization$LandingSpot;"}) arr.visit(null,method);arr.visitEnd();a.visit("remap",false);a.visit("require",4);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","L"+STATE+";isFlyoverEnabled()Z");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,"flyoverEnabled","(Ljava/lang/Object;)Z",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
    static void display() throws Exception {
        String name=ROOT+"mixin/doctorwho/client/TardisShipWaypointMixin";ClassWriter w=GenerateAddon.writer(name,"net/drgmes/dwm/blocks/tardis/consoleunits/screens/TardisConsoleUnitMonitorWaypointsScreen");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$worldWaypointPosition","(L"+NBT+";Ljava/lang/String;)J",null,null);
        redirect(m,"lambda$init$14(Lnet/minecraft/class_4185;)V","L"+NBT+";method_10537(Ljava/lang/String;)J");m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,HELPER,"waypointPosition","(Ljava/lang/Object;Ljava/lang/String;)J",false);m.visitInsn(LRETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE,"fan4$worldWaypointFacing","(L"+NBT+";Ljava/lang/String;)Ljava/lang/String;",null,null);
        redirect(m,"lambda$init$14(Lnet/minecraft/class_4185;)V","L"+NBT+";method_10558(Ljava/lang/String;)Ljava/lang/String;");m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,HELPER,"waypointString","(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;",false);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=ROOT+"mixin/doctorwho/client/TardisShipConsoleMixin";w=GenerateAddon.writer(name,"net/drgmes/dwm/blocks/tardis/consoleunits/BaseTardisConsoleUnitBlockRenderer");
        String target="renderScreenPage1(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;L"+STATE+";)V";
        for(String slot:new String[]{"prev","curr","dest"}) for(boolean face:new boolean[]{false,true}) {
            String word=switch(slot){case "prev"->"Previous";case "dest"->"Destination";default->"Current";};
            String type=face?DIR:POS,helper=face?"displayFacing":"displayPosition";
            m=w.visitMethod(ACC_PRIVATE,"fan4$"+slot+(face?"Facing":"Position"),"(L"+STATE+";)L"+type+";",null,null);
            redirect(m,target,"L"+STATE+";get"+word+"Exterior"+(face?"Facing":"Position")+"()L"+type+";");
            m.visitCode();m.visitVarInsn(ALOAD,1);m.visitLdcInsn(slot);m.visitMethodInsn(INVOKESTATIC,HELPER,helper,"(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,type);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        GenerateAddon.save(name,w);
    }
    static void collision() throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipPortalDataMixin";ClassWriter w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/portal/Portal");
        MethodVisitor m;
        for(boolean read:new boolean[]{false,true}) {
            String helper=read?"readExteriorPortal":"writeExteriorPortal",target=read?"method_5749":"method_5652";
            m=w.visitMethod(ACC_PRIVATE,"fan4$"+helper,"(L"+NBT+";L"+CI+";)V",null,null);
            GenerateAddon.inject(m,target+"(L"+NBT+";)V","RETURN",false);
            m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,helper,"(Ljava/lang/Object;Ljava/lang/Object;)V",false);end(m);
        }
        GenerateAddon.save(name,w);
    }
    static void sonic() throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipSonicMixin";ClassWriter w=GenerateAddon.writer(name,"net/drgmes/dwm/common/sonicdevice/modes/tardis/SonicDeviceTardisMode");
        String player="net/minecraft/class_1657",world="net/minecraft/class_1937",text="net/minecraft/class_2561",slot="net/minecraft/class_1304",hit="net/minecraft/class_3965";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$sonicWorldCoordinates","(L"+player+";L"+text+";ZL"+world+";L"+player+";L"+slot+";L"+hit+";)V",null,null);
        redirect(m,"interactWithBlockNative(L"+world+";L"+player+";L"+slot+";L"+hit+";)Lnet/minecraft/class_1269;","L"+player+";method_7353(L"+text+";Z)V");
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitVarInsn(ILOAD,3);m.visitVarInsn(ALOAD,4);m.visitVarInsn(ALOAD,7);m.visitMethodInsn(INVOKESTATIC,HELPER,"sonicMessage","(Ljava/lang/Object;Ljava/lang/Object;ZLjava/lang/Object;Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
    }
    static void exteriorShape()throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipExteriorShapeMixin",base="net/drgmes/dwm/blocks/tardis/exteriors/BaseTardisExteriorBlock";
        ClassWriter w=GenerateAddon.writer(name,base);
        // The two-argument state collision getter returns its precomputed cache.
        // This shell depends on a live portal, so its settings must disable that cache.
        String settings="net/minecraft/class_4970$class_2251";
        MethodVisitor dynamic=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$liveDoorwayShapes","(L"+settings+";)L"+settings+";",null,null);
        AnnotationVisitor annotation=dynamic.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/ModifyArg;",true);
        AnnotationVisitor selectors=annotation.visitArray("method");selectors.visit(null,"<init>(L"+settings+";Lnet/drgmes/dwm/common/tardis/exteriors/TardisExteriorEntry;)V");selectors.visitEnd();
        annotation.visit("index",0);annotation.visit("require",1);annotation.visit("remap",false);
        AnnotationVisitor point=annotation.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");point.visit("value","INVOKE");point.visit("target","Lnet/drgmes/dwm/utils/base/blocks/BaseRotatableWaterloggedDoubleBlockWithEntity;<init>(L"+settings+";)V");point.visit("remap",false);point.visitEnd();annotation.visitEnd();
        dynamic.visitCode();dynamic.visitVarInsn(ALOAD,0);dynamic.visitMethodInsn(INVOKEVIRTUAL,settings,"method_9624","()L"+settings+";",false);dynamic.visitInsn(ARETURN);dynamic.visitMaxs(0,0);dynamic.visitEnd();
        String args="Lnet/minecraft/class_2680;Lnet/minecraft/class_1922;L"+POS+";Lnet/minecraft/class_3726;";
        for(String target:new String[]{"method_9530","method_9549"}) {
            MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$openShipShell$"+target,"("+args+"L"+CIR+";)V",null,null);
            GenerateAddon.inject(m,target+"("+args+")Lnet/minecraft/class_265;","RETURN",true);
            m.visitCode();m.visitVarInsn(ALOAD,5);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitVarInsn(ALOAD,3);m.visitVarInsn(ALOAD,5);
            m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValue","()Ljava/lang/Object;",false);
            m.visitLdcInsn(target.equals("method_9549")?"collision":"outline");
            m.visitMethodInsn(INVOKESTATIC,HELPER,"doorwayShape","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",false);
            m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);end(m);
        }
        GenerateAddon.save(name,w);
    }
    static void keyRange()throws Exception {
        String name=ROOT+"mixin/doctorwho/common/TardisShipKeyRangeMixin",key="net/drgmes/dwm/items/tardis/keys/TardisKeyItem";
        ClassWriter w=GenerateAddon.writer(name,key);
        String enclosing="Lnet/minecraft/class_1799;Lnet/minecraft/class_1657;Lnet/minecraft/class_1937;L"+STATE+";";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$worldKeyDistance","(L"+POS+";Lnet/minecraft/class_2382;"+enclosing+")I",null,null);
        redirect(m,"lambda$use$4("+enclosing+")V","L"+POS+";method_19455(Lnet/minecraft/class_2382;)I");
        m.visitCode();m.visitVarInsn(ALOAD,4);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);
        m.visitMethodInsn(INVOKESTATIC,HELPER,"keyDistance","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I",false);
        m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
    public static void generate() throws Exception { state();portal();recall();coordinateScan();flight();display();collision();sonic();exteriorShape();keyRange(); }
}
