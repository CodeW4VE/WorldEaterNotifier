package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.bot.DiscordBotManager;
import com.example.worldeaternotifier.config.ModConfig;
import com.example.worldeaternotifier.config.ModConfig.MessageTemplates;
import com.example.worldeaternotifier.config.ModConfig.PingSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Function;

public class DiscordNotifier {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    private enum Event {
        START(t -> t.start, p -> p.onStart, true),
        STUCK(t -> t.stuck, p -> p.onStuck, false),
        RESUMED(t -> t.resumed, p -> p.onResumed, false),
        MANUAL_STOP(t -> t.manualStop, p -> p.onStop, false),
        SHUTDOWN(t -> t.shutdown, p -> p.onShutdown, false);

        final Function<MessageTemplates, String> template;
        final Function<PingSettings, Boolean> pingFlag;
        final boolean buttonEligible;

        Event(Function<MessageTemplates, String> template, Function<PingSettings, Boolean> pingFlag, boolean buttonEligible) {
            this.template = template;
            this.pingFlag = pingFlag;
            this.buttonEligible = buttonEligible;
        }
    }

    private static ModConfig config() {
        return MachineRegistry.getConfig();
    }

    private static String notificationMode() {
        var config = config();
        return config == null ? "webhook" : config.notificationMode;
    }

    private static String channelId() {
        var config = config();
        return config == null ? "" : config.channelId;
    }

    private static String pingRoleId() {
        var config = config();
        return config == null ? "" : config.pingRoleId;
    }

    private static String webhookUrl() {
        var config = config();
        return config == null ? "" : config.webhookUrl;
    }

    private static MessageTemplates templatesFor(String machineType) {
        MachineManager manager = MachineRegistry.get(machineType);
        if (manager == null || manager.getConfig() == null) return new MessageTemplates();
        return manager.getSettings().messages;
    }

    private static String buildMentionIfAllowed(boolean mentionAllowed) {
        if (!mentionAllowed) return "";
        String roleId = pingRoleId();
        if (roleId == null || roleId.isBlank() || roleId.equals("0")) return "";
        return "<@&" + roleId + "> ";
    }

    private static String fmt(String template, String type, String name) {
        return template.replace("{type}", type).replace("{name}", name);
    }

    private static void send(String content, String machineType, String machineName, boolean shouldMention, boolean withButton) {
        String mode = notificationMode();
        if ("webhook".equals(mode)) {
            sendWebhook(content, shouldMention);
        } else if ("bot".equals(mode)) {
            DiscordBotManager bot = DiscordBotManager.getInstance();
            if (bot.isRunning()) {
                bot.sendNotification(channelId(), content, pingRoleId(), shouldMention, machineType, machineName, withButton);
            }
        }
    }

    private static void sendWebhook(String content, boolean shouldMention) {
        String url = webhookUrl();
        if (url == null || url.isBlank()) return;
        String fullContent = buildMentionIfAllowed(shouldMention) + content;
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("content", fullContent);
            JsonObject allowedMentions = new JsonObject();
            JsonArray parse = new JsonArray();
            parse.add("roles");
            allowedMentions.add("parse", parse);
            payload.add("allowed_mentions", allowedMentions);
            String json = GSON.toJson(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void send(Event event, String machineType, String machineName, PingSettings pings) {
        var config = config();
        boolean withButton = event.buttonEligible && config != null && config.showSubscriptionButton;
        String content = fmt(event.template.apply(templatesFor(machineType)), machineType, machineName);
        send(content, machineType, machineName, pings.enabled && event.pingFlag.apply(pings), withButton);
    }

    public static void sendStart(String machineType, String machineName, PingSettings pings) {
        send(Event.START, machineType, machineName, pings);
    }

    public static void sendStuck(String machineType, String machineName, PingSettings pings) {
        send(Event.STUCK, machineType, machineName, pings);
    }

    public static void sendResumed(String machineType, String machineName, PingSettings pings) {
        send(Event.RESUMED, machineType, machineName, pings);
    }

    public static void sendManuallyStopped(String machineType, String machineName, PingSettings pings) {
        send(Event.MANUAL_STOP, machineType, machineName, pings);
    }

    public static void sendServerShutdown(String machineType, String machineName, PingSettings pings) {
        send(Event.SHUTDOWN, machineType, machineName, pings);
    }
}
