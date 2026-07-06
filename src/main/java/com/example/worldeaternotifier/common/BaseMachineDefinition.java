package com.example.worldeaternotifier.common;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public record BaseMachineDefinition(
        String name,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        RegistryKey<World> dimension) {
}