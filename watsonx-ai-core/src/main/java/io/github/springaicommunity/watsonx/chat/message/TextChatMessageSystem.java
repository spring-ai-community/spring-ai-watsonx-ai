package io.github.springaicommunity.watsonx.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.Role;

/** A message from the system in a text chat, typically used for instructions or context. */
public final class TextChatMessageSystem extends TextChatMessage {

  @JsonProperty("content")
  private final String content;

  public TextChatMessageSystem(String name, String content) {
    super(Role.SYSTEM, name);
    this.content = content;
  }

  public String getContent() {
    return content;
  }
}
