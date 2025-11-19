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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response from the Watsonx AI Text Detection/Moderation API. Full documentation can be found at <a
 * href="https://cloud.ibm.com/apidocs/watsonx-ai#text-detection">Watsonx AI Text Detection</a>.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiModerationResponse(@JsonProperty("detections") List<Detection> detections) {

  /** Individual detection result from a specific detector. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Detection(
      @JsonProperty("start") Integer start,
      @JsonProperty("end") Integer end,
      @JsonProperty("text") String text,
      @JsonProperty("detection_type") String detectionType,
      @JsonProperty("detection") String detection,
      @JsonProperty("score") Float score,
      @JsonProperty("entity") String entity) {}
}
