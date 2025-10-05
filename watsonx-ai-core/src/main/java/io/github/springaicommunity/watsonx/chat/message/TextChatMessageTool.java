package io.github.springaicommunity.watsonx.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.Role;

/** A message from a tool in the chat. */
public final class TextChatMessageTool extends TextChatMessage {

  @JsonProperty("content")
  private final String content;

  @JsonProperty("tool_call_id")
  private final String toolCallId;

  public TextChatMessageTool(String name, String content, String tool_call_id) {
    super(Role.TOOL, name);
    this.content = content;
    this.toolCallId = tool_call_id;
  }

  public String getContent() {
    return content;
  }

  public String getToolCallId() {
    return toolCallId;
  }
}
