package org.valkyrienskies.core.api.util.functions;

/** Stand-in for the VS core callback used to walk a ship's active chunks; never packaged. */
public interface IntBinaryConsumer {
    void accept(int x,int z);
}
