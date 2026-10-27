import dev.fan4.compat.aether.PortalPack;

public final class PortalPackTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) {
        Object first=new Object(),second=new Object();
        check(PortalPack.firstTime(first),"the first registration for an event runs");
        check(!PortalPack.firstTime(first),"a second call for the same event (the Aether's own, or ours) is skipped");
        check(PortalPack.firstTime(second),"the next pack event registers again");
        check(PortalPack.firstTime(first),"and an earlier event is a new one once another has been seen");
    }
}
