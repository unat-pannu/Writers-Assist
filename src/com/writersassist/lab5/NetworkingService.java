package com.writersassist.lab5;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Lab 5: InetAddress and URLConnection
 */
public class NetworkingService {
    public static Map<String, String> getHostInfo() {
        Map<String, String> info = new HashMap<>();
        try {
            InetAddress local = InetAddress.getLocalHost();
            info.put("Host Name", local.getHostName());
            info.put("IP Address", local.getHostAddress());
        } catch (Exception e) {
            info.put("Error", e.getMessage());
        }
        return info;
    }

    public static String fetchMetadata(String urlString) {
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 20) {
                    response.append(line).append("\n");
                    count++;
                }
            }
        } catch (Exception e) {
            return "Failed to fetch metadata: " + e.getMessage();
        }
        return response.toString();
    }
}
