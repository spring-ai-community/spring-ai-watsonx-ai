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

import org.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import org.springaicommunity.watsonx.texttospeech.WatsonxAiTextToSpeechApi;
import org.springaicommunity.watsonx.texttospeech.WatsonxAiTextToSpeechModel;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configures watsonx.ai text-to-speech services as part of Spring AI.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, WebClientAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(WatsonxAiTextToSpeechApi.class)
@ConditionalOnProperty(name = SpringAIModelProperties.AUDIO_SPEECH_MODEL,
		havingValue = WatsonxAiTextToSpeechAutoConfiguration.MODEL_ID, matchIfMissing = true)
@ConditionalOnProperty(prefix = WatsonxAiTextToSpeechProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties({ WatsonxAiConnectionProperties.class, WatsonxAiTextToSpeechProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class })
public class WatsonxAiTextToSpeechAutoConfiguration {

	public static final String MODEL_ID = "watsonx-ai";

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiTextToSpeechApi watsonxAiTextToSpeechApi(final WatsonxAiConnectionProperties connectionProperties,
			final WatsonxAiTextToSpeechProperties speechProperties,
			final ObjectProvider<RestClient.Builder> restClientObjectProvider,
			final ObjectProvider<WebClient.Builder> webClientObjectProvider,
			ResponseErrorHandler responseErrorHandler) {

		return new WatsonxAiTextToSpeechApi(connectionProperties.getBaseUrl(), speechProperties.getSpeechEndpoint(),
				speechProperties.getVersion(), connectionProperties.getProjectId(), connectionProperties.getSpaceId(),
				connectionProperties.getApiKey(), restClientObjectProvider.getIfAvailable(RestClient::builder),
				webClientObjectProvider.getIfAvailable(WebClient::builder), responseErrorHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiTextToSpeechModel watsonxAiTextToSpeechModel(WatsonxAiTextToSpeechApi watsonxAiTextToSpeechApi,
			WatsonxAiTextToSpeechProperties speechProperties, RetryTemplate retryTemplate) {

		return WatsonxAiTextToSpeechModel.builder()
			.watsonxAiTextToSpeechApi(watsonxAiTextToSpeechApi)
			.options(speechProperties.getOptions())
			.retryTemplate(retryTemplate)
			.build();
	}

}
