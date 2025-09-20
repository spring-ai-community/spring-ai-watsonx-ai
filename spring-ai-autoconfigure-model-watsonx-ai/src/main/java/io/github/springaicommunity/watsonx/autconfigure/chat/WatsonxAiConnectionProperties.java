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

package io.github.springaicommunity.watsonx.autconfigure.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(WatsonxAiConnectionProperties.CONFIG_PREFIX)
public class WatsonxAiConnectionProperties {
  public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai";

  private String baseUrl = "https://us-south.ml.cloud.ibm.com";

  private String chatEndpoint = "/ml/v1/text/chat";

  private String streamEndpoint = "/ml/v1/text/chat_stream";

  private String embeddingEndpoint = "/ml/v1/text/embeddings";

  private String version = "2024-10-17";

  private String projectId;

  private String IAMToken;

  public String getBaseUrl() {
    return this.baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getChatEndpoint() {
    return this.chatEndpoint;
  }

  public void setChatEndpoint(String chatEndpoint) {
    this.chatEndpoint = chatEndpoint;
  }

  public String getStreamEndpoint() {
    return this.streamEndpoint;
  }

  public void setStreamEndpoint(String streamEndpoint) {
    this.streamEndpoint = streamEndpoint;
  }

  public String getEmbeddingEndpoint() {
    return this.embeddingEndpoint;
  }

  public void setEmbeddingEndpoint(String embeddingEndpoint) {
    this.embeddingEndpoint = embeddingEndpoint;
  }

  public String getVersion() {
    return this.version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getProjectId() {
    return this.projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getIAMToken() {
    return this.IAMToken;
  }

  public void setIAMToken(String IAMToken) {
    this.IAMToken = IAMToken;
  }
}
