package dev.fan4.compat.elytratrails;

import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Supply vanilla-sized wing emitters only when a gliding player's renderer supplied none. */
public final class WingTrailCompat {
    private static final String BASE="dbrighthd.elytratrails.",MC="net.minecraft.";
    private static Object model;
    public static void capture(Object entity,Object matrices) {
        if(!type(MC+"class_1657").isInstance(entity)||!(Boolean)call(entity,"method_6128")||(Boolean)call(entity,"method_5767")
            ||(Boolean)call(type(BASE+"util.ShaderChecksUtil"),"isShadowPass"))return;
        Object sampler=call(type(BASE+"rendering.TrailSystem"),"getWingtipSampler");
        int id=((Number)call(entity,"method_5628")).intValue();
        Object existing=((Map<?,?>)field(sampler,"gatheredTrailsThisFrame")).get(id);
        if(existing instanceof List<?> list&&!list.isEmpty())return;
        if(model==null)model=createExact(MC+"class_563",new String[]{MC+"class_630"},call(call(type(MC+"class_563"),"method_31994"),"method_32109"));
        call(model,"method_17079",entity,0f,0f,0f,0f,0f);
        Object left=call(model,"elytratrails$getLeftWing"),right=call(model,"elytratrails$getRightWing"),util=type(BASE+"util.ModelTransformationUtil");
        Object client=call(type(MC+"class_310"),"method_1551"),camera=call(call(field(client,"field_1773"),"method_19418"),"method_19326");
        call(matrices,"method_22903");
        try {
            call(matrices,"method_22904",0d,0d,.125d);
            Object l=call(util,"transformLocalPointThroughPart",matrices,left,call(util,"computeWingTipLocal",left,true));
            Object r=call(util,"transformLocalPointThroughPart",matrices,right,call(util,"computeWingTipLocal",right,false));
            call(sampler,"insertWingTips",id,call(l,"method_1019",camera),call(r,"method_1019",camera));
        }finally{call(matrices,"method_22909");}
    }
}
