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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Request payload for the watsonx.ai Text Extraction API. Full documentation can be found
 * at <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-extractions">watsonx.ai Text
 * Extractions</a>.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiTextExtractionRequest {

	@JsonProperty("project_id")
	private String projectId;

	@JsonProperty("space_id")
	private String spaceId;

	@JsonProperty("document_reference")
	private DocumentReference documentReference;

	@JsonProperty("results_reference")
	private DocumentReference resultsReference;

	@JsonProperty("parameters")
	private ExtractionParameters parameters;

	@JsonIgnore
	private Resource resource;

	public WatsonxAiTextExtractionRequest() {
	}

	private WatsonxAiTextExtractionRequest(Builder builder) {
		this.projectId = builder.projectId;
		this.spaceId = builder.spaceId;
		this.documentReference = builder.documentReference;
		this.resultsReference = builder.resultsReference;
		this.parameters = builder.parameters;
		this.resource = builder.resource;
	}

	public String projectId() {
		return projectId;
	}

	public String spaceId() {
		return spaceId;
	}

	public DocumentReference documentReference() {
		return documentReference;
	}

	public DocumentReference resultsReference() {
		return resultsReference;
	}

	public ExtractionParameters parameters() {
		return parameters;
	}

	public Resource resource() {
		return resource;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().projectId(this.projectId)
			.spaceId(this.spaceId)
			.documentReference(this.documentReference)
			.resultsReference(this.resultsReference)
			.parameters(this.parameters)
			.resource(this.resource);
	}

	/**
	 * Reference to an input document or output results location in watsonx.ai.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DocumentReference(@JsonProperty("type") String type,
			@JsonProperty("connection") Connection connection, @JsonProperty("location") Map<String, Object> location) {

		public static DocumentReference ofConnectionAsset(String connectionId, String bucket, String filePath) {
			return new DocumentReference("connection_asset", new Connection(connectionId),
					Map.of("bucket", bucket, "file_name", filePath));
		}

		public static DocumentReference ofContainer(String filePath) {
			return new DocumentReference("container", null, Map.of("path", filePath));
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Connection(@JsonProperty("id") String id) {
	}

	/**
	 * Parameters passed to watsonx.ai Text Extraction API.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ExtractionParameters(@JsonProperty("model_id") String model,
			@JsonProperty("output_formats") List<String> outputFormats,
			@JsonProperty("languages") List<String> languages, @JsonProperty("enable_ocr") Boolean enableOcr) {

		public static ExtractionParameters of(WatsonxAiTextExtractionOptions options) {
			if (options == null) {
				return null;
			}
			return new ExtractionParameters(options.getModel(), options.getOutputFormats(), options.getLanguages(),
					options.getEnableOcr());
		}
	}

	public static final class Builder {

		private String projectId;

		private String spaceId;

		private DocumentReference documentReference;

		private DocumentReference resultsReference;

		private ExtractionParameters parameters;

		private Resource resource;

		private Builder() {
		}

		public Builder projectId(String projectId) {
			this.projectId = projectId;
			return this;
		}

		public Builder spaceId(String spaceId) {
			this.spaceId = spaceId;
			return this;
		}

		public Builder documentReference(DocumentReference documentReference) {
			this.documentReference = documentReference;
			return this;
		}

		public Builder resultsReference(DocumentReference resultsReference) {
			this.resultsReference = resultsReference;
			return this;
		}

		public Builder parameters(ExtractionParameters parameters) {
			this.parameters = parameters;
			return this;
		}

		public Builder resource(Resource resource) {
			this.resource = resource;
			return this;
		}

		public Builder documentBytes(byte[] bytes, String filename) {
			this.resource = new ByteArrayResource(bytes, filename) {
				@Override
				public String getFilename() {
					return filename;
				}
			};
			return this;
		}

		public Builder documentBytes(byte[] bytes) {
			return documentBytes(bytes, "document");
		}

		public WatsonxAiTextExtractionRequest build() {
			return new WatsonxAiTextExtractionRequest(this);
		}

	}

}
