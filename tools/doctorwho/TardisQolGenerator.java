import org.objectweb.asm.*;

/** Exact DWM 1.0.38.4 QoL selectors, with optional Jade client hooks. */
public final class TardisQolGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,H=ROOT+"doctorwho/TardisShipQol",S=TardisBridgeGenerator.STATE;
    static final String CI=GenerateAddon.CI,CIR=TardisBridgeGenerator.CIR,P=TardisBridgeGenerator.POS,D=TardisBridgeGenerator.DIR,N=TardisBridgeGenerator.NBT;
    static final String F="net/drgmes/dwm/common/tardis/systems/TardisSystemFlight",W="net/drgmes/dwm/common/tardis/systems/flight/TardisFlightWaypointEntry",SCAN="net/drgmes/dwm/enums/TardisVerticalScanning";
    static void end(MethodVisitor m){TardisBridgeGenerator.end(m);}
    static void forward(MethodVisitor m,String helper,int... locals){m.visitCode();for(int local:locals)m.visitVarInsn(ALOAD,local);m.visitMethodInsn(INVOKESTATIC,H,helper,"("+"Ljava/lang/Object;".repeat(locals.length)+")V",false);end(m);}
    static void generate()throws Exception{
        String name=ROOT+"mixin/doctorwho/common/TardisShipFlightQolMixin";ClassWriter w=GenerateAddon.writer(name,F);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$startChase","(Ljava/lang/Boolean;L"+CI+";)V",null,null);GenerateAddon.inject(m,"lambda$takeoff$6(Ljava/lang/Boolean;)V","RETURN",false);forward(m,"startFlight",0);
        m=w.visitMethod(ACC_PRIVATE,"fan4$extendChase","(L"+CI+";)V",null,null);GenerateAddon.inject(m,"tick()V","HEAD",false);forward(m,"updateFlight",0);
        m=w.visitMethod(ACC_PRIVATE,"fan4$saveChase","(L"+N+";L"+CIR+";)V",null,null);GenerateAddon.inject(m,"writeNbt(L"+N+";)L"+N+";","RETURN",false);forward(m,"writeFlight",0,1);
        m=w.visitMethod(ACC_PRIVATE,"fan4$loadChase","(L"+N+";L"+CI+";)V",null,null);GenerateAddon.inject(m,"readNbt(L"+N+";)V","RETURN",false);forward(m,"readFlight",0,1);
        m=w.visitMethod(ACC_PRIVATE,"fan4$immutableShipWaypoint","(L"+W+";L"+W+";L"+CIR+";)V",null,null);GenerateAddon.inject(m,"updateWaypointEntry(L"+W+";L"+W+";)Z","HEAD",true);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,H,"immutableWaypoint","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",false);Label done=new Label();m.visitJumpInsn(IFEQ,done);m.visitVarInsn(ALOAD,3);m.visitFieldInsn(GETSTATIC,"java/lang/Boolean","FALSE","Ljava/lang/Boolean;");m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);m.visitLabel(done);end(m);GenerateAddon.save(name,w);
        name=ROOT+"mixin/doctorwho/common/TardisShipScanningQolMixin";w=GenerateAddon.writer(name,"net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization");
        String args="Lnet/minecraft/class_3218;L"+P+";L"+D+";",landing="net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization$LandingSpot";
        m=w.visitMethod(ACC_PRIVATE,"fan4$localScanOnly","("+args+"L"+SCAN+";)L"+SCAN+";",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/ModifyArg;",true);AnnotationVisitor arr=a.visitArray("method");arr.visit(null,"findLandingSpot("+args+")L"+landing+";");arr.visitEnd();a.visit("index",3);a.visit("require",1);a.visit("remap",false);AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lnet/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization;findLandingSpot("+args+"L"+SCAN+";)L"+landing+";");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitVarInsn(ALOAD,4);m.visitMethodInsn(INVOKESTATIC,H,"scanning","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,SCAN);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=ROOT+"mixin/doctorwho/common/TardisShipControlQolMixin";w=GenerateAddon.writer(name,"net/drgmes/dwm/common/tardis/consoleunits/controls/TardisConsoleControlsStorage");
        m=w.visitMethod(ACC_PRIVATE,"fan4$worldLeverPosition","(L"+S+";)L"+P+";",null,null);TardisBridgeGenerator.redirect(m,"displayNotification(L"+S+";Lnet/drgmes/dwm/enums/TardisConsoleUnitControlRole;Lnet/minecraft/class_1657;Z)V","L"+S+";getDestinationExteriorPosition()L"+P+";");
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,H,"leverPosition","(Ljava/lang/Object;)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,P);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        String packet="net/drgmes/dwm/network/server/TardisConsoleUnitMonitorWaypointCreatePacket",player="net/minecraft/class_1657";
        name=ROOT+"mixin/doctorwho/common/TardisShipWaypointCreateMixin";w=GenerateAddon.writer(name,packet);
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$shipWaypoint","(L"+F+";L"+W+";L"+packet+";L"+player+";L"+S+";)V",null,null);
        TardisBridgeGenerator.redirect(m,"lambda$handle$0(L"+packet+";L"+player+";L"+S+";)V","L"+F+";addWaypointEntry(L"+W+";)V");forward(m,"addWaypoint",0,1,3);GenerateAddon.save(name,w);
        packet="net/drgmes/dwm/network/server/TardisConsoleUnitMonitorWaypointApplyPacket";name=ROOT+"mixin/doctorwho/common/TardisShipWaypointApplyMixin";w=GenerateAddon.writer(name,packet);
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$applyShipWaypoint","(L"+player+";L"+packet+";L"+S+";L"+CI+";)V",null,null);GenerateAddon.inject(m,"lambda$handle$0(L"+player+";L"+packet+";L"+S+";)V","HEAD",true);
        m.visitCode();m.visitVarInsn(ALOAD,2);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,H,"applyWaypoint","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",false);done=new Label();m.visitJumpInsn(IFEQ,done);m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKEVIRTUAL,CI,"cancel","()V",false);m.visitLabel(done);end(m);GenerateAddon.save(name,w);
        screen();creation();jade();
    }
    static void screen()throws Exception{
        String name=ROOT+"mixin/doctorwho/client/TardisShipWaypointQolMixin",screen="net/drgmes/dwm/blocks/tardis/consoleunits/screens/TardisConsoleUnitMonitorWaypointsScreen";ClassWriter w=GenerateAddon.writer(name,screen);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$shipWaypointFields","(L"+CI+";)V",null,null);GenerateAddon.inject(m,"update()V","RETURN",false);forward(m,"updateScreen",0);
        coordinateLabel(w);GenerateAddon.save(name,w);
        name=ROOT+"mixin/doctorwho/client/TardisShipWaypointStatusMixin";
        w=GenerateAddon.writer(name,screen+"$WaypointsListWidget$WaypointEntry");
        m=w.visitMethod(ACC_PRIVATE,"fan4$deletedShipName","(L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"getText()Lnet/minecraft/class_2561;","RETURN",true);
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);
        m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValue","()Ljava/lang/Object;",false);
        m.visitMethodInsn(INVOKESTATIC,H,"waypointText","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false);
        m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
    }
    static void coordinateLabel(ClassWriter w){
        String ctx="net/minecraft/class_332",renderer="net/minecraft/class_327",text="net/minecraft/class_2561";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$shipCoordinateStatus","(L"+ctx+";L"+renderer+";L"+text+";IIIZ)I",null,null);
        TardisBridgeGenerator.redirect(m,"renderAdditional(L"+ctx+";IIF)V","L"+ctx+";method_51439(L"+renderer+";L"+text+";IIIZ)I");
        m.visitCode();for(int i=0;i<=3;i++)m.visitVarInsn(ALOAD,i);for(int i=4;i<=7;i++)m.visitVarInsn(ILOAD,i);
        m.visitMethodInsn(INVOKESTATIC,H,"coordinateLabel","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;IIIZ)I",false);
        m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    static void label(ClassWriter w,String helper){
        String ctx="net/minecraft/class_332",renderer="net/minecraft/class_327",text="net/minecraft/class_2561";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$hideShipCoordinateLabel","(L"+ctx+";L"+renderer+";L"+text+";IIIZ)I",null,null);TardisBridgeGenerator.redirect(m,"renderAdditional(L"+ctx+";IIF)V","L"+ctx+";method_51439(L"+renderer+";L"+text+";IIIZ)I");
        m.visitCode();m.visitVarInsn(ALOAD,3);m.visitFieldInsn(GETSTATIC,"net/drgmes/dwm/DWM$TEXTS","MONITOR_WAYPOINTS_COORDS","L"+text+";");Label draw=new Label();m.visitJumpInsn(IF_ACMPNE,draw);m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,H,helper,"(Ljava/lang/Object;)Z",false);m.visitJumpInsn(IFEQ,draw);m.visitInsn(ICONST_0);m.visitInsn(IRETURN);m.visitLabel(draw);for(int i=1;i<=3;i++)m.visitVarInsn(ALOAD,i);for(int i=4;i<=7;i++)m.visitVarInsn(ILOAD,i);m.visitMethodInsn(INVOKEVIRTUAL,ctx,"method_51439","(L"+renderer+";L"+text+";IIIZ)I",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    static void creation()throws Exception{
        String name=ROOT+"mixin/doctorwho/client/TardisShipWaypointCreationQolMixin",screen="net/drgmes/dwm/blocks/tardis/consoleunits/screens/TardisConsoleUnitMonitorWaypointCreateScreen";
        ClassWriter w=GenerateAddon.writer(name,screen);String args="Lnet/drgmes/dwm/blocks/tardis/consoleunits/BaseTardisConsoleUnitBlockEntity;Ljava/lang/String;Lnet/minecraft/class_5321;L"+P+";L"+D+";Lnet/minecraft/class_437;";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$rememberShipCreation","(L"+CI+";)V",null,null);GenerateAddon.inject(m,"<init>("+args+")V","RETURN",false);forward(m,"rememberCreation",0);
        for(String target:new String[]{"method_25426()V","update()V"}){
            m=w.visitMethod(ACC_PRIVATE,"fan4$shipCreationFields$"+target.substring(0,target.indexOf('(')),"(L"+CI+";)V",null,null);GenerateAddon.inject(m,target,"RETURN",false);forward(m,"updateCreation",0);
        }
        m=w.visitMethod(ACC_PRIVATE,"fan4$shipWaypointDefaultName","(L"+CIR+";)V",null,null);GenerateAddon.inject(m,"getGeneratedWaypointName()Ljava/lang/String;","RETURN",true);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,H,"creationSelected","(Ljava/lang/Object;)Z",false);Label ordinary=new Label();m.visitJumpInsn(IFEQ,ordinary);m.visitVarInsn(ALOAD,1);m.visitLdcInsn("Ship location");m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);m.visitLabel(ordinary);end(m);
        args="Lnet/minecraft/class_5321;L"+P+";L"+D+";Ljava/lang/String;";
        m=w.visitMethod(ACC_PRIVATE,"fan4$createShipAnchor","("+args+")L"+W+";",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true);AnnotationVisitor arr=a.visitArray("method");arr.visit(null,"apply()V");arr.visitEnd();a.visit("remap",false);a.visit("require",1);AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","NEW");at.visit("target","L"+W+";");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitTypeInsn(NEW,W);m.visitInsn(DUP);for(int local=1;local<=4;local++)m.visitVarInsn(ALOAD,local);m.visitMethodInsn(INVOKESPECIAL,W,"<init>","("+args+")V",false);m.visitVarInsn(ASTORE,5);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,5);m.visitMethodInsn(INVOKESTATIC,H,"createdWaypoint","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,W);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();label(w,"creationSelected");GenerateAddon.save(name,w);
    }
    static void jade()throws Exception{
        String name=ROOT+"mixin/jade/client/JadeHelmMixin";ClassWriter w=GenerateAddon.writer(name,"snownee/jade/overlay/OverlayRenderer");MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$hideWhileHelming","(Lnet/minecraft/class_332;FL"+CI+";)V",null,null);
        GenerateAddon.inject(m,"renderOverlay478757(Lnet/minecraft/class_332;F)V","HEAD",true);m.visitCode();m.visitMethodInsn(INVOKESTATIC,ROOT+"jade/HelmOverlayCompat","piloting","()Z",false);Label done=new Label();m.visitJumpInsn(IFEQ,done);m.visitInsn(ICONST_0);m.visitFieldInsn(PUTSTATIC,"snownee/jade/overlay/OverlayRenderer","shown","Z");m.visitMethodInsn(INVOKESTATIC,"snownee/jade/overlay/OverlayRenderer","clearState","()V",false);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEVIRTUAL,CI,"cancel","()V",false);m.visitLabel(done);end(m);GenerateAddon.save(name,w);
    }
}
