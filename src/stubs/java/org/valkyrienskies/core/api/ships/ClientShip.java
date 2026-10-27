package org.valkyrienskies.core.api.ships;

import org.valkyrienskies.core.api.ships.properties.IShipActiveChunksSet;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;

/** Stand-in for VS core's ClientShip (the active chunk set is inherited there); never packaged. */
public interface ClientShip {
    ShipTransform getRenderTransform();
    IShipActiveChunksSet getActiveChunksSet();
}
