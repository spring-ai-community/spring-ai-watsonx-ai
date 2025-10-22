/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.springaicommunity.watsonx.chat;

import io.github.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * API implementation of watsonx.ai Chat Model API.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiChatApi {
  private static final Log logger = LogFactory.getLog(WatsonxAiChatApi.class);

  private final RestClient restClient;
  private final WebClient webClient;
  private final WatsonxAiAuthentication watsonxAiAuthentication;
  private String textEndpoint;
  private String streamEndpoint;
  private String projectId;
  private String version;

  public WatsonxAiChatApi(
      final String baseUrl,
      final String textEndpoint,
      final String streamEndpoint,
      final String version,
      final String projectId,
      final String apiKey,
      final RestClient.Builder restClientBuilder,
      final WebClient.Builder webClientBuilder,
      final ResponseErrorHandler responseErrorHandler) {

    this.textEndpoint = textEndpoint;
    this.streamEndpoint = streamEndpoint;
    this.version = version;
    this.projectId = projectId;
    this.watsonxAiAuthentication = new WatsonxAiAuthentication(apiKey);

    final Consumer<HttpHeaders> defaultHeaders =
        headers -> {
          headers.setContentType(MediaType.APPLICATION_JSON);
          headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        };

    this.restClient =
        restClientBuilder
            .baseUrl(baseUrl)
            .defaultStatusHandler(responseErrorHandler)
            .defaultHeaders(defaultHeaders)
            .build();

    this.webClient = webClientBuilder.baseUrl(baseUrl).defaultHeaders(defaultHeaders).build();
  }

  /**
   * Synchronous call to watsonx.ai Chat API.
   *
   * @param watsonxAiChatRequest the watsonx.ai chat request
   * @return the response entity containing the watsonx.ai chat response
   */
  public ResponseEntity<WatsonxAiChatResponse> chat(
      final WatsonxAiChatRequest watsonxAiChatRequest) {
    Assert.notNull(watsonxAiChatRequest, "Watsonx.ai request cannot be null");

    return restClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path(this.textEndpoint).queryParam("version", this.version).build())
        .header(
            HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
        .body(watsonxAiChatRequest.builder().projectId(projectId).build())
        .retrieve()
        .toEntity(WatsonxAiChatResponse.class);
  }

  /**
   * Asynchronous call to watsonx.ai Chat API using streaming.
   *
   * @param watsonxAiChatRequest the watsonx.ai chat request
   * @return a Flux stream of watsonx.ai chat responses
   */
  public Flux<WatsonxAiChatResponse> stream(final WatsonxAiChatRequest watsonxAiChatRequest) {
    Assert.notNull(watsonxAiChatRequest, "Watsonx.ai request cannot be null");

    return this.webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path(this.streamEndpoint).queryParam("version", this.version).build())
        .header(
            HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
        .bodyValue(watsonxAiChatRequest.builder().projectId(projectId).build())
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
