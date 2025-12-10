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

package org.springaicommunity.watsonx.embedding;

import java.util.List;
import java.util.function.Consumer;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * API implementation of watsonx.ai Embedding Model API.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiEmbeddingApi {

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
      final RestClient.Builder restClientBuilder,
      final ResponseErrorHandler responseErrorHandler) {

    this.embeddingEndpoint = embeddingEndpoint;
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
  }

  /**
   * Synchronous call to watsonx.ai Embedding API.
   *
   * @param watsonxAiEmbeddingRequest the watsonx.ai embedding request
   * @return the response entity containing the watsonx.ai embedding response
   */
  public ResponseEntity<WatsonxAiEmbeddingResponse> embed(
      final WatsonxAiEmbeddingRequest watsonxAiEmbeddingRequest) {
    Assert.notNull(watsonxAiEmbeddingRequest, "Watsonx.ai embedding request cannot be null");

    return restClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path(this.embeddingEndpoint).queryParam("version", this.version).build())
        .header(
            HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
        .body(watsonxAiEmbeddingRequest.toBuilder().projectId(projectId).build())
        .retrieve()
        .toEntity(WatsonxAiEmbeddingResponse.class);
  }
}
