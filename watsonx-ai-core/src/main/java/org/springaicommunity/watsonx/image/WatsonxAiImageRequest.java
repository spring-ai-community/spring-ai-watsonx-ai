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

/**
 * Request for the Watson AI Image Generation API. Full documentation can be found at <a
 * href="https://cloud.ibm.com/apidocs/watsonx-ai#text-image">Watson AI Text to Image</a>.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiImageRequest {

  @JsonProperty("input")
  private String input;

  @JsonProperty("model_id")
  private String model;

  @JsonProperty("project_id")
  private String projectId;

  @JsonProperty("parameters")
  private ImageParameters parameters;

  public WatsonxAiImageRequest() {}

  private WatsonxAiImageRequest(Builder builder) {
    this.input = builder.input;
    this.model = builder.model;
    this.projectId = builder.projectId;
    this.parameters = builder.parameters;
  }

  public String input() {
    return input;
  }

  public String model() {
    return model;
  }

  public String projectId() {
    return projectId;
  }

  public ImageParameters parameters() {
    return parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .input(this.input)
        .model(this.model)
        .projectId(this.projectId)
        .parameters(this.parameters);
  }

  public static class Builder {
    private String input;
    private String model;
    private String projectId;
    private ImageParameters parameters;

    private Builder() {}

    public Builder input(String input) {
      this.input = input;
      return this;
    }

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder parameters(ImageParameters parameters) {
      this.parameters = parameters;
      return this;
    }

    public WatsonxAiImageRequest build() {
      return new WatsonxAiImageRequest(this);
    }
  }

  /** Parameters for image generation. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ImageParameters {
    @JsonProperty("n")
    private final Integer n;

    @JsonProperty("width")
    private final Integer width;

    @JsonProperty("height")
    private final Integer height;

    @JsonProperty("response_format")
    private final String responseFormat;

    @JsonProperty("style")
    private final String style;

    @JsonProperty("negative_prompt")
    private final String negativePrompt;

    @JsonProperty("steps")
    private final Integer steps;

    @JsonProperty("guidance_scale")
    private final Double guidanceScale;

    @JsonProperty("seed")
    private final Long seed;

    public ImageParameters(
        Integer n,
        Integer width,
        Integer height,
        String responseFormat,
        String style,
        String negativePrompt,
        Integer steps,
        Double guidanceScale,
        Long seed) {
      this.n = n;
      this.width = width;
      this.height = height;
      this.responseFormat = responseFormat;
      this.style = style;
      this.negativePrompt = negativePrompt;
      this.steps = steps;
      this.guidanceScale = guidanceScale;
      this.seed = seed;
    }

    public Integer n() {
      return n;
    }

    public Integer width() {
      return width;
    }

    public Integer height() {
      return height;
    }

    public String responseFormat() {
      return responseFormat;
    }

    public String style() {
      return style;
    }

    public String negativePrompt() {
      return negativePrompt;
    }

    public Integer steps() {
      return steps;
    }

    public Double guidanceScale() {
      return guidanceScale;
    }

    public Long seed() {
      return seed;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private Integer n;
      private Integer width;
      private Integer height;
      private String responseFormat;
      private String style;
      private String negativePrompt;
      private Integer steps;
      private Double guidanceScale;
      private Long seed;

      private Builder() {}

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

      public Builder negativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
        return this;
      }

      public Builder steps(Integer steps) {
        this.steps = steps;
        return this;
      }

      public Builder guidanceScale(Double guidanceScale) {
        this.guidanceScale = guidanceScale;
        return this;
      }

      public Builder seed(Long seed) {
        this.seed = seed;
        return this;
      }

      public ImageParameters build() {
        return new ImageParameters(
            n, width, height, responseFormat, style, negativePrompt, steps, guidanceScale, seed);
      }
    }
  }
}
