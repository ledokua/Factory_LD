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

    private final List<String> defaultUnlockedGroups;

    public FactoryLdConfig(List<String> defaultUnlockedGroups) {
        this.defaultUnlockedGroups = defaultUnlockedGroups;
    }

    public List<String> defaultUnlockedGroups() {
        return defaultUnlockedGroups;
    }

    public static FactoryLdConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                FactoryLdConfig config = GSON.fromJson(reader, FactoryLdConfig.class);
                if (config != null && config.defaultUnlockedGroups != null) {
                    return config;
                }
            } catch (IOException ignored) {
            }
        }
        FactoryLdConfig config = new FactoryLdConfig(List.of("base"));
        save(path, config);
        return config;
    }

    private static void save(Path path, FactoryLdConfig config) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(config, writer);
        } catch (IOException ignored) {
        }
    }
}
