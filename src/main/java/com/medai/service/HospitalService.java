package com.medai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.dto.HospitalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    /**
     * Query Overpass API for hospitals within radius metres of lat/lon.
     * Returns a list sorted by distance from the user.
     */
    public List<HospitalResult> findNearby(double lat, double lon, int radiusMetres) {
        String query = buildOverpassQuery(lat, lon, radiusMetres);

        try {
            String json = webClientBuilder.build()
                    .post()
                    .uri(OVERPASS_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("data=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(15))
                    .block();

            return parseOverpassResponse(json, lat, lon);
        } catch (Exception e) {
            log.error("Overpass API error: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildOverpassQuery(double lat, double lon, int radius) {
        return String.format("""
            [out:json][timeout:15];
            (
              node(around:%d,%.6f,%.6f)["amenity"="hospital"];
              way(around:%d,%.6f,%.6f)["amenity"="hospital"];
              node(around:%d,%.6f,%.6f)["amenity"="clinic"];
              way(around:%d,%.6f,%.6f)["amenity"="clinic"];
            );
            out center;
            """,
            radius, lat, lon,
            radius, lat, lon,
            radius, lat, lon,
            radius, lat, lon
        );
    }

    private List<HospitalResult> parseOverpassResponse(String json, double userLat, double userLon) {
        List<HospitalResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode elements = root.get("elements");
            if (elements == null) return results;

            for (JsonNode el : elements) {
                double elLat, elLon;

                // node has lat/lon directly; way has them in "center"
                if (el.has("lat")) {
                    elLat = el.get("lat").asDouble();
                    elLon = el.get("lon").asDouble();
                } else if (el.has("center")) {
                    elLat = el.get("center").get("lat").asDouble();
                    elLon = el.get("center").get("lon").asDouble();
                } else {
                    continue;
                }

                JsonNode tags = el.get("tags");
                if (tags == null) continue;

                String name = getTag(tags, "name", "Unnamed Hospital");
                if (name.equals("Unnamed Hospital") && !getTag(tags, "amenity", "").contains("hospital")) {
                    name = "Unnamed Clinic";
                }

                String phone = getTag(tags, "phone",
                        getTag(tags, "contact:phone", ""));
                String website = getTag(tags, "website",
                        getTag(tags, "contact:website", ""));
                boolean emergency = "yes".equalsIgnoreCase(getTag(tags, "emergency", "no"));
                String specialty = getTag(tags, "healthcare:speciality", "");

                // Build address from tags
                StringBuilder addr = new StringBuilder();
                appendIfPresent(addr, tags, "addr:housenumber");
                appendIfPresent(addr, tags, "addr:street");
                appendIfPresent(addr, tags, "addr:city");
                String address = addr.length() > 0 ? addr.toString().trim() : "";

                double distance = haversineKm(userLat, userLon, elLat, elLon);

                results.add(HospitalResult.builder()
                        .osmId(el.get("id").asLong())
                        .name(name)
                        .lat(elLat)
                        .lon(elLon)
                        .phone(phone)
                        .address(address)
                        .website(website)
                        .emergency(emergency)
                        .specialty(specialty)
                        .distanceKm(Math.round(distance * 100.0) / 100.0)
                        .build());
            }

            results.sort(Comparator.comparingDouble(HospitalResult::getDistanceKm));
        } catch (Exception e) {
            log.error("Error parsing Overpass response: {}", e.getMessage());
        }
        return results;
    }

    private String getTag(JsonNode tags, String key, String defaultVal) {
        JsonNode node = tags.get(key);
        return node != null ? node.asText() : defaultVal;
    }

    private void appendIfPresent(StringBuilder sb, JsonNode tags, String key) {
        JsonNode node = tags.get(key);
        if (node != null && !node.asText().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(node.asText());
        }
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
