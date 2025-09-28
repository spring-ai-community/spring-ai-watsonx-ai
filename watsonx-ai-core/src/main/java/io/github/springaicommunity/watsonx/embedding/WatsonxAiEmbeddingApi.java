package io.github.springaicommunity.watsonx.embedding;

import io.github.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import io.github.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

public class WatsonxAiEmbeddingApi {

  private static final Log logger = LogFactory.getLog(WatsonxAiChatApi.class);

  private final RestClient restClient;
  private final WatsonxAiAuthentication watsonxAiAuthentication;
  private String embeddingEndpoint;
  private String projectId;
  private String version;

  public WatsonxAiEmbeddingApi(
      final String baseUrl,
      final String embeddingEndpoint,
      final String version,
      final String projectId,
      final String apiKey,
      final MultiValueMap<String, String> customizedHeaders,
      final ResponseErrorHandler responseErrorHandler,
      final RestClient.Builder restClientBuilder) {

    this.embeddingEndpoint = embeddingEndpoint;
    this.version = version;
    this.projectId = projectId;
    this.watsonxAiAuthentication = new WatsonxAiAuthentication(apiKey);

    final Consumer<HttpHeaders> defaultHeaders =
        headers -> {
          headers.setContentType(MediaType.APPLICATION_JSON);
          headers.setAccept(List.of(MediaType.APPLICATION_JSON));
          headers.addAll(customizedHeaders);
        };

    this.restClient =
        restClientBuilder
            .baseUrl(baseUrl)
            .defaultStatusHandler(responseErrorHandler)
            .defaultHeaders(defaultHeaders)
            .build();
  }

  @Retryable(
      retryFor = Exception.class,
      maxAttempts = 3,
      backoff = @Backoff(random = true, delay = 1200, maxDelay = 7000, multiplier = 2.5))
  public ResponseEntity<WatsonxAiEmbeddingResponse> getEmbeddings(
      WatsonxAiEmbeddingRequest watsonxAiEmbeddingRequest) {
    Assert.notNull(watsonxAiEmbeddingRequest, "Watsonx Request cannot be null.");
    return this.restClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path(this.embeddingEndpoint).queryParam("version", this.version).build())
        .header(
            HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
        .body(watsonxAiEmbeddingRequest)
        .retrieve()
        .toEntity(WatsonxAiEmbeddingResponse.class);
  }
}
