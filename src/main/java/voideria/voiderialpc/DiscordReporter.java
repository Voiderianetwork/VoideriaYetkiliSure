package voideria.voiderialpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordReporter {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DiscordReporter(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public CompletableFuture<Boolean> sendReport() {
        return CompletableFuture.supplyAsync(() -> {
            String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                return false;
            }

            try {
                Map<UUID, PlayerData> allData = dataManager.getAllData();
                if (allData.isEmpty()) {
                    return false;
                }

                ObjectNode embed = mapper.createObjectNode();
                embed.put("title", "Haftalik Yetkili Sure Raporu");
                embed.put("color", 5814783);
                embed.put("timestamp", java.time.Instant.now().toString());

                ArrayNode fields = mapper.createArrayNode();
                allData.values().stream()
                    .sorted((d1, d2) -> Long.compare(d2.getTotalSeconds(), d1.getTotalSeconds()))
                    .forEach(data -> {
                        fields.add(createField(
                            data.getPlayerName(), 
                            data.getFormattedTime(),
                            false
                        ));
                    });

                embed.set("fields", fields);

                ObjectNode payload = mapper.createObjectNode();
                ArrayNode embedsArray = mapper.createArrayNode();
                embedsArray.add(embed);
                payload.set("embeds", embedsArray);

                return sendWebhook(webhookUrl, payload.toString());
            } catch (Exception e) {
                return false;
            }
        }, executor);
    }

    private ObjectNode createField(String name, String value, boolean inline) {
        ObjectNode field = mapper.createObjectNode();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", inline);
        return field;
    }

    private boolean sendWebhook(String url, String json) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}