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

package org.springaicommunity.watsonx.moderation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.moderation.ModerationOptions;

/**
 * Watsonx AI Moderation API options. WatsonxAiModerationOptions.java
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatsonxAiModerationOptions implements ModerationOptions {

  /**
   * The model to use for moderation (not used in text detection API, kept for interface
   * compatibility).
   */
  private String model = "granite_guardian";

  /** HAP (Hate, Abuse, Profanity) detector configuration. */
  @JsonProperty("hap")
  private WatsonxAiModerationRequest.DetectorConfig hap;

  /** PII (Personally Identifiable Information) detector configuration. */
  @JsonProperty("pii")
  private WatsonxAiModerationRequest.DetectorConfig pii;

  /** Granite Guardian detector configuration. */
  @JsonProperty("granite_guardian")
  private WatsonxAiModerationRequest.DetectorConfig graniteGuardian;

  public WatsonxAiModerationOptions() {}

  private WatsonxAiModerationOptions(Builder builder) {
    this.model = builder.model;
    this.hap = builder.hap;
    this.pii = builder.pii;
    this.graniteGuardian = builder.graniteGuardian;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String getModel() {
    return this.model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public WatsonxAiModerationRequest.DetectorConfig getHap() {
    return hap;
  }

  public void setHap(WatsonxAiModerationRequest.DetectorConfig hap) {
    this.hap = hap;
  }

  public WatsonxAiModerationRequest.DetectorConfig getPii() {
    return pii;
  }

  public void setPii(WatsonxAiModerationRequest.DetectorConfig pii) {
    this.pii = pii;
  }

  public WatsonxAiModerationRequest.DetectorConfig getGraniteGuardian() {
    return graniteGuardian;
  }

  public void setGraniteGuardian(WatsonxAiModerationRequest.DetectorConfig graniteGuardian) {
    this.graniteGuardian = graniteGuardian;
  }

  public WatsonxAiModerationRequest.Detectors toDetectors() {
    return WatsonxAiModerationRequest.Detectors.builder()
        .hap(this.hap)
        .pii(this.pii)
        .graniteGuardian(this.graniteGuardian)
        .build();
  }

  public static final class Builder {

    private String model;
    private WatsonxAiModerationRequest.DetectorConfig hap;
    private WatsonxAiModerationRequest.DetectorConfig pii;
    private WatsonxAiModerationRequest.DetectorConfig graniteGuardian;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder hap(WatsonxAiModerationRequest.DetectorConfig hap) {
      this.hap = hap;
      return this;
    }

    /** Convenience method to enable HAP detector with the given threshold. */
    public Builder hap(Float threshold) {
      this.hap = WatsonxAiModerationRequest.DetectorConfig.of(threshold);
      return this;
    }

    public Builder pii(WatsonxAiModerationRequest.DetectorConfig pii) {
      this.pii = pii;
      return this;
    }

    /** Convenience method to enable PII detector with the given threshold. */
    public Builder pii(Float threshold) {
      this.pii = WatsonxAiModerationRequest.DetectorConfig.of(threshold);
      return this;
    }

    public Builder graniteGuardian(WatsonxAiModerationRequest.DetectorConfig graniteGuardian) {
      this.graniteGuardian = graniteGuardian;
      return this;
    }

    /** Convenience method to enable Granite Guardian detector with the given threshold. */
    public Builder graniteGuardian(Float threshold) {
      this.graniteGuardian = WatsonxAiModerationRequest.DetectorConfig.of(threshold);
      return this;
    }

    public WatsonxAiModerationOptions build() {
      return new WatsonxAiModerationOptions(this);
    }
  }
}
