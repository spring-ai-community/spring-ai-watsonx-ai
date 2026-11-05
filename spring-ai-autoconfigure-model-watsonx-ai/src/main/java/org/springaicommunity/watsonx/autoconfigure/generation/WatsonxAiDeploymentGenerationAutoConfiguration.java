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

import org.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import org.springaicommunity.watsonx.generation.WatsonxAiDeploymentGenerationApi;
import org.springaicommunity.watsonx.generation.WatsonxAiDeploymentGenerationModel;
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
 * Auto-configures calls to watsonx.ai text generation deployments as part of Spring AI.
 * The deployment ID can be set as a default
 * ({@code spring.ai.watsonx.ai.deployment-generation.options.deployment-id}) or per call.
 *
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, WebClientAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(WatsonxAiDeploymentGenerationApi.class)
@ConditionalOnProperty(prefix = WatsonxAiDeploymentGenerationProperties.CONFIG_PREFIX, name = "enabled",
		havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({ WatsonxAiConnectionProperties.class, WatsonxAiDeploymentGenerationProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class })
public class WatsonxAiDeploymentGenerationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiDeploymentGenerationApi watsonxAiDeploymentGenerationApi(
			final WatsonxAiConnectionProperties connectionProperties,
			final WatsonxAiDeploymentGenerationProperties generationProperties,
			final ObjectProvider<RestClient.Builder> restClientObjectProvider,
			final ObjectProvider<WebClient.Builder> webClientObjectProvider,
			final ResponseErrorHandler responseErrorHandler) {

		// No project or space ID: a deployment belongs to its space, and watsonx.ai
		// rejects them in a deployment request
		return new WatsonxAiDeploymentGenerationApi(connectionProperties.getBaseUrl(),
				generationProperties.getGenerationEndpoint(), generationProperties.getStreamEndpoint(),
				generationProperties.getVersion(), connectionProperties.getApiKey(),
				restClientObjectProvider.getIfAvailable(RestClient::builder),
				webClientObjectProvider.getIfAvailable(WebClient::builder), responseErrorHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiDeploymentGenerationModel watsonxAiDeploymentGenerationModel(
			final WatsonxAiDeploymentGenerationApi watsonxAiDeploymentGenerationApi,
			final WatsonxAiDeploymentGenerationProperties generationProperties, final RetryTemplate retryTemplate) {

		return new WatsonxAiDeploymentGenerationModel(watsonxAiDeploymentGenerationApi,
				generationProperties.getOptions(), retryTemplate);
	}

}
