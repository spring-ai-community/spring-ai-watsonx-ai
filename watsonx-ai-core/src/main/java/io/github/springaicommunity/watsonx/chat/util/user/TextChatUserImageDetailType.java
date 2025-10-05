package io.github.springaicommunity.watsonx.chat.util.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Supports different levels of detail for user images in chat interactions in watsonx.ai. */
public enum TextChatUserImageDetailType {
  @JsonProperty("auto")
  AUTO,

  @JsonProperty("low")
  LOW,

  @JsonProperty("high")
  HIGH
}
