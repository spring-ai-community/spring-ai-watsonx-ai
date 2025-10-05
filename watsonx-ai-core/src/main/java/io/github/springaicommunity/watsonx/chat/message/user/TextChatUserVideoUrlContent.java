package io.github.springaicommunity.watsonx.chat.message.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.user.TextChatUserType;

/** Video URL content from a user in a text chat. */
public final class TextChatUserVideoUrlContent extends TextChatUserContent {

  @JsonProperty("video_url")
  private final TextChatUserVideoUrl videoUrl;

  @JsonProperty("data_asset")
  private final DataAsset dataAsset;

  public TextChatUserVideoUrlContent(TextChatUserVideoUrl videoUrl, DataAsset dataAsset) {
    super(TextChatUserType.VIDEO_URL);
    this.videoUrl = videoUrl;
    this.dataAsset = dataAsset;
  }

  public DataAsset getDataAsset() {
    return dataAsset;
  }

  public TextChatUserVideoUrl getVideoUrl() {
    return videoUrl;
  }

  public record TextChatUserVideoUrl(@JsonProperty("url") String url) {}
}
