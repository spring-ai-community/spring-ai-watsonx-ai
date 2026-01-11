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

package org.springaicommunity.watsonx.chat.util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Type of reasoning effort used by watsonx.ai. A lower effort results in faster responses, and
 * shorter reasoning content.
 *
 * @author Harry Pardo
 * @since 1.0.2
 */
public enum ReasoningEffortType {
  /** Low reasoning effort */
  @JsonProperty("low")
  LOW("low"),
  /** Medium reasoning effort */
  @JsonProperty("medium")
  MEDIUM("medium"),
  /** High reasoning effort */
  @JsonProperty("high")
  HIGH("high");

  private final String reasoningEffort;

  ReasoningEffortType(String reasoningEffort) {
    this.reasoningEffort = reasoningEffort;
  }

  public String getReasoningEffort() {
    return reasoningEffort;
  }
}
