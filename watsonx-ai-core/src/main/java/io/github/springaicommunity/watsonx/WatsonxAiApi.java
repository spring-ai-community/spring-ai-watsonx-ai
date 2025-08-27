package io.github.springaicommunity.watsonx;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

public class WatsonxAiApi {

  public static final String WATSONX_REQUEST_CANNOT_BE_NULL = "Watsonx Request cannot be null";

  private static final Log logger = LogFactory.getLog(WatsonxAiApi.class);

  private final RestClient restClient;
  private final WebClient webClient;
  private final IamAuthenticator iamAuthenticator;
  private final String chatEndpoint;
  private final String embeddingEndpoint;
  private final String projectId;
  private IamToken token;

  /**
   * Constructor for WatsonxAiApi
   *
   * @param baseUrl
   * @param streamEndpoint
   * @param textEndpoint
   * @param embeddingEndpoint
   * @param projectId
   * @param IAMToken
   * @param restClientBuilder
   */
  public WatsonxAiApi(
      String baseUrl,
      String chatEndpoint,
      String textEndpoint,
      String embeddingEndpoint,
      String projectId,
      String IAMToken,
      RestClient.Builder restClientBuilder) {
    this.chatEndpoint = chatEndpoint;
    this.embeddingEndpoint = embeddingEndpoint;
    this.projectId = projectId;
    this.iamAuthenticator = IamAuthenticator.fromConfiguration(Map.of("APIKEY", IAMToken));
    this.token = this.iamAuthenticator.requestToken();

    Consumer<HttpHeaders> defaultHeaders =
        headers -> {
          headers.setContentType(MediaType.APPLICATION_JSON);
          headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        };

    this.restClient =
        restClientBuilder
            .baseUrl(baseUrl)
            .defaultStatusHandler(RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER)
            .defaultHeaders(defaultHeaders)
            .build();

    this.webClient = WebClient.builder().baseUrl(baseUrl).defaultHeaders(defaultHeaders).build();
  }

  @Retryable(
      retryFor = Exception.class,
      maxAttempts = 3,
      backoff = @Backoff(random = true, delay = 1200, maxDelay = 7000, multiplier = 2.5))
  public Flux<WatsonxAiChatResponse> chat(WatsonxAiChatRequest watsonxAiChatRequest) {
    Assert.notNull(watsonxAiChatRequest, WATSONX_REQUEST_CANNOT_BE_NULL);

    if (this.token.needsRefresh()) {
      this.token = this.iamAuthenticator.requestToken();
    }

    return this.webClient
        .post()
        .uri(this.streamEndpoint)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token.getAccessToken())
        .bodyValue(watsonxAiChatRequest.withProjectId(this.projectId))
        .retrieve()
        .bodyToFlux(WatsonxAiChatResponse.class)
        .handle(
            (data, sink) -> {
              if (logger.isTraceEnabled()) {
                logger.trace(data);
              }
              sink.next(data);
            });
  }
}
