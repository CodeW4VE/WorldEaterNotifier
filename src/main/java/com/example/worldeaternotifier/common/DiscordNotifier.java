package com.example.worldeaternotifier.common;

import com.example.worldeaternotifier.config.ModConfig.MessageTemplates;
import com.example.worldeaternotifier.config.ModConfig.PingSettings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DiscordNotifier {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static String webhookUrl;
    private static String pingRoleId;
    private static MessageTemplates weMessages;
    private static MessageTemplates trencherMessages;
    private static MessageTemplates bbMessages;

    public static void setConfig(String webhookUrl, String pingRoleId,
                                 MessageTemplates we, MessageTemplates trencher, MessageTemplates bb) {
        DiscordNotifier.webhookUrl = webhookUrl;
        DiscordNotifier.pingRoleId = pingRoleId;
        DiscordNotifier.weMessages = we;
        DiscordNotifier.trencherMessages = trencher;
        DiscordNotifier.bbMessages = bb;
    }

    private static MessageTemplates templatesFor(String machineType) {
        return switch (machineType) {
            case "WorldEater" -> weMessages;
            case "Trencher" -> trencherMessages;
            case "BedrockBreaker" -> bbMessages;
            default -> weMessages;
        };
    }

    private static String buildMentionIfAllowed(boolean mentionAllowed) {
        if (!mentionAllowed) return "";
        if (pingRoleId == null || pingRoleId.isBlank() || pingRoleId.equals("0")) return "";
        return "<@&" + pingRoleId + "> ";
    }

    private static String fmt(String template, String type, String name) {
        return template.replace("{type}", type).replace("{name}", name);
    }

    private static void send(String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        try {
            String json = "{\"content\":\"" + content.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
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
        String mention = buildMentionIfAllowed(pings.enabled && pings.onStart);
        send(mention + fmt(templatesFor(machineType).start, machineType, machineName));
    }

    public static void sendStuck(String machineType, String machineName, PingSettings pings) {
        String mention = buildMentionIfAllowed(pings.enabled && pings.onStuck);
        send(mention + fmt(templatesFor(machineType).stuck, machineType, machineName));
    }

    public static void sendResumed(String machineType, String machineName, PingSettings pings) {
        String mention = buildMentionIfAllowed(pings.enabled && pings.onResumed);
        send(mention + fmt(templatesFor(machineType).resumed, machineType, machineName));
    }

    public static void sendManuallyStopped(String machineType, String machineName, PingSettings pings) {
        String mention = buildMentionIfAllowed(pings.enabled && pings.onStop);
        send(mention + fmt(templatesFor(machineType).manualStop, machineType, machineName));
    }

    public static void sendServerShutdown(String machineType, String machineName, PingSettings pings) {
        String mention = buildMentionIfAllowed(pings.enabled && pings.onShutdown);
        send(mention + fmt(templatesFor(machineType).shutdown, machineType, machineName));
    }
}