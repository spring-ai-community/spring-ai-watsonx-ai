/*
 * Copyright 2025-2026 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springaicommunity.watsonx.auth.StubWatsonxAiAuthentication;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tests that {@link WatsonxAiChatApi#stream(WatsonxAiChatRequest)} reports an error
 * response the same way as {@link WatsonxAiChatApi#chat(WatsonxAiChatRequest)}: through
 * the configured {@link ResponseErrorHandler}, with watsonx.ai's error body in the
 * message. Uses stubbed HTTP, so no watsonx.ai or IBM IAM call is made.
 *
 * @author Ana Katrina Inguengan
 */
@ExtendWith(StubWatsonxAiAuthentication.class)
class WatsonxAiChatApiStreamErrorTest {

	private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";

	private static final String MODEL_NOT_FOUND = """
			{"errors":[{"code":"model_not_supported","message":"Model 'ibm/no-such-model' was not found."}],"status_code":404}""";

	private static final String SERVICE_UNAVAILABLE = """
			{"errors":[{"code":"service_unavailable","message":"Try again later."}],"status_code":503}""";

	@Test
	void clientErrorInStreamIsTheSameExceptionAsInCall() {
		Throwable callError = callError(HttpStatus.NOT_FOUND, MODEL_NOT_FOUND,
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
		Throwable streamError = streamError(HttpStatus.NOT_FOUND, MODEL_NOT_FOUND,
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);

		assertThat(callError).isInstanceOf(NonTransientAiException.class).hasMessageContaining("model_not_supported");
		assertThat(streamError).isInstanceOf(NonTransientAiException.class).hasMessage(callError.getMessage());
	}

	@Test
	void serverErrorInStreamIsTheSameExceptionAsInCall() {
		Throwable callError = callError(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE,
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
		Throwable streamError = streamError(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE,
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);

		assertThat(callError).isInstanceOf(TransientAiException.class).hasMessageContaining("service_unavailable");
		assertThat(streamError).isInstanceOf(TransientAiException.class).hasMessage(callError.getMessage());
	}

	@Test
	void customErrorHandlerIsUsedForStreams() {
		ResponseErrorHandler customHandler = new ResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) {
				return true;
			}

			@Override
			public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
				throw new IllegalStateException(method + " " + url.getPath() + " -> " + response.getStatusCode().value()
						+ " " + new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
			}
		};

		assertThat(streamError(HttpStatus.NOT_FOUND, MODEL_NOT_FOUND, customHandler))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("POST /ml/v1/text/chat_stream -> 404 " + MODEL_NOT_FOUND);
	}

	@Test
	void handlerThatDoesNotThrowFallsBackToWebClientException() {
		Throwable streamError = streamError(HttpStatus.NOT_FOUND, MODEL_NOT_FOUND, response -> false);

		assertThat(streamError).isInstanceOfSatisfying(WebClientResponseException.class, exception -> {
			assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(exception.getResponseBodyAsString()).isEqualTo(MODEL_NOT_FOUND);
		});
	}

	private static Throwable callError(HttpStatus status, String body, ResponseErrorHandler errorHandler) {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=2024-10-17"))
			.andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body));

		WatsonxAiChatApi chatApi = chatApi(restClientBuilder, WebClient.builder(), errorHandler);
		return catchThrowable(() -> chatApi.chat(request()));
	}

	private static Throwable streamError(HttpStatus status, String body, ResponseErrorHandler errorHandler) {
		WebClient.Builder webClientBuilder = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(status)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8))))
				.request(httpRequest(request))
				.build()));

		WatsonxAiChatApi chatApi = chatApi(RestClient.builder(), webClientBuilder, errorHandler);
		return catchThrowable(() -> chatApi.stream(request()).collectList().block());
	}

	private static HttpRequest httpRequest(ClientRequest request) {
		return new HttpRequest() {
			@Override
			public HttpMethod getMethod() {
				return request.method();
			}

			@Override
			public URI getURI() {
				return request.url();
			}

			@Override
			public Map<String, Object> getAttributes() {
				return request.attributes();
			}

			@Override
			public HttpHeaders getHeaders() {
				return request.headers();
			}
		};
	}

	private static WatsonxAiChatApi chatApi(RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
			ResponseErrorHandler errorHandler) {
		return new WatsonxAiChatApi(BASE_URL, "/ml/v1/text/chat", "/ml/v1/text/chat_stream", "2024-10-17", null,
				"test-space-id", "test-api-key", restClientBuilder, webClientBuilder, errorHandler);
	}

	private static WatsonxAiChatRequest request() {
		return WatsonxAiChatRequest.builder()
			.model("ibm/no-such-model")
			.messages(List.of(new TextChatMessage("Say ping", null)))
			.build();
	}

}
