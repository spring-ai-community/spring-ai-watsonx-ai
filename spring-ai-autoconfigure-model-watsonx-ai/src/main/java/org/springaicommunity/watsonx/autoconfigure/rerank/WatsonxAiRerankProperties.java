/*
 * Copyright 2026 the original author or authors.
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

package org.springaicommunity.watsonx.autoconfigure.rerank;

import org.springaicommunity.watsonx.rerank.WatsonxAiRerankOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for watsonx.ai Rerank Model.
 *
 * @author Federico Mariani
 * @since 1.1.0
 */
@ConfigurationProperties(WatsonxAiRerankProperties.CONFIG_PREFIX)
public class WatsonxAiRerankProperties {

  public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.rerank";

  /** Enable or disable the watsonx.ai Rerank auto-configuration. */
  private boolean enabled = true;

  /** The endpoint for the rerank API. */
  private String rerankEndpoint = "/ml/v1/text/rerank";

  /**
   * API version date to use, in YYYY-MM-DD format. Example: 2024-05-31. See the <a
   * href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API versioning</a>
   */
  private String version = "2024-05-31";

  /**
   * The default options to use when calling the watsonx.ai Rerank API. These can be overridden by
   * passing options in the request.
   */
  @NestedConfigurationProperty
  private WatsonxAiRerankOptions options =
      WatsonxAiRerankOptions.builder()
          .model("cross-encoder/ms-marco-minilm-l-12-v2")
          .truncateInputTokens(512)
          .build();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getRerankEndpoint() {
    return rerankEndpoint;
  }

  public void setRerankEndpoint(String rerankEndpoint) {
    this.rerankEndpoint = rerankEndpoint;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public WatsonxAiRerankOptions getOptions() {
    return options;
  }

  public void setOptions(WatsonxAiRerankOptions options) {
    this.options = options;
  }
}
