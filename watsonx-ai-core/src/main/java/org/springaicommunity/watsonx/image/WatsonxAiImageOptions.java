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

  /** The number of images to generate. Default is 1. */
  @JsonProperty("n")
  private Integer n;

  /** The width of the generated image in pixels. */
  @JsonProperty("width")
  private Integer width;

  /** The height of the generated image in pixels. */
  @JsonProperty("height")
  private Integer height;

  /** The response format for the generated images. Typically "url" or "b64_json". */
  @JsonProperty("response_format")
  private String responseFormat;

  /** The style preset to use for image generation (optional). */
  @JsonProperty("style")
  private String style;

  public WatsonxAiImageOptions() {}

  private WatsonxAiImageOptions(Builder builder) {
    this.model = builder.model;
    this.n = builder.n;
    this.width = builder.width;
    this.height = builder.height;
    this.responseFormat = builder.responseFormat;
    this.style = builder.style;
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
    return n;
  }

  public void setN(Integer n) {
    this.n = n;
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

  @Override
  public String getResponseFormat() {
    return responseFormat;
  }

  public void setResponseFormat(String responseFormat) {
    this.responseFormat = responseFormat;
  }

  public String getStyle() {
    return style;
  }

  public void setStyle(String style) {
    this.style = style;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .model(this.model)
        .n(this.n)
        .width(this.width)
        .height(this.height)
        .responseFormat(this.responseFormat)
        .style(this.style);
  }

  public static WatsonxAiImageOptions fromOptions(WatsonxAiImageOptions fromOptions) {
    return builder()
        .model(fromOptions.getModel())
        .n(fromOptions.getN())
        .width(fromOptions.getWidth())
        .height(fromOptions.getHeight())
        .responseFormat(fromOptions.getResponseFormat())
        .style(fromOptions.getStyle())
        .build();
  }

  public static class Builder {
    private String model;
    private Integer n;
    private Integer width;
    private Integer height;
    private String responseFormat;
    private String style;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder n(Integer n) {
      this.n = n;
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

    public Builder responseFormat(String responseFormat) {
      this.responseFormat = responseFormat;
      return this;
    }

    public Builder style(String style) {
      this.style = style;
      return this;
    }

    public WatsonxAiImageOptions build() {
      return new WatsonxAiImageOptions(this);
    }
  }
}
