package dev.fan4.compat.wwoo;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Give lightweight WWOO feature batches one transient neighbour ring. */
public final class WwooGenerationCompat {
    private WwooGenerationCompat() {}
    public static int border(Object generator,Object event,int original){
        if(original!=0||!Boolean.parseBoolean(System.getProperty("fan4compat.wwooBorder","true")))return original;
        if(!((Enum<?>)field(event,"generatorMode")).name().equals("FEATURES")
            ||!((Enum<?>)field(event,"targetGenerationStep")).name().equals("FEATURES"))return original;
        Object world=field(field(generator,"globalParams"),"mcServerLevel");
        Object key=call(world,"method_27983");
        return call(key,"method_29177").toString().equals("minecraft:overworld")?1:original;
    }
}
