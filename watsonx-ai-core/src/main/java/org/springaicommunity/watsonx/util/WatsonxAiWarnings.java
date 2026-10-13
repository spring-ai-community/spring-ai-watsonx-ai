/*
 * Copyright 2025-2026 the original author or authors.
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

package org.springaicommunity.watsonx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Logs the warnings that watsonx.ai returns in {@code system.warnings}, such as the date
 * a model is deprecated or withdrawn, at DEBUG level. watsonx.ai repeats a warning in
 * every response, and each one is logged, without keeping any state. Enable
 * {@code logging.level.org.springaicommunity.watsonx.util.WatsonxAiWarnings=DEBUG} to see
 * them. The warnings are also in the response metadata, under {@code warnings}.
 *
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
public final class WatsonxAiWarnings {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiWarnings.class);

	private WatsonxAiWarnings() {
	}

	/**
	 * Logs a watsonx.ai warning at DEBUG level.
	 * @param model the model the warning came with
	 * @param id the warning ID, e.g. {@code deprecation_warning}
	 * @param message the warning message
	 */
	public static void log(final String model, final String id, final String message) {
		if (!StringUtils.hasText(message)) {
			return;
		}
		logger.debug("watsonx.ai warning for model '{}' ({}): {}", model, id, message);
	}

}
