package dev.fan4.compat.iris;

import java.util.*;
import java.util.regex.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Keep IP's compiler context scoped even if Iris aborts compilation. */
public final class ShaderCompileCompat {
    public static void clearContext() {
        Object shader=type("net.minecraft.class_281");
        ((ThreadLocal<?>)field(shader,"ip_programType")).remove();
        ((ThreadLocal<?>)field(shader,"ip_programName")).remove();
    }
    private static final Pattern FAR=Pattern.compile("\\bdhFarPlane\\b");
    private static final Pattern DECLARED=Pattern.compile("\\bfloat\\s+dhFarPlane\\b|(?m)^\\s*#define\\s+dhFarPlane\\b");
    /** Iris 1.8.1 already binds this live DH uniform; repair a missing declaration. */
    public static List<String> declarations(List<String> sources) {
        String joined=String.join("",sources);
        String code=joined.replaceAll("(?s)/\\*.*?\\*/|//[^\\r\\n]*","");
        if(!FAR.matcher(code).find()||DECLARED.matcher(code).find())return sources;
        // Shader source is a sequence of fragments. Keep #version and every
        // extension directive ahead of the declaration, even across fragments.
        Matcher directive=Pattern.compile("(?m)^\\s*#(?:version|extension)[^\\r\\n]*(?:\\r?\\n|$)").matcher(joined);
        int offset=0;while(directive.find())offset=directive.end();
        return List.of(joined.substring(0,offset)+"\nuniform float dhFarPlane;\n"+joined.substring(offset));
    }
}
