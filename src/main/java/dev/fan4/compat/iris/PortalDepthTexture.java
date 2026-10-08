package dev.fan4.compat.iris;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import static dev.fan4.compat.shared.CompatCalls.exact;

/** A complete one-pixel depth sampler representing no distant terrain hit. */
public final class PortalDepthTexture {
    private static final String GL="org.lwjgl.opengl.GL11";
    private PortalDepthTexture() {}
    public static int create() {
        int binding=(Integer)exact(GL,"glGetInteger",new String[]{"int"},32873);
        int texture=(Integer)exact(GL,"glGenTextures",new String[]{});
        try {
            exact(GL,"glBindTexture",new String[]{"int","int"},3553,texture);
            for(int parameter:new int[]{10241,10240})
                exact(GL,"glTexParameteri",new String[]{"int","int","int"},3553,parameter,9728);
            for(int parameter:new int[]{10242,10243})
                exact(GL,"glTexParameteri",new String[]{"int","int","int"},3553,parameter,33071);
            FloatBuffer depth=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            depth.put(0,1.0f);
            exact(GL,"glTexImage2D",new String[]{"int","int","int","int","int","int","int","int","java.nio.FloatBuffer"},
                3553,0,36012,1,1,0,6402,5126,depth);
            return texture;
        } catch(RuntimeException|Error error) {
            delete(texture);throw error;
        } finally {
            exact(GL,"glBindTexture",new String[]{"int","int"},3553,binding);
        }
    }
    public static void delete(int texture) {
        if(texture!=0)exact(GL,"glDeleteTextures",new String[]{"int"},texture);
    }
}
