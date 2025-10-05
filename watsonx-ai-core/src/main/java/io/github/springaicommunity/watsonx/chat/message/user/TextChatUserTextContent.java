package io.github.springaicommunity.watsonx.chat.message.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.user.TextChatUserType;

/** Text content from a user in a text chat. */
public final class TextChatUserTextContent extends TextChatUserContent {

  @JsonProperty("text")
  private final String text;

  public TextChatUserTextContent(String text) {
    super(TextChatUserType.TEXT);
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
