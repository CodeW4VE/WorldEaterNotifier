package com.example.worldeaternotifier;

import com.example.worldeaternotifier.bot.DiscordBotManager;
import com.example.worldeaternotifier.common.BaseMachineDefinition;
import com.example.worldeaternotifier.common.BaseMachineInstance;
import com.example.worldeaternotifier.common.DiscordNotifier;
import com.example.worldeaternotifier.common.MachineCommand;
import com.example.worldeaternotifier.common.MachineManager;
import com.example.worldeaternotifier.common.MachineRegistry;
import com.example.worldeaternotifier.common.PermissionManager;
import com.example.worldeaternotifier.config.ModConfig;
import com.example.worldeaternotifier.monitor.MonitorCheckHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class WorldEaterNotifierMod implements ModInitializer {
    public static MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
        ModConfig config = ModConfig.load();
        PermissionManager.setConfig(config);
        MachineRegistry.setConfig(config);

        if ("bot".equals(config.notificationMode) && !config.botToken.isBlank()) {
            DiscordBotManager.getInstance().start(config.botToken);
        }

        // Load machines – all inactive by default
        for (MachineManager manager : MachineRegistry.all()) {
            for (ModConfig.SavedMachine saved : manager.getSavedList()) {
                RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(saved.dimension));
                BaseMachineDefinition def = new BaseMachineDefinition(saved.name,
                        saved.minX, saved.minY, saved.minZ,
                        saved.maxX, saved.maxY, saved.maxZ, dimKey);
                String detectionType = saved.detectionType == null ? "quarry-like" : saved.detectionType;
                BaseMachineInstance instance = new BaseMachineInstance(def, manager.getMachineType(),
                        manager.getSettings().pingSettings, detectionType);
                manager.loadInstance(instance);
            }
        }

        // Register shutdown hook
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (MachineManager manager : MachineRegistry.all()) {
                for (BaseMachineInstance inst : manager.getAll()) {
                    if (inst.isActive()) {
                        DiscordNotifier.sendServerShutdown(manager.getMachineType(), inst.getDefinition().name(), inst.getPingSettings());
                        inst.stop();
                    }
                }
                for (ModConfig.SavedMachine saved : manager.getSavedList()) {
                    saved.active = false;
                }
            }
            MachineRegistry.getConfig().save();
            DiscordBotManager.getInstance().stop();
        });

        MonitorCheckHandler.register();

        MachineCommand worldEaterCommand = new MachineCommand("WorldEater", "world eater", MachineRegistry.WORLD_EATER,
                false, true, false);
        MachineCommand trencherCommand = new MachineCommand("Trencher", "trencher", MachineRegistry.TRENCHER,
                true, true, true);
        MachineCommand bedrockBreakerCommand = new MachineCommand("BedrockBreaker", "bedrock breaker", MachineRegistry.BEDROCK_BREAKER,
                false, false, true);
        CommandRegistrationCallback.EVENT.register(worldEaterCommand::register);
        CommandRegistrationCallback.EVENT.register(trencherCommand::register);
        CommandRegistrationCallback.EVENT.register(bedrockBreakerCommand::register);
    }
}
