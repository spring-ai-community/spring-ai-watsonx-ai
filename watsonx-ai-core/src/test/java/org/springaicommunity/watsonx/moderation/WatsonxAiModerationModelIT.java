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

package org.springaicommunity.watsonx.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.moderation.Moderation;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

/**
 * JUnit 5 test class for WatsonxAiModerationModel functionality. Tests moderation model operations
 * using mocking for external dependencies.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
class WatsonxAiModerationModelIT {

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
              RetryCallback callback = invocation.getArgument(0, RetryCallback.class);
              return callback.doWithRetry(null);
            });

    moderationModel =
        WatsonxAiModerationModel.builder()
            .watsonxAiModerationApi(watsonxAiModerationApi)
            .defaultOptions(defaultOptions)
            .retryTemplate(retryTemplate)
            .build();
  }

  @Test
  void moderationWithHAPDetection() {
    // Given
    String textToModerate = "This is hate speech and offensive content";

    WatsonxAiModerationResponse.Detection detection =
        new WatsonxAiModerationResponse.Detection(
            0, 10, "This is hate speech and offensive content", "hap", "hate", 0.95f, null);

    WatsonxAiModerationResponse mockResponse = new WatsonxAiModerationResponse(List.of(detection));

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
  void moderationWithNoDetections() {
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
  void moderationWithMultipleDetectors() {
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
  void moderationWithGraniteGuardianDetector() {
    // Given
    String textToModerate = "Content with harmful material";

    WatsonxAiModerationResponse.Detection detection =
        new WatsonxAiModerationResponse.Detection(
            0, 20, "Content with harmful material", "granite_guardian", "violence", 0.92f, null);

    WatsonxAiModerationResponse mockResponse = new WatsonxAiModerationResponse(List.of(detection));

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
  void moderationWithAllDetectors() {
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
  void moderationWithCustomOptions() {
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
  void moderationWithNullResponse() {
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

  @Test
  void moderationMetadataContainsDetectionsAndRawResponse() {
    // Given
    String textToModerate = "This is hate speech and offensive content";

    WatsonxAiModerationResponse.Detection detection =
        new WatsonxAiModerationResponse.Detection(
            0, 42, "This is hate speech and offensive content", "hap", "hate", 0.95f, null);

    WatsonxAiModerationResponse mockResponse = new WatsonxAiModerationResponse(List.of(detection));

    when(watsonxAiModerationApi.moderate(any(WatsonxAiModerationRequest.class)))
        .thenReturn(ResponseEntity.ok(mockResponse));

    // When
    ModerationPrompt prompt = new ModerationPrompt(textToModerate);
    ModerationResponse response = moderationModel.call(prompt);

    // Then
    assertNotNull(response);
    assertNotNull(response.getMetadata());

    // Verify metadata is the correct type
    assertTrue(response.getMetadata() instanceof WatsonxAiModerationResponseMetadata);
    WatsonxAiModerationResponseMetadata metadata =
        (WatsonxAiModerationResponseMetadata) response.getMetadata();

    // Verify detections metadata
    var detections = metadata.getDetections();
    assertNotNull(detections);
    assertEquals(1, detections.size());

    var detectionInfo = detections.get(0);
    assertEquals(0, detectionInfo.get("start"));
    assertEquals(42, detectionInfo.get("end"));
    assertEquals("This is hate speech and offensive content", detectionInfo.get("text"));
    assertEquals("hap", detectionInfo.get("detectionType"));
    assertEquals("hate", detectionInfo.get("detection"));
    assertEquals(0.95f, detectionInfo.get("score"));

    // Verify raw response metadata
    var rawResponse = metadata.getRawResponse();
    assertNotNull(rawResponse);
    assertEquals(mockResponse, rawResponse);

    // Verify API was called
    verify(watsonxAiModerationApi, times(1)).moderate(any(WatsonxAiModerationRequest.class));
  }
}
