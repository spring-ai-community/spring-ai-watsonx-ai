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

package io.github.springaicommunity.watsonx.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * Request for the Watsonx AI Chat API.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiChatRequest {

  @JsonProperty("model_id")
  private String modelId = "";

  @JsonProperty("project_id")
  private String projectId = "";

  private WatsonxAiChatRequest(String modelId, String projectId) {
    this.modelId = modelId;
    this.projectId = projectId;
  }

  public static Builder builder(String input) {
    return new Builder(input);
  }

  public WatsonxAiChatRequest withProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getModelId() {
    return this.modelId;
  }

  public static class Builder {
    public static final String MODEL_PARAMETER_IS_REQUIRED = "Model parameter is required";
    private final String input;
    private Map<String, Object> parameters;
    private String model = "";

    public Builder(String input) {
      this.input = input;
    }

    public Builder withParameters(Map<String, Object> parameters) {
      Assert.notNull(parameters.get("model"), MODEL_PARAMETER_IS_REQUIRED);
      this.model = parameters.get("model").toString();
      this.parameters = WatsonxAiChatOptions.filterNonSupportedFields(parameters);
      return this;
    }
  }
}
