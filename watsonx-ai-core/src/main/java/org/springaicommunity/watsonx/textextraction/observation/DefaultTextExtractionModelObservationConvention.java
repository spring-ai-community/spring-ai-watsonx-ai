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

package org.springaicommunity.watsonx.textextraction.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Default conventions to populate observations for text extraction model operations.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public class DefaultTextExtractionModelObservationConvention implements TextExtractionModelObservationConvention {

	public static final String DEFAULT_NAME = "gen_ai.client.operation";

	private static final KeyValue REQUEST_MODEL_NONE = KeyValue
		.of(TextExtractionModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL, KeyValue.NONE_VALUE);

	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	@Override
	public String getContextualName(TextExtractionModelObservationContext context) {
		return Optional.ofNullable(context.getOptions())
			.map(options -> options.getModel())
			.filter(StringUtils::hasText)
			.map(model -> "%s %s".formatted(context.getOperationMetadata().operationType(), model))
			.orElseGet(() -> context.getOperationMetadata().operationType());
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(TextExtractionModelObservationContext context) {
		return KeyValues.of(aiOperationType(context), aiProvider(context), requestModel(context), status(context));
	}

	protected KeyValue aiOperationType(TextExtractionModelObservationContext context) {
		return KeyValue.of(TextExtractionModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE,
				context.getOperationMetadata().operationType());
	}

	protected KeyValue aiProvider(TextExtractionModelObservationContext context) {
		return KeyValue.of(TextExtractionModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER,
				context.getOperationMetadata().provider());
	}

	protected KeyValue requestModel(TextExtractionModelObservationContext context) {
		return Optional.ofNullable(context.getOptions())
			.map(options -> options.getModel())
			.filter(StringUtils::hasText)
			.map(model -> KeyValue.of(TextExtractionModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL,
					model))
			.orElse(REQUEST_MODEL_NONE);
	}

	protected KeyValue status(TextExtractionModelObservationContext context) {
		String state = Optional.ofNullable(context.getResponse())
			.map(response -> response.getStatus())
			.filter(StringUtils::hasText)
			.orElse("unknown");

		return KeyValue.of(TextExtractionModelObservationDocumentation.LowCardinalityKeyNames.STATUS, state);
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(TextExtractionModelObservationContext context) {
		var keyValues = KeyValues.empty();
		keyValues = documentName(keyValues, context);
		keyValues = pageCount(keyValues, context);
		return keyValues;
	}

	protected KeyValues documentName(KeyValues keyValues, TextExtractionModelObservationContext context) {
		return Optional.ofNullable(context.getDocumentName())
			.filter(StringUtils::hasText)
			.map(name -> keyValues.and(
					TextExtractionModelObservationDocumentation.HighCardinalityKeyNames.DOCUMENT_NAME.asString(), name))
			.orElse(keyValues);
	}

	protected KeyValues pageCount(KeyValues keyValues, TextExtractionModelObservationContext context) {
		return Optional.ofNullable(context.getResponse())
			.map(response -> response.pages())
			.map(pages -> keyValues.and(
					TextExtractionModelObservationDocumentation.HighCardinalityKeyNames.PAGE_COUNT.asString(),
					String.valueOf(pages.size())))
			.orElse(keyValues);
	}

}
