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

package org.springaicommunity.watsonx.rerank.observation;

import io.micrometer.observation.Observation;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankOptions;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankResponse;
import org.springframework.ai.observation.AiOperationMetadata;
import org.springframework.util.Assert;

/**
 * Context used to store metadata for rerank model exchanges.
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
public class RerankModelObservationContext extends Observation.Context {

  public static final String OPERATION_TYPE = "rerank";

  private final String query;
  private final int documentCount;
  private final WatsonxAiRerankOptions options;
  private final AiOperationMetadata operationMetadata;
  private WatsonxAiRerankResponse response;

  RerankModelObservationContext(
      String query, int documentCount, WatsonxAiRerankOptions options, String provider) {
    this.query = query;
    this.documentCount = documentCount;
    this.options = options;
    this.operationMetadata =
        AiOperationMetadata.builder().operationType(OPERATION_TYPE).provider(provider).build();
  }

  public String getQuery() {
    return query;
  }

  public int getDocumentCount() {
    return documentCount;
  }

  public WatsonxAiRerankOptions getOptions() {
    return options;
  }

  public AiOperationMetadata getOperationMetadata() {
    return operationMetadata;
  }

  public WatsonxAiRerankResponse getResponse() {
    return response;
  }

  public void setResponse(WatsonxAiRerankResponse response) {
    Assert.notNull(response, "response cannot be null");
    this.response = response;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String query;
    private int documentCount;
    private WatsonxAiRerankOptions options;
    private String provider;

    private Builder() {}

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder documentCount(int documentCount) {
      this.documentCount = documentCount;
      return this;
    }

    public Builder options(WatsonxAiRerankOptions options) {
      this.options = options;
      return this;
    }

    public Builder provider(String provider) {
      this.provider = provider;
      return this;
    }

    public RerankModelObservationContext build() {
      Assert.hasText(this.query, "query cannot be null or empty");
      Assert.notNull(this.options, "options cannot be null");
      Assert.hasText(this.provider, "provider cannot be null or empty");
      return new RerankModelObservationContext(
          this.query, this.documentCount, this.options, this.provider);
    }
  }
}
