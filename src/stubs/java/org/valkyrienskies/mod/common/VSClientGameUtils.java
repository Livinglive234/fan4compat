package org.valkyrienskies.mod.common;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;

/** Stand-in for VS's VSClientGameUtils: records the calls it receives; never packaged. */
public class VSClientGameUtils {
    public static final List<Object[]> calls=new ArrayList<>();
    public static void transformRenderWithShip(ShipTransform transform,Matrix4f matrix,double a,double b,double c,double d,double e,double f){calls.add(new Object[]{transform,matrix,a,b,c,d,e,f});}
}
