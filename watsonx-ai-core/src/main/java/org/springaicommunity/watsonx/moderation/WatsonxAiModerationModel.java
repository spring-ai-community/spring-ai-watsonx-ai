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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.moderation.Categories;
import org.springframework.ai.moderation.CategoryScores;
import org.springframework.ai.moderation.Generation;
import org.springframework.ai.moderation.Moderation;
import org.springframework.ai.moderation.ModerationModel;
import org.springframework.ai.moderation.ModerationOptions;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.moderation.ModerationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * {@link ModerationModel} implementation that provides access to Watsonx AI detection API.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiModerationModel implements ModerationModel {

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiModerationModel.class);

  private final WatsonxAiModerationApi watsonxAiModerationApi;
  private final RetryTemplate retryTemplate;
  private WatsonxAiModerationOptions defaultOptions;

  public WatsonxAiModerationModel(
      WatsonxAiModerationApi watsonxAiModerationApi, RetryTemplate retryTemplate) {
    Assert.notNull(watsonxAiModerationApi, "WatsonxAiModerationApi must not be null");
    Assert.notNull(retryTemplate, "retryTemplate must not be null");
    this.watsonxAiModerationApi = watsonxAiModerationApi;
    this.retryTemplate = retryTemplate;
  }

  public WatsonxAiModerationOptions getDefaultOptions() {
    return this.defaultOptions;
  }

  public WatsonxAiModerationModel withDefaultOptions(WatsonxAiModerationOptions defaultOptions) {
    this.defaultOptions = defaultOptions;
    return this;
  }

  @Override
  public ModerationResponse call(ModerationPrompt moderationPrompt) {
    return this.retryTemplate.execute(
        ctx -> {
          String instructions = moderationPrompt.getInstructions().getText();

          WatsonxAiModerationRequest.Builder requestBuilder =
              WatsonxAiModerationRequest.builder().input(instructions);

          // Apply default options
          WatsonxAiModerationOptions mergedOptions =
              mergeOptions(moderationPrompt.getOptions(), this.defaultOptions);

          if (mergedOptions != null) {
            requestBuilder.detectors(mergedOptions.toDetectors());
          }

          WatsonxAiModerationRequest moderationRequest = requestBuilder.build();

          ResponseEntity<WatsonxAiModerationResponse> moderationResponseEntity =
              this.watsonxAiModerationApi.moderate(moderationRequest);

          return convertResponse(moderationResponseEntity, moderationRequest);
        });
  }

  private WatsonxAiModerationOptions mergeOptions(
      ModerationOptions runtimeOptions, WatsonxAiModerationOptions defaultOptions) {

    if (runtimeOptions == null && defaultOptions == null) {
      // Return default detector configuration
      return WatsonxAiModerationOptions.builder()
          .hap(WatsonxAiModerationRequest.DetectorConfig.of(0.75f))
          .build();
    }

    WatsonxAiModerationOptions.Builder builder = WatsonxAiModerationOptions.builder();

    // Start with default options
    if (defaultOptions != null) {
      builder
          .model(defaultOptions.getModel())
          .hap(defaultOptions.getHap())
          .pii(defaultOptions.getPii())
          .graniteGuardian(defaultOptions.getGraniteGuardian());
    }

    // Override with runtime options
    if (runtimeOptions != null) {
      if (runtimeOptions.getModel() != null) {
        builder.model(runtimeOptions.getModel());
      }

      if (runtimeOptions instanceof WatsonxAiModerationOptions watsonxOptions) {
        if (watsonxOptions.getHap() != null) {
          builder.hap(watsonxOptions.getHap());
        }
        if (watsonxOptions.getPii() != null) {
          builder.pii(watsonxOptions.getPii());
        }
        if (watsonxOptions.getGraniteGuardian() != null) {
          builder.graniteGuardian(watsonxOptions.getGraniteGuardian());
        }
      }
    }

    return builder.build();
  }

  private ModerationResponse convertResponse(
      ResponseEntity<WatsonxAiModerationResponse> moderationResponseEntity,
      WatsonxAiModerationRequest watsonxAiModerationRequest) {

    WatsonxAiModerationResponse moderationApiResponse = moderationResponseEntity.getBody();
    if (moderationApiResponse == null) {
      logger.warn("No moderation response returned for request: {}", watsonxAiModerationRequest);
      return new ModerationResponse(new Generation());
    }

    // Watsonx AI returns detections for each detector type
    List<ModerationResult> moderationResults = new ArrayList<>();

    // Track detection positions for metadata
    List<Map<String, Object>> detectionPositions = new ArrayList<>();

    if (moderationApiResponse.detections() != null
        && !moderationApiResponse.detections().isEmpty()) {

      // Group detections by type to build categories and scores
      Map<String, Boolean> categoryFlags = new HashMap<>();
      Map<String, Double> categoryScoreMap = new HashMap<>();
      boolean anyFlagged = false;

      for (WatsonxAiModerationResponse.Detection detection : moderationApiResponse.detections()) {
        String detectionType = detection.detectionType();
        String detectionValue = detection.detection();
        Float score = detection.score();

        if (score != null && score > 0) {
          anyFlagged = true;
        }

        // Track detection positions and metadata
        Map<String, Object> detectionInfo = new HashMap<>();
        detectionInfo.put("start", detection.start());
        detectionInfo.put("end", detection.end());
        detectionInfo.put("text", detection.text());
        detectionInfo.put("detectionType", detection.detectionType());
        detectionInfo.put("detection", detection.detection());
        detectionInfo.put("score", detection.score());
        if (detection.entity() != null) {
          detectionInfo.put("entity", detection.entity());
        }
        detectionPositions.add(detectionInfo);

        // Map Watsonx AI detections to Spring AI categories
        mapDetectionToCategories(
            detectionType, detectionValue, score, categoryFlags, categoryScoreMap);
      }

      // Build Categories from the collected flags
      Categories categories =
          Categories.builder()
              .sexual(categoryFlags.getOrDefault("sexual", false))
              .hate(categoryFlags.getOrDefault("hate", false))
              .harassment(categoryFlags.getOrDefault("harassment", false))
              .selfHarm(categoryFlags.getOrDefault("self-harm", false))
              .sexualMinors(categoryFlags.getOrDefault("sexual/minors", false))
              .hateThreatening(categoryFlags.getOrDefault("hate/threatening", false))
              .violenceGraphic(categoryFlags.getOrDefault("violence/graphic", false))
              .selfHarmIntent(categoryFlags.getOrDefault("self-harm/intent", false))
              .selfHarmInstructions(categoryFlags.getOrDefault("self-harm/instructions", false))
              .harassmentThreatening(categoryFlags.getOrDefault("harassment/threatening", false))
              .violence(categoryFlags.getOrDefault("violence", false))
              .build();

      // Build CategoryScores from the collected scores
      CategoryScores categoryScores =
          CategoryScores.builder()
              .hate(categoryScoreMap.getOrDefault("hate", 0.0))
              .hateThreatening(categoryScoreMap.getOrDefault("hate/threatening", 0.0))
              .harassment(categoryScoreMap.getOrDefault("harassment", 0.0))
              .harassmentThreatening(categoryScoreMap.getOrDefault("harassment/threatening", 0.0))
              .selfHarm(categoryScoreMap.getOrDefault("self-harm", 0.0))
              .selfHarmIntent(categoryScoreMap.getOrDefault("self-harm/intent", 0.0))
              .selfHarmInstructions(categoryScoreMap.getOrDefault("self-harm/instructions", 0.0))
              .sexual(categoryScoreMap.getOrDefault("sexual", 0.0))
              .sexualMinors(categoryScoreMap.getOrDefault("sexual/minors", 0.0))
              .violence(categoryScoreMap.getOrDefault("violence", 0.0))
              .violenceGraphic(categoryScoreMap.getOrDefault("violence/graphic", 0.0))
              .build();

      ModerationResult moderationResult =
          ModerationResult.builder()
              .categories(categories)
              .categoryScores(categoryScores)
              .flagged(anyFlagged)
              .build();

      moderationResults.add(moderationResult);
    }

    // If no detections, create an empty result
    if (moderationResults.isEmpty()) {
      Categories emptyCategories = Categories.builder().build();
      CategoryScores emptyScores = CategoryScores.builder().build();
      ModerationResult emptyResult =
          ModerationResult.builder()
              .categories(emptyCategories)
              .categoryScores(emptyScores)
              .flagged(false)
              .build();
      moderationResults.add(emptyResult);
    }

    Moderation moderation =
        Moderation.builder()
            .id("watsonx-ai-moderation")
            .model("granite_guardian")
            .results(moderationResults)
            .build();

    // Build metadata with detection positions and raw response
    // Create a custom watsonx-specific metadata that extends the base
    WatsonxAiModerationResponseMetadata moderationResponseMetadata =
        new WatsonxAiModerationResponseMetadata(detectionPositions, moderationApiResponse);

    return new ModerationResponse(new Generation(moderation), moderationResponseMetadata);
  }

  private void mapDetectionToCategories(
      String detectionType,
      String detectionValue,
      Float score,
      Map<String, Boolean> categoryFlags,
      Map<String, Double> categoryScoreMap) {

    if (score == null || score == 0.0f) {
      return;
    }

    boolean flagged = score > 0;
    double scoreAsDouble = score.doubleValue();

    // Map based on detector type
    switch (detectionType) {
      case "hap": // Hate, Abuse, Profanity
        // HAP detector returns general detections, map to hate/harassment
        categoryFlags.put("hate", flagged);
        categoryFlags.put("harassment", flagged);
        categoryScoreMap.put("hate", scoreAsDouble);
        categoryScoreMap.put("harassment", scoreAsDouble);
        break;

      case "pii":
        // PII doesn't map directly to OpenAI categories
        // Log for awareness but don't set flags
        logger.debug("PII detected: {} (score: {})", detectionValue, score);
        break;

      case "granite_guardian":
        // Granite Guardian is a general-purpose content moderation detector
        // Map to multiple categories based on the detection value
        mapGraniteGuardianDetection(detectionValue, scoreAsDouble, categoryFlags, categoryScoreMap);
        break;

      default:
        logger.debug("Unknown detector type: {}", detectionType);
    }
  }

  private void mapGraniteGuardianDetection(
      String detectionValue,
      double score,
      Map<String, Boolean> categoryFlags,
      Map<String, Double> categoryScoreMap) {

    // Granite Guardian provides comprehensive content moderation
    // Map based on the specific detection value if available
    if (detectionValue != null) {
      String detection = detectionValue.toLowerCase();

      // Map to appropriate categories based on detection content
      if (detection.contains("hate") || detection.contains("offensive")) {
        categoryFlags.put("hate", true);
        categoryScoreMap.put("hate", score);
      }
      if (detection.contains("harassment") || detection.contains("bullying")) {
        categoryFlags.put("harassment", true);
        categoryScoreMap.put("harassment", score);
      }
      if (detection.contains("violence") || detection.contains("violent")) {
        categoryFlags.put("violence", true);
        categoryScoreMap.put("violence", score);
      }
      if (detection.contains("sexual") || detection.contains("nsfw")) {
        categoryFlags.put("sexual", true);
        categoryScoreMap.put("sexual", score);
      }
      if (detection.contains("self-harm") || detection.contains("suicide")) {
        categoryFlags.put("self-harm", true);
        categoryScoreMap.put("self-harm", score);
      }
    } else {
      // If no specific detection value, map to general harassment category
      categoryFlags.put("harassment", true);
      categoryScoreMap.put("harassment", score);
    }

    logger.debug("Granite Guardian detected: {} (score: {})", detectionValue, score);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private WatsonxAiModerationApi watsonxAiModerationApi;
    private WatsonxAiModerationOptions defaultOptions;
    private RetryTemplate retryTemplate;

    private Builder() {}

    public Builder watsonxAiModerationApi(WatsonxAiModerationApi watsonxAiModerationApi) {
      this.watsonxAiModerationApi = watsonxAiModerationApi;
      return this;
    }

    public Builder defaultOptions(WatsonxAiModerationOptions defaultOptions) {
      this.defaultOptions = defaultOptions;
      return this;
    }

    public Builder retryTemplate(RetryTemplate retryTemplate) {
      this.retryTemplate = retryTemplate;
      return this;
    }

    public WatsonxAiModerationModel build() {
      Assert.notNull(watsonxAiModerationApi, "watsonxAiModerationApi must not be null");
      Assert.notNull(retryTemplate, "retryTemplate must not be null");

      WatsonxAiModerationModel model =
          new WatsonxAiModerationModel(watsonxAiModerationApi, retryTemplate);
      if (defaultOptions != null) {
        model.withDefaultOptions(defaultOptions);
      }
      return model;
    }
  }
}
