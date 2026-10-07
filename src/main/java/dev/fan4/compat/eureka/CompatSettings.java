package dev.fan4.compat.eureka;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class CompatSettings implements ModInitializer {
    private static volatile boolean eurekaDebugLogging;
    public void onInitialize() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("fan4compat.properties");
        Properties properties = new Properties();
        properties.setProperty("eurekaDebugLogging", "false");
        try {
            if (Files.exists(file)) {
                try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { properties.load(reader); }
            } else {
                Files.createDirectories(file.getParent());
                try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Fan4Compat: set eurekaDebugLogging=true and restart to enable ship debug INFO messages.");
                }
            }
            eurekaDebugLogging = Boolean.parseBoolean(properties.getProperty("eurekaDebugLogging", "false"));
        } catch (IOException | IllegalArgumentException e) {
            eurekaDebugLogging = false;
            System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING, "Could not load Fan4Compat settings; Eureka debug logging remains disabled", e);
        }
    }
    public static boolean eurekaDebugEnabled() {
        String override = System.getProperty("fan4compat.shipDebug");
        return override == null ? eurekaDebugLogging : Boolean.parseBoolean(override);
    }
}
