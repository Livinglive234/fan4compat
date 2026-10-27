package org.valkyrienskies.core.api.ships.properties;

import org.valkyrienskies.core.api.util.functions.IntBinaryConsumer;

/** Stand-in for VS core's IShipActiveChunksSet; never packaged. */
public interface IShipActiveChunksSet {
    void forEach(IntBinaryConsumer consumer);
}
