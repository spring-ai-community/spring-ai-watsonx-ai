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

package io.github.springaicommunity.watsonx.embedding;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * {@class EmbeddingModel} implementation that provides access to watsonx supported embedding
 * models.
 *
 * <p>TODO: add more explanation
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiEmbeddingModel implements EmbeddingModel {

  @Override
  public EmbeddingResponse call(EmbeddingRequest request) {

    // Implement this for watsonx support.
    return null;
  }

  @Override
  public float[] embed(Document document) {

    // Implement this for watsonx support.
    return null;
  }
}
