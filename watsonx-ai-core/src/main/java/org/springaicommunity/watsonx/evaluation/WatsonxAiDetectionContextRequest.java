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
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * Request of the watsonx.ai detection on context documents
 * ({@code POST /ml/v1/text/detection/context}).
 *
 * @param input the text to examine: a question for context relevance, an answer for
 * faithfulness
 * @param detectors the detectors to run, by name, such as {@code context_relevance} or
 * {@code faithfulness}, with their properties
 * @param contextType the type of the context, always {@code docs}
 * @param context the context documents, as text (1 to 100)
 * @param projectId the project ID, set by {@link WatsonxAiDetectionApi}
 * @param spaceId the space ID, set by {@link WatsonxAiDetectionApi}
 * @author Federico Mariani
 * @since 2.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiDetectionContextRequest(@JsonProperty("input") String input,
		@JsonProperty("detectors") Map<String, Map<String, Object>> detectors,
		@JsonProperty("context_type") String contextType, @JsonProperty("context") List<String> context,
		@JsonProperty("project_id") String projectId, @JsonProperty("space_id") String spaceId) {

	/** The only context type of the API. */
	public static final String DOCS_CONTEXT_TYPE = "docs";

	public WatsonxAiDetectionContextRequest {
		Assert.hasText(input, "input must not be empty");
		Assert.notEmpty(detectors, "detectors must not be empty");
		Assert.notEmpty(context, "context must not be empty");
	}

	/**
	 * Creates a request with {@code docs} context.
	 * @param input the text to examine
	 * @param detectors the detectors to run, by name, with their properties
	 * @param context the context documents, as text
	 */
	public WatsonxAiDetectionContextRequest(String input, Map<String, Map<String, Object>> detectors,
			List<String> context) {
		this(input, detectors, DOCS_CONTEXT_TYPE, context, null, null);
	}

	WatsonxAiDetectionContextRequest withScope(String projectId, String spaceId) {
		return new WatsonxAiDetectionContextRequest(this.input, this.detectors, this.contextType, this.context,
				projectId, spaceId);
	}

}
