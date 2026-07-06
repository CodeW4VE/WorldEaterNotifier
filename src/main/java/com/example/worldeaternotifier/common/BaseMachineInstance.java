package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.config.ModConfig.PingSettings;

public class BaseMachineInstance {
    private final BaseMachineDefinition definition;
    private final String machineType;
    private final PingSettings pingSettings;
    private boolean active;
    private long lastActivityTick = -1;
    private boolean stuckAlertSent = false;

    public BaseMachineInstance(BaseMachineDefinition definition, String machineType, PingSettings pingSettings) {
        this.definition = definition;
        this.machineType = machineType;
        this.pingSettings = pingSettings;
        this.active = false;
    }

    public BaseMachineDefinition getDefinition() { return definition; }
    public boolean isActive() { return active; }

    public void start() {
        this.active = true;
        this.lastActivityTick = -1;
        this.stuckAlertSent = false;
    }

    public void stop() {
        this.active = false;
        this.lastActivityTick = -1;
        this.stuckAlertSent = false;
    }

    public long getLastActivityTick() { return lastActivityTick; }

    public void updateLastActivityTick(long tick) {
        this.lastActivityTick = tick;
        if (stuckAlertSent) {
            DiscordNotifier.sendResumed(machineType, definition.name(), pingSettings);
            stuckAlertSent = false;
        }
    }

    public boolean isStuckAlertSent() { return stuckAlertSent; }
    public void markStuckAlertSent() { this.stuckAlertSent = true; }
    public PingSettings getPingSettings() { return pingSettings; }
    public String getMachineType() { return machineType; }
}