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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

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
  private final RetryTemplate retryTemplate;
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
      final MultiValueMap<String, String> customizedHeaders,
      final ResponseErrorHandler responseErrorHandler,
      final RetryTemplateBuilder retryTemplateBuilder) {

    this.textEndpoint = textEndpoint;
    this.streamEndpoint = streamEndpoint;
    this.version = version;
    this.projectId = projectId;
    this.watsonxAiAuthentication = new WatsonxAiAuthentication(apiKey);

    Consumer<HttpHeaders> defaultHeaders =
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

    this.webClient = webClientBuilder.baseUrl(baseUrl).defaultHeaders(defaultHeaders).build();

    retryTemplate = retryTemplateBuilder.build();
  }

  // public Flux<WatsonxAiChatResponse> chat(WatsonxAiChatRequest
  // watsonxAiChatRequest) {
  // Assert.notNull(watsonxAiChatRequest, WATSONX_REQUEST_CANNOT_BE_NULL);

  // if (this.token.needsRefresh()) {
  // this.token = this.iamAuthenticator.requestToken();
  // }

  // return this.webClient
  // .post()
  // .uri(this.streamEndpoint)
  // .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token.getAccessToken())
  // .bodyValue(watsonxAiChatRequest.withProjectId(this.projectId))
  // .retrieve()
  // .bodyToFlux(WatsonxAiChatResponse.class)
  // .handle(
  // (data, sink) -> {
  // if (logger.isTraceEnabled()) {
  // logger.trace(data);
  // }
  // sink.next(data);
  // });
  // }

}
