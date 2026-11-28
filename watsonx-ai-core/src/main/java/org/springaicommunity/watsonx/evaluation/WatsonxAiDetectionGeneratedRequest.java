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

package org.springaicommunity.watsonx.evaluation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * Request of the watsonx.ai detection on generated text
 * ({@code POST /ml/v1/text/detection/generated}).
 *
 * @param prompt the prompt, such as the question of the user
 * @param generatedText the text generated for the prompt
 * @param detectors the detectors to run, by name, such as {@code answer_relevance}, with
 * their properties
 * @param projectId the project ID, set by {@link WatsonxAiDetectionApi}
 * @param spaceId the space ID, set by {@link WatsonxAiDetectionApi}
 * @author Federico Mariani
 * @since 2.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiDetectionGeneratedRequest(@JsonProperty("prompt") String prompt,
		@JsonProperty("generated_text") String generatedText,
		@JsonProperty("detectors") Map<String, Map<String, Object>> detectors,
		@JsonProperty("project_id") String projectId, @JsonProperty("space_id") String spaceId) {

	public WatsonxAiDetectionGeneratedRequest {
		Assert.hasText(prompt, "prompt must not be empty");
		Assert.hasText(generatedText, "generatedText must not be empty");
		Assert.notEmpty(detectors, "detectors must not be empty");
	}

	/**
	 * Creates a request.
	 * @param prompt the prompt
	 * @param generatedText the text generated for the prompt
	 * @param detectors the detectors to run, by name, with their properties
	 */
	public WatsonxAiDetectionGeneratedRequest(String prompt, String generatedText,
			Map<String, Map<String, Object>> detectors) {
		this(prompt, generatedText, detectors, null, null);
	}

	WatsonxAiDetectionGeneratedRequest withScope(String projectId, String spaceId) {
		return new WatsonxAiDetectionGeneratedRequest(this.prompt, this.generatedText, this.detectors, projectId,
				spaceId);
	}

}
