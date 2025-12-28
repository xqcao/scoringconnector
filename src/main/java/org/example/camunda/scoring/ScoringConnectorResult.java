package org.example.camunda.scoring;

import java.util.Map;

public record ScoringConnectorResult(
    int status,
    Object body,
    Map<String, String> headers
) {}
