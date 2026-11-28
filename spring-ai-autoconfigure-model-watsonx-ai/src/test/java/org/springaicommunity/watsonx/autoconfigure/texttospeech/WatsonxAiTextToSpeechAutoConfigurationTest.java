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

package org.springaicommunity.watsonx.autoconfigure.texttospeech;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.texttospeech.WatsonxAiTextToSpeechApi;
import org.springaicommunity.watsonx.texttospeech.WatsonxAiTextToSpeechModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Unit tests for {@link WatsonxAiTextToSpeechAutoConfiguration}.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
class WatsonxAiTextToSpeechAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WatsonxAiTextToSpeechAutoConfiguration.class));

	@Test
	void textToSpeechDisabledByProperty() {
		this.contextRunner.withPropertyValues("spring.ai.watsonx.ai.text-to-speech.enabled=false").run(context -> {
			assertThat(context).doesNotHaveBean(WatsonxAiTextToSpeechApi.class);
			assertThat(context).doesNotHaveBean(WatsonxAiTextToSpeechModel.class);
		});
	}

	@Test
	void textToSpeechDisabledByModelSelection() {
		this.contextRunner.withPropertyValues("spring.ai.model.audio.speech=other-provider").run(context -> {
			assertThat(context).doesNotHaveBean(WatsonxAiTextToSpeechApi.class);
			assertThat(context).doesNotHaveBean(WatsonxAiTextToSpeechModel.class);
		});
	}

	@Test
	void textToSpeechEnabledByDefaultWithCredentials() {
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key",
					"spring.ai.watsonx.ai.project-id=test-project-id",
					"spring.ai.watsonx.ai.base-url=https://us-south.ml.cloud.ibm.com")
			.run(context -> {
				assertThat(context).hasSingleBean(WatsonxAiTextToSpeechApi.class);
				assertThat(context).hasSingleBean(WatsonxAiTextToSpeechModel.class);
			});
	}

	@Test
	void textToSpeechWithOptions() {
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.api-key=test-api-key",
					"spring.ai.watsonx.ai.project-id=test-project-id",
					"spring.ai.watsonx.ai.text-to-speech.options.model=custom-tts-model",
					"spring.ai.watsonx.ai.text-to-speech.options.voice=custom-voice",
					"spring.ai.watsonx.ai.text-to-speech.options.speed=1.25",
					"spring.ai.watsonx.ai.text-to-speech.options.response-format=wav")
			.run(context -> {
				assertThat(context).hasSingleBean(WatsonxAiTextToSpeechModel.class);
				WatsonxAiTextToSpeechModel model = context.getBean(WatsonxAiTextToSpeechModel.class);
				assertThat(model.getOptions().getModel()).isEqualTo("custom-tts-model");
				assertThat(model.getOptions().getVoice()).isEqualTo("custom-voice");
				assertThat(model.getOptions().getSpeed()).isEqualTo(1.25);
				assertThat(model.getOptions().getResponseFormat()).isEqualTo("wav");
			});
	}

}
