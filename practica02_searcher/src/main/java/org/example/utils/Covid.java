package org.example.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Covid(
        @JsonProperty("_id") String id,
        String title,
        String text,
        Covid.Metadata metadata) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Metadata(String url, String pubmed_id) {
    }

    public String url() {
        return metadata.url;
    }

    public String pubmed_id() {
        return metadata.pubmed_id;
    }
}