package io.github.springaicommunity.watsonx.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.message.user.TextChatUserContent;
import io.github.springaicommunity.watsonx.chat.util.Role;
import java.util.List;

/** A message from a user in a text chat. */
public final class TextChatMessageUser extends TextChatMessage {

  @JsonProperty("content")
  private final List<TextChatUserContent> content;

  public TextChatMessageUser(String name, List<TextChatUserContent> content) {
    super(Role.USER, name);
    this.content = content;
  }

  public List<TextChatUserContent> getContent() {
    return content;
  }
}
