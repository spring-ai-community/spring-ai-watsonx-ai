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

package org.springaicommunity.watsonx.generation;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.util.WatsonxAiWarnings;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * Calls a watsonx.ai text generation deployment, such as a deployed freeform or
 * structured prompt template. The prompt is in the deployment: the caller sends the
 * values of its variables, and gets the generated text.
 * <p>
 * This is not a Spring AI {@code ChatModel}: a text generation deployment doesn't take
 * messages, so a chat prompt template's deployment goes through
 * {@code WatsonxAiChatModel} with a {@code deploymentId} instead.
 *
 * <pre>{@code
 * WatsonxAiDeploymentGenerationResponse response = model.generate(
 * 		Map.of("question", "What is the capital of Japan?"),
 * 		WatsonxAiDeploymentGenerationOptions.builder().deploymentId(deploymentId).build());
 * String text = response.generatedText();
 * }</pre>
 *
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
public class WatsonxAiDeploymentGenerationModel {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiDeploymentGenerationModel.class);

	private final WatsonxAiDeploymentGenerationApi api;

	private final WatsonxAiDeploymentGenerationOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	/**
	 * Creates the model.
	 * @param api the API
	 * @param defaultOptions the options for every call, such as the deployment ID
	 * @param retryTemplate the retry template for synchronous calls
	 */
	public WatsonxAiDeploymentGenerationModel(final WatsonxAiDeploymentGenerationApi api,
			final WatsonxAiDeploymentGenerationOptions defaultOptions, final RetryTemplate retryTemplate) {
		Assert.notNull(api, "WatsonxAiDeploymentGenerationApi must not be null");
		Assert.notNull(defaultOptions, "WatsonxAiDeploymentGenerationOptions must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");
		this.api = api;
		this.defaultOptions = defaultOptions;
		this.retryTemplate = retryTemplate;
	}

	/**
	 * Generates text with the default options.
	 * @param promptVariables the values of the prompt template's variables
	 * @return the response
	 */
	public WatsonxAiDeploymentGenerationResponse generate(final Map<String, String> promptVariables) {
		return generate(promptVariables, null);
	}

	/**
	 * Generates text.
	 * @param promptVariables the values of the prompt template's variables
	 * @param options options for this call, which take precedence over the default
	 * options; may be {@code null}
	 * @return the response
	 */
	public WatsonxAiDeploymentGenerationResponse generate(final Map<String, String> promptVariables,
			final WatsonxAiDeploymentGenerationOptions options) {
		WatsonxAiDeploymentGenerationOptions mergedOptions = this.defaultOptions.merge(options);
		String deploymentId = deploymentIdOf(mergedOptions);
		WatsonxAiDeploymentGenerationRequest request = createRequest(promptVariables, mergedOptions);

		WatsonxAiDeploymentGenerationResponse response = RetryUtils.execute(this.retryTemplate,
				() -> this.api.generate(deploymentId, request).getBody());
		logWarnings(response);
		return response;
	}

	/**
	 * Streams the generated text with the default options.
	 * @param promptVariables the values of the prompt template's variables
	 * @return one response per chunk of text
	 */
	public Flux<WatsonxAiDeploymentGenerationResponse> stream(final Map<String, String> promptVariables) {
		return stream(promptVariables, null);
	}

	/**
	 * Streams the generated text.
	 * @param promptVariables the values of the prompt template's variables
	 * @param options options for this call, which take precedence over the default
	 * options; may be {@code null}
	 * @return one response per chunk of text
	 */
	public Flux<WatsonxAiDeploymentGenerationResponse> stream(final Map<String, String> promptVariables,
			final WatsonxAiDeploymentGenerationOptions options) {
		WatsonxAiDeploymentGenerationOptions mergedOptions = this.defaultOptions.merge(options);
		String deploymentId = deploymentIdOf(mergedOptions);
		WatsonxAiDeploymentGenerationRequest request = createRequest(promptVariables, mergedOptions);

		return this.api.stream(deploymentId, request).doOnNext(WatsonxAiDeploymentGenerationModel::logWarnings);
	}

	/**
	 * The default options.
	 * @return a copy of the default options
	 */
	public WatsonxAiDeploymentGenerationOptions getDefaultOptions() {
		return this.defaultOptions.copy();
	}

	private static String deploymentIdOf(final WatsonxAiDeploymentGenerationOptions options) {
		Assert.isTrue(StringUtils.hasText(options.getDeploymentId()),
				"A deployment ID is required: set spring.ai.watsonx.ai.deployment-generation.options.deployment-id, "
						+ "or pass it in the options");
		return options.getDeploymentId();
	}

	private static WatsonxAiDeploymentGenerationRequest createRequest(final Map<String, String> promptVariables,
			final WatsonxAiDeploymentGenerationOptions options) {
		if (CollectionUtils.isEmpty(promptVariables)) {
			// watsonx.ai doesn't apply the template's default values, so the model gets
			// the raw {variable} placeholders
			logger.warn(
					"No prompt variables for deployment '{}': watsonx.ai doesn't apply the prompt template's "
							+ "default values, so the model gets the template's placeholders",
					options.getDeploymentId());
		}
		return new WatsonxAiDeploymentGenerationRequest(new WatsonxAiDeploymentGenerationRequest.Parameters(
				CollectionUtils.isEmpty(promptVariables) ? null : Map.copyOf(promptVariables),
				options.getDecodingMethod(), options.getMaxNewTokens(), options.getMinNewTokens(),
				options.getTemperature(), options.getTopP(), options.getTopK(), options.getRepetitionPenalty(),
				options.getRandomSeed(), options.getStopSequences(), options.getTimeLimit()));
	}

	private static void logWarnings(final WatsonxAiDeploymentGenerationResponse response) {
		if (response != null && response.system() != null && response.system().warnings() != null) {
			response.system()
				.warnings()
				.forEach(warning -> WatsonxAiWarnings.log(response.modelId(), warning.id(), warning.message()));
		}
	}

}
