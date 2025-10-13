/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.springaicommunity.watsonx.chat.message.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.springaicommunity.watsonx.chat.util.audio.AudioFormat;
import io.github.springaicommunity.watsonx.chat.util.user.TextChatUserImageDetailType;
import io.github.springaicommunity.watsonx.chat.util.user.TextChatUserType;

/**
 * The content of a user message in a text-based chat interaction with the Watsonx AI service. This
 * record encapsulates various types of content that a user can send, including text, images,
 * videos, audio inputs, and data assets.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextChatUserContent(
    @JsonProperty("type") TextChatUserType type,
    @JsonProperty("text") String text,
    @JsonProperty("image_url") TextChatUserImageUrl imageUrl,
    @JsonProperty("video_url") TextChatUserVideoUrl videoUrl,
    @JsonProperty("input_audio") TextChatUserInputAudio inputAudio,
    @JsonProperty("data_asset") DataAsset dataAsset) {

  /**
   * Constructor for creating a text-based user content message.
   *
   * @param text the text content of the message
   */
  public TextChatUserContent(String text) {
    this(TextChatUserType.TEXT, text, null, null, null, null);
  }

  /**
   * Constructor for creating an image URL-based user content message.
   *
   * @param imageUrl the image URL content of the message
   * @param dataAsset The data asset of an image uploaded into the IBM project space.
   */
  public TextChatUserContent(TextChatUserImageUrl imageUrl, DataAsset dataAsset) {
    this(TextChatUserType.IMAGE_URL, null, imageUrl, null, null, dataAsset);
  }

  /**
   * Constructor for creating a video URL-based user content message.
   *
   * @param videoUrl the video URL content of the message
   * @param dataAsset The data asset of a video uploaded into IBM project space.
   */
  public TextChatUserContent(TextChatUserVideoUrl videoUrl, DataAsset dataAsset) {
    this(TextChatUserType.VIDEO_URL, null, null, videoUrl, null, dataAsset);
  }

  /**
   * Constructor for creating an input audio-based user content message.
   *
   * @param inputAudio the input audio content of the message
   * @param dataAsset The data asset of an audio file uploaded into the IBM project space.
   */
  public TextChatUserContent(TextChatUserInputAudio inputAudio, DataAsset dataAsset) {
    this(TextChatUserType.INPUT_AUDIO, null, null, null, inputAudio, dataAsset);
  }

  public record TextChatUserImageUrl(
      @JsonProperty("url") String url, @JsonProperty("detail") TextChatUserImageDetailType detail) {

    public TextChatUserImageUrl(String url) {
      this(url, TextChatUserImageDetailType.AUTO);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TextChatUserVideoUrl(@JsonProperty("url") String url) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TextChatUserInputAudio(
      @JsonProperty("data") String data, @JsonProperty("format") AudioFormat format) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record DataAsset(@JsonProperty("id") String id) {}
}
