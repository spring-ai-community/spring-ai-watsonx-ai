/*
 * Copyright 2025 the original author or authors.
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

package org.springaicommunity.watsonx.aot;

import java.util.List;
import org.springaicommunity.watsonx.chat.WatsonxAiChatRequest;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingRequest;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationRequest;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankOptions;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankRequest;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankResponse;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionRequest;
import org.springframework.ai.aot.AiRuntimeHints;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * {@code WatsonxAiRuntimeHints} is responsible for registering runtime hints for watsonx API
 * classes.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiRuntimeHints implements RuntimeHintsRegistrar {

  // The rerank package is not scanned: scanning loads every class in it, and
  // WatsonxAiDocumentReranker needs the optional spring-ai-rag dependency.
  private static final List<Class<?>> SCANNED_PACKAGE_ANCHORS =
      List.of(
          WatsonxAiChatRequest.class,
          WatsonxAiEmbeddingRequest.class,
          WatsonxAiModerationRequest.class,
          WatsonxAiTextExtractionRequest.class);

  private static final List<Class<?>> RERANK_JSON_TYPES =
      List.of(
          WatsonxAiRerankRequest.class,
          WatsonxAiRerankResponse.class,
          WatsonxAiRerankOptions.class);

  @Override
  public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
    var memberCategories = MemberCategory.values();

    for (var anchor : SCANNED_PACKAGE_ANCHORS) {
      for (var typedReference : AiRuntimeHints.findJsonAnnotatedClassesInPackage(anchor)) {
        hints.reflection().registerType(typedReference, memberCategories);
      }
    }

    for (var type : RERANK_JSON_TYPES) {
      hints.reflection().registerType(type, memberCategories);
      for (var typedReference : AiRuntimeHints.findInnerClassesFor(type)) {
        hints.reflection().registerType(typedReference, memberCategories);
      }
    }
  }
}
