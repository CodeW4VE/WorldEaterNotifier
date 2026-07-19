package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.config.ModConfig;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Shared whitelist / permission logic used by both /worldeater and /trencher.
 *
 * Rules:
 *  - Op players (permission level 2+, e.g. console too) can always use every command.
 *  - Non-op players can only use the commands if their name is in the shared whitelist.
 *  - Only op players can add or remove names from the whitelist.
 */
public class PermissionManager {
    private static ModConfig config;

    // NOTE (1.21.11 port): numeric hasPermissionLevel(int) was replaced by the new
    // net.minecraft.command.permission API. Old level 2 ("cheats"/op level 2) maps to
    // PermissionLevel.GAMEMASTERS.
    private static final Permission OP_LEVEL_2 = new Permission.Level(PermissionLevel.GAMEMASTERS);

    private PermissionManager() {}

    public static void setConfig(ModConfig cfg) {
        config = cfg;
    }

    public static boolean isOp(ServerCommandSource source) {
        return source.getPermissions().hasPermission(OP_LEVEL_2);
    }

    /** Gate for the root of /worldeater and /trencher: op OR whitelisted player. */
    public static boolean canUseCommands(ServerCommandSource source) {
        if (isOp(source)) return true;
        if (config == null) return false;

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return false; // non-player, non-op source (e.g. a command block)

        return isWhitelisted(player.getGameProfile().name());
    }

    public static boolean isWhitelisted(String playerName) {
        if (config == null || playerName == null) return false;
        for (String name : config.whitelist) {
            if (name.equalsIgnoreCase(playerName)) return true;
        }
        return false;
    }

    /** Returns false if the player was already whitelisted. */
    public static boolean addToWhitelist(String playerName) {
        if (config == null) return false;
        if (isWhitelisted(playerName)) return false;
        config.whitelist.add(playerName);
        config.save();
        return true;
    }

    /** Returns false if the player wasn't on the whitelist. */
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
