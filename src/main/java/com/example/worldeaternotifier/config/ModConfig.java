package com.example.worldeaternotifier.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("worldeaternotifier.json");

    public String webhookUrl = "";
    public String pingRoleId = "";   // empty or "0" means no mention

    public WorldEaterSettings worldEaterSettings = new WorldEaterSettings();
    public TrencherSettings trencherSettings = new TrencherSettings();
    public BedrockBreakerSettings bedrockBreakerSettings = new BedrockBreakerSettings();

    public List<SavedMachine> worldEaters = new ArrayList<>();
    public List<SavedMachine> trenchers = new ArrayList<>();
    public List<SavedMachine> bedrockBreakers = new ArrayList<>();

    // Player names (case-insensitive match) allowed to use the commands even without op.
    // Op players (permission level 2+) can always use every command regardless of this list.
    public List<String> whitelist = new ArrayList<>();

    public static class WorldEaterSettings {
        public int stopTimeoutSeconds = 60;
        public int minTntCount = 20;
        public PingSettings pingSettings = new PingSettings();
    }

    public static class TrencherSettings {
        public int stopTimeoutSeconds = 180;
        public int minBlocksBroken = 3;
        public PingSettings pingSettings = new PingSettings();
    }

    public static class BedrockBreakerSettings {
        public int stopTimeoutSeconds = 60;
        public int minBlocksBroken = 1;
        public PingSettings pingSettings = new PingSettings();
    }

    public static class PingSettings {
        public boolean enabled = true;          // global ping switch
        public boolean onStart = true;
        public boolean onStop = true;           // manual stop
        public boolean onStuck = true;
        public boolean onResumed = true;
        public boolean onShutdown = true;
    }

    public static class SavedMachine {
        public String name;
        public int minX, minY, minZ;
        public int maxX, maxY, maxZ;
        public String dimension;
        public boolean active;

        public SavedMachine(String name, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, String dimension, boolean active) {
            this.name = name;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.dimension = dimension;
            this.active = active;
        }
    }

    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                if (config.worldEaters == null) config.worldEaters = new ArrayList<>();
                if (config.trenchers == null) config.trenchers = new ArrayList<>();
                if (config.bedrockBreakers == null) config.bedrockBreakers = new ArrayList<>();
                if (config.whitelist == null) config.whitelist = new ArrayList<>();
                if (config.worldEaterSettings == null) config.worldEaterSettings = new WorldEaterSettings();
                if (config.trencherSettings == null) config.trencherSettings = new TrencherSettings();
                if (config.bedrockBreakerSettings == null) config.bedrockBreakerSettings = new BedrockBreakerSettings();
                if (config.worldEaterSettings.pingSettings == null) config.worldEaterSettings.pingSettings = new PingSettings();
                if (config.trencherSettings.pingSettings == null) config.trencherSettings.pingSettings = new PingSettings();
                if (config.bedrockBreakerSettings.pingSettings == null) config.bedrockBreakerSettings.pingSettings = new PingSettings();

                // Apply defaults if values are invalid
                if (config.worldEaterSettings.stopTimeoutSeconds <= 0) config.worldEaterSettings.stopTimeoutSeconds = 60;
                if (config.worldEaterSettings.minTntCount < 1) config.worldEaterSettings.minTntCount = 3;
                if (config.trencherSettings.stopTimeoutSeconds <= 0) config.trencherSettings.stopTimeoutSeconds = 180;
                if (config.trencherSettings.minBlocksBroken < 0) config.trencherSettings.minBlocksBroken = 20;
                if (config.bedrockBreakerSettings.stopTimeoutSeconds <= 0) config.bedrockBreakerSettings.stopTimeoutSeconds = 180;
                if (config.bedrockBreakerSettings.minBlocksBroken < 0) config.bedrockBreakerSettings.minBlocksBroken = 1;

                return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}