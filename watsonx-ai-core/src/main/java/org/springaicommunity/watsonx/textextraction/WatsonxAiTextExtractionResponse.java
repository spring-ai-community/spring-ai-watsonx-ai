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

package org.springaicommunity.watsonx.textextraction;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response from the watsonx.ai Text Extraction API. Full documentation can be found at
 * <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-extractions">watsonx.ai Text
 * Extractions</a>.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiTextExtractionResponse(@JsonProperty("metadata") ExtractionMetadata metadata,
		@JsonProperty("entity") ExtractionEntity entity, @JsonProperty("text") String text,
		@JsonProperty("pages") List<ExtractedPage> pages, @JsonProperty("results") Map<String, Object> results) {

	public String getText() {
		if (this.text != null) {
			return this.text;
		}
		if (this.pages != null && !this.pages.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (ExtractedPage page : this.pages) {
				if (page.text() != null) {
					if (!sb.isEmpty()) {
						sb.append("\n\n");
					}
					sb.append(page.text());
				}
			}
			return sb.toString();
		}
		return "";
	}

	public String getId() {
		return (this.metadata != null) ? this.metadata.id() : null;
	}

	/**
	 * The extraction job status: {@code submitted}, {@code uploading}, {@code running},
	 * {@code downloading}, {@code downloaded}, {@code completed} or {@code failed}.
	 * @return the status, or {@code null} when the response has none
	 */
	public String getStatus() {
		if (this.entity != null && this.entity.results() != null && this.entity.results().status() != null) {
			return this.entity.results().status();
		}
		if (this.entity != null && this.entity.status() != null) {
			return this.entity.status().state();
		}
		return null;
	}

	/**
	 * @return the number of pages processed so far, or {@code null} when unknown
	 */
	public Integer getNumberPagesProcessed() {
		return (this.entity != null && this.entity.results() != null) ? this.entity.results().numberPagesProcessed()
				: null;
	}

	/**
	 * @return the error of a failed extraction job, or {@code null}
	 */
	public ExtractionError getError() {
		return (this.entity != null && this.entity.results() != null) ? this.entity.results().error() : null;
	}

	/**
	 * Returns a copy of this response with the given extracted text.
	 * @param text the extracted text
	 * @return the new response
	 */
	public WatsonxAiTextExtractionResponse withText(String text) {
		return new WatsonxAiTextExtractionResponse(this.metadata, this.entity, text, this.pages, this.results);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ExtractionMetadata(@JsonProperty("id") String id, @JsonProperty("created_at") LocalDateTime createdAt,
			@JsonProperty("modified_at") LocalDateTime modifiedAt) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ExtractionEntity(@JsonProperty("status") ExtractionStatus status,
			@JsonProperty("document_reference") WatsonxAiTextExtractionRequest.DocumentReference documentReference,
			@JsonProperty("results_reference") WatsonxAiTextExtractionRequest.DocumentReference resultsReference,
			@JsonProperty("results") ExtractionResults results) {

		public ExtractionEntity(ExtractionStatus status,
				WatsonxAiTextExtractionRequest.DocumentReference documentReference,
				WatsonxAiTextExtractionRequest.DocumentReference resultsReference) {
			this(status, documentReference, resultsReference, null);
		}
	}

	/**
	 * Status of an extraction job, as returned in {@code entity.results}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ExtractionResults(@JsonProperty("status") String status,
			@JsonProperty("number_pages_processed") Integer numberPagesProcessed,
			@JsonProperty("error") ExtractionError error) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ExtractionError(@JsonProperty("code") String code, @JsonProperty("message") String message) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ExtractionStatus(@JsonProperty("state") String state, @JsonProperty("message") String message) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ExtractedPage(@JsonProperty("page_number") Integer pageNumber, @JsonProperty("text") String text,
			@JsonProperty("tables") List<Map<String, Object>> tables,
			@JsonProperty("metadata") Map<String, Object> metadata) {
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private ExtractionMetadata metadata;

		private ExtractionEntity entity;

		private String text;

		private List<ExtractedPage> pages;

		private Map<String, Object> results;

		private Builder() {
		}

		public Builder metadata(ExtractionMetadata metadata) {
			this.metadata = metadata;
			return this;
		}

		public Builder entity(ExtractionEntity entity) {
			this.entity = entity;
			return this;
		}

		public Builder text(String text) {
			this.text = text;
			return this;
		}

		public Builder pages(List<ExtractedPage> pages) {
			this.pages = pages;
			return this;
		}

		public Builder results(Map<String, Object> results) {
			this.results = results;
			return this;
		}

		public WatsonxAiTextExtractionResponse build() {
			return new WatsonxAiTextExtractionResponse(this.metadata, this.entity, this.text, this.pages, this.results);
		}

	}

}
