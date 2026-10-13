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

package org.springaicommunity.watsonx.rerank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response from the watsonx.ai Rerank API. Full documentation can be found at
 * <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-rerank">watsonx.ai Text
 * Rerank</a>.
 *
 * @author Federico Mariani
 * @since 1.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiRerankResponse(@JsonProperty("model_id") String model,
		@JsonProperty("model_version") String modelVersion, @JsonProperty("results") List<RerankResult> results,
		@JsonProperty("created_at") LocalDateTime createdAt, @JsonProperty("input_token_count") Integer inputTokenCount,
		@JsonProperty("query") String query, @JsonProperty("system") SystemDetails system) {

	/**
	 * Creates a response without system details.
	 * @param model the model ID
	 * @param modelVersion the model version
	 * @param results the rerank results
	 * @param createdAt when the response was created
	 * @param inputTokenCount the number of input tokens
	 * @param query the query
	 */
	public WatsonxAiRerankResponse(String model, String modelVersion, List<RerankResult> results,
			LocalDateTime createdAt, Integer inputTokenCount, String query) {
		this(model, modelVersion, results, createdAt, inputTokenCount, query, null);
	}

	/** Individual rerank result containing the document index and relevance score. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RerankResult(@JsonProperty("index") Integer index, @JsonProperty("score") Double score,
			@JsonProperty("input") RerankInputResult input) {
	}

	/** The input text returned when return_options.inputs is true. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RerankInputResult(@JsonProperty("text") String text) {
	}

	/**
	 * Optional details coming from the service and related to the API call or the
	 * associated resource.
	 *
	 * @since 2.1.0
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SystemDetails(@JsonProperty("warnings") List<Warning> warnings) {
	}

	/**
	 * Any warnings coming from the system.
	 *
	 * @since 2.1.0
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Warning(@JsonProperty("message") String message, @JsonProperty("id") String id,
			@JsonProperty("more_info") String moreInfo,
			@JsonProperty("additional_properties") Map<String, Object> additionalProperties) {
	}
}
