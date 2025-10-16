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

package io.github.springaicommunity.watsonx.chat.util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reason for finishing a chat generation in watsonx.ai.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public enum ChatFinishReason {

  /** The model hit a natural stop point or a provided stop sequence. */
  @JsonProperty("stop")
  STOPPED,

  /** The model reached the maximum tokens for the generation. */
  @JsonProperty("length")
  LENGTH,

  /** The model called a tool call. */
  @JsonProperty("tool_calls")
  TOOL_CALLS,

  /** The model reached the maximum time limit for the generation. */
  @JsonProperty("time_limit")
  TIME_LIMIT,

  /** The model was interrupted by an external signal, such as a user canceling the request. */
  @JsonProperty("cancelled")
  CANCELLED,

  /**
   * The model encountered an error during generation. This could be due to various reasons, such as
   * internal server errors or issues with the input.
   */
  @JsonProperty("error")
  ERROR,

  /** The reponse is still in progress or incomplete. */
  @JsonProperty("null")
  NULL
}
