package io.github.springaicommunity.watsonx.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

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

  @Test
  void optionsBuilderWithThreshold() {
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
  void detectorConfigEnabled() {
    // When
    WatsonxAiModerationRequest.DetectorConfig config =
        WatsonxAiModerationRequest.DetectorConfig.enabled();

    // Then
    assertNotNull(config);
    assertNull(config.threshold());
  }

  @Test
  void detectorConfigWithThreshold() {
    // When
    WatsonxAiModerationRequest.DetectorConfig config =
        WatsonxAiModerationRequest.DetectorConfig.of(0.85f);

    // Then
    assertNotNull(config);
    assertEquals(0.85f, config.threshold());
  }

  @Test
  void toDetectorsConversion() {
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
