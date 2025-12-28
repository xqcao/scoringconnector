package org.example.camunda.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringConnectorTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;

    private ScoringConnectorFunction function;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        function = new ScoringConnectorFunction(objectMapper, httpClient);
    }

    @Test
    void shouldExecutePostRequest() throws Exception {
        // Given
        ScoringConnectorRequest request = new ScoringConnectorRequest(
                "http://example.com/api", "POST", "Bearer token", Map.of("key", "value"), null);

        OutboundConnectorContext context = OutboundConnectorContextBuilder.create()
                .variables(objectMapper.valueToTree(request))
                .build();

        when(httpClient.send(any(HttpRequest.class),
                (HttpResponse.BodyHandler<String>) any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"result\": \"success\"}");

        // When
        Object result = function.execute(context);

        // Then
        assertThat(result).isInstanceOf(ScoringConnectorResult.class);
        ScoringConnectorResult connectorResult = (ScoringConnectorResult) result;
        assertThat(connectorResult.status()).isEqualTo(200);
        assertThat(connectorResult.body()).isInstanceOf(Map.class);
    }
}
