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

import io.micrometer.observation.ObservationRegistry;
import org.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionApi;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionModel;
import org.springaicommunity.watsonx.textextraction.observation.TextExtractionModelObservationConvention;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Auto-configures watsonx.ai text extraction services as part of Spring AI.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(WatsonxAiTextExtractionApi.class)
@ConditionalOnProperty(prefix = WatsonxAiTextExtractionProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties({ WatsonxAiConnectionProperties.class, WatsonxAiTextExtractionProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class })
public class WatsonxAiTextExtractionAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiTextExtractionApi watsonxAiTextExtractionApi(
			final WatsonxAiConnectionProperties connectionProperties,
			final WatsonxAiTextExtractionProperties textExtractionProperties,
			final ObjectProvider<RestClient.Builder> restClientObjectProvider,
			ResponseErrorHandler responseErrorHandler) {

		return new WatsonxAiTextExtractionApi(connectionProperties.getBaseUrl(),
				textExtractionProperties.getTextExtractionEndpoint(), textExtractionProperties.getVersion(),
				connectionProperties.getProjectId(), connectionProperties.getSpaceId(),
				connectionProperties.getApiKey(), restClientObjectProvider.getIfAvailable(RestClient::builder),
				responseErrorHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiTextExtractionModel watsonxAiTextExtractionModel(
			WatsonxAiTextExtractionApi watsonxAiTextExtractionApi,
			WatsonxAiTextExtractionProperties textExtractionProperties,
			ObjectProvider<ObservationRegistry> observationRegistry, RetryTemplate retryTemplate,
			ObjectProvider<TextExtractionModelObservationConvention> observationConvention) {

		var model = new WatsonxAiTextExtractionModel(watsonxAiTextExtractionApi, textExtractionProperties.getOptions(),
				observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP), retryTemplate);

		observationConvention.ifUnique(model::setObservationConvention);

		return model;
	}

}
