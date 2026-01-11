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

package org.springaicommunity.watsonx.image;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.image.ImageOptions;

/**
 * Options for watsonx.ai Image Generation API. Configuration options that can be passed to control
 * the behavior of the image generation model.
 *
 * <p>Based on the watsonx.ai Text to Image API: <a
 * href="https://cloud.ibm.com/apidocs/watsonx-ai#text-image">Watson AI Text to Image</a>.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatsonxAiImageOptions implements ImageOptions {

  /** The model ID to use for image generation. */
  @JsonProperty("model_id")
  private String model;

  /** The width of the generated image in pixels. */
  @JsonProperty("width")
  private Integer width;

  /** The height of the generated image in pixels. */
  @JsonProperty("height")
  private Integer height;

  /** Moderation settings for image generation. */
  @JsonProperty("moderations")
  private WatsonxAiImageRequest.ModerationsInput moderations;

  public WatsonxAiImageOptions() {}

  private WatsonxAiImageOptions(Builder builder) {
    this.model = builder.model;
    this.width = builder.width;
    this.height = builder.height;
    this.moderations = builder.moderations;
  }

  @Override
  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  @Override
  public Integer getN() {
    return null;
  }

  public void setN(Integer n) {
    // Not supported
  }

  @Override
  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  @Override
  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public WatsonxAiImageRequest.ModerationsInput getModerations() {
    return moderations;
  }

  public void setModerations(WatsonxAiImageRequest.ModerationsInput moderations) {
    this.moderations = moderations;
  }

  @Override
  public String getResponseFormat() {
    return null;
  }

  public void setResponseFormat(String responseFormat) {
    // Not supported
  }

  @Override
  public String getStyle() {
    return null;
  }

  public void setStyle(String style) {
    // Not supported
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .model(this.model)
        .width(this.width)
        .height(this.height)
        .moderations(this.moderations);
  }

  public static WatsonxAiImageOptions fromOptions(WatsonxAiImageOptions fromOptions) {
    return builder()
        .model(fromOptions.getModel())
        .width(fromOptions.getWidth())
        .height(fromOptions.getHeight())
        .moderations(fromOptions.getModerations())
        .build();
  }

  public static class Builder {
    private String model;
    private Integer width;
    private Integer height;
    private WatsonxAiImageRequest.ModerationsInput moderations;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder width(Integer width) {
      this.width = width;
      return this;
    }

    public Builder height(Integer height) {
      this.height = height;
      return this;
    }

    public Builder moderations(WatsonxAiImageRequest.ModerationsInput moderations) {
      this.moderations = moderations;
      return this;
    }

    public WatsonxAiImageOptions build() {
      return new WatsonxAiImageOptions(this);
    }
  }
}
