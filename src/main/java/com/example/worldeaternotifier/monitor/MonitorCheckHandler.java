package com.example.worldeaternotifier.monitor;

import com.example.worldeaternotifier.common.BaseMachineDefinition;
import com.example.worldeaternotifier.common.BaseMachineInstance;
import com.example.worldeaternotifier.common.DiscordNotifier;
import com.example.worldeaternotifier.common.ExplosionBlockCallback;
import com.example.worldeaternotifier.common.MachineRegistry;
import com.example.worldeaternotifier.config.ModConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class MonitorCheckHandler {
    private static final long CHECK_INTERVAL_TICKS = 20; // 1 second

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(MonitorCheckHandler::onWorldTick);
        ExplosionBlockCallback.EVENT.register(MonitorCheckHandler::onExplosionBlocksDestroyed);
    }

    // ---- Explosion callback for block-break machines (TRENCHERS + BEDROCK BREAKERS) ----
    private static void onExplosionBlocksDestroyed(World world, List<BlockPos> affectedBlocks) {
        if (MachineRegistry.getConfig() == null) return;
        long currentTick = world.getTime();

        int trencherMinBlocks = MachineRegistry.TRENCHER.getSettings().minBlocksBroken;
        for (BaseMachineInstance instance : MachineRegistry.TRENCHER.getAll()) {
            if (!instance.isActive() || !instance.getDefinition().dimension().equals(world.getRegistryKey())) continue;
            if ("2-way".equals(instance.getDetectionType())) continue;
            int count = countBlocksInside(instance.getDefinition(), affectedBlocks);
            if (count >= trencherMinBlocks) {
                instance.updateLastActivityTick(currentTick);
            }
        }

        int bedrockBreakerMinBlocks = MachineRegistry.BEDROCK_BREAKER.getSettings().minBlocksBroken;
        for (BaseMachineInstance instance : MachineRegistry.BEDROCK_BREAKER.getAll()) {
            if (!instance.isActive() || !instance.getDefinition().dimension().equals(world.getRegistryKey())) continue;
            int count = countBlocksInside(instance.getDefinition(), affectedBlocks);
            if (count >= bedrockBreakerMinBlocks) {
                instance.updateLastActivityTick(currentTick);
            }
        }
    }

    // ---- Periodic world tick ----
    private static void onWorldTick(ServerWorld world) {
        if (world.getTime() % CHECK_INTERVAL_TICKS != 0) return;
        if (MachineRegistry.getConfig() == null) return;

        long currentTick = world.getTime();

        // World eater checks (TNT counting)
        ModConfig.MachineSettings weSettings = MachineRegistry.WORLD_EATER.getSettings();
        long worldEaterTimeout = weSettings.stopTimeoutSeconds * 20L;
        for (BaseMachineInstance instance : MachineRegistry.WORLD_EATER.getAll()) {
            if (!instance.isActive() || !instance.getDefinition().dimension().equals(world.getRegistryKey())) continue;
            int tntCount = countTntInArea(world, instance.getDefinition());
            if (tntCount >= weSettings.minTntCount) {
                instance.updateLastActivityTick(currentTick);
            }
            checkStuck(instance, currentTick, worldEaterTimeout);
        }

        // Trencher checks (block break timeout + TNT counting for 2-way trenchers)
        ModConfig.MachineSettings trencherSettings = MachineRegistry.TRENCHER.getSettings();
        long trencherTimeout = trencherSettings.stopTimeoutSeconds * 20L;
        for (BaseMachineInstance instance : MachineRegistry.TRENCHER.getAll()) {
            if (!instance.isActive() || !instance.getDefinition().dimension().equals(world.getRegistryKey())) continue;
            if ("2-way".equals(instance.getDetectionType())) {
                int tntCount = countTntInArea(world, instance.getDefinition());
                if (tntCount >= trencherSettings.minTntCount) {
                    instance.updateLastActivityTick(currentTick);
                }
            }
            checkStuck(instance, currentTick, trencherTimeout);
        }

        // Bedrock breaker checks (block break timeout only, same detection as trenchers)
        long bedrockBreakerTimeout = MachineRegistry.BEDROCK_BREAKER.getSettings().stopTimeoutSeconds * 20L;
        for (BaseMachineInstance instance : MachineRegistry.BEDROCK_BREAKER.getAll()) {
            if (!instance.isActive() || !instance.getDefinition().dimension().equals(world.getRegistryKey())) continue;
            checkStuck(instance, currentTick, bedrockBreakerTimeout);
        }
    }

    private static void checkStuck(BaseMachineInstance instance, long currentTick, long timeoutTicks) {
        long lastActivity = instance.getLastActivityTick();
        if (lastActivity < 0) {
            instance.updateLastActivityTick(currentTick);
            return;
        }
        if (currentTick - lastActivity > timeoutTicks && !instance.isStuckAlertSent()) {
            instance.markStuckAlertSent();
            DiscordNotifier.sendStuck(instance.getMachineType(), instance.getDefinition().name(), instance.getPingSettings());
        }
    }

    private static int countTntInArea(ServerWorld world, BaseMachineDefinition def) {
        Box box = new Box(def.minX(), def.minY(), def.minZ(),
                def.maxX() + 1, def.maxY() + 1, def.maxZ() + 1);
        List<TntEntity> tntList = world.getEntitiesByType(EntityType.TNT, box, tnt -> true);
        return tntList.size();
    }

    private static int countBlocksInside(BaseMachineDefinition def, List<BlockPos> blocks) {
        int count = 0;
        for (BlockPos pos : blocks) {
            if (pos.getX() >= def.minX() && pos.getX() <= def.maxX()
                    && pos.getY() >= def.minY() && pos.getY() <= def.maxY()
                    && pos.getZ() >= def.minZ() && pos.getZ() <= def.maxZ()) {
                count++;
            }
        }
        return count;
    }
}
