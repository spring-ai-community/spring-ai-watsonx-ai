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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Logs the warnings that watsonx.ai returns in {@code system.warnings}, such as the date
 * a model is deprecated or withdrawn. watsonx.ai repeats a warning in every response, so
 * each distinct warning is logged once per model.
 *
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
public final class WatsonxAiWarnings {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiWarnings.class);

	private static final Set<String> loggedWarnings = ConcurrentHashMap.newKeySet();

	private WatsonxAiWarnings() {
	}

	/**
	 * Logs a watsonx.ai warning at WARN level, unless the same warning was already logged
	 * for the same model.
	 * @param model the model the warning came with
	 * @param id the warning ID, e.g. {@code deprecation_warning}
	 * @param message the warning message
	 */
	public static void log(final String model, final String id, final String message) {
		if (!StringUtils.hasText(message)) {
			return;
		}
		if (loggedWarnings.add(model + '\n' + id + '\n' + message)) {
			logger.warn("watsonx.ai warning for model '{}' ({}): {}", model, id, message);
		}
	}

}
