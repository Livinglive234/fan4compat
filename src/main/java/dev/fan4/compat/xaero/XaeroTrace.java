package dev.fan4.compat.xaero;

import dev.fan4.compat.shared.Diagnostics;
import dev.fan4.compat.shared.GameLog;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Troubleshooting log lines for the Xaero features ("Xaero diagnostics" switch, off by default). Nothing is built or logged while it is off. */
final class XaeroTrace {
    private static final Map<String,String> LAST=new ConcurrentHashMap<>();
    private XaeroTrace() {}
    /** One line per call. */
    static void event(Supplier<String> message){if(Diagnostics.xaero())log(message.get());}
    /** One line each time the text for this key changes. */
    static void changed(String key,Supplier<String> message) {
        if(!Diagnostics.xaero())return;
        String text=message.get();
        if(!text.equals(LAST.put(key,text)))log(text);
    }
    private static void log(String text){GameLog.info("[Fan4Compat XaeroDiag] "+text);}
}
