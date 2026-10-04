package com.example.worldeaternotifier.mixin;

import com.example.worldeaternotifier.common.ExplosionBlockCallback;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// The 26.3 implementation supplies the affected positions to interactWithBlocks.
@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin {

    @Shadow @Final private ServerLevel level;

    @Unique
    private final Map<BlockPos, BlockState> weNotifier$beforeState = new HashMap<>();

    // Capture the state of all soon-to-be-destroyed blocks before destruction happens
    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void weNotifier$captureBeforeState(List<BlockPos> positions, CallbackInfo ci) {
        weNotifier$beforeState.clear();
        for (BlockPos pos : positions) {
            weNotifier$beforeState.put(pos.immutable(), level.getBlockState(pos));
        }
    }

    // Count only blocks destroyed by this explosion.
    @Inject(method = "interactWithBlocks", at = @At("TAIL"))
    private void weNotifier$onDestroyBlocksTail(List<BlockPos> positions, CallbackInfo ci) {
        List<BlockPos> actuallyDestroyed = new ArrayList<>();
        for (BlockPos pos : positions) {
            BlockState prev = weNotifier$beforeState.get(pos);
            if (prev == null) continue;

            // Ignore blocks that were already air before the explosion
            if (prev.isAir()) continue;

            // Ignore TNT blocks (the TNT entity itself that exploded)
            if (prev.is(Blocks.TNT)) continue;

            // Count only if the block is now air (i.e. it was successfully destroyed)
            if (level.getBlockState(pos).isAir()) {
                actuallyDestroyed.add(pos.immutable());
            }
        }
        weNotifier$beforeState.clear();

        if (!actuallyDestroyed.isEmpty()) {
            ExplosionBlockCallback.EVENT.invoker().onExplosionBlocksDestroyed(level, actuallyDestroyed);
        }
    }
}
