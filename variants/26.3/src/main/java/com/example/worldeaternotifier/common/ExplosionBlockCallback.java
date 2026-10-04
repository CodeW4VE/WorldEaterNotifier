package com.example.worldeaternotifier.common;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.List;

public interface ExplosionBlockCallback {
    Event<ExplosionBlockCallback> EVENT = EventFactory.createArrayBacked(ExplosionBlockCallback.class,
        (listeners) -> (world, affectedBlocks) -> {
            for (ExplosionBlockCallback listener : listeners) {
                listener.onExplosionBlocksDestroyed(world, affectedBlocks);
            }
        });

    void onExplosionBlocksDestroyed(Level world, List<BlockPos> affectedBlocks);
}