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
    private final Map<BlockPos, BlockState> weNotifier$beforeState = new HashMap<>();

    @Inject(method = "affectWorld", at = @At("HEAD"))
    private void weNotifier$captureBeforeState(boolean bl, CallbackInfo ci) {
        weNotifier$beforeState.clear();
        for (BlockPos pos : getAffectedBlocks()) {
            weNotifier$beforeState.put(pos.toImmutable(), world.getBlockState(pos));
        }
    }

    @Inject(method = "affectWorld", at = @At("TAIL"))
    private void weNotifier$onAffectWorldTail(boolean bl, CallbackInfo ci) {
        List<BlockPos> actuallyDestroyed = new ArrayList<>();
        for (BlockPos pos : getAffectedBlocks()) {
            BlockState prev = weNotifier$beforeState.get(pos);
            if (prev == null) continue;

            if (prev.isAir()) continue;
            if (prev.isOf(Blocks.TNT)) continue;

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
