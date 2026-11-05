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

import java.util.List;
import java.util.function.Consumer;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springaicommunity.watsonx.util.WatsonxAiStreamErrors;
import org.springframework.ai.util.JsonHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API for watsonx.ai text generation deployments, such as a deployed freeform or
 * structured prompt template ({@code /ml/v1/deployments/{id}/text/generation} and
 * {@code generation_stream}). The deployment belongs to a space, so no project or space
 * ID is sent.
 *
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
public class WatsonxAiDeploymentGenerationApi {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private final RestClient restClient;

	private final WebClient webClient;

	private final WatsonxAiAuthentication watsonxAiAuthentication;

	private final ResponseErrorHandler responseErrorHandler;

	private final String generationEndpoint;

	private final String streamEndpoint;

	private final String version;

	/**
	 * Creates the API.
	 * @param baseUrl the watsonx.ai URL, such as
	 * {@code https://us-south.ml.cloud.ibm.com}
	 * @param generationEndpoint the endpoint, with a {@code {deployment_id}} placeholder
	 * @param streamEndpoint the streaming endpoint, with a {@code {deployment_id}}
	 * placeholder
	 * @param version the API version date, such as {@code 2024-10-17}
	 * @param apiKey the IBM Cloud API key
	 * @param restClientBuilder the builder for synchronous calls
	 * @param webClientBuilder the builder for streaming
	 * @param responseErrorHandler the handler for error responses
	 */
	public WatsonxAiDeploymentGenerationApi(final String baseUrl, final String generationEndpoint,
			final String streamEndpoint, final String version, final String apiKey,
			final RestClient.Builder restClientBuilder, final WebClient.Builder webClientBuilder,
			final ResponseErrorHandler responseErrorHandler) {
		Assert.hasText(generationEndpoint, "Generation endpoint cannot be null or empty");
		Assert.hasText(streamEndpoint, "Stream endpoint cannot be null or empty");

		this.generationEndpoint = generationEndpoint;
		this.streamEndpoint = streamEndpoint;
		this.version = version;
		this.watsonxAiAuthentication = new WatsonxAiAuthentication(apiKey);
		this.responseErrorHandler = responseErrorHandler;

		final Consumer<HttpHeaders> defaultHeaders = headers -> {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		};

		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultStatusHandler(responseErrorHandler)
			.defaultHeaders(defaultHeaders)
			.build();

		this.webClient = webClientBuilder.baseUrl(baseUrl).defaultHeaders(defaultHeaders).build();
	}

	/**
	 * Synchronous call to a text generation deployment.
	 * @param deploymentId the deployment ID or serving name
	 * @param request the prompt variables and generation parameters
	 * @return the response entity containing the generated text
	 */
	public ResponseEntity<WatsonxAiDeploymentGenerationResponse> generate(final String deploymentId,
			final WatsonxAiDeploymentGenerationRequest request) {
		Assert.hasText(deploymentId, "Watsonx.ai deployment ID cannot be null or empty");
		Assert.notNull(request, "Watsonx.ai deployment generation request cannot be null");

		return this.restClient.post()
			.uri(uriBuilder -> uriBuilder.path(this.generationEndpoint)
				.queryParam("version", this.version)
				.build(deploymentId))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.body(request)
			.retrieve()
			.toEntity(WatsonxAiDeploymentGenerationResponse.class);
	}

	/**
	 * Streaming call to a text generation deployment: one response per chunk of text.
	 * @param deploymentId the deployment ID or serving name
	 * @param request the prompt variables and generation parameters
	 * @return the chunks of the response
	 */
	public Flux<WatsonxAiDeploymentGenerationResponse> stream(final String deploymentId,
			final WatsonxAiDeploymentGenerationRequest request) {
		Assert.hasText(deploymentId, "Watsonx.ai deployment ID cannot be null or empty");
		Assert.notNull(request, "Watsonx.ai deployment generation request cannot be null");

		// Deferred, so every subscription (such as a retry) gets a fresh IAM token
		return Flux.defer(() -> this.webClient.post()
			.uri(uriBuilder -> uriBuilder.path(this.streamEndpoint)
				.queryParam("version", this.version)
				.build(deploymentId))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.accept(MediaType.TEXT_EVENT_STREAM)
			.body(Mono.just(request), WatsonxAiDeploymentGenerationRequest.class)
			.retrieve()
			.onStatus(HttpStatusCode::isError,
					response -> WatsonxAiStreamErrors.toException(response, this.responseErrorHandler))
			.bodyToFlux(String.class)
			.map(content -> JSON_HELPER.fromJson(content, WatsonxAiDeploymentGenerationResponse.class)));
	}

}
