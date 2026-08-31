package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.config.ModConfig;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MachineRegistry {
    public static final MachineManager WORLD_EATER =
            new MachineManager("WorldEater", cfg -> cfg.worldEaters, cfg -> cfg.worldEaterSettings);
    public static final MachineManager TRENCHER =
            new MachineManager("Trencher", cfg -> cfg.trenchers, cfg -> cfg.trencherSettings);
    public static final MachineManager BEDROCK_BREAKER =
            new MachineManager("BedrockBreaker", cfg -> cfg.bedrockBreakers, cfg -> cfg.bedrockBreakerSettings);

    private static final Map<String, MachineManager> BY_TYPE = new LinkedHashMap<>();
    static {
        for (MachineManager m : new MachineManager[]{WORLD_EATER, TRENCHER, BEDROCK_BREAKER}) {
            BY_TYPE.put(m.getMachineType(), m);
        }
    }

    private MachineRegistry() {}

    public static MachineManager get(String machineType) { return BY_TYPE.get(machineType); }

    public static Collection<MachineManager> all() { return BY_TYPE.values(); }

    public static void setConfig(ModConfig config) {
        for (MachineManager m : all()) m.setConfig(config);
    }

    public static ModConfig getConfig() { return WORLD_EATER.getConfig(); }
}
