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

package org.springaicommunity.watsonx.autoconfigure.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Tests the default chat options of {@link WatsonxAiChatProperties}. Every default is
 * sent to watsonx.ai, so a default must work with every chat model.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatPropertiesDefaultsTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(PropertiesConfiguration.class);

	// mistral-large-2512 rejects max_completion_tokens with 400 extra_forbidden, so it
	// is only sent when the application sets it
	@Test
	void maxCompletionTokensHasNoDefault() {
		this.contextRunner.run(context -> assertThat(
				context.getBean(WatsonxAiChatProperties.class).getOptions().getMaxCompletionTokens())
			.isNull());
	}

	@Test
	void maxCompletionTokensIsSetWhenConfigured() {
		this.contextRunner.withPropertyValues("spring.ai.watsonx.ai.chat.options.max-completion-tokens=256")
			.run(context -> {
				WatsonxAiChatOptions options = context.getBean(WatsonxAiChatProperties.class).getOptions();
				assertThat(options.getMaxCompletionTokens()).isEqualTo(256);
			});
	}

	// Every chat model on watsonx.ai answers logprobs, even false, with a
	// param_not_supported warning, so it is only sent when the application sets it
	@Test
	void logprobsHasNoDefault() {
		this.contextRunner
			.run(context -> assertThat(context.getBean(WatsonxAiChatProperties.class).getOptions().getLogprobs())
				.isNull());
	}

	@Test
	void logprobsIsSetWhenConfigured() {
		this.contextRunner.withPropertyValues("spring.ai.watsonx.ai.chat.options.logprobs=true")
			.run(context -> assertThat(context.getBean(WatsonxAiChatProperties.class).getOptions().getLogprobs())
				.isTrue());
	}

	@Test
	void logprobsAndTopLogprobsCanBeConfigured() {
		this.contextRunner
			.withPropertyValues("spring.ai.watsonx.ai.chat.options.logprobs=true",
					"spring.ai.watsonx.ai.chat.options.top-logprobs=5")
			.run(context -> {
				assertThat(context).hasNotFailed();
				WatsonxAiChatOptions options = context.getBean(WatsonxAiChatProperties.class).getOptions();
				assertThat(options.getLogprobs()).isTrue();
				assertThat(options.getTopLogprobs()).isEqualTo(5);
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WatsonxAiChatProperties.class)
	static class PropertiesConfiguration {

	}

}
