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
