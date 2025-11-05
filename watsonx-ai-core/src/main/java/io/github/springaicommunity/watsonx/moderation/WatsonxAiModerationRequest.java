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

package io.github.springaicommunity.watsonx.moderation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request for the Watsonx AI Text Detection/Moderation API. Full documentation can be found at <a
 * href="https://cloud.ibm.com/apidocs/watsonx-ai#text-detection">Watsonx AI Text Detection</a>.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiModerationRequest {

  @JsonProperty("input")
  private String input;

  @JsonProperty("project_id")
  private String projectId;

  @JsonProperty("detectors")
  private Detectors detectors;

  public WatsonxAiModerationRequest() {}

  private WatsonxAiModerationRequest(Builder builder) {
    this.input = builder.input;
    this.projectId = builder.projectId;
    this.detectors = builder.detectors;
  }

  public String input() {
    return input;
  }

  public String projectId() {
    return projectId;
  }

  public Detectors detectors() {
    return detectors;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder().input(this.input).projectId(this.projectId).detectors(this.detectors);
  }

  public static class Builder {
    private String input;
    private String projectId;
    private Detectors detectors;

    private Builder() {}

    public Builder input(String input) {
      this.input = input;
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder detectors(Detectors detectors) {
      this.detectors = detectors;
      return this;
    }

    public WatsonxAiModerationRequest build() {
      return new WatsonxAiModerationRequest(this);
    }
  }

  /**
   * Detector configuration for text detection/moderation. Supports HAP (Hate, Abuse, Profanity),
   * PII (Personally Identifiable Information) and Granite Guardian detectors.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Detectors(
      @JsonProperty("hap") DetectorConfig hap,
      @JsonProperty("pii") DetectorConfig pii,
      @JsonProperty("granite_guardian") DetectorConfig graniteGuardian) {

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private DetectorConfig hap;
      private DetectorConfig pii;
      private DetectorConfig graniteGuardian;

      public Builder hap(DetectorConfig hap) {
        this.hap = hap;
        return this;
      }

      public Builder pii(DetectorConfig pii) {
        this.pii = pii;
        return this;
      }

      public Builder graniteGuardian(DetectorConfig graniteGuardian) {
        this.graniteGuardian = graniteGuardian;
        return this;
      }

      public Detectors build() {
        return new Detectors(hap, pii, graniteGuardian);
      }
    }
  }

  /** Configuration for individual detector with optional threshold. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record DetectorConfig(@JsonProperty("threshold") Float threshold) {

    public static DetectorConfig of(Float threshold) {
      return new DetectorConfig(threshold);
    }

    public static DetectorConfig enabled() {
      return new DetectorConfig(null);
    }
  }
}
