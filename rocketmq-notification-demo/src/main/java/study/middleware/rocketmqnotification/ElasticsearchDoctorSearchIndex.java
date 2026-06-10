package study.middleware.rocketmqnotification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ElasticsearchDoctorSearchIndex implements DoctorSearchIndex {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String doctorIndex;

    public ElasticsearchDoctorSearchIndex(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.elasticsearch.url}") String elasticsearchUrl,
            @Value("${app.elasticsearch.doctor-index}") String doctorIndex
    ) {
        this.restClient = restClientBuilder.baseUrl(elasticsearchUrl).build();
        this.objectMapper = objectMapper;
        this.doctorIndex = doctorIndex;
    }

    @Override
    public int rebuild(List<Doctor> doctors) {
        deleteIndexIfExists();
        createIndex();
        if (doctors.isEmpty()) {
            return 0;
        }

        StringBuilder bulkBody = new StringBuilder();
        for (Doctor doctor : doctors) {
            bulkBody.append(json(Map.of("index", Map.of("_index", doctorIndex, "_id", doctor.id())))).append('\n');
            bulkBody.append(json(toDocument(doctor))).append('\n');
        }

        restClient.post()
                .uri("/_bulk?refresh=true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bulkBody.toString())
                .retrieve()
                .toBodilessEntity();
        return doctors.size();
    }

    @Override
    public SearchPage<Doctor> search(String keyword, int page, int size) {
        Map<String, Object> query = keyword == null || keyword.isBlank()
                ? Map.of("match_all", Map.of())
                : Map.of("multi_match", Map.of(
                "query", keyword,
                "fields", List.of("name^3", "department^2", "specialty")
        ));
        Map<String, Object> request = Map.of(
                "from", page * size,
                "size", size,
                "query", query,
                "sort", List.of(Map.of("_score", "desc"), Map.of("id", "asc"))
        );

        String response = restClient.post()
                .uri("/{index}/_search", doctorIndex)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        return parseSearchResponse(response, page, size);
    }

    private void deleteIndexIfExists() {
        try {
            restClient.delete()
                    .uri("/{index}", doctorIndex)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
        }
    }

    private void createIndex() {
        Map<String, Object> mapping = Map.of(
                "mappings", Map.of("properties", Map.of(
                        "id", Map.of("type", "long"),
                        "name", Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword"))),
                        "department", Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword"))),
                        "specialty", Map.of("type", "text"),
                        "available", Map.of("type", "boolean"),
                        "updated_at", Map.of("type", "date")
                ))
        );

        restClient.put()
                .uri("/{index}", doctorIndex)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapping)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> toDocument(Doctor doctor) {
        return Map.of(
                "id", doctor.id(),
                "name", doctor.name(),
                "department", doctor.department(),
                "specialty", doctor.specialty(),
                "available", doctor.available(),
                "updated_at", doctor.updatedAt().toString()
        );
    }

    private SearchPage<Doctor> parseSearchResponse(String response, int page, int size) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode hits = root.path("hits");
            long total = hits.path("total").path("value").asLong();
            List<Doctor> doctors = new ArrayList<>();
            for (JsonNode hit : hits.path("hits")) {
                JsonNode source = hit.path("_source");
                doctors.add(new Doctor(
                        source.path("id").asLong(),
                        source.path("name").asText(),
                        source.path("department").asText(),
                        source.path("specialty").asText(),
                        source.path("available").asBoolean(),
                        java.time.Instant.parse(source.path("updated_at").asText())
                ));
            }
            return SearchPage.of(doctors, page, size, total);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Failed to parse Elasticsearch search response", e);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Failed to serialize Elasticsearch request", e);
        }
    }
}
