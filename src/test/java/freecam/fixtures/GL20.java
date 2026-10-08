package org.lwjgl.opengl;
public final class GL20 {
    public static int location,value,calls;
    public static void glUniform1i(int l,int v){location=l;value=v;calls++;}
}
