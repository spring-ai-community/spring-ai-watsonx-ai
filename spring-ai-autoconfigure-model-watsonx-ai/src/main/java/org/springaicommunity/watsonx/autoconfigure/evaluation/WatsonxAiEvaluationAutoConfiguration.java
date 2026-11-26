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

import org.springaicommunity.watsonx.autoconfigure.WatsonxAiConnectionProperties;
import org.springaicommunity.watsonx.evaluation.WatsonxAiAnswerRelevanceEvaluator;
import org.springaicommunity.watsonx.evaluation.WatsonxAiContextRelevanceEvaluator;
import org.springaicommunity.watsonx.evaluation.WatsonxAiDetectionApi;
import org.springaicommunity.watsonx.evaluation.WatsonxAiFaithfulnessEvaluator;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Auto-configures the watsonx.ai detection evaluators: answer relevance, context
 * relevance and faithfulness. They are off unless
 * {@code spring.ai.watsonx.ai.evaluation.enabled} is {@code true}, and then need
 * {@code spring.ai.watsonx.ai.evaluation.governance-instance-id}. A
 * {@link WatsonxAiDetectionApi} bean of the application, such as one scoped by project,
 * replaces the auto-configured one.
 *
 * @author Federico Mariani
 * @since 2.2.0
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(WatsonxAiDetectionApi.class)
@ConditionalOnProperty(prefix = WatsonxAiEvaluationProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties({ WatsonxAiConnectionProperties.class, WatsonxAiEvaluationProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class })
public class WatsonxAiEvaluationAutoConfiguration {

	static final String GOVERNANCE_INSTANCE_ID_PROPERTY = WatsonxAiEvaluationProperties.CONFIG_PREFIX
			+ ".governance-instance-id";

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiDetectionApi watsonxAiDetectionApi(final WatsonxAiConnectionProperties connectionProperties,
			final WatsonxAiEvaluationProperties evaluationProperties,
			final ObjectProvider<RestClient.Builder> restClientObjectProvider,
			final ResponseErrorHandler responseErrorHandler) {

		// watsonx.ai runs these detectors only for a watsonx.governance instance, and
		// then
		// takes no project or space
		if (!StringUtils.hasText(evaluationProperties.getGovernanceInstanceId())) {
			throw new InvalidConfigurationPropertyValueException(GOVERNANCE_INSTANCE_ID_PROPERTY, null,
					"The watsonx.ai detection evaluators need a watsonx.governance instance. Set "
							+ GOVERNANCE_INSTANCE_ID_PROPERTY
							+ " to its GUID, or set spring.ai.watsonx.ai.evaluation.enabled=false.");
		}

		return new WatsonxAiDetectionApi(connectionProperties.getBaseUrl(), evaluationProperties.getContextEndpoint(),
				evaluationProperties.getGeneratedEndpoint(), evaluationProperties.getVersion(),
				connectionProperties.getProjectId(), connectionProperties.getSpaceId(),
				evaluationProperties.getGovernanceInstanceId(), connectionProperties.getApiKey(),
				restClientObjectProvider.getIfAvailable(RestClient::builder), responseErrorHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiAnswerRelevanceEvaluator watsonxAiAnswerRelevanceEvaluator(WatsonxAiDetectionApi detectionApi,
			WatsonxAiEvaluationProperties evaluationProperties, RetryTemplate retryTemplate) {
		return WatsonxAiAnswerRelevanceEvaluator.builder()
			.detectionApi(detectionApi)
			.threshold(evaluationProperties.getAnswerRelevance().getThreshold())
			.retryTemplate(retryTemplate)
			.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiContextRelevanceEvaluator watsonxAiContextRelevanceEvaluator(WatsonxAiDetectionApi detectionApi,
			WatsonxAiEvaluationProperties evaluationProperties, RetryTemplate retryTemplate) {
		return WatsonxAiContextRelevanceEvaluator.builder()
			.detectionApi(detectionApi)
			.threshold(evaluationProperties.getContextRelevance().getThreshold())
			.retryTemplate(retryTemplate)
			.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public WatsonxAiFaithfulnessEvaluator watsonxAiFaithfulnessEvaluator(WatsonxAiDetectionApi detectionApi,
			WatsonxAiEvaluationProperties evaluationProperties, RetryTemplate retryTemplate) {
		return WatsonxAiFaithfulnessEvaluator.builder()
			.detectionApi(detectionApi)
			.threshold(evaluationProperties.getFaithfulness().getThreshold())
			.retryTemplate(retryTemplate)
			.build();
	}

}
