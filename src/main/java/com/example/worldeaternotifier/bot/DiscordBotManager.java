package com.example.worldeaternotifier.bot;

import com.example.worldeaternotifier.WorldEaterNotifierMod;
import com.example.worldeaternotifier.bedrockbreaker.BedrockBreakerManager;
import com.example.worldeaternotifier.common.BaseMachineInstance;
import com.example.worldeaternotifier.common.DiscordNotifier;
import com.example.worldeaternotifier.config.ModConfig;
import com.example.worldeaternotifier.trencher.TrencherManager;
import com.example.worldeaternotifier.worldeater.WorldEaterManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class DiscordBotManager {
    private static final DiscordBotManager INSTANCE = new DiscordBotManager();
    private static final int MAX_PENDING = 50;

    private JDA jda;
    private final List<Runnable> pendingMessages = new ArrayList<>();

    private DiscordBotManager() {}

    public static DiscordBotManager getInstance() { return INSTANCE; }

    public boolean isRunning() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    public void start(String token) {
        if (jda != null) stop();
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(new BotListener())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            jda = null;
        }
    }

    public void stop() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
        pendingMessages.clear();
    }

    public void restart(String token) {
        stop();
        start(token);
    }

    public void sendNotification(String channelId, String content, String roleId,
                                  boolean shouldMention, String machineType, String machineName, boolean withButton) {
        if (jda == null) return;
        if (jda.getStatus() != JDA.Status.CONNECTED) {
            synchronized (pendingMessages) {
                if (pendingMessages.size() < MAX_PENDING) {
                    pendingMessages.add(() -> doSend(channelId, content, roleId, shouldMention, machineType, machineName, withButton));
                }
            }
            return;
        }
        doSend(channelId, content, roleId, shouldMention, machineType, machineName, withButton);
    }

    private void doSend(String channelId, String content, String roleId,
                         boolean shouldMention, String machineType, String machineName, boolean withButton) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        String mention = (shouldMention && roleId != null && !roleId.isBlank() && !roleId.equals("0"))
                ? "<@&" + roleId + "> " : "";
        String message = mention + content;

        if (withButton) {
            Button toggle = Button.secondary("wen:toggle:" + machineType + ":" + machineName, "\uD83D\uDD14 Toggle Ping");
            channel.sendMessage(message).setAllowedMentions(EnumSet.of(Message.MentionType.ROLE)).setActionRow(toggle).queue();
        } else {
            channel.sendMessage(message).setAllowedMentions(EnumSet.of(Message.MentionType.ROLE)).queue();
        }
    }

    private void flushPending() {
        List<Runnable> batch;
        synchronized (pendingMessages) {
            batch = new ArrayList<>(pendingMessages);
            pendingMessages.clear();
        }
        for (Runnable msg : batch) {
            try {
                msg.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static ModConfig config() {
        return WorldEaterManager.getInstance().getConfig();
    }

    private static BaseMachineInstance resolveInstance(String type, String name) {
        return switch (type) {
            case "WorldEater" -> WorldEaterManager.getInstance().get(name);
            case "Trencher" -> TrencherManager.getInstance().get(name);
            case "BedrockBreaker" -> BedrockBreakerManager.getInstance().get(name);
            default -> null;
        };
    }

    private static Collection<BaseMachineInstance> allMachines(String typeFilter) {
        List<BaseMachineInstance> all = new ArrayList<>();
        if (typeFilter == null || "WorldEater".equals(typeFilter)) all.addAll(WorldEaterManager.getInstance().getAll());
        if (typeFilter == null || "Trencher".equals(typeFilter)) all.addAll(TrencherManager.getInstance().getAll());
        if (typeFilter == null || "BedrockBreaker".equals(typeFilter)) all.addAll(BedrockBreakerManager.getInstance().getAll());
        return all;
    }

    private static void broadcastMinecraft(String msg) {
        MinecraftServer server = WorldEaterNotifierMod.SERVER;
        if (server != null) server.getPlayerManager().broadcast(Text.literal(msg), false);
    }

    private static boolean hasAccess(Member member) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) return true;
        ModConfig cfg = config();
        if (cfg == null) return false;
        String roleId = cfg.memberDiscordRole;
        if (roleId == null || roleId.isBlank()) return false;
        return member.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
    }

    private class BotListener extends ListenerAdapter {
        @Override
        public void onReady(ReadyEvent event) {
            flushPending();
            registerSlashCommands();
        }

        @Override
        public void onRoleDelete(RoleDeleteEvent event) {
            ModConfig cfg = config();
            if (cfg == null) return;
            String deletedId = event.getRole().getId();
            String deletedName = event.getRole().getName();
            boolean changed = false;

            if (deletedId.equals(cfg.memberDiscordRole)) {
                cfg.memberDiscordRole = "";
                changed = true;
                broadcastMinecraft("⚠ Discord role '" + deletedName + "' (member access) was deleted. "
                        + "Member-only start/stop/list access has been cleared; only admins can use it via Discord until a new role is set.");
            }
            if (deletedId.equals(cfg.pingRoleId)) {
                cfg.pingRoleId = "";
                changed = true;
                broadcastMinecraft("⚠ Discord role '" + deletedName + "' (ping role) was deleted. Ping role has been cleared.");
            }

            if (changed) cfg.save();
        }

        private void registerSlashCommands() {
            ModConfig cfg = config();
            if (cfg == null || cfg.guildId == null || cfg.guildId.isBlank()) return;
            Guild guild = jda.getGuildById(cfg.guildId);
            if (guild == null) return;

            guild.updateCommands()
                    .addCommands(
                            Commands.slash("config", "Configure bot settings")
                                    .addSubcommands(
                                            new SubcommandData("subscription-button", "Show or hide the subscribe button on start messages")
                                                    .addOption(OptionType.BOOLEAN, "value", "true = show, false = hide", true),
                                            new SubcommandData("ping-role", "Set the role used for pings")
                                                    .addOption(OptionType.ROLE, "role", "The role to ping", true),
                                            new SubcommandData("channel", "Set the channel for notifications")
                                                    .addOption(OptionType.CHANNEL, "channel", "The notification channel", true),
                                            new SubcommandData("pings", "Configure ping settings for a machine type"),
                                            new SubcommandData("member-discord-role", "Set (or omit to clear) the role for start/stop/list access")
                                                    .addOption(OptionType.ROLE, "role", "The Discord role (omit to clear)", false)
                                    ),
                            Commands.slash("worldeater", "Manage world eaters")
                                    .addSubcommands(
                                            new SubcommandData("start", "Start a world eater")
                                                    .addOption(OptionType.STRING, "name", "Name of the world eater", true, true),
                                            new SubcommandData("stop", "Stop a world eater")
                                                    .addOption(OptionType.STRING, "name", "Name of the world eater", true, true),
                                            new SubcommandData("list", "List world eaters")
                                    ),
                            Commands.slash("trencher", "Manage trenchers")
                                    .addSubcommands(
                                            new SubcommandData("start", "Start a trencher")
                                                    .addOption(OptionType.STRING, "name", "Name of the trencher", true, true),
                                            new SubcommandData("stop", "Stop a trencher")
                                                    .addOption(OptionType.STRING, "name", "Name of the trencher", true, true),
                                            new SubcommandData("list", "List trenchers")
                                    ),
                            Commands.slash("bedrockbreaker", "Manage bedrock breakers")
                                    .addSubcommands(
                                            new SubcommandData("start", "Start a bedrock breaker")
                                                    .addOption(OptionType.STRING, "name", "Name of the bedrock breaker", true, true),
                                            new SubcommandData("stop", "Stop a bedrock breaker")
                                                    .addOption(OptionType.STRING, "name", "Name of the bedrock breaker", true, true),
                                            new SubcommandData("list", "List bedrock breakers")
                                    )
                    )
                    .queue();
        }

        @Override
        public void onButtonInteraction(ButtonInteractionEvent event) {
            String id = event.getComponentId();
            if (!id.startsWith("wen:")) return;

            String[] parts = id.split(":", 4);
            if (parts.length < 4) return;

            String action = parts[1];
            String machineType = parts[2];
            String machineName = parts[3];

            ModConfig cfg = config();
            if (cfg == null) return;

            if ("pings".equals(action) && "val".equals(machineType)) {
                handlePingValueButton(event, id);
                return;
            }

            String roleId = cfg.pingRoleId;
            if (roleId == null || roleId.isBlank() || roleId.equals("0")) {
                event.reply("No ping role configured.").setEphemeral(true).queue();
                return;
            }

            Role role = event.getGuild().getRoleById(roleId);
            if (role == null) {
                event.reply("Ping role not found on this server.").setEphemeral(true).queue();
                return;
            }

            var member = event.getMember();
            if (member == null) return;

            if (!"toggle".equals(action)) return;

            boolean hasRole = member.getRoles().contains(role);
            try {
                if (hasRole) {
                    event.getGuild().removeRoleFromMember(member, role).queue(
                            v -> event.reply("You won't get pinged for " + machineType + " events.").setEphemeral(true).queue(),
                            e -> event.reply("\u274C Failed to remove role.").setEphemeral(true).queue()
                    );
                } else {
                    event.getGuild().addRoleToMember(member, role).queue(
                            v -> event.reply("You'll now get pinged for " + machineType + " events!").setEphemeral(true).queue(),
                            e -> event.reply("\u274C Failed to assign role.").setEphemeral(true).queue()
                    );
                }
            } catch (HierarchyException e) {
                event.reply("\u274C Bot's role must be above the ping role in server settings. Move it higher in Roles tab.").setEphemeral(true).queue();
            }
        }

        private void handlePingValueButton(ButtonInteractionEvent event, String id) {
            Member member = event.getMember();
            if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("❌ You need Administrator permission.").setEphemeral(true).queue();
                return;
            }
            String[] parts = id.split(":", 6);
            if (parts.length < 6) return;
            String type = parts[3];
            String setting = parts[4];
            boolean value = Boolean.parseBoolean(parts[5]);

            ModConfig cfg = config();
            if (cfg == null) { event.reply("Config not loaded.").setEphemeral(true).queue(); return; }

            ModConfig.PingSettings ps = switch (type) {
                case "WorldEater" -> cfg.worldEaterSettings.pingSettings;
                case "Trencher" -> cfg.trencherSettings.pingSettings;
                case "BedrockBreaker" -> cfg.bedrockBreakerSettings.pingSettings;
                default -> null;
            };
            if (ps == null) { event.reply("Unknown type: " + type).setEphemeral(true).queue(); return; }

            switch (setting) {
                case "enabled" -> ps.enabled = value;
                case "onStart" -> ps.onStart = value;
                case "onStop" -> ps.onStop = value;
                case "onStuck" -> ps.onStuck = value;
                case "onResumed" -> ps.onResumed = value;
                case "onShutdown" -> ps.onShutdown = value;
                default -> { event.reply("Unknown setting: " + setting).setEphemeral(true).queue(); return; }
            }

            cfg.save();
            showPingSettingPicker(event, type);
        }

        private void showPingSettingPicker(ButtonInteractionEvent event, String type) {
            ModConfig cfg = config();
            if (cfg == null) return;
            ModConfig.PingSettings ps = switch (type) {
                case "WorldEater" -> cfg.worldEaterSettings.pingSettings;
                case "Trencher" -> cfg.trencherSettings.pingSettings;
                case "BedrockBreaker" -> cfg.bedrockBreakerSettings.pingSettings;
                default -> null;
            };
            if (ps == null) return;
            event.editMessage("Select which setting to change:")
                    .setEmbeds(embedFor(ps, type))
                    .setActionRow(selectFor(type))
                    .queue();
        }

        private MessageEmbed embedFor(ModConfig.PingSettings ps, String type) {
            return new EmbedBuilder()
                    .setTitle(type + " Ping Settings")
                    .setColor(0xFFFFFF)
                    .addField("Enabled", String.valueOf(ps.enabled), true)
                    .addField("On Start", String.valueOf(ps.onStart), true)
                    .addField("On Stop", String.valueOf(ps.onStop), true)
                    .addField("On Stuck", String.valueOf(ps.onStuck), true)
                    .addField("On Resumed", String.valueOf(ps.onResumed), true)
                    .addField("On Shutdown", String.valueOf(ps.onShutdown), true)
                    .build();
        }

        private StringSelectMenu selectFor(String type) {
            return StringSelectMenu.create("wen:pings:setting:" + type)
                    .setPlaceholder("Select setting to change")
                    .addOption("Enabled", "enabled")
                    .addOption("On Start", "onStart")
                    .addOption("On Stop", "onStop")
                    .addOption("On Stuck", "onStuck")
                    .addOption("On Resumed", "onResumed")
                    .addOption("On Shutdown", "onShutdown")
                    .build();
        }

        @Override
        public void onStringSelectInteraction(StringSelectInteractionEvent event) {
            String id = event.getComponentId();
            if (!id.startsWith("wen:")) return;

            Member member = event.getMember();
            if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("❌ You need Administrator permission.").setEphemeral(true).queue();
                return;
            }

            if (id.equals("wen:pings:type")) {
                String type = event.getValues().get(0);
                ModConfig cfg = config();
                if (cfg == null) return;

                ModConfig.PingSettings ps = switch (type) {
                    case "WorldEater" -> cfg.worldEaterSettings.pingSettings;
                    case "Trencher" -> cfg.trencherSettings.pingSettings;
                    case "BedrockBreaker" -> cfg.bedrockBreakerSettings.pingSettings;
                    default -> null;
                };
                if (ps == null) return;

                event.editMessage("Select which setting to change:")
                        .setEmbeds(embedFor(ps, type))
                        .setActionRow(selectFor(type))
                        .queue();
            } else if (id.startsWith("wen:pings:setting:")) {
                String type = id.substring("wen:pings:setting:".length());
                String setting = event.getValues().get(0);

                Button trueBtn = Button.success("wen:pings:val:" + type + ":" + setting + ":true", "True");
                Button falseBtn = Button.danger("wen:pings:val:" + type + ":" + setting + ":false", "False");

                event.editMessage("Set " + type + " '" + setting + "' to:")
                        .setActionRow(trueBtn, falseBtn)
                        .queue();
            }
        }

        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
            String command = event.getName();
            String type = switch (command) {
                case "worldeater" -> "WorldEater";
                case "trencher" -> "Trencher";
                case "bedrockbreaker" -> "BedrockBreaker";
                default -> null;
            };
            if (type == null) return;

            Member member = event.getMember();
            if (member == null || !hasAccess(member)) {
                event.replyChoiceStrings(List.of()).queue();
                return;
            }

            Collection<String> names = switch (type) {
                case "WorldEater" -> WorldEaterManager.getInstance().getAll().stream().map(i -> i.getDefinition().name()).toList();
                case "Trencher" -> TrencherManager.getInstance().getAll().stream().map(i -> i.getDefinition().name()).toList();
                case "BedrockBreaker" -> BedrockBreakerManager.getInstance().getAll().stream().map(i -> i.getDefinition().name()).toList();
                default -> List.of();
            };

            String focused = event.getFocusedOption().getValue();
            List<String> filtered = names.stream()
                    .filter(n -> focused == null || n.toLowerCase().startsWith(focused.toLowerCase()))
                    .limit(25)
                    .toList();

            event.replyChoiceStrings(filtered).queue();
        }

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            try {
                handleSlashCommand(event);
            } catch (Exception e) {
                e.printStackTrace();
                if (!event.isAcknowledged()) {
                    event.reply("\u274C Internal error: " + e.getMessage()).setEphemeral(true).queue();
                }
            }
        }

        private void handleSlashCommand(SlashCommandInteractionEvent event) {
            Member member = event.getMember();
            if (member == null) {
                event.reply("This command is only available in a server.").setEphemeral(true).queue();
                return;
            }

            ModConfig cfg = config();
            if (cfg == null) {
                event.reply("Config not loaded.").setEphemeral(true).queue();
                return;
            }

            String command = event.getName();
            String sub = event.getSubcommandName();

            if ("config".equals(command)) {
                handleConfigSlash(event, member, cfg);
                return;
            }

            String type = switch (command) {
                case "worldeater" -> "WorldEater";
                case "trencher" -> "Trencher";
                case "bedrockbreaker" -> "BedrockBreaker";
                default -> null;
            };
            if (type == null) {
                event.reply("Unknown command: /" + command).setEphemeral(true).queue();
                return;
            }

            if (!hasAccess(member)) {
                event.reply("\u274C You don't have permission to use this command.").setEphemeral(true).queue();
                return;
            }

            switch (sub) {
                case "start" -> handleMachineStart(event, type);
                case "stop" -> handleMachineStop(event, type);
                case "list" -> handleMachineList(event, type);
                default -> event.reply("Unknown subcommand: " + sub).setEphemeral(true).queue();
            }
        }

        private void handleConfigSlash(SlashCommandInteractionEvent event, Member member, ModConfig cfg) {
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("\u274C You need Administrator permission.").setEphemeral(true).queue();
                return;
            }
            switch (event.getSubcommandName()) {
                case "subscription-button" -> {
                    boolean val = event.getOption("value").getAsBoolean();
                    cfg.showSubscriptionButton = val;
                    cfg.save();
                    event.reply("Subscription button " + (val ? "shown" : "hidden") + " on start messages.").setEphemeral(true).queue();
                }
                case "ping-role" -> {
                    Role role = event.getOption("role").getAsRole();
                    cfg.pingRoleId = role.getId();
                    cfg.save();
                    event.reply("Ping role set to " + role.getAsMention() + ".").setEphemeral(true).queue();
                }
                case "channel" -> {
                    var channel = event.getOption("channel").getAsChannel();
                    cfg.channelId = channel.getId();
                    cfg.save();
                    event.reply("Notification channel set to " + channel.getAsMention() + ".").setEphemeral(true).queue();
                }
                case "member-discord-role" -> {
                    var roleOption = event.getOption("role");
                    if (roleOption == null) {
                        cfg.memberDiscordRole = "";
                        cfg.save();
                        event.reply("Member role cleared. Only admins can use start/stop/list now.").setEphemeral(true).queue();
                    } else {
                        Role role = roleOption.getAsRole();
                        cfg.memberDiscordRole = role.getId();
                        cfg.save();
                        event.reply("Member role set to " + role.getAsMention() + ".").setEphemeral(true).queue();
                    }
                }
                case "pings" -> {
                    var select = StringSelectMenu.create("wen:pings:type")
                            .setPlaceholder("Select machine type")
                            .addOption("World Eater", "WorldEater")
                            .addOption("Trencher", "Trencher")
                            .addOption("Bedrock Breaker", "BedrockBreaker")
                            .build();
                    event.reply("Select which machine type to configure:")
                            .addActionRow(select)
                            .setEphemeral(true)
                            .queue();
                }
                default -> event.reply("Unknown config subcommand.").setEphemeral(true).queue();
            }
        }

        private void handleMachineStart(SlashCommandInteractionEvent event, String type) {
            String name = event.getOption("name").getAsString();
            BaseMachineInstance inst = resolveInstance(type, name);
            if (inst == null) {
                event.reply("No " + type + " named '" + name + "' found.").setEphemeral(true).queue();
                return;
            }
            if (inst.isActive()) {
                event.reply(type + " '" + name + "' is already active.").setEphemeral(true).queue();
                return;
            }
            switch (type) {
                case "WorldEater" -> WorldEaterManager.getInstance().start(name);
                case "Trencher" -> TrencherManager.getInstance().start(name);
                case "BedrockBreaker" -> BedrockBreakerManager.getInstance().start(name);
            }
            broadcastMinecraft(type + " '" + name + "' started via Discord.");
            DiscordNotifier.sendStart(type, name, inst.getPingSettings());
            event.reply("Started " + type + " '" + name + "'.").setEphemeral(true).queue();
        }

        private void handleMachineStop(SlashCommandInteractionEvent event, String type) {
            String name = event.getOption("name").getAsString();
            BaseMachineInstance inst = resolveInstance(type, name);
            if (inst == null) {
                event.reply("No " + type + " named '" + name + "' found.").setEphemeral(true).queue();
                return;
            }
            if (!inst.isActive()) {
                event.reply(type + " '" + name + "' is already inactive.").setEphemeral(true).queue();
                return;
            }
            switch (type) {
                case "WorldEater" -> WorldEaterManager.getInstance().stop(name);
                case "Trencher" -> TrencherManager.getInstance().stop(name);
                case "BedrockBreaker" -> BedrockBreakerManager.getInstance().stop(name);
            }
            broadcastMinecraft(type + " '" + name + "' stopped via Discord.");
            DiscordNotifier.sendManuallyStopped(type, name, inst.getPingSettings());
            event.reply("Stopped " + type + " '" + name + "'.").setEphemeral(true).queue();
        }

        private void handleMachineList(SlashCommandInteractionEvent event, String type) {
            Collection<BaseMachineInstance> all = allMachines(type);
            if (all.isEmpty()) {
                event.reply("No " + type + " machines found.").setEphemeral(true).queue();
                return;
            }
            List<MessageEmbed> embeds = new ArrayList<>();
            var builder = new EmbedBuilder();
            builder.setColor(0x00AAFF);
            builder.setTitle(type + " Machines");
            int fieldCount = 0;
            for (BaseMachineInstance inst : all) {
                String status = inst.isActive() ? "\uD83D\uDFE2 Active" : "\uD83D\uDD34 Inactive";
                builder.addField(inst.getDefinition().name() + " (" + inst.getMachineType() + ")", status, false);
                fieldCount++;
                if (fieldCount >= 25) {
                    embeds.add(builder.build());
                    builder = new EmbedBuilder();
                    builder.setColor(0x00AAFF);
                    fieldCount = 0;
                }
            }
            if (fieldCount > 0 || embeds.isEmpty()) embeds.add(builder.build());
            event.replyEmbeds(embeds).setEphemeral(true).queue();
        }
    }
}
