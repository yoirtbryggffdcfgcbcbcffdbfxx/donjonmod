package com.dungeonmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LobbyClient {
    private static String baseUrl = "https://dungeon-lobby.onrender.com";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void setBaseUrl(String url) {
        baseUrl = url.replaceAll("/+$", "");
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    private static JsonObject post(String path, JsonObject body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new Exception("Lobby " + res.statusCode() + ": " + res.body());
        }
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private static JsonObject get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new Exception("Lobby " + res.statusCode() + ": " + res.body());
        }
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    public static JsonObject createParty(String playerUuid, String playerName) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("player_uuid", playerUuid);
        body.addProperty("player_name", playerName);
        return post("/party/create", body);
    }

    public static JsonObject joinParty(String playerUuid, String playerName, String code) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("player_uuid", playerUuid);
        body.addProperty("player_name", playerName);
        body.addProperty("code", code);
        return post("/party/join", body);
    }

    public static JsonObject startRun(String partyId, String seed) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("party_id", partyId);
        body.addProperty("seed", seed);
        return post("/run/start", body);
    }

    public static JsonObject completeRun(String runId, String playerUuid, boolean survived, int roomsCleared) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("run_id", runId);
        body.addProperty("player_uuid", playerUuid);
        body.addProperty("survived", survived);
        body.addProperty("rooms_cleared", roomsCleared);
        return post("/run/complete", body);
    }

    public static JsonObject getPlayerStats(String playerUuid) throws Exception {
        return get("/players/stats?uuid=" + playerUuid);
    }

    public static boolean healthCheck() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
