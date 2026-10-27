package org.valkyrienskies.core.internal.world;

import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.internal.ships.VsiQueryableShipData;

/** Stand-in for VS core's VsiClientShipWorld; never packaged. */
public interface VsiClientShipWorld {
    VsiQueryableShipData<ClientShip> getLoadedShips();
}
