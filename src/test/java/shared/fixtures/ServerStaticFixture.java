package fixtures;

/** Model VS's common utility class with unrelated client overloads. */
public final class ServerStaticFixture {
    public static String ship(String world) { return "server:" + world; }
    public static MissingClientWorld ship(MissingClientWorld world) { return world; }
    public static boolean shipyard(int x, int z) { return x == z; }
    public static void fail() { throw new IllegalArgumentException("original static failure"); }
}
