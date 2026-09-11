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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;
import org.springframework.ai.observation.conventions.AiObservationAttributes;

/**
 * Documented conventions for text extraction model observations.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public enum TextExtractionModelObservationDocumentation implements ObservationDocumentation {

	TEXT_EXTRACTION_MODEL_OPERATION {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultTextExtractionModelObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}
	};

	/** Low-cardinality observation key names for text extraction operations. */
	public enum LowCardinalityKeyNames implements KeyName {

		AI_OPERATION_TYPE {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_OPERATION_TYPE.value();
			}
		},

		AI_PROVIDER {
			@Override
			public String asString() {
				return AiObservationAttributes.AI_PROVIDER.value();
			}
		},

		REQUEST_MODEL {
			@Override
			public String asString() {
				return AiObservationAttributes.REQUEST_MODEL.value();
			}
		},

		STATUS {
			@Override
			public String asString() {
				return "gen_ai.text_extraction.status";
			}
		}

	}

	/** High-cardinality observation key names for text extraction operations. */
	public enum HighCardinalityKeyNames implements KeyName {

		DOCUMENT_NAME {
			@Override
			public String asString() {
				return "gen_ai.text_extraction.document_name";
			}
		},

		PAGE_COUNT {
			@Override
			public String asString() {
				return "gen_ai.text_extraction.page_count";
			}
		}

	}

}
