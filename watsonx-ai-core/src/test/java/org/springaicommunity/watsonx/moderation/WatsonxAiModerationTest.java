/*
 * Copyright 2025-2026 the original author or authors.
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

package org.springaicommunity.watsonx.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

/**
 * JUnit 5 test class for WatsonxAiModerationModel functionality. Tests moderation model operations
 * using mocking for external dependencies.
 *
 * @author Federico Mariani
 * @since 1.0.0
 */
public class WatsonxAiModerationTest {

  @Test
  void moderationRequestBuilder() {
    // When
    WatsonxAiModerationRequest request =
        WatsonxAiModerationRequest.builder()
            .input("Test input")
            .projectId("test-project-id")
            .detectors(
                WatsonxAiModerationRequest.Detectors.builder()
                    .hap(WatsonxAiModerationRequest.DetectorConfig.of(0.75f))
                    .build())
            .build();

    // Then
    assertNotNull(request);
    assertEquals("Test input", request.input());
    assertEquals("test-project-id", request.projectId());
    assertNotNull(request.detectors());
  }

  @Test
  void defaultOptionsAreApplied() {
    WatsonxAiModerationOptions defaultOptions =
        WatsonxAiModerationOptions.builder()
            .hap(WatsonxAiModerationRequest.DetectorConfig.of(0.75f))
            .build();

    WatsonxAiModerationApi watsonxAiModerationApi =
        new WatsonxAiModerationApi(
            "baseUrl",
            "textDetectionEndpoint",
            "version",
            "projectId",
            "apiKey",
            RestClient.builder(),
            response -> false);

    WatsonxAiModerationModel moderationModel =
        WatsonxAiModerationModel.builder()
            .retryTemplate(RetryTemplate.defaultInstance())
            .watsonxAiModerationApi(watsonxAiModerationApi)
            .defaultOptions(defaultOptions)
            .build();

    assertNotNull(moderationModel.getDefaultOptions());
    assertEquals(defaultOptions, moderationModel.getDefaultOptions());
  }
}
