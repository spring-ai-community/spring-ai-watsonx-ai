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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.moderation.Moderation;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;

/**
 * JUnit 5 test class for WatsonxAiModerationModel functionality. Tests moderation model operations
 * using mocking for external dependencies.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
class WatsonxAiModerationModelTest {

  @Mock private WatsonxAiModerationApi watsonxAiModerationApi;

  @Mock private RetryTemplate retryTemplate;

  private WatsonxAiModerationModel moderationModel;
  private WatsonxAiModerationOptions defaultOptions;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Set up default moderation options with HAP detector
    defaultOptions =
        WatsonxAiModerationOptions.builder()
            .hap(WatsonxAiModerationRequest.DetectorConfig.of(0.75f))
            .build();

    // Configure RetryTemplate mock to execute the callback directly
    when(retryTemplate.execute(any()))
        .thenAnswer(
            invocation -> {
              org.springframework.retry.RetryCallback callback =
                  invocation.getArgument(0, org.springframework.retry.RetryCallback.class);
              return callback.doWithRetry(null);
            });

    moderationModel =
        WatsonxAiModerationModel.builder()
            .watsonxAiModerationApi(watsonxAiModerationApi)
            .defaultOptions(defaultOptions)
            .retryTemplate(retryTemplate)
            .build();
  }

  @Nested
  class ModerationTests {

    @Test
    void testModerationWithHAPDetection() {
      // Given
      String textToModerate = "This is hate speech and offensive content";

      WatsonxAiModerationResponse.Detection detection =
          new WatsonxAiModerationResponse.Detection(
              0, 10, "This is hate speech and offensive content", "hap", "hate", 0.95f, null);

      WatsonxAiModerationResponse mockResponse =
          new WatsonxAiModerationResponse(List.of(detection));

      when(watsonxAiModerationApi.moderate(any(WatsonxAiModerationRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      // When
      ModerationPrompt prompt = new ModerationPrompt(textToModerate);
      ModerationResponse response = moderationModel.call(prompt);

      // Then
      assertNotNull(response);
      assertNotNull(response.getResult());
      Moderation moderation = response.getResult().getOutput();
      assertNotNull(moderation);
      assertNotNull(moderation.getResults());
      assertFalse(moderation.getResults().isEmpty());

      var result = moderation.getResults().get(0);
      assertTrue(result.isFlagged());
      assertNotNull(result.getCategories());
      assertTrue(result.getCategories().isHate());
      assertTrue(result.getCategories().isHarassment());
      assertTrue(result.getCategoryScores().getHate() > 0);

      // Verify API was called
      verify(watsonxAiModerationApi, times(1)).moderate(any(WatsonxAiModerationRequest.class));
    }

    @Test
    void testModerationWithNoDetections() {
      // Given
      String textToModerate = "This is perfectly fine content";

      WatsonxAiModerationResponse mockResponse = new WatsonxAiModerationResponse(List.of());

      when(watsonxAiModerationApi.moderate(any(WatsonxAiModerationRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      // When
      ModerationPrompt prompt = new ModerationPrompt(textToModerate);
      ModerationResponse response = moderationModel.call(prompt);

      // Then
      assertNotNull(response);
      assertNotNull(response.getResult());
      Moderation moderation = response.getResult().getOutput();
      assertNotNull(moderation);
      assertNotNull(moderation.getResults());
      assertFalse(moderation.getResults().isEmpty());

      var result = moderation.getResults().get(0);
      assertFalse(result.isFlagged());

      // Verify API was called
      verify(watsonxAiModerationApi, times(1)).moderate(any(WatsonxAiModerationRequest.class));
    }

    @Test
    void testModerationWithMultipleDetectors() {
      // Given
      String textToModerate = "This contains hate speech and PII like john@example.com";

      List<WatsonxAiModerationResponse.Detection> detections =
          List.of(
              new WatsonxAiModerationResponse.Detection(
                  0, 10, "This contains hate speech", "hap", "hate", 0.85f, null),
              new WatsonxAiModerationResponse.Detection(
                  30, 46, "john@example.com", "pii", "EMAIL_ADDRESS", 0.95f, "EMAIL_ADDRESS"));

      WatsonxAiModerationResponse mockResponse = new WatsonxAiModerationResponse(detections);

      when(watsonxAiModerationApi.moderate(any(WatsonxAiModerationRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      // When
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().hap(0.75f).pii(0.8f).build();

      ModerationPrompt prompt = new ModerationPrompt(textToModerate, options);
      ModerationResponse response = moderationModel.call(prompt);

      // Then
      assertNotNull(response);
      var result = response.getResult().getOutput().getResults().get(0);
      assertTrue(result.isFlagged());
      assertTrue(result.getCategories().isHate());
      assertTrue(result.getCategories().isHarassment());
      assertTrue(result.getCategoryScores().getHate() > 0);

      // Verify API was called
      verify(watsonxAiModerationApi, times(1)).moderate(any(WatsonxAiModerationRequest.class));
    }

    @Test
    void testModerationWithGraniteGuardianDetector() {
      // Given
      String textToModerate = "Content with harmful material";

      WatsonxAiModerationResponse.Detection detection =
          new WatsonxAiModerationResponse.Detection(
              0, 20, "Content with harmful material", "granite_guardian", "violence", 0.92f, null);

      WatsonxAiModerationResponse mockResponse =
          new WatsonxAiModerationResponse(List.of(detection));

      when(watsonxAiModerationApi.moderate(any(WatsonxAiModerationRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      // When
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().graniteGuardian(0.7f).build();

      ModerationPrompt prompt = new ModerationPrompt(textToModerate, options);
      ModerationResponse response = moderationModel.call(prompt);

      // Then
      assertNotNull(response);
      var result = response.getResult().getOutput().getResults().get(0);
      assertTrue(result.isFlagged());
      assertTrue(result.getCategories().isViolence());
      assertTrue(result.getCategoryScores().getViolence() > 0);

      // Verify API was called
      verify(watsonxAiModerationApi, times(1)).moderate(any(WatsonxAiModerationRequest.class));
    }

    @Test
    void testModerationWithAllDetectors() {
      // Given
      String textToModerate = "Content to moderate with all detectors";

      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().hap(0.75f).pii(0.8f).graniteGuardian(0.6f).build();

      WatsonxAiModerationResponse mockResponse = new WatsonxAiModerationResponse(List.of());

      when(watsonxAiModerationApi.moderate(any(WatsonxAiModerationRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      // When
      ModerationPrompt prompt = new ModerationPrompt(textToModerate, options);
      ModerationResponse response = moderationModel.call(prompt);

      // Then
      assertNotNull(response);
      var result = response.getResult().getOutput().getResults().get(0);
      assertFalse(result.isFlagged());

      // Verify API was called
      verify(watsonxAiModerationApi, times(1)).moderate(any(WatsonxAiModerationRequest.class));
    }

    @Test
    void testModerationWithCustomOptions() {
      // Given
      String textToModerate = "Test text";

      WatsonxAiModerationOptions customOptions =
          WatsonxAiModerationOptions.builder()
              .hap(WatsonxAiModerationRequest.DetectorConfig.of(0.5f))
              .pii(WatsonxAiModerationRequest.DetectorConfig.enabled())
              .build();

      WatsonxAiModerationResponse mockResponse = new WatsonxAiModerationResponse(List.of());

      when(watsonxAiModerationApi.moderate(any(WatsonxAiModerationRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      // When
      ModerationPrompt prompt = new ModerationPrompt(textToModerate, customOptions);
      ModerationResponse response = moderationModel.call(prompt);

      // Then
      assertNotNull(response);
      verify(watsonxAiModerationApi, times(1)).moderate(any(WatsonxAiModerationRequest.class));
    }

    @Test
    void testModerationWithNullResponse() {
      // Given
      String textToModerate = "Test text";

      when(watsonxAiModerationApi.moderate(any(WatsonxAiModerationRequest.class)))
          .thenReturn(ResponseEntity.ok(null));

      // When
      ModerationPrompt prompt = new ModerationPrompt(textToModerate);
      ModerationResponse response = moderationModel.call(prompt);

      // Then
      assertNotNull(response);
      assertNotNull(response.getResult());
      assertNull(response.getResult().getOutput());

      // Verify API was called
      verify(watsonxAiModerationApi, times(1)).moderate(any(WatsonxAiModerationRequest.class));
    }
  }

  @Nested
  class OptionsTests {

    @Test
    void testDefaultOptionsAreApplied() {
      // Given
      assertNotNull(moderationModel.getDefaultOptions());
      assertEquals(defaultOptions, moderationModel.getDefaultOptions());
    }

    @Test
    void testOptionsBuilderWithThreshold() {
      // When
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().hap(0.8f).pii(0.9f).graniteGuardian(0.6f).build();

      // Then
      assertNotNull(options);
      assertNotNull(options.getHap());
      assertEquals(0.8f, options.getHap().threshold());
      assertNotNull(options.getPii());
      assertEquals(0.9f, options.getPii().threshold());
      assertNotNull(options.getGraniteGuardian());
      assertEquals(0.6f, options.getGraniteGuardian().threshold());
    }

    @Test
    void testDetectorConfigEnabled() {
      // When
      WatsonxAiModerationRequest.DetectorConfig config =
          WatsonxAiModerationRequest.DetectorConfig.enabled();

      // Then
      assertNotNull(config);
      assertNull(config.threshold());
    }

    @Test
    void testDetectorConfigWithThreshold() {
      // When
      WatsonxAiModerationRequest.DetectorConfig config =
          WatsonxAiModerationRequest.DetectorConfig.of(0.85f);

      // Then
      assertNotNull(config);
      assertEquals(0.85f, config.threshold());
    }

    @Test
    void testToDetectorsConversion() {
      // Given
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().hap(0.75f).pii(0.85f).graniteGuardian(0.6f).build();

      // When
      WatsonxAiModerationRequest.Detectors detectors = options.toDetectors();

      // Then
      assertNotNull(detectors);
      assertNotNull(detectors.hap());
      assertNotNull(detectors.pii());
      assertNotNull(detectors.graniteGuardian());
    }
  }

  @Nested
  class RequestTests {

    @Test
    void testModerationRequestBuilder() {
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
  }
}
