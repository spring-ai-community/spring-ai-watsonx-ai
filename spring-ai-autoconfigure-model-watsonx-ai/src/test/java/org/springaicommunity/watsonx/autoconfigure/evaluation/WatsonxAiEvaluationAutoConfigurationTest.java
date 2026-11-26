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

package org.springaicommunity.watsonx.autoconfigure.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.evaluation.WatsonxAiAnswerRelevanceEvaluator;
import org.springaicommunity.watsonx.evaluation.WatsonxAiContextRelevanceEvaluator;
import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionApi;
import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionEvaluator;
import org.springaicommunity.watsonx.evaluation.WatsonxAiFaithfulnessEvaluator;
import org.springaicommunity.watsonx.autoconfigure.chat.WatsonxAiChatAutoConfiguration;
import org.springaicommunity.watsonx.autoconfigure.embedding.WatsonxAiEmbeddingAutoConfiguration;
import org.springaicommunity.watsonx.autoconfigure.moderation.WatsonxAiModerationAutoConfiguration;
import org.springaicommunity.watsonx.autoconfigure.rerank.WatsonxAiRerankAutoConfiguration;
import org.springaicommunity.watsonx.autoconfigure.textextraction.WatsonxAiTextExtractionAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests {@link WatsonxAiEvaluationAutoConfiguration}. No watsonx.ai call is made.
 *
 * @author Federico Mariani
 */
class WatsonxAiEvaluationAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WatsonxAiEvaluationAutoConfiguration.class))
		.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key",
				"spring.ai.watsonx.ai.base-url=https://eu-de.ml.cloud.ibm.com");

	@Test
	void evaluatorsAreOffByDefault() {
		this.contextRunner.withPropertyValues("spring.ai.watsonx.ai.project-id=test-project-id").run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(WatsonxAiDetectionApi.class);
			assertThat(context).doesNotHaveBean(WatsonxAiDetectionEvaluator.class);
		});
	}

	@Test
	void enabledWithAGovernanceInstanceCreatesTheEvaluators() {
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.evaluation.enabled=true",
					"spring.ai.watsonx.ai.evaluation.governance-instance-id=test-governance-instance-id",
					"spring.ai.watsonx.ai.evaluation.faithfulness.threshold=0.8")
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBean(WatsonxAiDetectionApi.class).getGovernanceInstanceId())
					.isEqualTo("test-governance-instance-id");
				assertThat(context.getBean(WatsonxAiAnswerRelevanceEvaluator.class).getThreshold())
					.isEqualTo(WatsonxAiDetectionEvaluator.DEFAULT_THRESHOLD);
				assertThat(context.getBean(WatsonxAiContextRelevanceEvaluator.class).getThreshold())
					.isEqualTo(WatsonxAiDetectionEvaluator.DEFAULT_THRESHOLD);
				assertThat(context.getBean(WatsonxAiFaithfulnessEvaluator.class).getThreshold()).isEqualTo(0.8);
			});
	}

	// watsonx.ai answers 401 detector_not_accessible for a project or a space
	@Test
	void enabledWithoutAGovernanceInstanceFailsNamingTheProperty() {
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.evaluation.enabled=true",
					"spring.ai.watsonx.ai.project-id=test-project-id")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.isInstanceOf(InvalidConfigurationPropertyValueException.class)
				.hasMessageContaining("spring.ai.watsonx.ai.evaluation.governance-instance-id"));
	}

	// The properties of the documentation, with every watsonx.ai auto-configuration of
	// the starter
	@Test
	void documentedPropertiesStartTheStarter() {
		starterContextRunner()
			.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key",
					"spring.ai.watsonx.ai.base-url=https://eu-de.ml.cloud.ibm.com",
					"spring.ai.watsonx.ai.project-id=test-project-id", "spring.ai.watsonx.ai.evaluation.enabled=true",
					"spring.ai.watsonx.ai.evaluation.governance-instance-id=test-governance-instance-id",
					"spring.ai.watsonx.ai.evaluation.faithfulness.threshold=0.7")
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(WatsonxAiFaithfulnessEvaluator.class);
				assertThat(context.getBean(WatsonxAiFaithfulnessEvaluator.class).getThreshold()).isEqualTo(0.7);
			});
	}

	@Test
	void documentedEvaluatorOnlyPropertiesStartTheStarterWithoutAProject() {
		starterContextRunner()
			.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key",
					"spring.ai.watsonx.ai.base-url=https://eu-de.ml.cloud.ibm.com", "spring.ai.model.chat=none",
					"spring.ai.model.embedding=none", "spring.ai.model.moderation=none",
					"spring.ai.watsonx.ai.rerank.enabled=false", "spring.ai.watsonx.ai.text-extraction.enabled=false",
					"spring.ai.watsonx.ai.evaluation.enabled=true",
					"spring.ai.watsonx.ai.evaluation.governance-instance-id=test-governance-instance-id")
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(WatsonxAiAnswerRelevanceEvaluator.class);
			});
	}

	private static ApplicationContextRunner starterContextRunner() {
		return new ApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(WatsonxAiChatAutoConfiguration.class, WatsonxAiEmbeddingAutoConfiguration.class,
						WatsonxAiEvaluationAutoConfiguration.class, WatsonxAiModerationAutoConfiguration.class,
						WatsonxAiRerankAutoConfiguration.class, WatsonxAiTextExtractionAutoConfiguration.class));
	}

	@Test
	void evaluatorsOfTheApplicationWin() {
		WatsonxAiFaithfulnessEvaluator faithfulness = WatsonxAiFaithfulnessEvaluator.builder()
			.detectionApi(mock(WatsonxAiDetectionApi.class))
			.threshold(0.9)
			.build();
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.evaluation.enabled=true",
					"spring.ai.watsonx.ai.evaluation.governance-instance-id=test-governance-instance-id")
			.withBean(WatsonxAiFaithfulnessEvaluator.class, () -> faithfulness)
			.run(context -> assertThat(context.getBean(WatsonxAiFaithfulnessEvaluator.class)).isSameAs(faithfulness));
	}

}
