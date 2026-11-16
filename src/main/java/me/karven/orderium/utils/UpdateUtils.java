package me.karven.orderium.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static me.karven.orderium.Orderium.plugin;

public class UpdateUtils {
    private static final String API_URL = "https://api.modrinth.com/v2/project/";
    private static final String PROJECT_ID = "EH2l9h8i";
    private static final String mcVer = Bukkit.getMinecraftVersion();

    /// Check for updates
    /// Returns the newer version if available, otherwise returns null
    public static String checkForUpdates() {
        final String latestVer = fetchLatestVer();
        if (latestVer == null || compareSemanticVersion(latestVer, plugin.getPluginMeta().getVersion()) <= 0) return null;
        return latestVer;
    }

    private static String fetchLatestVer() {
        final String urlText =  API_URL + PROJECT_ID + "/version?game_versions=" + mcVer;
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(urlText).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Orderium Update Checker");
            connection.setRequestMethod("GET");
            connection.connect();
            final int resCode = connection.getResponseCode();
            if (resCode != HttpURLConnection.HTTP_OK) {
                Log.warn("Failed to check for updates");
                return null;
            }

            final String sRes = getRes(connection);
            final JsonArray jRes = JsonParser.parseString(sRes).getAsJsonArray();
            final JsonObject latest = jRes.get(0).getAsJsonObject();
            return latest.get("version_number").getAsString();
        } catch (IOException e) {
            Log.error("Failed to check for updates", e);
        }
        return null;
    }

    private static String getRes(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static int compareSemanticVersion(final String a, final String b) {
        final List<Integer> aSegments;
        final List<Integer> bSegments;

        try {
            aSegments = Arrays.stream(a.split("\\.")).map(Integer::parseInt).toList();
            bSegments = Arrays.stream(b.split("\\.")).map(Integer::parseInt).toList();

            if (aSegments.size() != 3 || aSegments.size() != bSegments.size()) {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            Log.warn("Cannot compare versions to check for updates, you are running a custom/development Orderium build.");
            return 0;
        }

        for (int i = 0; i < 3; i++) {
            final int aSegment = aSegments.get(i);
            final int bSegment = bSegments.get(i);
            if (aSegment != bSegment) return aSegment - bSegment;
        }
        return 0;
    }
}
