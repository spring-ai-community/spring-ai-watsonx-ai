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

package org.springaicommunity.watsonx.autoconfigure.textextraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionApi;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Unit tests for {@link WatsonxAiTextExtractionAutoConfiguration}.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
class WatsonxAiTextExtractionAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WatsonxAiTextExtractionAutoConfiguration.class));

	@Test
	void textExtractionDisabled() {
		this.contextRunner.withPropertyValues("spring.ai.watsonx.ai.text-extraction.enabled=false").run(context -> {
			assertThat(context).doesNotHaveBean(WatsonxAiTextExtractionApi.class);
			assertThat(context).doesNotHaveBean(WatsonxAiTextExtractionModel.class);
		});
	}

	@Test
	void textExtractionEnabledByDefaultWithCredentials() {
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key",
					"spring.ai.watsonx.ai.project-id=test-project-id",
					"spring.ai.watsonx.ai.base-url=https://us-south.ml.cloud.ibm.com")
			.run(context -> {
				assertThat(context).hasSingleBean(WatsonxAiTextExtractionApi.class);
				assertThat(context).hasSingleBean(WatsonxAiTextExtractionModel.class);
			});
	}

	@Test
	void textExtractionWithOptions() {
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key",
					"spring.ai.watsonx.ai.project-id=test-project-id",
					"spring.ai.watsonx.ai.text-extraction.options.model=ibm-custom-doc",
					"spring.ai.watsonx.ai.text-extraction.options.enable-ocr=true")
			.run(context -> {
				assertThat(context).hasSingleBean(WatsonxAiTextExtractionModel.class);
				WatsonxAiTextExtractionModel model = context.getBean(WatsonxAiTextExtractionModel.class);
				assertThat(model.getDefaultOptions().getModel()).isEqualTo("ibm-custom-doc");
				assertThat(model.getDefaultOptions().getEnableOcr()).isTrue();
			});
	}

}
