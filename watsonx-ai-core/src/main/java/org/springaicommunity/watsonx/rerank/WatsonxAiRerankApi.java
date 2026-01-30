/*
 * Copyright 2026 the original author or authors.
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

package org.springaicommunity.watsonx.rerank;

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
 * API implementation of watsonx.ai Rerank Model API.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiRerankApi {

  private final RestClient restClient;
  private final WatsonxAiAuthentication watsonxAiAuthentication;
  private final String rerankEndpoint;
  private final String projectId;
  private final String spaceId;
  private final String version;

  public WatsonxAiRerankApi(
      final String baseUrl,
      final String rerankEndpoint,
      final String version,
      final String projectId,
      final String spaceId,
      final String apiKey,
      final RestClient.Builder restClientBuilder,
      final ResponseErrorHandler responseErrorHandler) {

    this.rerankEndpoint = rerankEndpoint;
    this.version = version;
    this.projectId = projectId;
    this.spaceId = spaceId;
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
   * Synchronous call to watsonx.ai Rerank API.
   *
   * @param watsonxAiRerankRequest the watsonx.ai rerank request
   * @return the response entity containing the watsonx.ai rerank response
   */
  public ResponseEntity<WatsonxAiRerankResponse> rerank(
      final WatsonxAiRerankRequest watsonxAiRerankRequest) {
    Assert.notNull(watsonxAiRerankRequest, "Watsonx.ai rerank request cannot be null");

    return restClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path(this.rerankEndpoint).queryParam("version", this.version).build())
        .header(
            HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
        .body(watsonxAiRerankRequest.toBuilder().projectId(projectId).spaceId(spaceId).build())
        .retrieve()
        .toEntity(WatsonxAiRerankResponse.class);
  }
}
