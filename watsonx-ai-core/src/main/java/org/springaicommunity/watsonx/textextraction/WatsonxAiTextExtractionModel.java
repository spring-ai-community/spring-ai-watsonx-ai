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

import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.textextraction.observation.DefaultTextExtractionModelObservationConvention;
import org.springaicommunity.watsonx.textextraction.observation.TextExtractionModelObservationContext;
import org.springaicommunity.watsonx.textextraction.observation.TextExtractionModelObservationConvention;
import org.springaicommunity.watsonx.textextraction.observation.TextExtractionModelObservationDocumentation;
import org.springframework.ai.document.Document;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Text extraction model implementation that provides access to watsonx.ai document text
 * extraction services.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public class WatsonxAiTextExtractionModel {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiTextExtractionModel.class);

	private static final TextExtractionModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultTextExtractionModelObservationConvention();

	private static final String PROVIDER = "watsonx-ai";

	public static final String DOCUMENT_NAME_METADATA_KEY = "document_name";

	public static final String PAGE_NUMBER_METADATA_KEY = "page_number";

	private final WatsonxAiTextExtractionApi watsonxAiTextExtractionApi;

	private final WatsonxAiTextExtractionOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	private final ObservationRegistry observationRegistry;

	private TextExtractionModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	public WatsonxAiTextExtractionModel(WatsonxAiTextExtractionApi watsonxAiTextExtractionApi,
			WatsonxAiTextExtractionOptions defaultOptions, ObservationRegistry observationRegistry,
			RetryTemplate retryTemplate) {
		Assert.notNull(watsonxAiTextExtractionApi, "WatsonxAiTextExtractionApi must not be null");
		Assert.notNull(defaultOptions, "WatsonxAiTextExtractionOptions must not be null");
		Assert.notNull(observationRegistry, "ObservationRegistry must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");
		this.watsonxAiTextExtractionApi = watsonxAiTextExtractionApi;
		this.defaultOptions = defaultOptions;
		this.observationRegistry = observationRegistry;
		this.retryTemplate = retryTemplate;
	}

	/**
	 * Extract text from a document request using default options.
	 * @param request text extraction request
	 * @return extraction response
	 */
	public WatsonxAiTextExtractionResponse extract(WatsonxAiTextExtractionRequest request) {
		return extract(request, null);
	}

	/**
	 * Extract text from a document request with optional runtime options.
	 * @param request text extraction request
	 * @param runtimeOptions runtime options to override defaults
	 * @return extraction response
	 */
	public WatsonxAiTextExtractionResponse extract(WatsonxAiTextExtractionRequest request,
			WatsonxAiTextExtractionOptions runtimeOptions) {
		Assert.notNull(request, "WatsonxAiTextExtractionRequest must not be null");

		WatsonxAiTextExtractionOptions mergedOptions = mergeOptions(runtimeOptions);
		String documentName = resolveDocumentName(request);

		TextExtractionModelObservationContext observationContext = TextExtractionModelObservationContext.builder()
			.documentName(documentName)
			.options(mergedOptions)
			.provider(PROVIDER)
			.build();

		return TextExtractionModelObservationDocumentation.TEXT_EXTRACTION_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {
				WatsonxAiTextExtractionResponse response = RetryUtils.execute(this.retryTemplate, () -> {
					WatsonxAiTextExtractionRequest finalRequest = request.toBuilder()
						.parameters(WatsonxAiTextExtractionRequest.ExtractionParameters.of(mergedOptions))
						.build();

					ResponseEntity<WatsonxAiTextExtractionResponse> apiResponse = this.watsonxAiTextExtractionApi
						.extract(finalRequest);

					return apiResponse.getBody();
				});

				if (response != null) {
					observationContext.setResponse(response);
				}

				return response;
			});
	}

	/**
	 * Extract text from a Spring {@link Resource} using default options.
	 * @param resource document resource
	 * @return extraction response
	 */
	public WatsonxAiTextExtractionResponse extract(Resource resource) {
		return extract(resource, null);
	}

	/**
	 * Extract text from a Spring {@link Resource} with runtime options.
	 * @param resource document resource
	 * @param runtimeOptions runtime options
	 * @return extraction response
	 */
	public WatsonxAiTextExtractionResponse extract(Resource resource, WatsonxAiTextExtractionOptions runtimeOptions) {
		Assert.notNull(resource, "Resource must not be null");
		WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder().resource(resource).build();
		return extract(request, runtimeOptions);
	}

	/**
	 * Extract text from raw document bytes using default options.
	 * @param bytes document bytes
	 * @return extraction response
	 */
	public WatsonxAiTextExtractionResponse extract(byte[] bytes) {
		return extract(bytes, "document", null);
	}

	/**
	 * Extract text from raw document bytes with a filename.
	 * @param bytes document bytes
	 * @param fileName document filename
	 * @return extraction response
	 */
	public WatsonxAiTextExtractionResponse extract(byte[] bytes, String fileName) {
		return extract(bytes, fileName, null);
	}

	/**
	 * Extract text from raw document bytes with a filename and runtime options.
	 * @param bytes document bytes
	 * @param fileName document filename
	 * @param runtimeOptions runtime options
	 * @return extraction response
	 */
	public WatsonxAiTextExtractionResponse extract(byte[] bytes, String fileName,
			WatsonxAiTextExtractionOptions runtimeOptions) {
		Assert.notNull(bytes, "Document bytes must not be null");
		Resource resource = new ByteArrayResource(bytes, fileName) {
			@Override
			public String getFilename() {
				return fileName;
			}
		};
		return extract(resource, runtimeOptions);
	}

	/**
	 * Extract documents into Spring AI {@link Document} list for direct use in embedding
	 * models or vector stores.
	 * @param resource document resource
	 * @return list of Spring AI documents
	 */
	public List<Document> extractToDocuments(Resource resource) {
		return extractToDocuments(resource, null);
	}

	/**
	 * Extract documents into Spring AI {@link Document} list with custom options.
	 * @param resource document resource
	 * @param runtimeOptions runtime options
	 * @return list of Spring AI documents
	 */
	public List<Document> extractToDocuments(Resource resource, WatsonxAiTextExtractionOptions runtimeOptions) {
		Assert.notNull(resource, "Resource must not be null");
		WatsonxAiTextExtractionResponse response = extract(resource, runtimeOptions);

		if (response == null) {
			return List.of();
		}

		String documentName = resolveResourceName(resource);
		List<Document> documents = new ArrayList<>();

		if (response.pages() != null && !response.pages().isEmpty()) {
			for (WatsonxAiTextExtractionResponse.ExtractedPage page : response.pages()) {
				Map<String, Object> metadata = new HashMap<>();
				if (documentName != null) {
					metadata.put(DOCUMENT_NAME_METADATA_KEY, documentName);
				}
				if (page.pageNumber() != null) {
					metadata.put(PAGE_NUMBER_METADATA_KEY, page.pageNumber());
				}
				if (page.metadata() != null) {
					metadata.putAll(page.metadata());
				}

				Document doc = Document.builder()
					.text(page.text() != null ? page.text() : "")
					.metadata(metadata)
					.build();
				documents.add(doc);
			}
		}
		else if (StringUtils.hasText(response.getText())) {
			Map<String, Object> metadata = new HashMap<>();
			if (documentName != null) {
				metadata.put(DOCUMENT_NAME_METADATA_KEY, documentName);
			}
			Document doc = Document.builder().text(response.getText()).metadata(metadata).build();
			documents.add(doc);
		}

		return documents;
	}

	private String resolveResourceName(Resource resource) {
		if (resource == null) {
			return null;
		}
		if (StringUtils.hasText(resource.getFilename())) {
			return resource.getFilename();
		}
		if (StringUtils.hasText(resource.getDescription())) {
			return resource.getDescription();
		}
		return null;
	}

	private String resolveDocumentName(WatsonxAiTextExtractionRequest request) {
		if (request.resource() != null) {
			String name = resolveResourceName(request.resource());
			if (StringUtils.hasText(name)) {
				return name;
			}
		}
		if (request.documentReference() != null && request.documentReference().location() != null) {
			Object fileName = request.documentReference().location().get("file_name");
			if (fileName != null) {
				return String.valueOf(fileName);
			}
			Object path = request.documentReference().location().get("path");
			if (path != null) {
				return String.valueOf(path);
			}
		}
		return "unknown";
	}

	private WatsonxAiTextExtractionOptions mergeOptions(WatsonxAiTextExtractionOptions runtimeOptions) {
		WatsonxAiTextExtractionOptions.Builder builder = this.defaultOptions.toBuilder();

		if (runtimeOptions != null) {
			if (runtimeOptions.getModel() != null) {
				builder.model(runtimeOptions.getModel());
			}
			if (runtimeOptions.getOutputFormats() != null) {
				builder.outputFormats(runtimeOptions.getOutputFormats());
			}
			if (runtimeOptions.getLanguages() != null) {
				builder.languages(runtimeOptions.getLanguages());
			}
			if (runtimeOptions.getEnableOcr() != null) {
				builder.enableOcr(runtimeOptions.getEnableOcr());
			}
		}

		return builder.build();
	}

	public WatsonxAiTextExtractionOptions getDefaultOptions() {
		return this.defaultOptions;
	}

	public void setObservationConvention(TextExtractionModelObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention must not be null");
		this.observationConvention = observationConvention;
	}

}
