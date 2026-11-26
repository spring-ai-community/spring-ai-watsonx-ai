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

package org.springaicommunity.watsonx.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springaicommunity.watsonx.auth.StubWatsonxAiAuthentication;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
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
 * Tests that an {@code event: error} in a chat stream fails the stream like the same
 * error in a synchronous call, through the configured {@link ResponseErrorHandler}.
 * watsonx.ai answers such a stream with HTTP 200 and reports the error, such as an
 * invalid argument found by the model, as an event whose data is the error body with its
 * {@code status_code}. The streams were recorded from watsonx.ai eu-de; no watsonx.ai or
 * IBM IAM call is made.
 *
 * @author Federico Mariani
 */
@ExtendWith(StubWatsonxAiAuthentication.class)
class WatsonxAiChatApiStreamErrorEventTest {

	private static final String BASE_URL = "https://eu-de.ml.cloud.ibm.com";

	private static final String CHUNK = """
			{"id":"chat-1","model_id":"ibm/granite-4-h-small","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"finish_reason":null}]}""";

	// Recorded: n=2 with greedy sampling, a logit_bias with an unknown token, and a
	// tool_choice that names no tool. A synchronous call gets HTTP 400 for each
	@ParameterizedTest
	@ValueSource(strings = { "n2-greedy", "logit-bias-bad", "tool-choice-unknown" })
	void errorEventIsTheSameExceptionAsTheCallGets(String recording) throws IOException {
		String stream = recording(recording);
		String error = errorData(stream);

		Throwable streamError = catchThrowable(
				() -> chatApi(sseResponse(stream), RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER).stream(request())
					.collectList()
					.block());

		assertThat(streamError).isInstanceOf(NonTransientAiException.class).hasMessage("400 - " + error);
		assertThat(callError(HttpStatus.BAD_REQUEST, error)).isInstanceOf(NonTransientAiException.class)
			.hasMessage(streamError.getMessage());
	}

	// Before, the stream completed with a chunk without choices, so the chat model
	// returned an empty response
	@Test
	void chatModelStreamFailsInsteadOfAnsweringNothing() throws IOException {
		WatsonxAiChatModel chatModel = WatsonxAiChatModel.builder()
			.watsonxAiChatApi(chatApi(sseResponse(recording("n2-greedy")), RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER))
			.options(WatsonxAiChatOptions.builder().model("ibm/granite-4-h-small").build())
			.observationRegistry(ObservationRegistry.NOOP)
			.build();

		assertThat(catchThrowable(() -> chatModel.stream(new Prompt("Say hi")).collectList().block()))
			.isInstanceOf(NonTransientAiException.class)
			.hasMessageContaining("n must be 1 when using greedy sampling");
		assertThat(catchThrowable(
				() -> ChatClient.create(chatModel).prompt("Say hi").stream().content().collectList().block()))
			.isInstanceOf(NonTransientAiException.class);
	}

	// Derived from the recordings: a chunk, then the error
	@Test
	void chunksBeforeTheErrorEventAreEmittedAndTheErrorFollows() throws IOException {
		String stream = "id: 1\nevent: message\ndata: " + CHUNK + "\n\n" + recording("n2-greedy");
		List<WatsonxAiChatStream> chunks = new ArrayList<>();

		Throwable error = catchThrowable(
				() -> chatApi(sseResponse(stream), RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER).stream(request())
					.doOnNext(chunks::add)
					.blockLast());

		assertThat(chunks).singleElement()
			.satisfies(chunk -> assertThat(chunk.choices().get(0).delta().content()).isEqualTo("Hello"));
		assertThat(error).isInstanceOf(NonTransientAiException.class);
	}

	// A server error is transient, so Spring AI retries it, as for a synchronous call
	@Test
	void serverErrorEventIsTransient() {
		String error = """
				{"errors":[{"code":"service_unavailable","message":"Try again later."}],"status_code":503}""";

		assertThat(streamError(errorEvent(error), RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER))
			.isInstanceOf(TransientAiException.class)
			.hasMessage(callError(HttpStatus.SERVICE_UNAVAILABLE, error).getMessage());
	}

	@Test
	void errorEventWithoutAStatusIsAServerError() {
		assertThat(
				streamError(errorEvent("{\"errors\":[{\"code\":\"x\"}]}"), RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER))
			.isInstanceOf(TransientAiException.class)
			.hasMessageStartingWith("500 - ");
		assertThat(streamError(errorEvent("not json"), RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER))
			.isInstanceOf(TransientAiException.class)
			.hasMessage("500 - not json");
		assertThat(streamError("event: error\n\n", RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER))
			.isInstanceOf(TransientAiException.class)
			.hasMessage("500 - ");
	}

	@Test
	void customErrorHandlerGetsTheRequestAndTheError() throws IOException {
		String stream = recording("n2-greedy");
		ResponseErrorHandler customHandler = new ResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) {
				return true;
			}

			@Override
			public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
				throw new IllegalStateException(method + " " + url.getPath() + " -> " + response.getStatusCode().value()
						+ " " + response.getHeaders().getContentType() + " "
						+ new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
			}
		};

		assertThat(streamError(stream, customHandler)).isInstanceOf(IllegalStateException.class)
			.hasMessage("POST /ml/v1/text/chat_stream -> 400 application/json " + errorData(stream));
	}

	@Test
	void handlerThatDoesNotThrowFallsBackToWebClientException() throws IOException {
		String stream = recording("n2-greedy");

		assertThat(streamError(stream, response -> false)).isInstanceOfSatisfying(WebClientResponseException.class,
				exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(exception.getResponseBodyAsString()).isEqualTo(errorData(stream));
				});
	}

	@Test
	void crlfRecordingIsReadToo() throws IOException {
		String stream = recording("n2-greedy").replace("\r\n", "\n").replace("\n", "\r\n");

		assertThat(streamError(stream, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER))
			.isInstanceOf(NonTransientAiException.class)
			.hasMessage("400 - " + errorData(stream));
	}

	// What didn't change: other bodies, the status handlers of the WebClient.Builder,
	// and errors while reading the stream

	@Test
	void bodyThatIsNotAnEventStreamStillFails() {
		WebClient.Builder html = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
				.body("<html>Login required</html>")
				.request(httpRequest(request))
				.build()));

		assertThat(catchThrowable(
				() -> chatApi(html, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER).stream(request()).collectList().block()))
			.isNotNull();
	}

	@Test
	void bodyOfAnyTextTypeThatIsNotAnEventStreamStillFails() {
		WebClient.Builder anyText = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, "text/*")
				.body("<html>Login required</html>")
				.request(httpRequest(request))
				.build()));

		assertThat(catchThrowable(() -> chatApi(anyText, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER).stream(request())
			.collectList()
			.block())).isNotNull();
	}

	// A filter of the WebClient changed the URI: the handler gets the URI that was sent,
	// for an error event as for an error response
	@Test
	void errorHandlerGetsTheUriThatWasSent() throws IOException {
		String stream = recording("n2-greedy");
		List<URI> handledUris = new ArrayList<>();
		ResponseErrorHandler recordingHandler = new ResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) {
				return true;
			}

			@Override
			public void handleError(URI url, HttpMethod method, ClientHttpResponse response) {
				handledUris.add(url);
				throw new IllegalStateException("handled");
			}
		};
		WebClient.Builder rewritten = WebClient.builder()
			.filter((request, next) -> next
				.exchange(ClientRequest.from(request).url(URI.create(BASE_URL + "/rewritten/chat_stream")).build()))
			.clientConnector((method, url, requestCallback) -> {
				MockClientHttpRequest sent = new MockClientHttpRequest(method, url);
				MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
				response.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
				response.setBody(stream);
				return requestCallback.apply(sent).then(Mono.just(response));
			});

		assertThat(catchThrowable(() -> chatApi(rewritten, recordingHandler).stream(request()).collectList().block()))
			.hasMessage("handled");
		assertThat(handledUris).singleElement()
			.satisfies(uri -> assertThat(uri.getPath()).isEqualTo("/rewritten/chat_stream"));
	}

	@Test
	void defaultStatusHandlerOfTheWebClientBuilderStillApplies() {
		WebClient.Builder redirect = WebClient.builder()
			.defaultStatusHandler(HttpStatusCode::is3xxRedirection,
					response -> Mono.just(new IllegalStateException("redirected")))
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, "https://example.com")
				.request(httpRequest(request))
				.build()));

		assertThat(catchThrowable(() -> chatApi(redirect, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER).stream(request())
			.collectList()
			.block())).isInstanceOf(IllegalStateException.class).hasMessage("redirected");
	}

	@Test
	void errorWhileReadingTheStreamIsAWebClientResponseException() {
		IOException reset = new IOException("reset");
		WebClient.Builder broken = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(Flux.error(reset))
				.request(httpRequest(request))
				.build()));

		assertThat(catchThrowable(() -> chatApi(broken, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER).stream(request())
			.collectList()
			.block())).isInstanceOf(WebClientResponseException.class).hasRootCause(reset);
	}

	private static Throwable streamError(String stream, ResponseErrorHandler errorHandler) {
		return catchThrowable(() -> chatApi(sseResponse(stream), errorHandler).stream(request()).collectList().block());
	}

	private static Throwable callError(HttpStatus status, String body) {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=2024-10-17"))
			.andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body));
		WatsonxAiChatApi chatApi = new WatsonxAiChatApi(BASE_URL, "/ml/v1/text/chat", "/ml/v1/text/chat_stream",
				"2024-10-17", null, "test-space-id", "test-api-key", restClientBuilder, WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
		return catchThrowable(() -> chatApi.chat(request()));
	}

	private static WebClient.Builder sseResponse(String stream) {
		return WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(stream.getBytes(StandardCharsets.UTF_8))))
				.request(httpRequest(request))
				.build()));
	}

	private static WatsonxAiChatApi chatApi(WebClient.Builder webClientBuilder, ResponseErrorHandler errorHandler) {
		return new WatsonxAiChatApi(BASE_URL, "/ml/v1/text/chat", "/ml/v1/text/chat_stream", "2024-10-17", null,
				"test-space-id", "test-api-key", RestClient.builder(), webClientBuilder, errorHandler);
	}

	private static String errorEvent(String data) {
		return "id: 1\nevent: error\ndata: " + data + "\n\n";
	}

	/** The data of the error event of a recorded stream. */
	private static String errorData(String stream) {
		List<String> lines = stream.lines().toList();
		for (int i = 0; i < lines.size() - 1; i++) {
			if (lines.get(i).equals("event: error") && lines.get(i + 1).startsWith("data: ")) {
				return lines.get(i + 1).substring("data: ".length());
			}
		}
		throw new IllegalArgumentException("No error event");
	}

	private static String recording(String name) throws IOException {
		try (InputStream input = WatsonxAiChatApiStreamErrorEventTest.class
			.getResourceAsStream("/stream-errors/" + name + ".sse")) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
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

	private static WatsonxAiChatRequest request() {
		return WatsonxAiChatRequest.builder()
			.model("ibm/granite-4-h-small")
			.messages(List.of(new TextChatMessage("Say hi", null)))
			.build();
	}

}
