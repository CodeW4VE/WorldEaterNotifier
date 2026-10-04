package com.example.worldeaternotifier.common;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record BaseMachineDefinition(
        String name,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        ResourceKey<Level> dimension) {
}