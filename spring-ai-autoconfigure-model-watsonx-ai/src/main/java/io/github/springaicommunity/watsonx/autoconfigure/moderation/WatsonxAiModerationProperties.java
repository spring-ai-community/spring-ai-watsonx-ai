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

package io.github.springaicommunity.watsonx.autoconfigure.moderation;

import io.github.springaicommunity.watsonx.moderation.WatsonxAiModerationOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for Watsonx AI Moderation/Text Detection Model.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
@ConfigurationProperties(WatsonxAiModerationProperties.CONFIG_PREFIX)
public class WatsonxAiModerationProperties {

  public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.moderation";

  /** Text detection endpoint used for moderation. */
  private String textDetectionEndpoint = "/ml/v1/text/detection";

  /**
   * API version date to use, in YYYY-MM-DD format. Example: 2025-10-01. See the <a
   * href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API versioning</a>
   */
  private String version = "2025-10-01";

  /**
   * The default options to use when calling the Watsonx AI Text Detection/Moderation API. These can
   * be overridden by passing options in the request.
   */
  @NestedConfigurationProperty private WatsonxAiModerationOptions options;

  public String getTextDetectionEndpoint() {
    return textDetectionEndpoint;
  }

  public void setTextDetectionEndpoint(String textDetectionEndpoint) {
    this.textDetectionEndpoint = textDetectionEndpoint;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public WatsonxAiModerationOptions getOptions() {
    return options;
  }

  public void setOptions(WatsonxAiModerationOptions options) {
    this.options = options;
  }
}
