package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.config.ModConfig;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class MachineManager {
    private final String machineType;
    private final Function<ModConfig, List<ModConfig.SavedMachine>> savedList;
    private final Function<ModConfig, ModConfig.MachineSettings> settingsAccessor;
    private final Map<String, BaseMachineInstance> instances = new ConcurrentHashMap<>();
    private ModConfig config;

    public MachineManager(String machineType,
                           Function<ModConfig, List<ModConfig.SavedMachine>> savedList,
                           Function<ModConfig, ModConfig.MachineSettings> settingsAccessor) {
        this.machineType = machineType;
        this.savedList = savedList;
        this.settingsAccessor = settingsAccessor;
    }

    public void setConfig(ModConfig config) { this.config = config; }
    public ModConfig getConfig() { return config; }
    public String getMachineType() { return machineType; }
    public ModConfig.MachineSettings getSettings() { return settingsAccessor.apply(config); }
    public List<ModConfig.SavedMachine> getSavedList() { return savedList.apply(config); }

    public boolean create(BaseMachineDefinition definition, String detectionType) {
        String name = definition.name();
        if (instances.containsKey(name)) return false;
        BaseMachineInstance instance = new BaseMachineInstance(definition, machineType, getSettings().pingSettings, detectionType);
        instances.put(name, instance);

        ModConfig.SavedMachine saved = new ModConfig.SavedMachine(
                name,
                definition.minX(), definition.minY(), definition.minZ(),
                definition.maxX(), definition.maxY(), definition.maxZ(),
                definition.dimension().getValue().toString(),
                false
        );
        saved.detectionType = detectionType;
        getSavedList().add(saved);
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
        getSavedList().removeIf(m -> m.name.equals(name));
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
        for (ModConfig.SavedMachine saved : getSavedList()) {
            if (saved.name.equals(name)) {
                saved.active = active;
                config.save();
                return;
            }
        }
    }
}
