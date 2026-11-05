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

package org.springaicommunity.watsonx.autoconfigure.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.generation.WatsonxAiDeploymentGenerationApi;
import org.springaicommunity.watsonx.generation.WatsonxAiDeploymentGenerationModel;
import org.springaicommunity.watsonx.generation.WatsonxAiDeploymentGenerationOptions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests {@link WatsonxAiDeploymentGenerationAutoConfiguration}.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiDeploymentGenerationAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WatsonxAiDeploymentGenerationAutoConfiguration.class))
		.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key");

	// A deployment belongs to its space, so neither project-id nor space-id is needed
	@Test
	void createsTheApiAndModelWithoutProjectOrSpaceId() {
		this.contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(WatsonxAiDeploymentGenerationApi.class);
			assertThat(context).hasSingleBean(WatsonxAiDeploymentGenerationModel.class);
		});
	}

	@Test
	void canBeDisabled() {
		this.contextRunner.withPropertyValues("spring.ai.watsonx.ai.deployment-generation.enabled=false")
			.run(context -> {
				assertThat(context).doesNotHaveBean(WatsonxAiDeploymentGenerationApi.class);
				assertThat(context).doesNotHaveBean(WatsonxAiDeploymentGenerationModel.class);
			});
	}

	@Test
	void bindsTheDefaultOptions() {
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.deployment-generation.options.deployment-id=my-deployment",
					"spring.ai.watsonx.ai.deployment-generation.options.max-new-tokens=50",
					"spring.ai.watsonx.ai.deployment-generation.options.decoding-method=greedy",
					"spring.ai.watsonx.ai.deployment-generation.options.stop-sequences=END,STOP")
			.run(context -> {
				WatsonxAiDeploymentGenerationOptions options = context.getBean(WatsonxAiDeploymentGenerationModel.class)
					.getDefaultOptions();
				assertThat(options.getDeploymentId()).isEqualTo("my-deployment");
				assertThat(options.getMaxNewTokens()).isEqualTo(50);
				assertThat(options.getDecodingMethod()).isEqualTo("greedy");
				assertThat(options.getStopSequences()).isEqualTo(List.of("END", "STOP"));
				assertThat(options.getTemperature()).isNull();
			});
	}

	@Test
	void usesTheDefaultEndpointsAndVersion() {
		this.contextRunner.run(context -> {
			WatsonxAiDeploymentGenerationApi api = context.getBean(WatsonxAiDeploymentGenerationApi.class);
			assertThat(ReflectionTestUtils.getField(api, "generationEndpoint"))
				.isEqualTo("/ml/v1/deployments/{deployment_id}/text/generation");
			assertThat(ReflectionTestUtils.getField(api, "streamEndpoint"))
				.isEqualTo("/ml/v1/deployments/{deployment_id}/text/generation_stream");
			assertThat(ReflectionTestUtils.getField(api, "version")).isEqualTo("2024-10-17");
		});
	}

	@Test
	void endpointsAndVersionCanBeChanged() {
		this.contextRunner
			.withPropertyValues(
					"spring.ai.watsonx.ai.deployment-generation.generation-endpoint=/custom/{deployment_id}/generation",
					"spring.ai.watsonx.ai.deployment-generation.stream-endpoint=/custom/{deployment_id}/stream",
					"spring.ai.watsonx.ai.deployment-generation.version=2025-01-01")
			.run(context -> {
				WatsonxAiDeploymentGenerationApi api = context.getBean(WatsonxAiDeploymentGenerationApi.class);
				assertThat(ReflectionTestUtils.getField(api, "generationEndpoint"))
					.isEqualTo("/custom/{deployment_id}/generation");
				assertThat(ReflectionTestUtils.getField(api, "streamEndpoint"))
					.isEqualTo("/custom/{deployment_id}/stream");
				assertThat(ReflectionTestUtils.getField(api, "version")).isEqualTo("2025-01-01");
			});
	}

}
