package com.example.worldeaternotifier;

import com.example.worldeaternotifier.bot.DiscordBotManager;
import com.example.worldeaternotifier.common.BaseMachineDefinition;
import com.example.worldeaternotifier.common.BaseMachineInstance;
import com.example.worldeaternotifier.common.DiscordNotifier;
import com.example.worldeaternotifier.common.PermissionManager;
import com.example.worldeaternotifier.config.ModConfig;
import com.example.worldeaternotifier.bedrockbreaker.BedrockBreakerCommand;
import com.example.worldeaternotifier.bedrockbreaker.BedrockBreakerManager;
import com.example.worldeaternotifier.monitor.MonitorCheckHandler;
import com.example.worldeaternotifier.trencher.TrencherCommand;
import com.example.worldeaternotifier.trencher.TrencherManager;
import com.example.worldeaternotifier.worldeater.WorldEaterCommand;
import com.example.worldeaternotifier.worldeater.WorldEaterManager;
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
        DiscordNotifier.setConfig(config.webhookUrl, config.pingRoleId,
                config.worldEaterSettings.messages, config.trencherSettings.messages, config.bedrockBreakerSettings.messages);
        PermissionManager.setConfig(config);

        if ("bot".equals(config.notificationMode) && !config.botToken.isBlank()) {
            DiscordBotManager.getInstance().start(config.botToken);
        }

        // Load world eaters – all inactive by default
        WorldEaterManager weManager = WorldEaterManager.getInstance();
        weManager.setConfig(config);
        for (ModConfig.SavedMachine saved : config.worldEaters) {
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(saved.dimension));
            BaseMachineDefinition def = new BaseMachineDefinition(saved.name,
                    saved.minX, saved.minY, saved.minZ,
                    saved.maxX, saved.maxY, saved.maxZ, dimKey);
            BaseMachineInstance instance = new BaseMachineInstance(def, "WorldEater", config.worldEaterSettings.pingSettings);
            weManager.loadInstance(instance);
        }

        // Load trenchers – all inactive by default
        TrencherManager tManager = TrencherManager.getInstance();
        tManager.setConfig(config);
        for (ModConfig.SavedMachine saved : config.trenchers) {
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(saved.dimension));
            BaseMachineDefinition def = new BaseMachineDefinition(saved.name,
                    saved.minX, saved.minY, saved.minZ,
                    saved.maxX, saved.maxY, saved.maxZ, dimKey);
            BaseMachineInstance instance = new BaseMachineInstance(def, "Trencher", config.trencherSettings.pingSettings);
            tManager.loadInstance(instance);
        }

        // Load bedrock breakers – all inactive by default
        BedrockBreakerManager bbManager = BedrockBreakerManager.getInstance();
        bbManager.setConfig(config);
        for (ModConfig.SavedMachine saved : config.bedrockBreakers) {
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(saved.dimension));
            BaseMachineDefinition def = new BaseMachineDefinition(saved.name,
                    saved.minX, saved.minY, saved.minZ,
                    saved.maxX, saved.maxY, saved.maxZ, dimKey);
            BaseMachineInstance instance = new BaseMachineInstance(def, "BedrockBreaker", config.bedrockBreakerSettings.pingSettings);
            bbManager.loadInstance(instance);
        }

        // Register shutdown hook
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ModConfig cfg = WorldEaterManager.getInstance().getConfig();

            // Stop all active world eaters and notify
            for (BaseMachineInstance inst : WorldEaterManager.getInstance().getAll()) {
                if (inst.isActive()) {
                    DiscordNotifier.sendServerShutdown("WorldEater", inst.getDefinition().name(), inst.getPingSettings());
                    inst.stop();
                }
            }
            for (ModConfig.SavedMachine saved : cfg.worldEaters) {
                saved.active = false;
            }

            // Stop all active trenchers and notify
            for (BaseMachineInstance inst : TrencherManager.getInstance().getAll()) {
                if (inst.isActive()) {
                    DiscordNotifier.sendServerShutdown("Trencher", inst.getDefinition().name(), inst.getPingSettings());
                    inst.stop();
                }
            }
            for (ModConfig.SavedMachine saved : cfg.trenchers) {
                saved.active = false;
            }

            // Stop all active bedrock breakers and notify
            for (BaseMachineInstance inst : BedrockBreakerManager.getInstance().getAll()) {
                if (inst.isActive()) {
                    DiscordNotifier.sendServerShutdown("BedrockBreaker", inst.getDefinition().name(), inst.getPingSettings());
                    inst.stop();
                }
            }
            for (ModConfig.SavedMachine saved : cfg.bedrockBreakers) {
                saved.active = false;
            }

            cfg.save();

            DiscordBotManager.getInstance().stop();
        });

        MonitorCheckHandler.register();
        CommandRegistrationCallback.EVENT.register(WorldEaterCommand::register);
        CommandRegistrationCallback.EVENT.register(TrencherCommand::register);
        CommandRegistrationCallback.EVENT.register(BedrockBreakerCommand::register);
    }
}