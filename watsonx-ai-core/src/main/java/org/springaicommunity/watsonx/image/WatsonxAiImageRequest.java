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
  private TextImageParameters parameters;

  @JsonProperty("moderations")
  private ModerationsInput moderations;

  public WatsonxAiImageRequest() {}

  private WatsonxAiImageRequest(Builder builder) {
    this.input = builder.input;
    this.model = builder.model;
    this.projectId = builder.projectId;
    this.parameters = builder.parameters;
    this.moderations = builder.moderations;
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

  public TextImageParameters parameters() {
    return parameters;
  }

  public ModerationsInput moderations() {
    return moderations;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder()
        .input(this.input)
        .model(this.model)
        .projectId(this.projectId)
        .parameters(this.parameters)
        .moderations(this.moderations);
  }

  public static class Builder {
    private String input;
    private String model;
    private String projectId;
    private TextImageParameters parameters;
    private ModerationsInput moderations;

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

    public Builder parameters(TextImageParameters parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder moderations(ModerationsInput moderations) {
      this.moderations = moderations;
      return this;
    }

    public WatsonxAiImageRequest build() {
      return new WatsonxAiImageRequest(this);
    }
  }

  /** Parameters for image generation. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TextImageParameters(
      @JsonProperty("width") Integer width, @JsonProperty("height") Integer height) {

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private Integer width;
      private Integer height;

      private Builder() {}

      public Builder width(Integer width) {
        this.width = width;
        return this;
      }

      public Builder height(Integer height) {
        this.height = height;
        return this;
      }

      public TextImageParameters build() {
        return new TextImageParameters(width, height);
      }
    }
  }

  /** Moderation settings for image generation. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ModerationsInput(
      @JsonProperty("hap") ModerationsInputProperties hap,
      @JsonProperty("pii") ModerationsInputProperties pii) {

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private ModerationsInputProperties hap;
      private ModerationsInputProperties pii;

      private Builder() {}

      public Builder hap(ModerationsInputProperties hap) {
        this.hap = hap;
        return this;
      }

      public Builder pii(ModerationsInputProperties pii) {
        this.pii = pii;
        return this;
      }

      public ModerationsInput build() {
        return new ModerationsInput(hap, pii);
      }
    }
  }

  /** Moderation input properties for image generation. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ModerationsInputProperties(
      @JsonProperty("input") TextModeration input, @JsonProperty("mask") MaskProperties mask) {}

  /** Text moderation configuration for image generation. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TextModeration(
      @JsonProperty("enabled") Boolean enabled, @JsonProperty("threshold") Double threshold) {}

  /** Mask properties for image generation. */
  public record MaskProperties(@JsonProperty("remove_entity_value") Boolean removeEntityValue) {}
}
