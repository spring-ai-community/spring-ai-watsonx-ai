package io.github.springaicommunity.watsonx.chat.message.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.audio.AudioFormat;
import io.github.springaicommunity.watsonx.chat.util.user.TextChatUserType;

/** Audio content from a user in a text chat. */
public final class TextChatUserAudioContent extends TextChatUserContent {

  @JsonProperty("input_audio")
  private TextChatUserInputAudio inputAudio;

  @JsonProperty("data_asset")
  private DataAsset dataAsset;

  public TextChatUserAudioContent(TextChatUserInputAudio inputAudio, DataAsset dataAsset) {
    super(TextChatUserType.INPUT_AUDIO);
    this.inputAudio = inputAudio;
    this.dataAsset = dataAsset;
  }

  public DataAsset getDataAsset() {
    return dataAsset;
  }

  public TextChatUserInputAudio getInputAudio() {
    return inputAudio;
  }

  public record TextChatUserInputAudio(
      @JsonProperty("data") String data, @JsonProperty("format") AudioFormat format) {}
}
