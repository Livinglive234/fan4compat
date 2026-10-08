package org.lwjgl.opengl;

import java.nio.FloatBuffer;

/** Test-only OpenGL substitute; never packaged in the addon. */
public final class GL11 {
    public static int binding=77,allocations,deletions,images;
    public static boolean fail;
    public static int glGetInteger(int parameter){if(parameter!=32873)throw new AssertionError();return binding;}
    public static int glGenTextures(){return 100+ ++allocations;}
    public static void glBindTexture(int target,int id){if(target!=3553)throw new AssertionError();binding=id;}
    public static void glTexParameteri(int target,int parameter,int value){if(target!=3553||!(value==9728||value==33071))throw new AssertionError();}
    public static void glTexImage2D(int target,int level,int format,int width,int height,int border,int type,int dataType,FloatBuffer pixels){
        if(target!=3553||level!=0||format!=36012||width!=1||height!=1||border!=0||type!=6402||dataType!=5126||pixels.get(0)!=1f||!pixels.isDirect())throw new AssertionError();
        if(fail)throw new IllegalStateException("allocation failed");images++;
    }
    public static void glDeleteTextures(int id){if(id<=100)throw new AssertionError();deletions++;}
}
