package org.lwjgl.opengl;
public final class GL {
    public static final Capabilities capabilities=new Capabilities();
    public static Capabilities getCapabilities(){return capabilities;}
    public static final class Capabilities {public long glCopyImageSubData=1;}
}
