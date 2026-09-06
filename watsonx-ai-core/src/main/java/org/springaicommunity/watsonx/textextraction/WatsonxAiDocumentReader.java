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

import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A document reader implementation that uses watsonx.ai text extraction to extract
 * documents from a given {@link Resource}.
 *
 * <p>
 * This provides a bridge between watsonx.ai text extraction services and Spring AI
 * document pipelines (embeddings, vector stores, and RAG).
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public class WatsonxAiDocumentReader implements Supplier<List<Document>> {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiDocumentReader.class);

	private final WatsonxAiTextExtractionModel textExtractionModel;

	private final Resource resource;

	private final WatsonxAiTextExtractionOptions options;

	/**
	 * Create a new WatsonxAiDocumentReader with default options.
	 * @param textExtractionModel the text extraction model to use
	 * @param resource the document resource to read
	 */
	public WatsonxAiDocumentReader(WatsonxAiTextExtractionModel textExtractionModel, Resource resource) {
		this(textExtractionModel, resource, null);
	}

	/**
	 * Create a new WatsonxAiDocumentReader with custom options.
	 * @param textExtractionModel the text extraction model to use
	 * @param resource the document resource to read
	 * @param options optional extraction options to override defaults
	 */
	public WatsonxAiDocumentReader(WatsonxAiTextExtractionModel textExtractionModel, Resource resource,
			WatsonxAiTextExtractionOptions options) {
		Assert.notNull(textExtractionModel, "WatsonxAiTextExtractionModel must not be null");
		Assert.notNull(resource, "Resource must not be null");
		this.textExtractionModel = textExtractionModel;
		this.resource = resource;
		this.options = options;
	}

	/**
	 * Read and extract documents from the resource.
	 * @return list of extracted Spring AI documents
	 */
	public List<Document> read() {
		logger.debug("Extracting documents from resource: {}", this.resource.getFilename());
		return this.textExtractionModel.extractToDocuments(this.resource, this.options);
	}

	@Override
	public List<Document> get() {
		return read();
	}

}
