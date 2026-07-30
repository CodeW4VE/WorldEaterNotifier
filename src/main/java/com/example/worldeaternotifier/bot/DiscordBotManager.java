package com.example.worldeaternotifier.bot;

import com.example.worldeaternotifier.worldeater.WorldEaterManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.ArrayList;
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
                    .addEventListeners(new ButtonListener())
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
            channel.sendMessage(message).setActionRow(toggle).queue();
        } else {
            channel.sendMessage(message).queue();
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

    private class ButtonListener extends ListenerAdapter {
        @Override
        public void onReady(ReadyEvent event) {
            flushPending();
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

            var config = WorldEaterManager.getInstance().getConfig();
            if (config == null) return;

            String roleId = config.pingRoleId;
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
                            v -> event.reply("You won't get pinged anymore.").setEphemeral(true).queue(),
                            e -> event.reply("\u274C Failed to remove role.").setEphemeral(true).queue()
                    );
                } else {
                    event.getGuild().addRoleToMember(member, role).queue(
                            v -> event.reply("You'll now get pinged!").setEphemeral(true).queue(),
                            e -> event.reply("\u274C Failed to assign role.").setEphemeral(true).queue()
                    );
                }
            } catch (HierarchyException e) {
                event.reply("\u274C Bot's role must be above the ping role in server settings. Move it higher in Roles tab.").setEphemeral(true).queue();
            }
        }
    }
}
