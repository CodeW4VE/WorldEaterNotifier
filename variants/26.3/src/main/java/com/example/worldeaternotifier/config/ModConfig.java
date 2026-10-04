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
    public String botToken = "";
    public String guildId = "";
    public String channelId = "";
    public String memberDiscordRole = "";
    public String notificationMode = "webhook";
    public boolean showSubscriptionButton = true;

    public MachineSettings worldEaterSettings = new MachineSettings(60, 20, 0);
    public MachineSettings trencherSettings = new MachineSettings(180, 3, 3);
    public MachineSettings bedrockBreakerSettings = new MachineSettings(60, 0, 1);

    public List<SavedMachine> worldEaters = new ArrayList<>();
    public List<SavedMachine> trenchers = new ArrayList<>();
    public List<SavedMachine> bedrockBreakers = new ArrayList<>();

    // Player names (case-insensitive match) allowed to use the commands even without op.
    // Op players (permission level 2+) can always use every command regardless of this list.
    public List<String> whitelist = new ArrayList<>();

    public static class MachineSettings {
        public int stopTimeoutSeconds;
        public int minTntCount;
        public int minBlocksBroken;
        public PingSettings pingSettings = new PingSettings();
        public MessageTemplates messages = new MessageTemplates();

        public MachineSettings() {}

        public MachineSettings(int stopTimeoutSeconds, int minTntCount, int minBlocksBroken) {
            this.stopTimeoutSeconds = stopTimeoutSeconds;
            this.minTntCount = minTntCount;
            this.minBlocksBroken = minBlocksBroken;
        }
    }

    public static class PingSettings {
        public boolean enabled = true;
        public boolean onStart = true;
        public boolean onStop = true;
        public boolean onStuck = true;
        public boolean onResumed = true;
        public boolean onShutdown = true;
    }

    public static class MessageTemplates {
        public String start = "{type} **'{name}'** has started.";
        public String stuck = "{type} **'{name}'** has stopped due to an obstruction.";
        public String resumed = "{type} **'{name}'** has started again.";
        public String manualStop = "{type} **'{name}'** was stopped manually.";
        public String shutdown = "{type} **'{name}'** was shut down with the server and may have broken.";
    }

    public static class SavedMachine {
        public String name;
        public int minX, minY, minZ;
        public int maxX, maxY, maxZ;
        public String dimension;
        public String detectionType = "quarry-like";
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
                if (config.worldEaterSettings == null) config.worldEaterSettings = new MachineSettings(60, 20, 0);
                if (config.trencherSettings == null) config.trencherSettings = new MachineSettings(180, 3, 3);
                if (config.bedrockBreakerSettings == null) config.bedrockBreakerSettings = new MachineSettings(60, 0, 1);

                // Apply defaults if values are invalid
                clampDefaults(config.worldEaterSettings, 60, 3, 0);
                clampDefaults(config.trencherSettings, 180, 3, 20);
                clampDefaults(config.bedrockBreakerSettings, 180, 3, 1);

                for (ModConfig.SavedMachine saved : config.trenchers) {
                    if (saved.detectionType == null || (!saved.detectionType.equals("quarry-like") && !saved.detectionType.equals("2-way"))) {
                        saved.detectionType = "quarry-like";
                    }
                }

                return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    private static void clampDefaults(MachineSettings s, int timeoutDefault, int minTntDefault, int minBlocksDefault) {
        if (s.pingSettings == null) s.pingSettings = new PingSettings();
        if (s.messages == null) s.messages = new MessageTemplates();
        if (s.stopTimeoutSeconds <= 0) s.stopTimeoutSeconds = timeoutDefault;
        if (s.minTntCount < 1) s.minTntCount = minTntDefault;
        if (s.minBlocksBroken < 0) s.minBlocksBroken = minBlocksDefault;
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