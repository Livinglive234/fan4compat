/** The two Aether methods the portal pack mixin hooks (packSetup is public, setupImmersivePortalsPack private, both instance methods) must exist in the real Aether jar (args: the Aether jar). */
public final class AetherPortalPackNativeCheck {
    public static void main(String[] args)throws Exception {
        XaeroSyncNativeCheck.jars=args;
        String aether="com/aetherteam/aether/Aether",desc="(Lcom/aetherteam/aetherfabric/events/AddPackFindersEvent;)V";
        for(String name:new String[]{"packSetup","setupImmersivePortalsPack"})
            if(XaeroSyncNativeCheck.find(aether).methods.stream().noneMatch(m->m.name.equals(name)&&m.desc.equals(desc)&&(m.access&org.objectweb.asm.Opcodes.ACC_STATIC)==0))
                throw new AssertionError("Missing native method: "+aether+"."+name+desc);
        System.out.println("PASS: the Aether methods used by the Immersive Portals pack fix exist in the real jar");
    }
}
