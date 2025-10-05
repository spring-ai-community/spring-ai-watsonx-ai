package io.github.springaicommunity.watsonx.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.Role;

/** Base class for chat messages in a conversation for watsonx.ai. */
public sealed class TextChatMessage
    permits TextChatMessageAssistant,
        TextChatMessageSystem,
        TextChatMessageTool,
        TextChatMessageUser {

  @JsonProperty("role")
  private Role role;

  @JsonProperty("name")
  private String name;

  public TextChatMessage(Role role, String name) {
    this.role = role;
    this.name = name;
  }

  public Role getRole() {
    return role;
  }

  public String getName() {
    return name;
  }
}
