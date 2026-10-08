package dev.fan4.compat.freecam;

import static dev.fan4.compat.shared.CompatCalls.*;
import java.util.*;
import java.util.regex.*;

/** BSL's independent DH overlap discard must follow the active camera's chunk coverage. */
public final class FreecamShaderCompat {
    private static final String UNIFORM="fan4compatFreecamUnloaded";
    private static final Map<Object,Integer> LOCATIONS=new WeakHashMap<>();
    private static final Pattern CUTOFF=Pattern.compile("\\bfloat\\s+minDist\\s*=\\s*\\(\\s*dither\\s*-\\s*(?:DH_OVERDRAW|[0-9.]+)\\s*-\\s*0\\.75\\s*\\)\\s*\\*\\s*16\\.0\\s*\\+\\s*far\\s*;\\s*if\\s*\\(\\s*viewLength\\s*<=\\s*minDist\\b");
    private FreecamShaderCompat() {}
    public static String source(String source) {
        if(source==null||source.contains(UNIFORM))return source;
        String code=Pattern.compile("(?s)/\\*.*?\\*/|//[^\\r\\n]*").matcher(source).replaceAll(m->m.group().replaceAll("[^\\r\\n]"," "));
        Matcher match=CUTOFF.matcher(code);if(!match.find())return source;
        int end=match.end();if(match.find())return source;
        String patched=source.substring(0,end)+" && !"+UNIFORM+source.substring(end);
        Matcher directives=Pattern.compile("(?m)^\\s*#(?:version|extension)[^\\r\\n]*(?:\\r?\\n|$)").matcher(patched);
        int offset=0;while(directives.find())offset=directives.end();
        return patched.substring(0,offset)+"\nuniform bool "+UNIFORM+";\n"+patched.substring(offset);
    }
    public static void uniforms(Object program) {
        int location=LOCATIONS.computeIfAbsent(program,p->((Number)call(p,"tryGetUniformLocation2",UNIFORM)).intValue());
        if(location>=0)exact("org.lwjgl.opengl.GL20","glUniform1i",new String[]{"int","int"},location,FreecamTickCompat.unloadedCamera()?1:0);
    }
}
