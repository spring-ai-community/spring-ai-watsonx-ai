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

package org.springaicommunity.watsonx.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Response of a watsonx.ai text generation deployment. A streamed response has one of
 * these per chunk: {@code generated_text} is the next part of the text, and
 * {@code stop_reason} is {@code not_finished} until the last chunk.
 *
 * @param modelId the model that generated the text
 * @param modelVersion the version of the model
 * @param createdAt when the response was created
 * @param results the generated text
 * @param system warnings from watsonx.ai, such as a deprecated parameter
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiDeploymentGenerationResponse(@JsonProperty("model_id") String modelId,
		@JsonProperty("model_version") String modelVersion, @JsonProperty("created_at") String createdAt,
		@JsonProperty("results") List<Result> results, @JsonProperty("system") SystemDetails system) {

	/**
	 * The generated text, from the first result, or an empty string if there is none.
	 * @return the generated text
	 */
	public String generatedText() {
		if (this.results == null || this.results.isEmpty() || this.results.get(0).generatedText() == null) {
			return "";
		}
		return this.results.get(0).generatedText();
	}

	/**
	 * One generation result.
	 *
	 * @param generatedText the generated text
	 * @param generatedTokenCount the number of generated tokens
	 * @param inputTokenCount the number of input tokens
	 * @param stopReason why the generation stopped, such as {@code eos_token},
	 * {@code max_tokens} or {@code not_finished}
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Result(@JsonProperty("generated_text") String generatedText,
			@JsonProperty("generated_token_count") Integer generatedTokenCount,
			@JsonProperty("input_token_count") Integer inputTokenCount,
			@JsonProperty("stop_reason") String stopReason) {
	}

	/**
	 * Warnings from watsonx.ai.
	 *
	 * @param warnings the warnings
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SystemDetails(@JsonProperty("warnings") List<Warning> warnings) {
	}

	/**
	 * A warning from watsonx.ai.
	 *
	 * @param message the message
	 * @param id the warning ID, such as {@code param_deprecation}
	 * @param moreInfo a link to more information
	 * @param additionalProperties details of the warning
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Warning(@JsonProperty("message") String message, @JsonProperty("id") String id,
			@JsonProperty("more_info") String moreInfo,
			@JsonProperty("additional_properties") Map<String, Object> additionalProperties) {
	}

}
