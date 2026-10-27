package org.valkyrienskies.core.api.ships.properties;

import org.joml.Vector3dc;

/** Stand-in for VS core's ShipTransform (a default method there); never packaged. */
public interface ShipTransform {
    Vector3dc getPositionInShip();
}
