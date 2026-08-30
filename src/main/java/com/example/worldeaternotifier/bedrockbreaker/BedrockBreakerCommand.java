package com.example.worldeaternotifier.bedrockbreaker;

import com.example.worldeaternotifier.bot.DiscordBotManager;
import com.example.worldeaternotifier.common.BaseMachineDefinition;
import com.example.worldeaternotifier.common.BaseMachineInstance;
import com.example.worldeaternotifier.common.DiscordNotifier;
import com.example.worldeaternotifier.common.PermissionManager;
import com.example.worldeaternotifier.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket.CommandNodeInspector;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.net.URI;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BedrockBreakerCommand {

    private static final SuggestionProvider<ServerCommandSource> BEDROCK_BREAKER_NAMES = (context, builder) -> {
        String[] names = BedrockBreakerManager.getInstance().getAllNames();
        return CommandSource.suggestMatching(names, builder);
    };

    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYER_NAMES = (context, builder) -> {
        ServerCommandSource source = context.getSource();
        if (source.getServer() == null) return Suggestions.empty();
        String[] names = source.getServer().getPlayerManager().getPlayerList().stream()
                .map(p -> p.getGameProfile().getName())
                .toArray(String[]::new);
        return CommandSource.suggestMatching(names, builder);
    };

    private static final SuggestionProvider<ServerCommandSource> WHITELISTED_PLAYER_NAMES = (context, builder) ->
            CommandSource.suggestMatching(PermissionManager.getWhitelist(), builder);

    private static final SuggestionProvider<ServerCommandSource> MODE_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(new String[]{"webhook", "bot"}, builder);

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_X = (ctx, builder) -> {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return Suggestions.empty();
        builder.suggest(String.valueOf(p.getBlockPos().getX()));
        return builder.buildFuture();
    };
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_Y = (ctx, builder) -> {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return Suggestions.empty();
        builder.suggest(String.valueOf(p.getBlockPos().getY()));
        return builder.buildFuture();
    };
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_Z = (ctx, builder) -> {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return Suggestions.empty();
        builder.suggest(String.valueOf(p.getBlockPos().getZ()));
        return builder.buildFuture();
    };

    private static boolean isWebhookMode() {
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        return config != null && "webhook".equals(config.notificationMode);
    }

    private static boolean isBotMode() {
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        return config != null && "bot".equals(config.notificationMode);
    }

    private static boolean isDeliveryConfigured() {
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        if (config == null) return false;
        if ("webhook".equals(config.notificationMode)) {
            return !config.webhookUrl.isBlank();
        } else if ("bot".equals(config.notificationMode)) {
            return !config.botToken.isBlank() && !config.guildId.isBlank() && !config.channelId.isBlank();
        }
        return false;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("bedrockbreaker")
                .requires(PermissionManager::canUseCommands)
                .then(literal("create")
                        .then(argument("name", StringArgumentType.word())
                                .then(argument("x1", IntegerArgumentType.integer()).suggests(SUGGEST_X)
                                        .then(argument("y1", IntegerArgumentType.integer()).suggests(SUGGEST_Y)
                                                .then(argument("z1", IntegerArgumentType.integer()).suggests(SUGGEST_Z)
                                                        .then(argument("x2", IntegerArgumentType.integer()).suggests(SUGGEST_X)
                                                                .then(argument("y2", IntegerArgumentType.integer()).suggests(SUGGEST_Y)
                                                                        .then(argument("z2", IntegerArgumentType.integer()).suggests(SUGGEST_Z)
                                                                                .executes(BedrockBreakerCommand::executeCreate)
                                                                        ))))))))
                .then(literal("start")
                        .then(argument("name", StringArgumentType.word()).suggests(BEDROCK_BREAKER_NAMES)
                                .executes(BedrockBreakerCommand::executeStart)))
                .then(literal("stop")
                        .then(argument("name", StringArgumentType.word()).suggests(BEDROCK_BREAKER_NAMES)
                                .executes(BedrockBreakerCommand::executeStop)))
                .then(literal("list")
                        .executes(BedrockBreakerCommand::executeList))
                .then(literal("delete")
                        .then(argument("name", StringArgumentType.word()).suggests(BEDROCK_BREAKER_NAMES)
                                .executes(BedrockBreakerCommand::executeDelete)))
                .then(literal("settings")
                        .then(literal("show").executes(BedrockBreakerCommand::executeSettingsShow))
                        .then(literal("setWebhookUrl")
                                .requires(s -> isWebhookMode() && PermissionManager.isOp(s))
                                .then(argument("url", StringArgumentType.greedyString())
                                        .executes(BedrockBreakerCommand::executeSetWebhookUrl)))
                        .then(literal("setPingRoleId")
                                .requires(PermissionManager::isOp)
                                .then(argument("roleId", StringArgumentType.word())
                                        .executes(BedrockBreakerCommand::executeSetPingRoleId)))
                        .then(literal("setBotToken")
                                .requires(s -> isBotMode() && PermissionManager.isOp(s))
                                .then(argument("token", StringArgumentType.greedyString())
                                        .executes(BedrockBreakerCommand::executeSetBotToken)))
                        .then(literal("setGuildId")
                                .requires(s -> isBotMode() && PermissionManager.isOp(s))
                                .then(argument("id", StringArgumentType.word())
                                        .executes(BedrockBreakerCommand::executeSetGuildId)))
                        .then(literal("setChannelId")
                                .requires(s -> isBotMode() && PermissionManager.isOp(s))
                                .then(argument("id", StringArgumentType.word())
                                        .executes(BedrockBreakerCommand::executeSetChannelId)))
                        .then(literal("setMemberDiscordRole")
                                .requires(s -> isBotMode() && PermissionManager.isOp(s))
                                .then(argument("roleId", StringArgumentType.word())
                                        .executes(BedrockBreakerCommand::executeSetMemberDiscordRole)))
                        .then(literal("setNotificationMode")
                                .requires(PermissionManager::isOp)
                                .then(argument("mode", StringArgumentType.word()).suggests(MODE_SUGGESTIONS)
                                        .executes(BedrockBreakerCommand::executeSetNotificationMode)))
                        .then(literal("setStopTimeout")
                                .then(argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(BedrockBreakerCommand::executeSetStopTimeout)))
                        .then(literal("setMinBlocksBroken")
                                .then(argument("count", IntegerArgumentType.integer(0))
                                        .executes(BedrockBreakerCommand::executeSetMinBlocksBroken)))
                        .then(literal("showSubscriptionButton")
                                .requires(s -> isBotMode() && PermissionManager.isOp(s))
                                .then(argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean val = BoolArgumentType.getBool(ctx, "value");
                                            ModConfig config = BedrockBreakerManager.getInstance().getConfig();
                                            config.showSubscriptionButton = val;
                                            config.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("Subscription button " + (val ? "shown" : "hidden") + " on start messages."), true);
                                            return 1;
                                        })))
                        .then(literal("discordPings")
                                .then(literal("show").executes(BedrockBreakerCommand::executePingShow))
                                .then(literal("enable")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(BedrockBreakerCommand::executePingEnable)))
                                .then(literal("onStart")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(BedrockBreakerCommand::executePingOnStart)))
                                .then(literal("onStop")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(BedrockBreakerCommand::executePingOnStop)))
                                .then(literal("onStuck")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(BedrockBreakerCommand::executePingOnStuck)))
                                .then(literal("onResumed")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(BedrockBreakerCommand::executePingOnResumed)))
                                .then(literal("onShutdown")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(BedrockBreakerCommand::executePingOnShutdown)))
                        )
                        .then(literal("whitelist")
                                .then(literal("list")
                                        .executes(BedrockBreakerCommand::executeWhitelistList))
                                .then(literal("add")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYER_NAMES)
                                                .executes(BedrockBreakerCommand::executeWhitelistAdd)))
                                .then(literal("remove")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(argument("player", StringArgumentType.word()).suggests(WHITELISTED_PLAYER_NAMES)
                                                .executes(BedrockBreakerCommand::executeWhitelistRemove)))
                        )
                )
        );
    }

    private static int executeCreate(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        int x1 = IntegerArgumentType.getInteger(ctx, "x1");
        int y1 = IntegerArgumentType.getInteger(ctx, "y1");
        int z1 = IntegerArgumentType.getInteger(ctx, "z1");
        int x2 = IntegerArgumentType.getInteger(ctx, "x2");
        int y2 = IntegerArgumentType.getInteger(ctx, "y2");
        int z2 = IntegerArgumentType.getInteger(ctx, "z2");

        World world = ctx.getSource().getWorld();
        RegistryKey<World> dimension = world.getRegistryKey();

        int minX = Math.min(x1, x2), minY = Math.min(y1, y2), minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2), maxY = Math.max(y1, y2), maxZ = Math.max(z1, z2);

        BaseMachineDefinition definition = new BaseMachineDefinition(name, minX, minY, minZ, maxX, maxY, maxZ, dimension);
        boolean success = BedrockBreakerManager.getInstance().create(definition);
        if (success) {
            ctx.getSource().sendFeedback(() -> Text.literal("Bedrock breaker '" + name + "' created."), true);
        } else {
            ctx.getSource().sendError(Text.literal("A bedrock breaker with name '" + name + "' already exists."));
        }
        return 1;
    }

    private static int executeStart(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        BedrockBreakerManager manager = BedrockBreakerManager.getInstance();
        BaseMachineInstance instance = manager.get(name);
        if (instance == null) {
            ctx.getSource().sendError(Text.literal("No bedrock breaker named '" + name + "'."));
            return 0;
        }
        if (instance.isActive()) {
            ctx.getSource().sendError(Text.literal("Bedrock breaker '" + name + "' is already active."));
            return 0;
        }
        if (!isDeliveryConfigured()) {
            ctx.getSource().sendError(Text.literal("Cannot start: notification delivery not configured. Use /bedrockbreaker settings setWebhookUrl or setBotToken/setGuildId/setChannelId first."));
            return 0;
        }
        manager.start(name);
        ctx.getSource().sendFeedback(() -> Text.literal("Bedrock breaker '" + name + "' started."), true);
        DiscordNotifier.sendStart("BedrockBreaker", name, manager.getConfig().bedrockBreakerSettings.pingSettings);
        return 1;
    }

    private static int executeStop(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        BedrockBreakerManager manager = BedrockBreakerManager.getInstance();
        BaseMachineInstance instance = manager.get(name);
        if (instance == null) {
            ctx.getSource().sendError(Text.literal("No bedrock breaker named '" + name + "'."));
            return 0;
        }
        if (!instance.isActive()) {
            ctx.getSource().sendError(Text.literal("Bedrock breaker '" + name + "' is already inactive."));
            return 0;
        }
        manager.stop(name);
        ctx.getSource().sendFeedback(() -> Text.literal("Bedrock breaker '" + name + "' stopped."), true);
        DiscordNotifier.sendManuallyStopped("BedrockBreaker", name, manager.getConfig().bedrockBreakerSettings.pingSettings);
        return 1;
    }

    private static int executeList(CommandContext<ServerCommandSource> ctx) {
        var instances = BedrockBreakerManager.getInstance().getAll();
        if (instances.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("No bedrock breakers defined."), false);
            return 1;
        }
        for (var inst : instances) {
            String status = inst.isActive() ? "active" : "inactive";
            ctx.getSource().sendFeedback(() -> Text.literal("- " + inst.getDefinition().name() + " (" + status + ")"), false);
        }
        return 1;
    }

    private static int executeDelete(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        if (BedrockBreakerManager.getInstance().delete(name)) {
            ctx.getSource().sendFeedback(() -> Text.literal("Bedrock breaker '" + name + "' deleted."), true);
        } else {
            ctx.getSource().sendError(Text.literal("No bedrock breaker named '" + name + "'."));
        }
        return 1;
    }

    // ---- Settings ----
    private static int executeSettingsShow(CommandContext<ServerCommandSource> ctx) {
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        ctx.getSource().sendFeedback(() -> Text.literal("---------- Bedrock Breaker Settings ----------"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Notification mode: " + config.notificationMode), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Webhook URL: " + maskToken(config.webhookUrl)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Ping Role ID: " + (config.pingRoleId.isBlank() || config.pingRoleId.equals("0") ? "none" : config.pingRoleId)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Bot token: " + maskToken(config.botToken)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Guild ID: " + (config.guildId.isBlank() ? "not set" : config.guildId)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Channel ID: " + (config.channelId.isBlank() ? "not set" : config.channelId)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Stop timeout: " + config.bedrockBreakerSettings.stopTimeoutSeconds + " seconds"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Min blocks broken: " + config.bedrockBreakerSettings.minBlocksBroken), false);
        ctx.getSource().sendFeedback(() -> Text.literal("-----------------------------------------------"), false);
        return 1;
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) return "not set";
        if (token.length() <= 8) return "****";
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private static boolean isValidWebhookUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase();
            return host.equals("discord.com") || host.endsWith(".discord.com")
                    || host.equals("discordapp.com") || host.endsWith(".discordapp.com");
        } catch (Exception e) {
            return false;
        }
    }

    private static int executeSetWebhookUrl(CommandContext<ServerCommandSource> ctx) {
        String url = StringArgumentType.getString(ctx, "url");
        if (!isValidWebhookUrl(url)) {
            ctx.getSource().sendError(Text.literal("Invalid webhook URL. Must be an https://discord.com/api/webhooks/... URL."));
            return 0;
        }
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        config.webhookUrl = url;
        config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Webhook URL updated."), true);
        return 1;
    }

    private static int executeSetPingRoleId(CommandContext<ServerCommandSource> ctx) {
        String roleId = StringArgumentType.getString(ctx, "roleId");
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        config.pingRoleId = roleId;
        config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Ping Role ID updated."), true);
        return 1;
    }

    private static int executeSetBotToken(CommandContext<ServerCommandSource> ctx) {
        String token = StringArgumentType.getString(ctx, "token");
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        config.botToken = token;
        config.save();
        DiscordBotManager.getInstance().restart(token);
        ctx.getSource().sendFeedback(() -> Text.literal("Bot token updated and bot restarted."), true);
        return 1;
    }

    private static int executeSetGuildId(CommandContext<ServerCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        config.guildId = id;
        config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Guild ID updated."), true);
        return 1;
    }

    private static int executeSetChannelId(CommandContext<ServerCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        config.channelId = id;
        config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Channel ID updated."), true);
        return 1;
    }

    private static int executeSetMemberDiscordRole(CommandContext<ServerCommandSource> ctx) {
        String roleId = StringArgumentType.getString(ctx, "roleId");
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        if (isClearKeyword(roleId)) {
            config.memberDiscordRole = "";
            config.save();
            ctx.getSource().sendFeedback(() -> Text.literal("Member Discord role cleared. Only op players can use start/stop/list via Discord now."), true);
            return 1;
        }
        config.memberDiscordRole = roleId;
        config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Member Discord role ID updated."), true);
        return 1;
    }

    private static boolean isClearKeyword(String value) {
        return value.equalsIgnoreCase("none") || value.equalsIgnoreCase("clear") || value.equals("0");
    }

    private static int executeSetNotificationMode(CommandContext<ServerCommandSource> ctx) {
        String mode = StringArgumentType.getString(ctx, "mode");
        if (!"webhook".equals(mode) && !"bot".equals(mode)) {
            ctx.getSource().sendError(Text.literal("Invalid mode. Use 'webhook' or 'bot'."));
            return 0;
        }
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        String prev = config.notificationMode;
        config.notificationMode = mode;
        config.save();

        if ("bot".equals(mode) && !"bot".equals(prev)) {
            if (!config.botToken.isBlank()) {
                DiscordBotManager.getInstance().start(config.botToken);
            } else {
                ctx.getSource().sendFeedback(() -> Text.literal("Switched to bot mode, but no bot token is set. Use setBotToken to configure."), true);
            }
        } else if ("webhook".equals(mode) && !"webhook".equals(prev)) {
            DiscordBotManager.getInstance().stop();
        }
        syncCommandTree(ctx);
        ctx.getSource().sendFeedback(() -> Text.literal("Notification mode set to '" + mode + "'."), true);
        return 1;
    }

    private static void syncCommandTree(CommandContext<ServerCommandSource> ctx) {
        var server = ctx.getSource().getServer();
        if (server == null) return;
        var root = server.getCommandManager().getDispatcher().getRoot();
        CommandNodeInspector<ServerCommandSource> inspector = new CommandNodeInspector<>() {
            public Identifier getSuggestionProviderId(ArgumentCommandNode<ServerCommandSource, ?> node) { return null; }
            public boolean isExecutable(CommandNode<ServerCommandSource> node) { return true; }
            public boolean hasRequiredLevel(CommandNode<ServerCommandSource> node) { return true; }
        };
        var packet = new CommandTreeS2CPacket(root, inspector);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }

    private static int executeSetStopTimeout(CommandContext<ServerCommandSource> ctx) {
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        config.bedrockBreakerSettings.stopTimeoutSeconds = seconds;
        config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Stop timeout set to " + seconds + " seconds."), true);
        return 1;
    }

    private static int executeSetMinBlocksBroken(CommandContext<ServerCommandSource> ctx) {
        int count = IntegerArgumentType.getInteger(ctx, "count");
        ModConfig config = BedrockBreakerManager.getInstance().getConfig();
        config.bedrockBreakerSettings.minBlocksBroken = count;
        config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Minimum blocks broken per check set to " + count + "."), true);
        return 1;
    }

    // ---- Discord Pings ----
    private static int executePingShow(CommandContext<ServerCommandSource> ctx) {
        var pings = BedrockBreakerManager.getInstance().getConfig().bedrockBreakerSettings.pingSettings;
        ctx.getSource().sendFeedback(() -> Text.literal("--------- Bedrock Breaker Discord Pings ---------"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Global enabled: " + pings.enabled), false);
        ctx.getSource().sendFeedback(() -> Text.literal("On start: " + pings.onStart), false);
        ctx.getSource().sendFeedback(() -> Text.literal("On stop (manual): " + pings.onStop), false);
        ctx.getSource().sendFeedback(() -> Text.literal("On stuck: " + pings.onStuck), false);
        ctx.getSource().sendFeedback(() -> Text.literal("On resumed: " + pings.onResumed), false);
        ctx.getSource().sendFeedback(() -> Text.literal("On server shutdown: " + pings.onShutdown), false);
        ctx.getSource().sendFeedback(() -> Text.literal("--------------------------------------------------"), false);
        return 1;
    }

    private static void savePingSettings() {
        BedrockBreakerManager.getInstance().getConfig().save();
    }

    private static int executePingEnable(CommandContext<ServerCommandSource> ctx) {
        boolean val = BoolArgumentType.getBool(ctx, "enabled");
        BedrockBreakerManager.getInstance().getConfig().bedrockBreakerSettings.pingSettings.enabled = val;
        savePingSettings();
        ctx.getSource().sendFeedback(() -> Text.literal("Global ping " + (val ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int executePingOnStart(CommandContext<ServerCommandSource> ctx) {
        boolean val = BoolArgumentType.getBool(ctx, "enabled");
        BedrockBreakerManager.getInstance().getConfig().bedrockBreakerSettings.pingSettings.onStart = val;
        savePingSettings();
        ctx.getSource().sendFeedback(() -> Text.literal("Start ping " + (val ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int executePingOnStop(CommandContext<ServerCommandSource> ctx) {
        boolean val = BoolArgumentType.getBool(ctx, "enabled");
        BedrockBreakerManager.getInstance().getConfig().bedrockBreakerSettings.pingSettings.onStop = val;
        savePingSettings();
        ctx.getSource().sendFeedback(() -> Text.literal("Stop ping " + (val ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int executePingOnStuck(CommandContext<ServerCommandSource> ctx) {
        boolean val = BoolArgumentType.getBool(ctx, "enabled");
        BedrockBreakerManager.getInstance().getConfig().bedrockBreakerSettings.pingSettings.onStuck = val;
        savePingSettings();
        ctx.getSource().sendFeedback(() -> Text.literal("Stuck ping " + (val ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int executePingOnResumed(CommandContext<ServerCommandSource> ctx) {
        boolean val = BoolArgumentType.getBool(ctx, "enabled");
        BedrockBreakerManager.getInstance().getConfig().bedrockBreakerSettings.pingSettings.onResumed = val;
        savePingSettings();
        ctx.getSource().sendFeedback(() -> Text.literal("Resumed ping " + (val ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int executePingOnShutdown(CommandContext<ServerCommandSource> ctx) {
        boolean val = BoolArgumentType.getBool(ctx, "enabled");
        BedrockBreakerManager.getInstance().getConfig().bedrockBreakerSettings.pingSettings.onShutdown = val;
        savePingSettings();
        ctx.getSource().sendFeedback(() -> Text.literal("Shutdown ping " + (val ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    // ---- Whitelist ----
    private static int executeWhitelistList(CommandContext<ServerCommandSource> ctx) {
        String[] names = PermissionManager.getWhitelist();
        if (names.length == 0) {
            ctx.getSource().sendFeedback(() -> Text.literal("Whitelist is empty. Only op players can use the commands."), false);
            return 1;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("---------- Whitelist ----------"), false);
        for (String name : names) {
            ctx.getSource().sendFeedback(() -> Text.literal("- " + name), false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("--------------------------------"), false);
        return 1;
    }

    private static int executeWhitelistAdd(CommandContext<ServerCommandSource> ctx) {
        String player = StringArgumentType.getString(ctx, "player");
        if (PermissionManager.addToWhitelist(player)) {
            ctx.getSource().sendFeedback(() -> Text.literal("'" + player + "' added to the whitelist."), true);
        } else {
            ctx.getSource().sendError(Text.literal("'" + player + "' is already on the whitelist."));
        }
        return 1;
    }

    private static int executeWhitelistRemove(CommandContext<ServerCommandSource> ctx) {
        String player = StringArgumentType.getString(ctx, "player");
        if (PermissionManager.removeFromWhitelist(player)) {
            ctx.getSource().sendFeedback(() -> Text.literal("'" + player + "' removed from the whitelist."), true);
        } else {
            ctx.getSource().sendError(Text.literal("'" + player + "' is not on the whitelist."));
        }
        return 1;
    }
}
