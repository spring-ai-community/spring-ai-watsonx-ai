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

package org.springaicommunity.watsonx.aot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.chat.WatsonxAiChatRequest;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingRequest;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingResponse;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankOptions;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankRequest;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankResponse;
import org.springframework.ai.aot.AiRuntimeHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * JUnit 5 test class for {@link WatsonxAiRuntimeHints}. Verifies the registrar is
 * discoverable by Spring AOT and registers reflection hints for the watsonx.ai DTOs.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiRuntimeHintsTest {

	@Test
	void registrarIsDeclaredInAotFactories() {
		List<RuntimeHintsRegistrar> registrars = SpringFactoriesLoader
			.forResourceLocation("META-INF/spring/aot.factories")
			.load(RuntimeHintsRegistrar.class);

		assertTrue(registrars.stream().anyMatch(WatsonxAiRuntimeHints.class::isInstance),
				"WatsonxAiRuntimeHints must be registered in META-INF/spring/aot.factories");
	}

	@Test
	void registersReflectionHintsForJsonTypes() {
		RuntimeHints hints = new RuntimeHints();
		new WatsonxAiRuntimeHints().registerHints(hints, getClass().getClassLoader());

		for (Class<?> type : List.of(WatsonxAiChatRequest.class, WatsonxAiChatResponse.class,
				WatsonxAiChatResponse.TextChatResultChoice.class, TextChatMessage.class,
				WatsonxAiEmbeddingRequest.class, WatsonxAiEmbeddingResponse.class, WatsonxAiRerankRequest.class,
				WatsonxAiRerankRequest.RerankInput.class, WatsonxAiRerankResponse.class,
				WatsonxAiRerankResponse.RerankResult.class, WatsonxAiRerankOptions.class)) {
			assertTrue(RuntimeHintsPredicates.reflection().onType(type).test(hints),
					"Missing reflection hint for " + type.getName());
		}
	}

	/**
	 * Guards against new JSON types that the registrar does not cover. spring-ai-rag is
	 * on the test classpath, so the whole library can be scanned here even though the
	 * registrar itself cannot do that at AOT time.
	 */
	@Test
	void registersReflectionHintsForEveryJsonTypeInTheLibrary() {
		RuntimeHints hints = new RuntimeHints();
		new WatsonxAiRuntimeHints().registerHints(hints, getClass().getClassLoader());

		Set<TypeReference> jsonTypes = AiRuntimeHints
			.findJsonAnnotatedClassesInPackage("org.springaicommunity.watsonx");
		List<String> missing = jsonTypes.stream()
			.filter(type -> !RuntimeHintsPredicates.reflection().onType(type).test(hints))
			.map(TypeReference::getName)
			.sorted()
			.toList();

		assertFalse(jsonTypes.isEmpty(), "Expected to find JSON-annotated types in org.springaicommunity.watsonx");
		assertTrue(missing.isEmpty(),
				"Missing reflection hints. Add the package to WatsonxAiRuntimeHints.SCANNED_PACKAGES, "
						+ "or the type to RERANK_JSON_TYPES if it is in the rerank package: " + missing);
	}

}
