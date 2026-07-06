package com.example.worldeaternotifier.common;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.List;

public interface ExplosionBlockCallback {
    Event<ExplosionBlockCallback> EVENT = EventFactory.createArrayBacked(ExplosionBlockCallback.class,
        (listeners) -> (world, affectedBlocks) -> {
            for (ExplosionBlockCallback listener : listeners) {
                listener.onExplosionBlocksDestroyed(world, affectedBlocks);
            }
        });

    void onExplosionBlocksDestroyed(World world, List<BlockPos> affectedBlocks);
}