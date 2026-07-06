package com.example.worldeaternotifier.worldeater;

import com.example.worldeaternotifier.common.BaseMachineDefinition;
import com.example.worldeaternotifier.common.BaseMachineInstance;
import com.example.worldeaternotifier.config.ModConfig;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldEaterManager {
    private static final WorldEaterManager INSTANCE = new WorldEaterManager();
    private final Map<String, BaseMachineInstance> instances = new ConcurrentHashMap<>();
    private ModConfig config;

    private WorldEaterManager() {}

    public static WorldEaterManager getInstance() { return INSTANCE; }

    public void setConfig(ModConfig config) { this.config = config; }
    public ModConfig getConfig() { return config; }

    public boolean create(BaseMachineDefinition definition) {
        String name = definition.name();
        if (instances.containsKey(name)) return false;
        BaseMachineInstance instance = new BaseMachineInstance(definition, "WorldEater", config.worldEaterSettings.pingSettings);
        instances.put(name, instance);

        ModConfig.SavedMachine saved = new ModConfig.SavedMachine(
                name,
                definition.minX(), definition.minY(), definition.minZ(),
                definition.maxX(), definition.maxY(), definition.maxZ(),
                definition.dimension().getValue().toString(),
                false
        );
        config.worldEaters.add(saved);
        config.save();
        return true;
    }

    public boolean start(String name) {
        BaseMachineInstance instance = instances.get(name);
        if (instance == null) return false;
        if (instance.isActive()) return false;   // already active
        instance.start();
        updateSavedState(name, true);
        return true;
    }

    public boolean stop(String name) {
        BaseMachineInstance instance = instances.get(name);
        if (instance == null) return false;
        if (!instance.isActive()) return false;  // already inactive
        instance.stop();
        updateSavedState(name, false);
        return true;
    }

    public boolean delete(String name) {
        BaseMachineInstance removed = instances.remove(name);
        if (removed == null) return false;
        config.worldEaters.removeIf(we -> we.name.equals(name));
        config.save();
        return true;
    }

    public Collection<BaseMachineInstance> getAll() { return instances.values(); }

    public BaseMachineInstance get(String name) { return instances.get(name); }

    public String[] getAllNames() { return instances.keySet().toArray(new String[0]); }

    public void loadInstance(BaseMachineInstance instance) {
        instances.put(instance.getDefinition().name(), instance);
    }

    private void updateSavedState(String name, boolean active) {
        for (ModConfig.SavedMachine saved : config.worldEaters) {
            if (saved.name.equals(name)) {
                saved.active = active;
                config.save();
                return;
            }
        }
    }
}