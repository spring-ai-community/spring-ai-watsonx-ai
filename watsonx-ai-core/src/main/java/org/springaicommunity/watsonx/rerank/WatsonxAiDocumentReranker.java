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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A {@link DocumentPostProcessor} implementation that uses watsonx.ai reranking to reorder
 * documents based on their relevance to the query.
 *
 * <p>This can be used in a RAG pipeline with {@code RetrievalAugmentationAdvisor} to improve the
 * quality of retrieved documents by reranking them based on semantic relevance.
 *
 * @author Federico Mariani
 * @since 1.1.0
 */
public class WatsonxAiDocumentReranker implements DocumentPostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiDocumentReranker.class);

  public static final String RERANK_SCORE_METADATA_KEY = "rerank_score";

  private final WatsonxAiRerankModel rerankModel;
  private final WatsonxAiRerankOptions options;

  /**
   * Create a new WatsonxAiDocumentReranker with default options.
   *
   * @param rerankModel the rerank model to use
   */
  public WatsonxAiDocumentReranker(WatsonxAiRerankModel rerankModel) {
    this(rerankModel, null);
  }

  /**
   * Create a new WatsonxAiDocumentReranker with custom options.
   *
   * @param rerankModel the rerank model to use
   * @param options optional rerank options to override defaults
   */
  public WatsonxAiDocumentReranker(
      WatsonxAiRerankModel rerankModel, WatsonxAiRerankOptions options) {
    Assert.notNull(rerankModel, "WatsonxAiRerankModel must not be null");
    this.rerankModel = rerankModel;
    this.options = options;
  }

  @Override
  public List<Document> process(Query query, List<Document> documents) {
    Assert.notNull(query, "Query must not be null");

    if (CollectionUtils.isEmpty(documents)) {
      logger.debug("No documents to rerank, returning empty list");
      return List.of();
    }

    logger.debug("Reranking {} documents for query: {}", documents.size(), query.text());

    List<String> documentTexts = documents.stream().map(Document::getText).toList();

    List<WatsonxAiRerankResponse.RerankResult> results =
        this.rerankModel.rerank(query.text(), documentTexts, this.options);

    if (CollectionUtils.isEmpty(results)) {
      logger.warn("Rerank API returned no results, returning original documents");
      return documents;
    }

    List<Document> rerankedDocuments = new ArrayList<>(results.size());
    for (WatsonxAiRerankResponse.RerankResult result : results) {
      int originalIndex = result.index();
      if (originalIndex >= 0 && originalIndex < documents.size()) {
        Document originalDocument = documents.get(originalIndex);

        Map<String, Object> updatedMetadata = new HashMap<>(originalDocument.getMetadata());
        updatedMetadata.put(RERANK_SCORE_METADATA_KEY, result.score());

        Document rerankedDocument =
            Document.builder()
                .id(originalDocument.getId())
                .text(originalDocument.getText())
                .metadata(updatedMetadata)
                .score(result.score())
                .build();

        rerankedDocuments.add(rerankedDocument);
      } else {
        logger.warn("Rerank result index {} is out of bounds, skipping", originalIndex);
      }
    }

    logger.debug("Reranked {} documents", rerankedDocuments.size());
    return rerankedDocuments;
  }
}
