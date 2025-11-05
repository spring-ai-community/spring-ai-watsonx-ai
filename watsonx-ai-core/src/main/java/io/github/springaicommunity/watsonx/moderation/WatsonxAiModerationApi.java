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

package io.github.springaicommunity.watsonx.moderation;

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

/**
 * API implementation of Watsonx AI Text Detection API.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiModerationApi {
  private static final Log logger = LogFactory.getLog(WatsonxAiModerationApi.class);

  private final RestClient restClient;
  private final WatsonxAiAuthentication watsonxAiAuthentication;
  private String textDetectionEndpoint;
  private String projectId;
  private String version;

  public WatsonxAiModerationApi(
      final String baseUrl,
      final String textDetectionEndpoint,
      final String version,
      final String projectId,
      final String apiKey,
      final RestClient.Builder restClientBuilder,
      final ResponseErrorHandler responseErrorHandler) {

    this.textDetectionEndpoint = textDetectionEndpoint;
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
   * Synchronous call to Watsonx AI Text Detection/Moderation API.
   *
   * @param watsonxAiModerationRequest the Watsonx AI moderation request
   * @return the response entity containing the Watsonx AI moderation response
   */
  public ResponseEntity<WatsonxAiModerationResponse> moderate(
      final WatsonxAiModerationRequest watsonxAiModerationRequest) {
    Assert.notNull(watsonxAiModerationRequest, "Watsonx.ai moderation request cannot be null");
    Assert.hasText(watsonxAiModerationRequest.input(), "Input to moderate cannot be null or empty");

    return restClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(this.textDetectionEndpoint)
                    .queryParam("version", this.version)
                    .build())
        .header(
            HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
        .body(watsonxAiModerationRequest.toBuilder().projectId(projectId).build())
        .retrieve()
        .toEntity(WatsonxAiModerationResponse.class);
  }
}
