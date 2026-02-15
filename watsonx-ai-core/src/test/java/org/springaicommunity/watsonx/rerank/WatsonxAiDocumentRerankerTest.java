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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

/**
 * JUnit 5 test class for WatsonxAiDocumentReranker functionality. Tests the DocumentPostProcessor
 * integration with rerank model.
 *
 * @author Federico Mariani
 * @since 1.1.0
 */
class WatsonxAiDocumentRerankerTest {

  @Mock private WatsonxAiRerankModel rerankModel;

  private WatsonxAiDocumentReranker documentReranker;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    documentReranker = new WatsonxAiDocumentReranker(rerankModel);
  }

  @Nested
  class ConstructorTests {

    @Test
    void constructorWithValidRerankModel() {
      assertNotNull(documentReranker);
    }

    @Test
    void constructorWithNullRerankModelThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WatsonxAiDocumentReranker(null),
          "WatsonxAiRerankModel must not be null");
    }

    @Test
    void constructorWithOptionsIsValid() {
      WatsonxAiRerankOptions options = WatsonxAiRerankOptions.builder().topN(5).build();
      WatsonxAiDocumentReranker reranker = new WatsonxAiDocumentReranker(rerankModel, options);
      assertNotNull(reranker);
    }
  }

  @Nested
  class ProcessMethodTests {

    @Test
    void processWithValidDocuments() {
      Query query = new Query("What is machine learning?");
      List<Document> documents =
          List.of(
              Document.builder().id("1").text("Machine learning is AI.").build(),
              Document.builder().id("2").text("Cooking recipes.").build(),
              Document.builder().id("3").text("Deep learning networks.").build());

      WatsonxAiRerankResponse.RerankResult result1 =
          new WatsonxAiRerankResponse.RerankResult(0, 0.95, null);
      WatsonxAiRerankResponse.RerankResult result2 =
          new WatsonxAiRerankResponse.RerankResult(2, 0.85, null);
      WatsonxAiRerankResponse.RerankResult result3 =
          new WatsonxAiRerankResponse.RerankResult(1, 0.25, null);

      when(rerankModel.rerank(anyString(), anyList(), any()))
          .thenReturn(List.of(result1, result2, result3));

      List<Document> rerankedDocuments = documentReranker.process(query, documents);

      assertNotNull(rerankedDocuments);
      assertEquals(3, rerankedDocuments.size());

      // First document should be the highest scoring (index 0)
      assertEquals("1", rerankedDocuments.get(0).getId());
      assertEquals(0.95, rerankedDocuments.get(0).getScore(), 0.001);
      assertEquals(
          0.95,
          rerankedDocuments
              .get(0)
              .getMetadata()
              .get(WatsonxAiDocumentReranker.RERANK_SCORE_METADATA_KEY));

      // Second document should be index 2
      assertEquals("3", rerankedDocuments.get(1).getId());
      assertEquals(0.85, rerankedDocuments.get(1).getScore(), 0.001);

      // Third document should be index 1
      assertEquals("2", rerankedDocuments.get(2).getId());
      assertEquals(0.25, rerankedDocuments.get(2).getScore(), 0.001);

      verify(rerankModel, times(1)).rerank(anyString(), anyList(), any());
    }

    @Test
    void processWithEmptyDocuments() {
      Query query = new Query("What is AI?");
      List<Document> documents = List.of();

      List<Document> rerankedDocuments = documentReranker.process(query, documents);

      assertNotNull(rerankedDocuments);
      assertTrue(rerankedDocuments.isEmpty());
      verify(rerankModel, never()).rerank(anyString(), anyList(), any());
    }

    @Test
    void processWithNullDocuments() {
      Query query = new Query("What is AI?");

      List<Document> rerankedDocuments = documentReranker.process(query, null);

      assertNotNull(rerankedDocuments);
      assertTrue(rerankedDocuments.isEmpty());
      verify(rerankModel, never()).rerank(anyString(), anyList(), any());
    }

    @Test
    void processWithNullQueryThrowsException() {
      List<Document> documents = List.of(Document.builder().id("1").text("Test").build());

      assertThrows(
          IllegalArgumentException.class,
          () -> documentReranker.process(null, documents),
          "Query must not be null");
    }

    @Test
    void processPreservesDocumentMetadata() {
      Query query = new Query("Test query");
      Document originalDoc =
          Document.builder()
              .id("1")
              .text("Test document")
              .metadata(Map.of("source", "test", "page", 1))
              .build();

      WatsonxAiRerankResponse.RerankResult result =
          new WatsonxAiRerankResponse.RerankResult(0, 0.9, null);

      when(rerankModel.rerank(anyString(), anyList(), any())).thenReturn(List.of(result));

      List<Document> rerankedDocuments = documentReranker.process(query, List.of(originalDoc));

      assertEquals(1, rerankedDocuments.size());
      Document rerankedDoc = rerankedDocuments.get(0);

      // Original metadata should be preserved
      assertEquals("test", rerankedDoc.getMetadata().get("source"));
      assertEquals(1, rerankedDoc.getMetadata().get("page"));
      // Plus the new rerank score
      assertEquals(
          0.9, rerankedDoc.getMetadata().get(WatsonxAiDocumentReranker.RERANK_SCORE_METADATA_KEY));
    }

    @Test
    void processHandlesRerankModelReturningEmptyResults() {
      Query query = new Query("Test query");
      List<Document> documents = List.of(Document.builder().id("1").text("Test").build());

      when(rerankModel.rerank(anyString(), anyList(), any())).thenReturn(List.of());

      List<Document> rerankedDocuments = documentReranker.process(query, documents);

      // Should return original documents when rerank returns empty
      assertEquals(1, rerankedDocuments.size());
    }

    @Test
    void processHandlesRerankModelReturningNull() {
      Query query = new Query("Test query");
      List<Document> documents = List.of(Document.builder().id("1").text("Test").build());

      when(rerankModel.rerank(anyString(), anyList(), any())).thenReturn(null);

      List<Document> rerankedDocuments = documentReranker.process(query, documents);

      // Should return original documents when rerank returns null
      assertEquals(1, rerankedDocuments.size());
    }
  }

  @Nested
  class ApplyMethodTests {

    @Test
    void applyDelegatesToProcess() {
      Query query = new Query("Test query");
      List<Document> documents = List.of(Document.builder().id("1").text("Test").build());

      WatsonxAiRerankResponse.RerankResult result =
          new WatsonxAiRerankResponse.RerankResult(0, 0.9, null);

      when(rerankModel.rerank(anyString(), anyList(), any())).thenReturn(List.of(result));

      // apply() should delegate to process()
      List<Document> rerankedDocuments = documentReranker.apply(query, documents);

      assertNotNull(rerankedDocuments);
      assertEquals(1, rerankedDocuments.size());
      verify(rerankModel, times(1)).rerank(anyString(), anyList(), any());
    }
  }
}
