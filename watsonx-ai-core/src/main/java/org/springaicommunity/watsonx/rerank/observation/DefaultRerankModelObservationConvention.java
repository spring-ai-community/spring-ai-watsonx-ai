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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Default conventions to populate observations for rerank model operations.
 *
 * @author Federico Mariani
 * @since 1.1.0
 */
public class DefaultRerankModelObservationConvention implements RerankModelObservationConvention {

  public static final String DEFAULT_NAME = "gen_ai.client.operation";

  private static final KeyValue REQUEST_MODEL_NONE =
      KeyValue.of(
          RerankModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL,
          KeyValue.NONE_VALUE);

  private static final KeyValue RESPONSE_MODEL_NONE =
      KeyValue.of(
          RerankModelObservationDocumentation.LowCardinalityKeyNames.RESPONSE_MODEL,
          KeyValue.NONE_VALUE);

  @Override
  public String getName() {
    return DEFAULT_NAME;
  }

  @Override
  public String getContextualName(RerankModelObservationContext context) {
    return Optional.ofNullable(context.getOptions())
        .map(options -> options.getModel())
        .filter(StringUtils::hasText)
        .map(model -> "%s %s".formatted(context.getOperationMetadata().operationType(), model))
        .orElseGet(() -> context.getOperationMetadata().operationType());
  }

  @Override
  public KeyValues getLowCardinalityKeyValues(RerankModelObservationContext context) {
    return KeyValues.of(
        aiOperationType(context),
        aiProvider(context),
        requestModel(context),
        responseModel(context));
  }

  protected KeyValue aiOperationType(RerankModelObservationContext context) {
    return KeyValue.of(
        RerankModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE,
        context.getOperationMetadata().operationType());
  }

  protected KeyValue aiProvider(RerankModelObservationContext context) {
    return KeyValue.of(
        RerankModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER,
        context.getOperationMetadata().provider());
  }

  protected KeyValue requestModel(RerankModelObservationContext context) {
    return Optional.ofNullable(context.getOptions())
        .map(options -> options.getModel())
        .filter(StringUtils::hasText)
        .map(
            model ->
                KeyValue.of(
                    RerankModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL,
                    model))
        .orElse(REQUEST_MODEL_NONE);
  }

  protected KeyValue responseModel(RerankModelObservationContext context) {
    return Optional.ofNullable(context.getResponse())
        .map(response -> response.model())
        .filter(StringUtils::hasText)
        .map(
            model ->
                KeyValue.of(
                    RerankModelObservationDocumentation.LowCardinalityKeyNames.RESPONSE_MODEL,
                    model))
        .orElse(RESPONSE_MODEL_NONE);
  }

  @Override
  public KeyValues getHighCardinalityKeyValues(RerankModelObservationContext context) {
    var keyValues = KeyValues.empty();
    keyValues = documentCount(keyValues, context);
    keyValues = topN(keyValues, context);
    keyValues = inputTokenCount(keyValues, context);
    return keyValues;
  }

  protected KeyValues documentCount(KeyValues keyValues, RerankModelObservationContext context) {
    return keyValues.and(
        RerankModelObservationDocumentation.HighCardinalityKeyNames.DOCUMENT_COUNT.asString(),
        String.valueOf(context.getDocumentCount()));
  }

  protected KeyValues topN(KeyValues keyValues, RerankModelObservationContext context) {
    return Optional.ofNullable(context.getOptions())
        .map(options -> options.getTopN())
        .map(
            topN ->
                keyValues.and(
                    RerankModelObservationDocumentation.HighCardinalityKeyNames.TOP_N.asString(),
                    String.valueOf(topN)))
        .orElse(keyValues);
  }

  protected KeyValues inputTokenCount(KeyValues keyValues, RerankModelObservationContext context) {
    return Optional.ofNullable(context.getResponse())
        .map(response -> response.inputTokenCount())
        .map(
            inputTokenCount ->
                keyValues.and(
                    RerankModelObservationDocumentation.HighCardinalityKeyNames.USAGE_INPUT_TOKENS
                        .asString(),
                    String.valueOf(inputTokenCount)))
        .orElse(keyValues);
  }
}
