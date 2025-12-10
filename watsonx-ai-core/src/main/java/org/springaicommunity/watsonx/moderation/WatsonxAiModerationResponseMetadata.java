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

import java.util.List;
import java.util.Map;
import org.springframework.ai.moderation.ModerationResponseMetadata;

/**
 * Watsonx AI-specific moderation response metadata that extends Spring AI's base metadata. This
 * class provides access to watsonx.ai-specific information such as detection positions (start/end)
 * and the raw API response.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ModerationResponse response = moderationModel.call(prompt);
 * if (response.getMetadata() instanceof WatsonxAiModerationResponseMetadata watsonxMetadata) {
 *     List<Map<String, Object>> detections = watsonxMetadata.getDetections();
 *     for (Map<String, Object> detection : detections) {
 *         Integer start = (Integer) detection.get("start");
 *         Integer end = (Integer) detection.get("end");
 *         String text = (String) detection.get("text");
 *         // ... process detection information
 *     }
 *
 *     WatsonxAiModerationResponse rawResponse = watsonxMetadata.getRawResponse();
 *     // ... access raw API response
 * }
 * }</pre>
 *
 * @author Federico Mariani
 * @since 1.0.0
 */
public class WatsonxAiModerationResponseMetadata extends ModerationResponseMetadata {

  private final List<Map<String, Object>> detections;
  private final WatsonxAiModerationResponse rawResponse;

  public WatsonxAiModerationResponseMetadata(
      List<Map<String, Object>> detections, WatsonxAiModerationResponse rawResponse) {
    super();
    this.detections = detections;
    this.rawResponse = rawResponse;
  }

  public List<Map<String, Object>> getDetections() {
    return detections;
  }

  public WatsonxAiModerationResponse getRawResponse() {
    return rawResponse;
  }
}
