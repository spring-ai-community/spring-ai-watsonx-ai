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

package org.springaicommunity.watsonx.textextraction.observation;

import io.micrometer.observation.Observation;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionOptions;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionResponse;
import org.springframework.ai.observation.AiOperationMetadata;
import org.springframework.util.Assert;

/**
 * Context used to store metadata for text extraction model exchanges.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public class TextExtractionModelObservationContext extends Observation.Context {

	public static final String OPERATION_TYPE = "text_extraction";

	private final String documentName;

	private final WatsonxAiTextExtractionOptions options;

	private final AiOperationMetadata operationMetadata;

	private WatsonxAiTextExtractionResponse response;

	TextExtractionModelObservationContext(String documentName, WatsonxAiTextExtractionOptions options,
			String provider) {
		this.documentName = documentName;
		this.options = options;
		this.operationMetadata = AiOperationMetadata.builder().operationType(OPERATION_TYPE).provider(provider).build();
	}

	public String getDocumentName() {
		return documentName;
	}

	public WatsonxAiTextExtractionOptions getOptions() {
		return options;
	}

	public AiOperationMetadata getOperationMetadata() {
		return operationMetadata;
	}

	public WatsonxAiTextExtractionResponse getResponse() {
		return response;
	}

	public void setResponse(WatsonxAiTextExtractionResponse response) {
		Assert.notNull(response, "response cannot be null");
		this.response = response;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private String documentName;

		private WatsonxAiTextExtractionOptions options;

		private String provider;

		private Builder() {
		}

		public Builder documentName(String documentName) {
			this.documentName = documentName;
			return this;
		}

		public Builder options(WatsonxAiTextExtractionOptions options) {
			this.options = options;
			return this;
		}

		public Builder provider(String provider) {
			this.provider = provider;
			return this;
		}

		public TextExtractionModelObservationContext build() {
			Assert.hasText(this.provider, "provider cannot be null or empty");
			return new TextExtractionModelObservationContext(this.documentName, this.options, this.provider);
		}

	}

}
