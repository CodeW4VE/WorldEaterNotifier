package com.example.worldeaternotifier.mixin;

import com.example.worldeaternotifier.common.ExplosionBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

// NOTE (1.21.6 port): In 1.21.2 Mojang refactored explosions. `Explosion` is now an
// interface (net.minecraft.world.explosion.Explosion) and the real implementation
// lives in `ExplosionImpl`. The old `affectWorld(boolean)` / `getAffectedBlocks()`
// pair is gone; block destruction now happens in the private
// `destroyBlocks(List<BlockPos> positions)` method, which receives the exact list of
// positions about to be destroyed. That's actually a cleaner hook for us: we no longer
// need a separate "get affected blocks" call, since the positions arrive as a parameter.
@Mixin(ExplosionImpl.class)
public abstract class ExplosionMixin {

    @Shadow @Final private ServerWorld world;

    @Unique
    private final Map<BlockPos, BlockState> weNotifier$beforeState = new HashMap<>();

    // Capture the state of all soon-to-be-destroyed blocks before destruction happens
    @Inject(method = "destroyBlocks", at = @At("HEAD"))
    private void weNotifier$captureBeforeState(List<BlockPos> positions, CallbackInfo ci) {
        weNotifier$beforeState.clear();
        for (BlockPos pos : positions) {
            weNotifier$beforeState.put(pos.toImmutable(), world.getBlockState(pos));
        }
    }

    // After destroyBlocks runs, determine which blocks were actually destroyed
    @Inject(method = "destroyBlocks", at = @At("TAIL"))
    private void weNotifier$onDestroyBlocksTail(List<BlockPos> positions, CallbackInfo ci) {
        List<BlockPos> actuallyDestroyed = new ArrayList<>();
        for (BlockPos pos : positions) {
            BlockState prev = weNotifier$beforeState.get(pos);
            if (prev == null) continue;

            // Ignore blocks that were already air before the explosion
            if (prev.isAir()) continue;

            // Ignore TNT blocks (the TNT entity itself that exploded)
            if (prev.isOf(Blocks.TNT)) continue;

            // Count only if the block is now air (i.e. it was successfully destroyed)
            if (world.getBlockState(pos).isAir()) {
                actuallyDestroyed.add(pos.toImmutable());
            }
        }
        weNotifier$beforeState.clear();

        if (!actuallyDestroyed.isEmpty()) {
            ExplosionBlockCallback.EVENT.invoker().onExplosionBlocksDestroyed(world, actuallyDestroyed);
        }
    }
}
