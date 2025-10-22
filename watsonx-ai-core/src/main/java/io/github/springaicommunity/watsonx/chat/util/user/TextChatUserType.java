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

package io.github.springaicommunity.watsonx.chat.util.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.message.user.TextChatUserContent;

/**
 * Supports different types of user message in chat interactions in watsonx.ai. This is primarily
 * used in {@link TextChatUserContent}
 */
public enum TextChatUserType {

  /** Text message. */
  @JsonProperty("text")
  TEXT,

  /** Image URL message. */
  @JsonProperty("image_url")
  IMAGE_URL,

  /** Audio URL message. */
  @JsonProperty("input_audio")
  INPUT_AUDIO,

  /** Video URL message. */
  @JsonProperty("video_url")
  VIDEO_URL
}
