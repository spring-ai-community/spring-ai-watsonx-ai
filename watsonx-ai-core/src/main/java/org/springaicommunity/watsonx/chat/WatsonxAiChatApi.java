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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springaicommunity.watsonx.chat.util.WatsonxAiChatChunkMerger;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
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
import org.springframework.web.util.UriBuilder;
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

	private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_EVENT = new ParameterizedTypeReference<>() {
	};

	/**
	 * The name of the event with which watsonx.ai reports an error once the stream has
	 * started with HTTP 200, such as an invalid argument found by the model:
	 * {@code event: error} with the error body, and its {@code status_code}, as data.
	 */
	private static final String ERROR_EVENT = "error";

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private final WatsonxAiChatChunkMerger chunkMerger = new WatsonxAiChatChunkMerger();

	private static final String DEPLOYMENT_CHAT_ENDPOINT = "/ml/v1/deployments/{deployment_id}/text/chat";

	private static final String DEPLOYMENT_STREAM_ENDPOINT = "/ml/v1/deployments/{deployment_id}/text/chat_stream";

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
	 * Synchronous call to a watsonx.ai deployment, such as a deployed prompt template or
	 * tuned model. The deployment decides the model and its parameters, so only the
	 * messages and tools are sent.
	 * @param deploymentId the deployment ID or serving name
	 * @param watsonxAiChatRequest the watsonx.ai chat request
	 * @return the response entity containing the watsonx.ai chat response
	 * @since 2.1.0
	 */
	public ResponseEntity<WatsonxAiChatResponse> deploymentChat(final String deploymentId,
			final WatsonxAiChatRequest watsonxAiChatRequest) {
		Assert.hasText(deploymentId, "Watsonx.ai deployment ID cannot be null or empty");
		Assert.notNull(watsonxAiChatRequest, "Watsonx.ai request cannot be null");

		return restClient.post()
			.uri(uriBuilder -> uriBuilder.path(DEPLOYMENT_CHAT_ENDPOINT)
				.queryParam("version", this.version)
				.build(deploymentId))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.body(watsonxAiChatRequest)
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

		return stream(uriBuilder -> uriBuilder.path(this.streamEndpoint).queryParam("version", this.version).build(),
				watsonxAiChatRequest.toBuilder().projectId(projectId).spaceId(spaceId).build());
	}

	/**
	 * Asynchronous call to a watsonx.ai deployment using streaming. The deployment
	 * decides the model and its parameters, so only the messages and tools are sent.
	 * @param deploymentId the deployment ID or serving name
	 * @param watsonxAiChatRequest the watsonx.ai chat request
	 * @return a Flux stream of watsonx.ai chat responses
	 * @since 2.1.0
	 */
	public Flux<WatsonxAiChatStream> deploymentStream(final String deploymentId,
			final WatsonxAiChatRequest watsonxAiChatRequest) {
		Assert.hasText(deploymentId, "Watsonx.ai deployment ID cannot be null or empty");
		Assert.notNull(watsonxAiChatRequest, "Watsonx.ai request cannot be null");

		return stream(uriBuilder -> uriBuilder.path(DEPLOYMENT_STREAM_ENDPOINT)
			.queryParam("version", this.version)
			.build(deploymentId), watsonxAiChatRequest);
	}

	private Flux<WatsonxAiChatStream> stream(final Function<UriBuilder, URI> uri, final WatsonxAiChatRequest body) {
		// Deferred, so every subscription (including a retry or a second subscribe of
		// the same Flux) sends its own request and gets its own tool call state
		return Flux.defer(() -> {
			final AtomicBoolean isInsideTool = new AtomicBoolean(false);

			return this.webClient.post()
				.uri(uri)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
				.body(Mono.just(body), WatsonxAiChatRequest.class)
				.exchangeToFlux(
						response -> response.statusCode().isError() ? toStreamError(response).flatMapMany(Flux::error)
								: response.bodyToFlux(SSE_EVENT).concatMap(event -> dataOf(response, event)))
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
		// If the body can't be read (connection reset, codec limit), report the status
		// anyway, as Spring's DefaultClientResponse.createException() does
		return response.bodyToMono(byte[].class)
			.onErrorReturn(ex -> !(ex instanceof Error), new byte[0])
			.defaultIfEmpty(new byte[0])
			.map(body -> toError(response, response.statusCode(), response.headers().asHttpHeaders(), body));
	}

	/**
	 * The data of an event of the stream, or the error that an {@code error} event
	 * reports. Chunks sent before the error event have already been emitted.
	 */
	private Mono<String> dataOf(final ClientResponse response, final ServerSentEvent<String> event) {
		if (!ERROR_EVENT.equals(event.event())) {
			return Mono.justOrEmpty(event.data());
		}
		String data = (event.data() != null) ? event.data() : "";
		// The data is the error body that a synchronous call gets with the HTTP status
		// in its status_code, so the error is handled as that response would be
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return Mono.error(toError(response, errorStatusOf(data), headers, data.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * The HTTP status of an error event: its {@code status_code}, or 500 when it has
	 * none, as for an error that the server didn't classify.
	 */
	private static HttpStatusCode errorStatusOf(final String data) {
		try {
			Map<String, Object> error = JSON_HELPER.fromJsonToMap(data);
			if (error != null && error.get("status_code") instanceof Number statusCode
					&& HttpStatusCode.valueOf(statusCode.intValue()).isError()) {
				return HttpStatusCode.valueOf(statusCode.intValue());
			}
		}
		catch (RuntimeException ex) {
			// Not JSON, or not a status: the default below
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	/**
	 * The exception for an error response, from the configured
	 * {@link ResponseErrorHandler}.
	 */
	private Throwable toError(final ClientResponse response, final HttpStatusCode status, final HttpHeaders headers,
			final byte[] body) {
		BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(status, headers, body);
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
		return WebClientResponseException.create(status, bufferedResponse.getStatusText(), headers, body, null,
				response.request());
	}

	/**
	 * A {@link ClientHttpResponse} whose body has already been read: the error response
	 * of a stream, or the error that an {@code error} event of the stream reports.
	 */
	private static final class BufferedClientHttpResponse implements ClientHttpResponse {

		private final HttpStatusCode status;

		private final HttpHeaders headers;

		private final byte[] body;

		private BufferedClientHttpResponse(final HttpStatusCode status, final HttpHeaders headers, final byte[] body) {
			this.status = status;
			this.headers = headers;
			this.body = body;
		}

		@Override
		public HttpStatusCode getStatusCode() {
			return this.status;
		}

		@Override
		public String getStatusText() {
			HttpStatus resolved = HttpStatus.resolve(this.status.value());
			return resolved != null ? resolved.getReasonPhrase() : "";
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
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
