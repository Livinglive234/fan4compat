package dev.fan4.compat.xaero;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * A synced waypoint file is named by its path inside one server's Xaero waypoint folder, for example
 * {@code dim%minecraft$overworld/mw$default_1.txt}. Both ends validate that name and the file text,
 * so a modified client can never make the server (or a joining client) touch another path.
 */
public final class WaypointKeys {
    public static final int MAX_SEGMENT=120,MAX_KEY=240;
    private static final String FORBIDDEN="\\:*?\"<>|\u0000";
    private WaypointKeys() {}
    public static boolean valid(String key) {
        if(key==null||key.isEmpty()||key.length()>MAX_KEY||!key.endsWith(".txt"))return false;
        String[] segments=key.split("/",-1);
        if(segments.length<2||segments.length>3)return false;
        for(String segment:segments) {
            if(segment.isEmpty()||segment.length()>MAX_SEGMENT||segment.equals(".")||segment.equals("..")||segment.startsWith(" ")||segment.endsWith(" "))return false;
            for(int i=0;i<segment.length();i++) {
                char c=segment.charAt(i);
                if(c<0x20||c==0x7f||FORBIDDEN.indexOf(c)>=0)return false;
            }
        }
        // The file name alone must not be a hidden or reserved-looking entry.
        return !segments[segments.length-1].startsWith(".");
    }
    /** Text must encode as strict UTF-8 (no lone surrogates), contain no NUL and fit the byte limit. */
    public static boolean validText(String text,int maxBytes) {
        if(text==null||text.indexOf('\u0000')>=0)return false;
        try {
            ByteBuffer bytes=StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));
            return bytes.remaining()<=maxBytes;
        } catch(CharacterCodingException invalid){return false;}
    }
    public static int bytes(String text){return text.getBytes(StandardCharsets.UTF_8).length;}
}
