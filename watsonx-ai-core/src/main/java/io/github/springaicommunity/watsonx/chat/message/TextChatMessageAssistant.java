package io.github.springaicommunity.watsonx.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.Role;
import io.github.springaicommunity.watsonx.chat.util.ToolType;
import java.util.List;

/** Assistant message in a chat conversation. */
public final class TextChatMessageAssistant extends TextChatMessage {

  @JsonProperty("content")
  private final String content;

  @JsonProperty("refusal")
  private final String refusal;

  @JsonProperty("tool_calls")
  private final List<TextChatToolCall> toolCalls;

  public TextChatMessageAssistant(
      String name, String content, String refusal, List<TextChatToolCall> toolCalls) {
    super(Role.ASSISTANT, name);
    this.content = content;
    this.refusal = refusal;
    this.toolCalls = toolCalls;
  }

  public String getContent() {
    return content;
  }

  public String getRefusal() {
    return refusal;
  }

  public List<TextChatToolCall> getToolCall() {
    return toolCalls;
  }

  public record TextChatToolCall(
      @JsonProperty("id") String id,
      @JsonProperty("type") ToolType type,
      @JsonProperty("function") TextChatFunctionCall function) {}

  public record TextChatFunctionCall(
      @JsonProperty("name") String name, @JsonProperty("arguments") String arguments) {}
}
