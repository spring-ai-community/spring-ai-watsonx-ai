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

package org.springaicommunity.watsonx.texttospeech;

import java.util.List;
import java.util.function.Consumer;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API implementation of watsonx.ai gateway text-to-speech API.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public class WatsonxAiTextToSpeechApi {

	public static final String DEFAULT_SPEECH_ENDPOINT = "/ml/gateway/v1/audio/speech";

	public static final String DEFAULT_VERSION = "2024-03-14";

	private final RestClient restClient;

	private final WebClient webClient;

	private final WatsonxAiAuthentication watsonxAiAuthentication;

	private final String speechEndpoint;

	private final String projectId;

	private final String spaceId;

	private final String version;

	public WatsonxAiTextToSpeechApi(final String baseUrl, final String speechEndpoint, final String version,
			final String projectId, final String spaceId, final String apiKey,
			final RestClient.Builder restClientBuilder, final WebClient.Builder webClientBuilder,
			final ResponseErrorHandler responseErrorHandler) {

		this.speechEndpoint = speechEndpoint;
		this.version = version;
		this.projectId = projectId;
		this.spaceId = spaceId;
		this.watsonxAiAuthentication = new WatsonxAiAuthentication(apiKey);

		final Consumer<HttpHeaders> defaultHeaders = headers -> {
			headers.setAccept(List.of(MediaType.ALL));
		};

		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultStatusHandler(responseErrorHandler)
			.defaultHeaders(defaultHeaders)
			.build();

		this.webClient = webClientBuilder.baseUrl(baseUrl).defaultHeaders(defaultHeaders).build();
	}

	public WatsonxAiTextToSpeechApi(final String baseUrl, final String speechEndpoint, final String version,
			final String projectId, final String apiKey, final RestClient.Builder restClientBuilder,
			final WebClient.Builder webClientBuilder, final ResponseErrorHandler responseErrorHandler) {
		this(baseUrl, speechEndpoint, version, projectId, null, apiKey, restClientBuilder, webClientBuilder,
				responseErrorHandler);
	}

	/**
	 * Synchronous call to watsonx.ai text-to-speech gateway API.
	 * @param request the text-to-speech request
	 * @return the response entity containing audio bytes
	 */
	public ResponseEntity<byte[]> textToSpeech(final WatsonxAiTextToSpeechRequest request) {
		Assert.notNull(request, "Watsonx.ai text-to-speech request cannot be null");

		WatsonxAiTextToSpeechRequest payload = preparePayload(request);

		return this.restClient.post().uri(uriBuilder -> {
			uriBuilder.path(this.speechEndpoint);
			if (StringUtils.hasText(this.version)) {
				uriBuilder.queryParam("version", this.version);
			}
			return uriBuilder.build();
		})
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.contentType(MediaType.APPLICATION_JSON)
			.body(payload)
			.retrieve()
			.toEntity(byte[].class);
	}

	/**
	 * Asynchronous streaming call to watsonx.ai text-to-speech gateway API.
	 * @param request the text-to-speech request
	 * @return a Flux stream of audio byte chunks
	 */
	public Flux<byte[]> stream(final WatsonxAiTextToSpeechRequest request) {
		Assert.notNull(request, "Watsonx.ai text-to-speech request cannot be null");

		WatsonxAiTextToSpeechRequest payload = preparePayload(request);

		return this.webClient.post().uri(uriBuilder -> {
			uriBuilder.path(this.speechEndpoint);
			if (StringUtils.hasText(this.version)) {
				uriBuilder.queryParam("version", this.version);
			}
			return uriBuilder.build();
		})
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.ALL)
			.body(Mono.just(payload), WatsonxAiTextToSpeechRequest.class)
			.retrieve()
			.bodyToFlux(byte[].class);
	}

	private WatsonxAiTextToSpeechRequest preparePayload(WatsonxAiTextToSpeechRequest request) {
		WatsonxAiTextToSpeechRequest.Builder builder = request.toBuilder();
		if (request.projectId() == null && StringUtils.hasText(this.projectId)) {
			builder.projectId(this.projectId);
		}
		if (request.spaceId() == null && StringUtils.hasText(this.spaceId)) {
			builder.spaceId(this.spaceId);
		}
		return builder.build();
	}

}
