package org.example.camunda.scoring;

import java.util.Map;

public record ScoringConnectorRequest(
        String url,
        String method,
        String authentication,
        Object payload,
        Map<String, String> headers) {
}
