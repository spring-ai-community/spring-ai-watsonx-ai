package io.github.springaicommunity.watsonx.chat.message.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.user.TextChatUserImageDetailType;
import io.github.springaicommunity.watsonx.chat.util.user.TextChatUserType;

/** Image URL content from a user in a text chat. */
public final class TextChatUserImageUrlContent extends TextChatUserContent {

  @JsonProperty("image_url")
  private final TextChatUserImageUrl imageUrl;

  @JsonProperty("data_asset")
  private DataAsset dataAsset;

  public TextChatUserImageUrlContent(TextChatUserImageUrl image_url, DataAsset dataAsset) {
    super(TextChatUserType.IMAGE_URL);
    this.imageUrl = image_url;
    this.dataAsset = dataAsset;
  }

  public DataAsset getDataAsset() {
    return dataAsset;
  }

  public TextChatUserImageUrl getImageUrl() {
    return imageUrl;
  }

  public record TextChatUserImageUrl(
      @JsonProperty("url") String url, @JsonProperty("detail") TextChatUserImageDetailType detail) {

    public TextChatUserImageUrl(String url) {
      this(url, TextChatUserImageDetailType.AUTO);
    }
  }
}
