package dev.emi.trinkets.api;
import java.util.*;
import net.minecraft.class_1309;
public final class TrinketsApi {
    public static final Map<String,Trinket> TRINKETS=new HashMap<>();
    public static final Map<class_1309,TrinketComponent> COMPONENTS=new HashMap<>();
    private static final Trinket DEFAULT=new Trinket() {};
    public static Trinket getTrinket(Object item){return TRINKETS.getOrDefault(String.valueOf(item),DEFAULT);}
    public static Optional<TrinketComponent> getTrinketComponent(Object entity){return Optional.ofNullable(COMPONENTS.get(entity));}
}
