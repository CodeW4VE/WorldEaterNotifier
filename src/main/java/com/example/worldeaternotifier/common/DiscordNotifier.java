package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.bot.DiscordBotManager;
import com.example.worldeaternotifier.config.ModConfig;
import com.example.worldeaternotifier.config.ModConfig.MessageTemplates;
import com.example.worldeaternotifier.config.ModConfig.PingSettings;
import com.example.worldeaternotifier.worldeater.WorldEaterManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DiscordNotifier {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static ModConfig config() {
        return WorldEaterManager.getInstance().getConfig();
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
        var config = config();
        if (config == null) return new MessageTemplates();
        return switch (machineType) {
            case "WorldEater" -> config.worldEaterSettings.messages;
            case "Trencher" -> config.trencherSettings.messages;
            case "BedrockBreaker" -> config.bedrockBreakerSettings.messages;
            default -> config.worldEaterSettings.messages;
        };
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
            String json = "{\"content\":\"" + fullContent.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
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

    public static void sendStart(String machineType, String machineName, PingSettings pings) {
        var config = config();
        boolean withButton = config != null && config.showSubscriptionButton;
        send(fmt(templatesFor(machineType).start, machineType, machineName),
                machineType, machineName, pings.enabled && pings.onStart, withButton);
    }

    public static void sendStuck(String machineType, String machineName, PingSettings pings) {
        send(fmt(templatesFor(machineType).stuck, machineType, machineName),
                machineType, machineName, pings.enabled && pings.onStuck, false);
    }

    public static void sendResumed(String machineType, String machineName, PingSettings pings) {
        send(fmt(templatesFor(machineType).resumed, machineType, machineName),
                machineType, machineName, pings.enabled && pings.onResumed, false);
    }

    public static void sendManuallyStopped(String machineType, String machineName, PingSettings pings) {
        send(fmt(templatesFor(machineType).manualStop, machineType, machineName),
                machineType, machineName, pings.enabled && pings.onStop, false);
    }

    public static void sendServerShutdown(String machineType, String machineName, PingSettings pings) {
        send(fmt(templatesFor(machineType).shutdown, machineType, machineName),
                machineType, machineName, pings.enabled && pings.onShutdown, false);
    }
}
