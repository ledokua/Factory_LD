package net.ledok.factory_ld.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

public final class FactoryLdConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "factory_ld.json";
    private static final int DEFAULT_MAX_POWER_LINK_DISTANCE = 30;
    private static volatile FactoryLdConfig cached;

    private final List<String> defaultUnlockedGroups;
    private final int maxPowerLinkDistance;

    public FactoryLdConfig(List<String> defaultUnlockedGroups, int maxPowerLinkDistance) {
        this.defaultUnlockedGroups = defaultUnlockedGroups;
        this.maxPowerLinkDistance = maxPowerLinkDistance;
    }

    public List<String> defaultUnlockedGroups() {
        return defaultUnlockedGroups;
    }

    public int maxPowerLinkDistance() {
        return maxPowerLinkDistance;
    }

    public static void invalidateCache() {
        cached = null;
    }

    public static FactoryLdConfig reload() {
        invalidateCache();
        return load();
    }

    public static FactoryLdConfig load() {
        FactoryLdConfig existing = cached;
        if (existing != null) {
            return existing;
        }
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                FactoryLdConfig config = GSON.fromJson(reader, FactoryLdConfig.class);
                if (config != null && config.defaultUnlockedGroups != null) {
                    int clampedDistance = config.maxPowerLinkDistance > 0 ? config.maxPowerLinkDistance : DEFAULT_MAX_POWER_LINK_DISTANCE;
                    if (clampedDistance == config.maxPowerLinkDistance) {
                        cached = config;
                        return config;
                    }
                    FactoryLdConfig normalized = new FactoryLdConfig(config.defaultUnlockedGroups, clampedDistance);
                    save(path, normalized);
                    cached = normalized;
                    return normalized;
                }
            } catch (IOException ignored) {
            }
        }
        FactoryLdConfig config = new FactoryLdConfig(List.of("base"), DEFAULT_MAX_POWER_LINK_DISTANCE);
        save(path, config);
        cached = config;
        return config;
    }

    private static void save(Path path, FactoryLdConfig config) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(config, writer);
        } catch (IOException ignored) {
        }
    }
}
