package org.sensorhub.impl.sensor.krakensdr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sensorhub.api.common.SensorHubException;

/**
 * HTTP utility for the KrakenSDR driver.
 *
 * <p>Handles reading {@code settings.json} and uploading updated settings via the
 * miniserve HTTP server on port 8081. DoA data is no longer fetched here — it
 * arrives asynchronously over the WebSocket managed by {@link KrakenSdrSensor}.
 */
public class KrakenUtility {
    private final String settingsUrl;
    private final KrakenSdrSensor sensor;

    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 2000;

    public KrakenUtility(KrakenSdrSensor krakenSdrSensor) {
        this.sensor = krakenSdrSensor;
        this.settingsUrl = krakenSdrSensor.SETTINGS_URL;
    }

    public String connectAndGetDataAsString(String httpURL) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(httpURL).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    public JsonObject getSettings() throws SensorHubException {
        try {
            String json = connectAndGetDataAsString(settingsUrl);
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            sensor.getLogger().error("Failed to retrieve Kraken settings", e);
            throw new SensorHubException("Failed to retrieve Kraken settings", e);
        }
    }

    public void uploadSettings(JsonObject json) throws SensorHubException {
        String boundary = "----KrakenBoundary" + System.currentTimeMillis();
        String lineFeed = "\r\n";

        HttpURLConnection conn = null;
        try {
            URL url = new URL(sensor.OUTPUT_URL + "/upload?path=/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(("--" + boundary + lineFeed).getBytes());
                out.write(("Content-Disposition: form-data; name=\"path\"; filename=\"settings.json\"" + lineFeed).getBytes());
                out.write(("Content-Type: application/json" + lineFeed).getBytes());
                out.write(lineFeed.getBytes());
                out.write(json.toString().getBytes());
                out.write(lineFeed.getBytes());
                out.write(("--" + boundary + "--" + lineFeed).getBytes());
                out.flush();
            }

            int response = conn.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error " + response);
            }

            sensor.getLogger().info("Kraken settings.json uploaded successfully");

        } catch (Exception e) {
            throw new SensorHubException("Kraken settings upload failed. Most likely a permission error or miniserve is not set up on kraken device", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}