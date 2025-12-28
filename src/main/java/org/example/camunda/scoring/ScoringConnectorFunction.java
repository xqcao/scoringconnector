package org.example.camunda.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OutboundConnector(name = "ScoringConnector", inputVariables = { "url", "method", "authentication", "payload",
        "headers" }, type = "io.camunda:scoring-connector:1")
public class ScoringConnectorFunction implements OutboundConnectorFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScoringConnectorFunction.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ScoringConnectorFunction() {
        this(new ObjectMapper(), HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build());
    }

    public ScoringConnectorFunction(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Object execute(OutboundConnectorContext context) throws Exception {
        var request = context.bindVariables(ScoringConnectorRequest.class);
        LOGGER.info("Executing Scoring Connector with request: {}", request);

        String method = request.method();
        String url = request.url();
        Object payload = request.payload();
        String auth = request.authentication();
        Map<String, String> customHeaders = request.headers();

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));

        // Handle Headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        if (auth != null && !auth.isBlank()) {
            headers.put("Authorization", auth);
        }
        if (customHeaders != null) {
            headers.putAll(customHeaders);
        }
        headers.forEach(builder::header);

        // Handle Body
        HttpRequest.BodyPublisher bodyPublisher;
        if (payload != null) {
            String jsonPayload = (payload instanceof String) ? (String) payload
                    : objectMapper.writeValueAsString(payload);
            bodyPublisher = HttpRequest.BodyPublishers.ofString(jsonPayload);
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }

        // Handle Method
        switch (method.toUpperCase()) {
            case "GET" -> builder.GET();
            case "DELETE" -> builder.DELETE();
            case "POST", "SCORING" -> builder.POST(bodyPublisher);
            case "PUT" -> builder.PUT(bodyPublisher);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        // Log Request Details
        LOGGER.info("Sending request: Method={}, URL={}, Headers={}, Payload={}", method, url, headers,
                payload != null ? payload : "null");

        // Send Request
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        // Parse Response
        int statusCode = response.statusCode();
        String responseBody = response.body();

        // Log Response Details
        LOGGER.info("Received response: Status={}, Body={}", statusCode, responseBody);

        Map<String, String> responseHeaders = new HashMap<>();
        response.headers().map().forEach((k, v) -> responseHeaders.put(k, String.join(",", v)));

        Object parsedBody;
        try {
            parsedBody = objectMapper.readValue(responseBody, Object.class);
        } catch (IOException e) {
            LOGGER.warn("Failed to parse response body as JSON, returning raw string", e);
            parsedBody = responseBody;
        }

        return new ScoringConnectorResult(statusCode, parsedBody, responseHeaders);
    }
}
