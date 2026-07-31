package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.config.ModConfig;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionManager {
    private static ModConfig config;

    private PermissionManager() {}

    public static void setConfig(ModConfig cfg) {
        config = cfg;
    }

    public static boolean isOp(ServerCommandSource source) {
        return source.hasPermissionLevel(2);
    }

    public static boolean canUseCommands(ServerCommandSource source) {
        if (isOp(source)) return true;
        if (config == null) return false;

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return false;

        return isWhitelisted(player.getGameProfile().getName());
    }

    public static boolean isWhitelisted(String playerName) {
        if (config == null || playerName == null) return false;
        for (String name : config.whitelist) {
            if (name.equalsIgnoreCase(playerName)) return true;
        }
        return false;
    }

    public static boolean addToWhitelist(String playerName) {
        if (config == null) return false;
        if (isWhitelisted(playerName)) return false;
        config.whitelist.add(playerName);
        config.save();
        return true;
    }

    public static boolean removeFromWhitelist(String playerName) {
        if (config == null) return false;
        boolean removed = config.whitelist.removeIf(name -> name.equalsIgnoreCase(playerName));
        if (removed) config.save();
        return removed;
    }

    public static String[] getWhitelist() {
        if (config == null) return new String[0];
        return config.whitelist.toArray(new String[0]);
    }
}
