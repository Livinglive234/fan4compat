import dev.fan4.compat.pointblank.RenderDiagnostics;
import java.util.*;
import java.util.concurrent.atomic.*;

public final class RenderDiagnosticsTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static void main(String[] args) {
        AtomicLong clock=new AtomicLong();AtomicInteger reads=new AtomicInteger();List<String> logs=new ArrayList<>();
        Map<String,String> state=new LinkedHashMap<>();state.put("depthWrite","true");state.put("cached.depthWrite","true");state.put("drawFbo","7");
        var probe=new RenderDiagnostics.Probe(clock::get,()->{reads.incrementAndGet();return state;},logs::add);
        var sample=probe.begin("gun","xm3");state.put("depthWrite","false");probe.end(sample,false);
        check(logs.size()==1&&logs.get(0).contains("true -> false")&&logs.get(0).contains("GL=false, cached=true"),"before snapshot copied and cached/actual mismatch reported");
        check(probe.begin("gun","xm3")==null&&reads.get()==2,"per-stage rate cap avoids querying every frame");
        check(probe.begin("world","-")!=null,"independent world checkpoint");
        clock.addAndGet(1_000_000_000L);state.put("depthWrite","true");sample=probe.begin("gun","xm3");state.put("depthWrite","false");probe.end(sample,false);
        check(logs.size()==1,"identical transitions suppressed");
        clock.addAndGet(1_000_000_000L);sample=probe.begin("gun","xm3");probe.end(sample,true);
        check(logs.size()==2&&logs.get(1).contains("exception=true"),"exception completion recorded");
        List<String> failure=new ArrayList<>();AtomicInteger attempts=new AtomicInteger();
        var broken=new RenderDiagnostics.Probe(clock::get,()->{attempts.incrementAndGet();throw new IllegalStateException("missing GL context");},failure::add);
        check(broken.begin("gun","-")==null&&broken.begin("world","-")==null&&attempts.get()==1&&failure.size()==1,"diagnostic failure disables sampling without touching native render");
        List<String> capped=new ArrayList<>();var limit=new RenderDiagnostics.Probe(clock::get,()->state,capped::add);
        for(int i=0;i<120;i++){clock.addAndGet(1_000_000_000L);sample=limit.begin("gun","item"+i);limit.end(sample,false);}
        check(capped.size()==97&&limit.begin("world","-")==null,"96 reports plus one cap notice, then no GL queries");
        check(RenderDiagnostics.mismatches(Map.of("colorWrite","false,false,false,false","cached.colorWrite","true,true,true,true")).containsKey("colorWrite"),"color-write cache mismatch detected");
        check(RenderDiagnostics.mismatches(Map.of("depthWrite","true","cached.depthWrite","true")).isEmpty(),"matching state not labelled a leak");
        String previous=System.getProperty("fan4compat.renderDiagnostics");
        try {
            System.setProperty("fan4compat.renderDiagnostics","false");
            var installed=Map.of("pointblank","2.2.0");
            check(!dev.fan4.compat.shared.CompatibilityRules.applies(installed,false,"dev.fan4.compat.mixin.pointblank.client.GunWorldDiagnosticMixin"),"disabled diagnostic hooks not installed");
            check(dev.fan4.compat.shared.CompatibilityRules.applies(installed,false,"dev.fan4.compat.mixin.pointblank.client.StaleGunDrawMixin"),"existing gun fix unaffected by diagnostic opt-out");
        } finally {if(previous==null)System.clearProperty("fan4compat.renderDiagnostics");else System.setProperty("fan4compat.renderDiagnostics",previous);}
        System.out.println("PASS: graphics state comparison, cache mismatch, sampling/dedup/report caps and diagnostic failure isolation");
    }
}
