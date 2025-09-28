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

package io.github.springaicommunity.watsonx.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection properties to use watsonx.ai Services.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@ConfigurationProperties(WatsonxAiConnectionProperties.CONFIG_PREFIX)
public final class WatsonxAiConnectionProperties {
  public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai";

  private String baseUrl = "https://us-south.ml.cloud.ibm.com";

  private String apiKey;

  private String projectId;

  public String getApiKey() {
    return this.apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getBaseUrl() {
    return this.baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getProjectId() {
    return this.projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }
}
