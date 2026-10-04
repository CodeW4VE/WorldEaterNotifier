package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.bot.DiscordBotManager;
import com.example.worldeaternotifier.config.ModConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.net.URI;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket.NodeInspector;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.Level;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class MachineCommand {

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYER_NAMES = (context, builder) -> {
        CommandSourceStack source = context.getSource();
        if (source.getServer() == null) return Suggestions.empty();
        String[] names = source.getServer().getPlayerList().getPlayers().stream()
                .map(p -> p.getGameProfile().name())
                .toArray(String[]::new);
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> WHITELISTED_PLAYER_NAMES = (context, builder) ->
            SharedSuggestionProvider.suggest(PermissionManager.getWhitelist(), builder);

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"webhook", "bot"}, builder);

    private static final SuggestionProvider<CommandSourceStack> DETECTION_TYPE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(new String[]{"quarry-like", "2-way"}, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_X = (ctx, builder) -> suggestCoord(ctx, builder, p -> p.blockPosition().getX());
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_Y = (ctx, builder) -> suggestCoord(ctx, builder, p -> p.blockPosition().getY());
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_Z = (ctx, builder) -> suggestCoord(ctx, builder, p -> p.blockPosition().getZ());

    private static java.util.concurrent.CompletableFuture<Suggestions> suggestCoord(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            java.util.function.ToIntFunction<ServerPlayer> coord) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return Suggestions.empty();
        builder.suggest(String.valueOf(coord.applyAsInt(p)));
        return builder.buildFuture();
    }

    private final String commandName;      // "worldeater" / "trencher" / "bedrockbreaker"
    private final String displayName;      // "world eater" / "trencher" / "bedrock breaker"
    private final MachineManager manager;
    private final boolean hasDetectionTypeArg;
    private final boolean hasMinTntCount;
    private final boolean hasMinBlocksBroken;

    private final SuggestionProvider<CommandSourceStack> machineNames;

    public MachineCommand(String machineType, String displayName, MachineManager manager,
                           boolean hasDetectionTypeArg, boolean hasMinTntCount, boolean hasMinBlocksBroken) {
        this.commandName = machineType.toLowerCase();
        this.displayName = displayName;
        this.manager = manager;
        this.hasDetectionTypeArg = hasDetectionTypeArg;
        this.hasMinTntCount = hasMinTntCount;
        this.hasMinBlocksBroken = hasMinBlocksBroken;
        this.machineNames = (context, builder) -> SharedSuggestionProvider.suggest(manager.getAllNames(), builder);
    }

    private static String cap(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    private static String titleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String word : s.split(" ")) sb.append(cap(word)).append(' ');
        return sb.substring(0, sb.length() - 1);
    }

    private boolean isWebhookMode() {
        ModConfig config = manager.getConfig();
        return config != null && "webhook".equals(config.notificationMode);
    }

    private boolean isBotMode() {
        ModConfig config = manager.getConfig();
        return config != null && "bot".equals(config.notificationMode);
    }

    private boolean isDeliveryConfigured() {
        ModConfig config = manager.getConfig();
        if (config == null) return false;
        if ("webhook".equals(config.notificationMode)) {
            return !config.webhookUrl.isBlank();
        } else if ("bot".equals(config.notificationMode)) {
            return !config.botToken.isBlank() && !config.guildId.isBlank() && !config.channelId.isBlank();
        }
        return false;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher,
                          CommandBuildContext registryAccess,
                          Commands.CommandSelection environment) {
        var root = literal(commandName)
                .requires(PermissionManager::canUseCommands)
                .then(buildCreateNode())
                .then(literal("start")
                        .then(argument("name", StringArgumentType.word()).suggests(machineNames)
                                .executes(this::executeStart)))
                .then(literal("stop")
                        .then(argument("name", StringArgumentType.word()).suggests(machineNames)
                                .executes(this::executeStop)))
                .then(literal("list")
                        .executes(this::executeList))
                .then(literal("delete")
                        .then(argument("name", StringArgumentType.word()).suggests(machineNames)
                                .executes(this::executeDelete)))
                .then(buildSettingsNode());
        dispatcher.register(root);
    }

    private ArgumentBuilder<CommandSourceStack, ?> buildCreateNode() {
        if (hasDetectionTypeArg) {
            return literal("create")
                    .then(argument("name", StringArgumentType.word())
                            .then(argument("type", StringArgumentType.word()).suggests(DETECTION_TYPE_SUGGESTIONS)
                                    .then(coordArgs(this::executeCreate))));
        }
        return literal("create")
                .then(argument("name", StringArgumentType.word())
                        .then(coordArgs(this::executeCreate)));
    }

    private ArgumentBuilder<CommandSourceStack, ?> coordArgs(Command<CommandSourceStack> exec) {
        return argument("x1", IntegerArgumentType.integer()).suggests(SUGGEST_X)
                .then(argument("y1", IntegerArgumentType.integer()).suggests(SUGGEST_Y)
                        .then(argument("z1", IntegerArgumentType.integer()).suggests(SUGGEST_Z)
                                .then(argument("x2", IntegerArgumentType.integer()).suggests(SUGGEST_X)
                                        .then(argument("y2", IntegerArgumentType.integer()).suggests(SUGGEST_Y)
                                                .then(argument("z2", IntegerArgumentType.integer()).suggests(SUGGEST_Z)
                                                        .executes(exec))))));
    }

    private ArgumentBuilder<CommandSourceStack, ?> buildSettingsNode() {
        var settings = literal("settings")
                .then(literal("show").executes(this::executeSettingsShow))
                .then(literal("setWebhookUrl")
                        .requires(s -> isWebhookMode() && PermissionManager.isOp(s))
                        .then(argument("url", StringArgumentType.greedyString())
                                .executes(this::executeSetWebhookUrl)))
                .then(literal("setPingRoleId")
                        .requires(PermissionManager::isOp)
                        .then(argument("roleId", StringArgumentType.word())
                                .executes(this::executeSetPingRoleId)))
                .then(literal("setBotToken")
                        .requires(s -> isBotMode() && PermissionManager.isOp(s))
                        .then(argument("token", StringArgumentType.greedyString())
                                .executes(this::executeSetBotToken)))
                .then(literal("setGuildId")
                        .requires(s -> isBotMode() && PermissionManager.isOp(s))
                        .then(argument("id", StringArgumentType.word())
                                .executes(this::executeSetGuildId)))
                .then(literal("setChannelId")
                        .requires(s -> isBotMode() && PermissionManager.isOp(s))
                        .then(argument("id", StringArgumentType.word())
                                .executes(this::executeSetChannelId)))
                .then(literal("setMemberDiscordRole")
                        .requires(s -> isBotMode() && PermissionManager.isOp(s))
                        .then(argument("roleId", StringArgumentType.word())
                                .executes(this::executeSetMemberDiscordRole)))
                .then(literal("setNotificationMode")
                        .requires(PermissionManager::isOp)
                        .then(argument("mode", StringArgumentType.word()).suggests(MODE_SUGGESTIONS)
                                .executes(this::executeSetNotificationMode)))
                .then(literal("setStopTimeout")
                        .then(argument("seconds", IntegerArgumentType.integer(1))
                                .executes(this::executeSetStopTimeout)));
        if (hasMinBlocksBroken) {
            settings.then(literal("setMinBlocksBroken")
                    .then(argument("count", IntegerArgumentType.integer(0))
                            .executes(this::executeSetMinBlocksBroken)));
        }
        if (hasMinTntCount) {
            settings.then(literal("setMinTntCount")
                    .then(argument("count", IntegerArgumentType.integer(1))
                            .executes(this::executeSetMinTntCount)));
        }
        settings
                .then(literal("showSubscriptionButton")
                        .requires(s -> isBotMode() && PermissionManager.isOp(s))
                        .then(argument("value", BoolArgumentType.bool())
                                .executes(this::executeShowSubscriptionButton)))
                .then(literal("discordPings")
                        .then(literal("show").executes(this::executePingShow))
                        .then(literal("enable")
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> executePingToggle(ctx, (p, v) -> p.enabled = v, "Global ping"))))
                        .then(literal("onStart")
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> executePingToggle(ctx, (p, v) -> p.onStart = v, "Start ping"))))
                        .then(literal("onStop")
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> executePingToggle(ctx, (p, v) -> p.onStop = v, "Stop ping"))))
                        .then(literal("onStuck")
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> executePingToggle(ctx, (p, v) -> p.onStuck = v, "Stuck ping"))))
                        .then(literal("onResumed")
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> executePingToggle(ctx, (p, v) -> p.onResumed = v, "Resumed ping"))))
                        .then(literal("onShutdown")
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> executePingToggle(ctx, (p, v) -> p.onShutdown = v, "Shutdown ping"))))
                )
                .then(literal("whitelist")
                        .then(literal("list")
                                .executes(this::executeWhitelistList))
                        .then(literal("add")
                                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                .then(argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYER_NAMES)
                                        .executes(this::executeWhitelistAdd)))
                        .then(literal("remove")
                                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                                .then(argument("player", StringArgumentType.word()).suggests(WHITELISTED_PLAYER_NAMES)
                                        .executes(this::executeWhitelistRemove)))
                );
        return settings;
    }

    private int executeCreate(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        String detectionType = "quarry-like";
        if (hasDetectionTypeArg) {
            detectionType = StringArgumentType.getString(ctx, "type");
            if (!"quarry-like".equals(detectionType) && !"2-way".equals(detectionType)) {
                ctx.getSource().sendFailure(Component.literal("Invalid " + displayName + " type. Use 'quarry-like' or '2-way'."));
                return 0;
            }
        }
        int x1 = IntegerArgumentType.getInteger(ctx, "x1");
        int y1 = IntegerArgumentType.getInteger(ctx, "y1");
        int z1 = IntegerArgumentType.getInteger(ctx, "z1");
        int x2 = IntegerArgumentType.getInteger(ctx, "x2");
        int y2 = IntegerArgumentType.getInteger(ctx, "y2");
        int z2 = IntegerArgumentType.getInteger(ctx, "z2");

        Level world = ctx.getSource().getLevel();
        ResourceKey<Level> dimension = world.dimension();

        int minX = Math.min(x1, x2), minY = Math.min(y1, y2), minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2), maxY = Math.max(y1, y2), maxZ = Math.max(z1, z2);

        BaseMachineDefinition definition = new BaseMachineDefinition(name, minX, minY, minZ, maxX, maxY, maxZ, dimension);
        boolean success = manager.create(definition, detectionType);
        String suffix = hasDetectionTypeArg ? " (" + detectionType + ")" : "";
        if (success) {
            ctx.getSource().sendSuccess(() -> Component.literal(cap(displayName) + " '" + name + "'" + suffix + " created."), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("A " + displayName + " with name '" + name + "' already exists."));
        }
        return 1;
    }

    private int executeStart(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        BaseMachineInstance instance = manager.get(name);
        if (instance == null) {
            ctx.getSource().sendFailure(Component.literal("No " + displayName + " named '" + name + "'."));
            return 0;
        }
        if (instance.isActive()) {
            ctx.getSource().sendFailure(Component.literal(cap(displayName) + " '" + name + "' is already active."));
            return 0;
        }
        if (!isDeliveryConfigured()) {
            ctx.getSource().sendFailure(Component.literal("Cannot start: notification delivery not configured. Use /" + commandName
                    + " settings setWebhookUrl or setBotToken/setGuildId/setChannelId first."));
            return 0;
        }
        manager.start(name);
        ctx.getSource().sendSuccess(() -> Component.literal(cap(displayName) + " '" + name + "' started."), true);
        DiscordNotifier.sendStart(manager.getMachineType(), name, manager.getSettings().pingSettings);
        return 1;
    }

    private int executeStop(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        BaseMachineInstance instance = manager.get(name);
        if (instance == null) {
            ctx.getSource().sendFailure(Component.literal("No " + displayName + " named '" + name + "'."));
            return 0;
        }
        if (!instance.isActive()) {
            ctx.getSource().sendFailure(Component.literal(cap(displayName) + " '" + name + "' is already inactive."));
            return 0;
        }
        manager.stop(name);
        ctx.getSource().sendSuccess(() -> Component.literal(cap(displayName) + " '" + name + "' stopped."), true);
        DiscordNotifier.sendManuallyStopped(manager.getMachineType(), name, manager.getSettings().pingSettings);
        return 1;
    }

    private int executeList(CommandContext<CommandSourceStack> ctx) {
        var instances = manager.getAll();
        if (instances.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No " + displayName + "s defined."), false);
            return 1;
        }
        for (var inst : instances) {
            String status = inst.isActive() ? "active" : "inactive";
            ctx.getSource().sendSuccess(() -> Component.literal("- " + inst.getDefinition().name() + " (" + status + ")"), false);
        }
        return 1;
    }

    private int executeDelete(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        if (manager.delete(name)) {
            ctx.getSource().sendSuccess(() -> Component.literal(cap(displayName) + " '" + name + "' deleted."), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("No " + displayName + " named '" + name + "'."));
        }
        return 1;
    }

    // ---- Settings ----
    private int executeSettingsShow(CommandContext<CommandSourceStack> ctx) {
        ModConfig config = manager.getConfig();
        ModConfig.MachineSettings settings = manager.getSettings();
        String title = titleCase(displayName) + " Settings";
        String header = "---------- " + title + " ----------";
        ctx.getSource().sendSuccess(() -> Component.literal(header), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Notification mode: " + config.notificationMode), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Webhook URL: " + maskToken(config.webhookUrl)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Ping Role ID: " + (config.pingRoleId.isBlank() || config.pingRoleId.equals("0") ? "none" : config.pingRoleId)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Bot token: " + maskToken(config.botToken)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Guild ID: " + (config.guildId.isBlank() ? "not set" : config.guildId)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Channel ID: " + (config.channelId.isBlank() ? "not set" : config.channelId)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Stop timeout: " + settings.stopTimeoutSeconds + " seconds"), false);
        if (hasMinBlocksBroken) {
            ctx.getSource().sendSuccess(() -> Component.literal("Min blocks broken: " + settings.minBlocksBroken), false);
        }
        if (hasMinTntCount) {
            String tntLabel = hasMinBlocksBroken ? "Min TNT count (2-way): " : "Min TNT count: ";
            ctx.getSource().sendSuccess(() -> Component.literal(tntLabel + settings.minTntCount), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("-".repeat(header.length())), false);
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

    private int executeSetWebhookUrl(CommandContext<CommandSourceStack> ctx) {
        String url = StringArgumentType.getString(ctx, "url");
        if (!isValidWebhookUrl(url)) {
            ctx.getSource().sendFailure(Component.literal("Invalid webhook URL. Must be an https://discord.com/api/webhooks/... URL."));
            return 0;
        }
        ModConfig config = manager.getConfig();
        config.webhookUrl = url;
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Webhook URL updated."), true);
        return 1;
    }

    private int executeSetPingRoleId(CommandContext<CommandSourceStack> ctx) {
        String roleId = StringArgumentType.getString(ctx, "roleId");
        ModConfig config = manager.getConfig();
        config.pingRoleId = roleId;
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Ping Role ID updated."), true);
        return 1;
    }

    private int executeSetBotToken(CommandContext<CommandSourceStack> ctx) {
        String token = StringArgumentType.getString(ctx, "token");
        ModConfig config = manager.getConfig();
        config.botToken = token;
        config.save();
        DiscordBotManager.getInstance().restart(token);
        ctx.getSource().sendSuccess(() -> Component.literal("Bot token updated and bot restarted."), true);
        return 1;
    }

    private int executeSetGuildId(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        ModConfig config = manager.getConfig();
        config.guildId = id;
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Guild ID updated."), true);
        return 1;
    }

    private int executeSetChannelId(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        ModConfig config = manager.getConfig();
        config.channelId = id;
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Channel ID updated."), true);
        return 1;
    }

    private int executeSetMemberDiscordRole(CommandContext<CommandSourceStack> ctx) {
        String roleId = StringArgumentType.getString(ctx, "roleId");
        ModConfig config = manager.getConfig();
        if (isClearKeyword(roleId)) {
            config.memberDiscordRole = "";
            config.save();
            ctx.getSource().sendSuccess(() -> Component.literal("Member Discord role cleared. Only op players can use start/stop/list via Discord now."), true);
            return 1;
        }
        config.memberDiscordRole = roleId;
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Member Discord role ID updated."), true);
        return 1;
    }

    private static boolean isClearKeyword(String value) {
        return value.equalsIgnoreCase("none") || value.equalsIgnoreCase("clear") || value.equals("0");
    }

    private int executeSetNotificationMode(CommandContext<CommandSourceStack> ctx) {
        String mode = StringArgumentType.getString(ctx, "mode");
        if (!"webhook".equals(mode) && !"bot".equals(mode)) {
            ctx.getSource().sendFailure(Component.literal("Invalid mode. Use 'webhook' or 'bot'."));
            return 0;
        }
        ModConfig config = manager.getConfig();
        String prev = config.notificationMode;
        config.notificationMode = mode;
        config.save();

        if ("bot".equals(mode) && !"bot".equals(prev)) {
            if (!config.botToken.isBlank()) {
                DiscordBotManager.getInstance().start(config.botToken);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("Switched to bot mode, but no bot token is set. Use setBotToken to configure."), true);
            }
        } else if ("webhook".equals(mode) && !"webhook".equals(prev)) {
            DiscordBotManager.getInstance().stop();
        }
        syncCommandTree(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Notification mode set to '" + mode + "'."), true);
        return 1;
    }

    private static void syncCommandTree(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        if (server == null) return;
        var root = server.getCommands().getDispatcher().getRoot();
        NodeInspector<CommandSourceStack> inspector = new NodeInspector<>() {
            public Identifier suggestionId(ArgumentCommandNode<CommandSourceStack, ?> node) { return null; }
            public boolean isExecutable(CommandNode<CommandSourceStack> node) { return true; }
            public boolean isRestricted(CommandNode<CommandSourceStack> node) { return true; }
        };
        var packet = new ClientboundCommandsPacket(root, inspector);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }

    private int executeSetStopTimeout(CommandContext<CommandSourceStack> ctx) {
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        manager.getSettings().stopTimeoutSeconds = seconds;
        manager.getConfig().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Stop timeout set to " + seconds + " seconds."), true);
        return 1;
    }

    private int executeSetMinTntCount(CommandContext<CommandSourceStack> ctx) {
        int count = IntegerArgumentType.getInteger(ctx, "count");
        manager.getSettings().minTntCount = count;
        manager.getConfig().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Minimum TNT count set to " + count + "."), true);
        return 1;
    }

    private int executeSetMinBlocksBroken(CommandContext<CommandSourceStack> ctx) {
        int count = IntegerArgumentType.getInteger(ctx, "count");
        manager.getSettings().minBlocksBroken = count;
        manager.getConfig().save();
        ctx.getSource().sendSuccess(() -> Component.literal("Minimum blocks broken per check set to " + count + "."), true);
        return 1;
    }

    private int executeShowSubscriptionButton(CommandContext<CommandSourceStack> ctx) {
        boolean val = BoolArgumentType.getBool(ctx, "value");
        ModConfig config = manager.getConfig();
        config.showSubscriptionButton = val;
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Subscription button " + (val ? "shown" : "hidden") + " on start messages."), true);
        return 1;
    }

    // ---- Discord Pings ----
    private int executePingShow(CommandContext<CommandSourceStack> ctx) {
        var pings = manager.getSettings().pingSettings;
        String title = titleCase(displayName) + " Discord Pings";
        String header = "--------- " + title + " ---------";
        ctx.getSource().sendSuccess(() -> Component.literal(header), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Global enabled: " + pings.enabled), false);
        ctx.getSource().sendSuccess(() -> Component.literal("On start: " + pings.onStart), false);
        ctx.getSource().sendSuccess(() -> Component.literal("On stop (manual): " + pings.onStop), false);
        ctx.getSource().sendSuccess(() -> Component.literal("On stuck: " + pings.onStuck), false);
        ctx.getSource().sendSuccess(() -> Component.literal("On resumed: " + pings.onResumed), false);
        ctx.getSource().sendSuccess(() -> Component.literal("On server shutdown: " + pings.onShutdown), false);
        ctx.getSource().sendSuccess(() -> Component.literal("-".repeat(header.length())), false);
        return 1;
    }

    private int executePingToggle(CommandContext<CommandSourceStack> ctx,
                                   java.util.function.BiConsumer<ModConfig.PingSettings, Boolean> setter, String label) {
        boolean val = BoolArgumentType.getBool(ctx, "enabled");
        setter.accept(manager.getSettings().pingSettings, val);
        manager.getConfig().save();
        ctx.getSource().sendSuccess(() -> Component.literal(label + " " + (val ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    // ---- Whitelist ----
    private int executeWhitelistList(CommandContext<CommandSourceStack> ctx) {
        String[] names = PermissionManager.getWhitelist();
        if (names.length == 0) {
            ctx.getSource().sendSuccess(() -> Component.literal("Whitelist is empty. Only op players can use the commands."), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("---------- Whitelist ----------"), false);
        for (String name : names) {
            ctx.getSource().sendSuccess(() -> Component.literal("- " + name), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("--------------------------------"), false);
        return 1;
    }

    private int executeWhitelistAdd(CommandContext<CommandSourceStack> ctx) {
        String player = StringArgumentType.getString(ctx, "player");
        if (PermissionManager.addToWhitelist(player)) {
            ctx.getSource().sendSuccess(() -> Component.literal("'" + player + "' added to the whitelist."), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("'" + player + "' is already on the whitelist."));
        }
        return 1;
    }

    private int executeWhitelistRemove(CommandContext<CommandSourceStack> ctx) {
        String player = StringArgumentType.getString(ctx, "player");
        if (PermissionManager.removeFromWhitelist(player)) {
            ctx.getSource().sendSuccess(() -> Component.literal("'" + player + "' removed from the whitelist."), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("'" + player + "' is not on the whitelist."));
        }
        return 1;
    }
}
