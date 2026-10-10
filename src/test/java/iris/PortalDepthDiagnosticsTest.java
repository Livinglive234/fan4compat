import dev.fan4.compat.iris.PortalDepthDiagnostics;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class PortalDepthDiagnosticsTest {
    public static void main(String[] args) {
        List<String> logs=new ArrayList<>();AtomicInteger reads=new AtomicInteger();
        var probe=new PortalDepthDiagnostics.Probe(()->{reads.incrementAndGet();return "sourceDepth=24 destinationDepth=32";},logs::add);
        for(int i=0;i<20;i++)check(probe.observe(0)==0,"successful native result preserved");
        check(reads.get()==1&&logs.size()==1&&logs.get(0).contains("depthCopy=OK"),"one successful snapshot per launch");
        for(int i=0;i<20;i++)check(probe.observe(1282)==1282,"fallback error preserved");
        check(reads.get()==9&&logs.size()==9&&logs.get(1).contains("GL_INVALID_OPERATION(1282)"),"failure reports bounded at eight");
        AtomicInteger brokenReads=new AtomicInteger();logs.clear();
        probe=new PortalDepthDiagnostics.Probe(()->{brokenReads.incrementAndGet();throw new IllegalStateException("snapshot unavailable");},logs::add);
        check(probe.observe(1286)==1286&&probe.observe(0)==0&&brokenReads.get()==1,"snapshot failure disables diagnostics without changing fallback");
        probe=new PortalDepthDiagnostics.Probe(()->"ok",s->{throw new IllegalStateException("logger unavailable");});
        check(probe.observe(1281)==1281,"logger failure cannot escape to renderer");
        check(PortalDepthDiagnostics.errorName(999).equals("UNKNOWN(999)"),"unknown errors retained");
        System.out.println("PASS: portal-depth result preservation, bounded success/failure reporting and exception isolation");
    }
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
