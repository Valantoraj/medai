package com.medai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.dto.HospitalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    // Two Overpass mirrors — try primary first, fall back to secondary
    private static final String[] OVERPASS_MIRRORS = {
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    };

    // Simple in-memory cache: key = "lat_lon_radius", value = results + timestamp
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutes

    private record CachedResult(List<HospitalResult> results, long fetchedAt) {}

    public List<HospitalResult> findNearby(double lat, double lon, int radiusMetres) {
        // Round to 3 decimal places (~110m precision) for cache key
        String cacheKey = String.format("%.3f_%.3f_%d", lat, lon, radiusMetres);

        // Return cached result if fresh
        CachedResult cached = cache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.fetchedAt()) < CACHE_TTL_MS) {
            log.info("Hospital cache hit for key={}", cacheKey);
            return cached.results();
        }

        String query = buildOverpassQuery(lat, lon, radiusMetres);
        List<HospitalResult> results = List.of();

        // Try each mirror in order
        for (String mirrorUrl : OVERPASS_MIRRORS) {
            try {
                log.info("Querying Overpass mirror: {}", mirrorUrl);
                String json = webClientBuilder.build()
                        .post()
                        .uri(mirrorUrl)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .bodyValue("data=" + java.net.URLEncoder.encode(
                                query, java.nio.charset.StandardCharsets.UTF_8))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(20))
                        .block();

                results = parseOverpassResponse(json, lat, lon);

                if (!results.isEmpty()) {
                    // Cache successful result
                    cache.put(cacheKey, new CachedResult(results, System.currentTimeMillis()));
                    log.info("Found {} hospitals via {}", results.size(), mirrorUrl);
                    return results;
                }
                log.warn("Mirror {} returned 0 results, trying next", mirrorUrl);

            } catch (Exception e) {
                log.warn("Overpass mirror {} failed: {}", mirrorUrl, e.getMessage());
            }
        }

        // All mirrors failed or returned empty — return cached (possibly stale) data
        if (cached != null && !cached.results().isEmpty()) {
            log.info("All mirrors failed, returning stale cache for key={}", cacheKey);
            return cached.results();
        }

        log.error("All Overpass mirrors failed and no cache available");
        return results;
    }

    private String buildOverpassQuery(double lat, double lon, int radius) {
        return String.format(
            "[out:json][timeout:20];\n" +
            "(\n" +
            "  node(around:%d,%.6f,%.6f)[\"amenity\"=\"hospital\"];\n" +
            "  way(around:%d,%.6f,%.6f)[\"amenity\"=\"hospital\"];\n" +
            "  node(around:%d,%.6f,%.6f)[\"amenity\"=\"clinic\"];\n" +
            "  way(around:%d,%.6f,%.6f)[\"amenity\"=\"clinic\"];\n" +
            "  node(around:%d,%.6f,%.6f)[\"healthcare\"=\"hospital\"];\n" +
            "  way(around:%d,%.6f,%.6f)[\"healthcare\"=\"hospital\"];\n" +
            ");\n" +
            "out center;\n",
            radius, lat, lon,
            radius, lat, lon,
            radius, lat, lon,
            radius, lat, lon,
            radius, lat, lon,
            radius, lat, lon
        );
    }

    private List<HospitalResult> parseOverpassResponse(String json, double userLat, double userLon) {
        List<HospitalResult> results = new ArrayList<>();
        if (json == null || json.isBlank()) return results;

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode elements = root.get("elements");
            if (elements == null || !elements.isArray()) return results;

            Set<String> seen = new HashSet<>(); // deduplicate by name+coords

            for (JsonNode el : elements) {
                double elLat, elLon;

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

                String name = getTag(tags, "name", "");
                if (name.isBlank()) {
                    String amenity = getTag(tags, "amenity", "");
                    name = amenity.equals("hospital") ? "Unnamed Hospital" : "Unnamed Clinic";
                }

                // Deduplicate
                String dedupeKey = String.format("%s_%.4f_%.4f", name, elLat, elLon);
                if (!seen.add(dedupeKey)) continue;

                String phone   = getTag(tags, "phone",
                                 getTag(tags, "contact:phone", ""));
                boolean emergency = "yes".equalsIgnoreCase(getTag(tags, "emergency", "no"));
                String specialty  = getTag(tags, "healthcare:speciality",
                                   getTag(tags, "healthcare", ""));

                StringBuilder addr = new StringBuilder();
                appendIfPresent(addr, tags, "addr:housenumber");
                appendIfPresent(addr, tags, "addr:street");
                appendIfPresent(addr, tags, "addr:city");
                String address = addr.toString().trim();

                double distanceKm = haversineKm(userLat, userLon, elLat, elLon);

                results.add(HospitalResult.builder()
                        .osmId(el.get("id").asLong())
                        .name(name)
                        .lat(elLat)
                        .lon(elLon)
                        .phone(phone.isBlank() ? null : phone)
                        .address(address)
                        .emergency(emergency)
                        .specialty(specialty.isBlank() ? null : specialty)
                        .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
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
        return node != null && !node.isNull() ? node.asText() : defaultVal;
    }

    private void appendIfPresent(StringBuilder sb, JsonNode tags, String key) {
        JsonNode node = tags.get(key);
        if (node != null && !node.isNull() && !node.asText().isBlank()) {
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
