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

package org.springaicommunity.watsonx.autoconfigure.image;

import org.springaicommunity.watsonx.image.WatsonxAiImageOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for watsonx.ai Image Generation Model.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@ConfigurationProperties(WatsonxAiImageProperties.CONFIG_PREFIX)
public class WatsonxAiImageProperties {

  public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.image";

  private String imageEndpoint = "/ml/v1/text/image";

  /**
   * API version date to use, in YYYY-MM-DD format. Example: 2024-10-17. See the <a
   * href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API versioning</a>
   */
  private String version = "2024-10-17";

  /**
   * The default options to use when calling the watsonx.ai Image Generation API. These can be
   * overridden by passing options in the request.
   */
  @NestedConfigurationProperty
  private WatsonxAiImageOptions options =
      WatsonxAiImageOptions.builder()
          .model("meta-llama/llama-3-2-11b-vision-instruct")
          .width(1024)
          .height(1024)
          .build();

  public String getImageEndpoint() {
    return imageEndpoint;
  }

  public void setImageEndpoint(String imageEndpoint) {
    this.imageEndpoint = imageEndpoint;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public WatsonxAiImageOptions getOptions() {
    return options;
  }

  public void setOptions(WatsonxAiImageOptions options) {
    this.options = options;
  }
}
