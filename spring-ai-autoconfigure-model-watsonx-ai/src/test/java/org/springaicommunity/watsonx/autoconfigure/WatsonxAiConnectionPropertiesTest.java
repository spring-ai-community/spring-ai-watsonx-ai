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

package org.springaicommunity.watsonx.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springaicommunity.watsonx.autoconfigure.chat.WatsonxAiChatAutoConfiguration;
import org.springaicommunity.watsonx.autoconfigure.embedding.WatsonxAiEmbeddingAutoConfiguration;
import org.springaicommunity.watsonx.autoconfigure.moderation.WatsonxAiModerationAutoConfiguration;
import org.springaicommunity.watsonx.autoconfigure.rerank.WatsonxAiRerankAutoConfiguration;
import org.springaicommunity.watsonx.autoconfigure.textextraction.WatsonxAiTextExtractionAutoConfiguration;
import org.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingApi;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationApi;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankApi;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests that every watsonx.ai auto-configuration requires exactly one of
 * {@code project-id} and {@code space-id}.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiConnectionPropertiesTest {

	static Stream<Class<?>[]> autoConfigurations() {
		return Stream.of(new Class<?>[] { WatsonxAiChatAutoConfiguration.class, WatsonxAiChatApi.class },
				new Class<?>[] { WatsonxAiEmbeddingAutoConfiguration.class, WatsonxAiEmbeddingApi.class },
				new Class<?>[] { WatsonxAiRerankAutoConfiguration.class, WatsonxAiRerankApi.class },
				new Class<?>[] { WatsonxAiModerationAutoConfiguration.class, WatsonxAiModerationApi.class },
				new Class<?>[] { WatsonxAiTextExtractionAutoConfiguration.class, WatsonxAiTextExtractionApi.class });
	}

	private static ApplicationContextRunner contextRunner(Class<?> autoConfiguration) {
		return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(autoConfiguration))
			.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key");
	}

	@ParameterizedTest
	@MethodSource("autoConfigurations")
	void projectIdOnlyStarts(Class<?> autoConfiguration, Class<?> api) {
		contextRunner(autoConfiguration).withPropertyValues("spring.ai.watsonx.ai.project-id=test-project-id")
			.run(context -> {
				assertThat(context).hasSingleBean(api);
				assertThat(ReflectionTestUtils.getField(context.getBean(api), "projectId"))
					.isEqualTo("test-project-id");
				assertThat(ReflectionTestUtils.getField(context.getBean(api), "spaceId")).isNull();
			});
	}

	@ParameterizedTest
	@MethodSource("autoConfigurations")
	void spaceIdOnlyStarts(Class<?> autoConfiguration, Class<?> api) {
		contextRunner(autoConfiguration).withPropertyValues("spring.ai.watsonx.ai.space-id=test-space-id")
			.run(context -> {
				assertThat(context).hasSingleBean(api);
				assertThat(ReflectionTestUtils.getField(context.getBean(api), "projectId")).isNull();
				assertThat(ReflectionTestUtils.getField(context.getBean(api), "spaceId")).isEqualTo("test-space-id");
			});
	}

	@ParameterizedTest
	@MethodSource("autoConfigurations")
	void blankProjectIdCountsAsNotSet(Class<?> autoConfiguration, Class<?> api) {
		contextRunner(autoConfiguration)
			.withPropertyValues("spring.ai.watsonx.ai.project-id=", "spring.ai.watsonx.ai.space-id=test-space-id")
			.run(context -> {
				assertThat(context).hasSingleBean(api);
				assertThat(ReflectionTestUtils.getField(context.getBean(api), "projectId")).isNull();
			});
	}

	@ParameterizedTest
	@MethodSource("autoConfigurations")
	void bothFailToStart(Class<?> autoConfiguration, Class<?> api) {
		contextRunner(autoConfiguration)
			.withPropertyValues("spring.ai.watsonx.ai.project-id=test-project-id",
					"spring.ai.watsonx.ai.space-id=test-space-id")
			.run(context -> assertThat(context).getFailure()
				.rootCause()
				.isInstanceOfSatisfying(MutuallyExclusiveConfigurationPropertiesException.class,
						exception -> assertThat(exception.getConfiguredNames()).containsExactlyInAnyOrder(
								"spring.ai.watsonx.ai.project-id", "spring.ai.watsonx.ai.space-id")));
	}

	@ParameterizedTest
	@MethodSource("autoConfigurations")
	void neitherFailsToStart(Class<?> autoConfiguration, Class<?> api) {
		contextRunner(autoConfiguration)
			.withPropertyValues("spring.ai.watsonx.ai.project-id= ", "spring.ai.watsonx.ai.space-id=")
			.run(context -> assertThat(context).getFailure()
				.rootCause()
				.isInstanceOfSatisfying(InvalidConfigurationPropertyValueException.class, exception -> {
					assertThat(exception.getName()).isEqualTo("spring.ai.watsonx.ai.project-id");
					assertThat(exception.getReason()).isEqualTo(
							"watsonx.ai needs a project or a space. Set spring.ai.watsonx.ai.project-id or spring.ai.watsonx.ai.space-id.");
				}));
	}

	@ParameterizedTest
	@MethodSource("autoConfigurations")
	<T> void ownApiBeanSkipsTheCheck(Class<?> autoConfiguration, Class<T> api) {
		contextRunner(autoConfiguration).withBean(api, () -> mock(api))
			.run(context -> assertThat(context).hasNotFailed().hasSingleBean(api));
	}

}
