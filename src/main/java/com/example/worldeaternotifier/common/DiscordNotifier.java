package com.example.worldeaternotifier.common;

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
    private static String pingRoleId;   // can be empty or "0"

    public static void setConfig(String webhookUrl, String pingRoleId) {
        DiscordNotifier.webhookUrl = webhookUrl;
        DiscordNotifier.pingRoleId = pingRoleId;
    }

    // Builds the mention string only if the role ID is valid (not empty or "0")
    private static String buildMentionIfAllowed(boolean mentionAllowed) {
        if (!mentionAllowed) return "";
        if (pingRoleId == null || pingRoleId.isBlank() || pingRoleId.equals("0")) return "";
        return "<@&" + pingRoleId + "> ";
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
        send(mention + machineType + " **'" + machineName + "'** has started.");
    }

    public static void sendStuck(String machineType, String machineName, PingSettings pings) {
        String mention = buildMentionIfAllowed(pings.enabled && pings.onStuck);
        send(mention + machineType + " **'" + machineName + "'** has stopped due to an obstruction.");
    }

    public static void sendResumed(String machineType, String machineName, PingSettings pings) {
        String mention = buildMentionIfAllowed(pings.enabled && pings.onResumed);
        send(mention + machineType + " **'" + machineName + "'** has started again.");
    }

    public static void sendManuallyStopped(String machineType, String machineName, PingSettings pings) {
        String mention = buildMentionIfAllowed(pings.enabled && pings.onStop);
        send(mention + machineType + " **'" + machineName + "'** was stopped manually.");
    }

    public static void sendServerShutdown(String machineType, String machineName, PingSettings pings) {
        String mention = buildMentionIfAllowed(pings.enabled && pings.onShutdown);
        send(mention + machineType + " **'" + machineName + "'** was shut down with the server and may have broken.");
    }
}