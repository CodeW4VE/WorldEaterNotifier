package com.example.worldeaternotifier.mixin;

import com.example.worldeaternotifier.common.ExplosionBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Shadow @Final private World world;

    @Shadow public abstract List<BlockPos> getAffectedBlocks();

    @Unique
    private final Map<BlockPos, BlockState> beforeState = new HashMap<>();

    // Capture the state of all affected blocks before the explosion modifies them
    @Inject(method = "affectWorld", at = @At("HEAD"))
    private void captureBeforeState(CallbackInfo ci) {
        beforeState.clear();
        List<BlockPos> affected = getAffectedBlocks();
        for (BlockPos pos : affected) {
            beforeState.put(pos.toImmutable(), world.getBlockState(pos));
        }
    }

    // After the explosion, determine which blocks were actually destroyed
    @Inject(method = "affectWorld", at = @At("TAIL"))
    private void onAffectWorldTail(CallbackInfo ci) {
        List<BlockPos> affected = getAffectedBlocks();
        List<BlockPos> actuallyDestroyed = new ArrayList<>();
        for (BlockPos pos : affected) {
            BlockState prev = beforeState.get(pos);
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
        beforeState.clear();

        if (!actuallyDestroyed.isEmpty()) {
            ExplosionBlockCallback.EVENT.invoker().onExplosionBlocksDestroyed(world, actuallyDestroyed);
        }
    }
}