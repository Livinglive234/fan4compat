package net.minecraft;

import java.util.HashMap;
import java.util.Map;

/** Stand-in for ClientLevel: chunk columns by position and the section range; never packaged. */
public class class_638 {
    public int minSection=-4,maxSection=20;
    public final Map<Long,class_2818> chunks=new HashMap<>();
    public static long key(int x,int z){return ((long)x<<32)|(z&0xFFFFFFFFL);}
    public class_2818 method_8497(int x,int z){return chunks.get(key(x,z));}
    public int method_32891(){return minSection;}
    public int method_31597(){return maxSection;}
}
