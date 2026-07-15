package com.example.worldeaternotifier.monitor;

import com.example.worldeaternotifier.common.BaseMachineDefinition;
import com.example.worldeaternotifier.common.BaseMachineInstance;
import com.example.worldeaternotifier.common.DiscordNotifier;
import com.example.worldeaternotifier.common.ExplosionBlockCallback;
import com.example.worldeaternotifier.config.ModConfig;
import com.example.worldeaternotifier.bedrockbreaker.BedrockBreakerManager;
import com.example.worldeaternotifier.trencher.TrencherManager;
import com.example.worldeaternotifier.worldeater.WorldEaterManager;
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
        ModConfig config = TrencherManager.getInstance().getConfig();
        if (config == null) return;
        long currentTick = world.getTime();

        int trencherMinBlocks = config.trencherSettings.minBlocksBroken;
        for (BaseMachineInstance instance : TrencherManager.getInstance().getAll()) {
            if (!instance.isActive() || !instance.getDefinition().dimension().equals(world.getRegistryKey())) continue;
            int count = countBlocksInside(instance.getDefinition(), affectedBlocks);
            if (count >= trencherMinBlocks) {
                instance.updateLastActivityTick(currentTick);
            }
        }

        int bedrockBreakerMinBlocks = config.bedrockBreakerSettings.minBlocksBroken;
        for (BaseMachineInstance instance : BedrockBreakerManager.getInstance().getAll()) {
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

        ModConfig config = WorldEaterManager.getInstance().getConfig();
        if (config == null) return;

        long currentTick = world.getTime();

        // World eater checks (TNT counting)
        int worldEaterMinTnt = config.worldEaterSettings.minTntCount;
        long worldEaterTimeout = config.worldEaterSettings.stopTimeoutSeconds * 20L;
        for (BaseMachineInstance instance : WorldEaterManager.getInstance().getAll()) {
            if (!instance.isActive() || !instance.getDefinition().dimension().equals(world.getRegistryKey())) continue;
            int tntCount = countTntInArea(world, instance.getDefinition());
            if (tntCount >= worldEaterMinTnt) {
                instance.updateLastActivityTick(currentTick);
            }
            checkStuck(instance, currentTick, worldEaterTimeout);
        }

        // Trencher checks (block break timeout only)
        long trencherTimeout = config.trencherSettings.stopTimeoutSeconds * 20L;
        for (BaseMachineInstance instance : TrencherManager.getInstance().getAll()) {
            if (!instance.isActive() || !instance.getDefinition().dimension().equals(world.getRegistryKey())) continue;
            checkStuck(instance, currentTick, trencherTimeout);
        }

        // Bedrock breaker checks (block break timeout only, same detection as trenchers)
        long bedrockBreakerTimeout = config.bedrockBreakerSettings.stopTimeoutSeconds * 20L;
        for (BaseMachineInstance instance : BedrockBreakerManager.getInstance().getAll()) {
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