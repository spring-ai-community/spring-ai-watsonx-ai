/*
 * Copyright 2025 the original author or authors.
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

package org.springaicommunity.watsonx.chat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springaicommunity.watsonx.chat.util.WatsonxAiChatChunkMerger;
import org.springframework.ai.util.JsonHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API implementation of watsonx.ai Chat Model API.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiChatApi {

	private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private final WatsonxAiChatChunkMerger chunkMerger = new WatsonxAiChatChunkMerger();

	private final RestClient restClient;

	private final WebClient webClient;

	private final WatsonxAiAuthentication watsonxAiAuthentication;

	private final ResponseErrorHandler responseErrorHandler;

	private String textEndpoint;

	private String streamEndpoint;

	private String projectId;

	private String spaceId;

	private String version;

	public WatsonxAiChatApi(final String baseUrl, final String textEndpoint, final String streamEndpoint,
			final String version, final String projectId, final String spaceId, final String apiKey,
			final RestClient.Builder restClientBuilder, final WebClient.Builder webClientBuilder,
			final ResponseErrorHandler responseErrorHandler) {

		this.textEndpoint = textEndpoint;
		this.streamEndpoint = streamEndpoint;
		this.version = version;
		this.projectId = projectId;
		this.spaceId = spaceId;
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
	 * Synchronous call to watsonx.ai Chat API.
	 * @param watsonxAiChatRequest the watsonx.ai chat request
	 * @return the response entity containing the watsonx.ai chat response
	 */
	public ResponseEntity<WatsonxAiChatResponse> chat(final WatsonxAiChatRequest watsonxAiChatRequest) {
		Assert.notNull(watsonxAiChatRequest, "Watsonx.ai request cannot be null");

		return restClient.post()
			.uri(uriBuilder -> uriBuilder.path(this.textEndpoint).queryParam("version", this.version).build())
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.body(watsonxAiChatRequest.toBuilder().projectId(projectId).spaceId(spaceId).build())
			.retrieve()
			.toEntity(WatsonxAiChatResponse.class);
	}

	/**
	 * Asynchronous call to watsonx.ai Chat API using streaming.
	 * @param watsonxAiChatRequest the watsonx.ai chat request
	 * @return a Flux stream of watsonx.ai chat responses
	 */
	public Flux<WatsonxAiChatStream> stream(final WatsonxAiChatRequest watsonxAiChatRequest) {
		Assert.notNull(watsonxAiChatRequest, "Watsonx.ai request cannot be null");

		// Deferred, so every subscription (including a retry or a second subscribe of
		// the same Flux) sends its own request and gets its own tool call state
		return Flux.defer(() -> {
			final AtomicBoolean isInsideTool = new AtomicBoolean(false);

			return this.webClient.post()
				.uri(uriBuilder -> uriBuilder.path(this.streamEndpoint).queryParam("version", this.version).build())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
				.body(Mono.just(watsonxAiChatRequest.toBuilder().projectId(projectId).spaceId(spaceId).build()),
						WatsonxAiChatRequest.class)
				.retrieve()
				.onStatus(HttpStatusCode::isError, this::toStreamError)
				.bodyToFlux(String.class)
				.takeUntil(SSE_DONE_PREDICATE)
				.filter(SSE_DONE_PREDICATE.negate())
				.map(content -> JSON_HELPER.fromJson(content, WatsonxAiChatStream.class))
				.map(chunk -> {
					if (this.chunkMerger.isStreamingToolFunctionCall(chunk)) {
						isInsideTool.set(true);
					}
					return chunk;
				})
				.windowUntil(chunk -> {
					if (isInsideTool.get() && this.chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
						isInsideTool.set(false);
						return true;
					}
					return !isInsideTool.get();
				})
				.concatMapIterable(window -> {
					Mono<WatsonxAiChatStream> monoChunk = window.reduce(
							new WatsonxAiChatStream(null, null, null, null, null, null, null, null),
							(previous, current) -> this.chunkMerger.merge(previous, current));
					return List.of(monoChunk);
				})
				.flatMap(mono -> mono);
		});
	}

	/**
	 * Turns an error response of a stream into the same exception that a synchronous call
	 * gets, by passing it to the configured {@link ResponseErrorHandler}.
	 */
	private Mono<Throwable> toStreamError(final ClientResponse response) {
		return response.bodyToMono(byte[].class).defaultIfEmpty(new byte[0]).<Throwable>map(body -> {
			BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(response, body);
			try {
				if (this.responseErrorHandler.hasError(bufferedResponse)) {
					this.responseErrorHandler.handleError(response.request().getURI(), response.request().getMethod(),
							bufferedResponse);
				}
			}
			catch (IOException | RuntimeException ex) {
				return ex;
			}
			// The handler didn't throw: fall back to WebClient's default exception
			return WebClientResponseException.create(response.statusCode(), bufferedResponse.getStatusText(),
					response.headers().asHttpHeaders(), body, null, response.request());
		});
	}

	/**
	 * A {@link ClientHttpResponse} over a {@link ClientResponse} whose body has already
	 * been read.
	 */
	private static final class BufferedClientHttpResponse implements ClientHttpResponse {

		private final ClientResponse response;

		private final byte[] body;

		private BufferedClientHttpResponse(final ClientResponse response, final byte[] body) {
			this.response = response;
			this.body = body;
		}

		@Override
		public HttpStatusCode getStatusCode() {
			return this.response.statusCode();
		}

		@Override
		public String getStatusText() {
			HttpStatus status = HttpStatus.resolve(this.response.statusCode().value());
			return status != null ? status.getReasonPhrase() : "";
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.response.headers().asHttpHeaders();
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(this.body);
		}

		@Override
		public void close() {
		}

	}

}
