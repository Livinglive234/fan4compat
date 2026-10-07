package dev.fan4.compat.bclib;

import java.lang.reflect.*;
import java.util.Optional;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Accept 1.21's id key at BCLib's own custom stack decoder, independent of recipe hooks. */
public final class StackCodecCompat {
    public static Object alias(Object input,Object ops) {
        Class<?> map=type("com.mojang.serialization.MapLike");
        return Proxy.newProxyInstance(map.getClassLoader(),new Class<?>[]{map},(proxy,method,args)->{
            Object value;
            try{value=method.invoke(input,args);}catch(InvocationTargetException e){throw e.getCause();}
            if(!method.getName().equals("get")||value!=null)return value;
            String name;
            if(method.getParameterTypes()[0]==String.class)name=(String)args[0];
            else {
                Object result=callTyped("com.mojang.serialization.DynamicOps","getStringValue","com.mojang.serialization.DataResult",ops,new String[]{"java.lang.Object"},args[0]);
                name=(String)((Optional<?>)call(result,"result")).orElse(null);
            }
            if(!"item".equals(name))return null;
            return callTyped("com.mojang.serialization.MapLike","get","java.lang.Object",input,new String[]{"java.lang.String"},"id");
        });
    }
}
