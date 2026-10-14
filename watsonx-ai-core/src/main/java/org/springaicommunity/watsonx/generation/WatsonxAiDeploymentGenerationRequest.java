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
 * Request to a watsonx.ai text generation deployment, such as a deployed freeform prompt
 * template. The prompt is in the deployment, so the request only has the values of its
 * variables and the generation parameters: watsonx.ai rejects {@code input},
 * {@code project_id} and {@code space_id} here.
 *
 * @param parameters the prompt variables and generation parameters
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiDeploymentGenerationRequest(@JsonProperty("parameters") Parameters parameters) {

	/**
	 * The prompt variables and generation parameters.
	 *
	 * @param promptVariables the values of the prompt template's variables
	 * @param decodingMethod {@code greedy} or {@code sample}
	 * @param maxNewTokens the maximum number of tokens to generate
	 * @param minNewTokens the minimum number of tokens to generate
	 * @param temperature the sampling temperature
	 * @param topP the cumulative probability of the tokens to sample from
	 * @param topK the number of most likely tokens to sample from
	 * @param repetitionPenalty the penalty for repeated tokens
	 * @param randomSeed the seed for sampling
	 * @param stopSequences sequences that stop the generation
	 * @param timeLimit the time limit for the generation, in milliseconds
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Parameters(@JsonProperty("prompt_variables") Map<String, String> promptVariables,
			@JsonProperty("decoding_method") String decodingMethod,
			@JsonProperty("max_new_tokens") Integer maxNewTokens, @JsonProperty("min_new_tokens") Integer minNewTokens,
			@JsonProperty("temperature") Double temperature, @JsonProperty("top_p") Double topP,
			@JsonProperty("top_k") Integer topK, @JsonProperty("repetition_penalty") Double repetitionPenalty,
			@JsonProperty("random_seed") Integer randomSeed, @JsonProperty("stop_sequences") List<String> stopSequences,
			@JsonProperty("time_limit") Long timeLimit) {
	}

}
