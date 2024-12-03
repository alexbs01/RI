package org.example.utils.queries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Query(
        @JsonProperty("_id") String id,
        String text,
        Query.Metadata metadata) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Metadata(String query, String narrative) {
    }
    public String query() {
        return metadata.query;
    }
}
