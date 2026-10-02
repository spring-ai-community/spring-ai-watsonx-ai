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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Tests for {@link WatsonxAiChatApi#stream(WatsonxAiChatRequest)} using a stubbed
 * {@link WebClient} exchange, so no watsonx.ai or IBM IAM call is made. Verifies that the
 * tool call state used to group streamed chunks is not shared between streams.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatApiStreamTest {

	private static final String TOOL_CALL_START = """
			{"id":"chat-a","model_id":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call-1","type":"function","function":{"name":"getWeather","arguments":"{\\"city\\":"}}]}}]}""";

	private static final String TOOL_CALL_END = """
			{"id":"chat-a","model_id":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"\\"Manila\\"}"}}]},"finish_reason":"tool_calls"}]}""";

	private static final String TEXT_HELLO = """
			{"id":"chat-b","model_id":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"delta":{"content":"Hello"}}]}""";

	private static final String TEXT_WORLD = """
			{"id":"chat-b","model_id":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"delta":{"content":" world"},"finish_reason":"stop"}]}""";

	private final Queue<Flux<DataBuffer>> responseBodies = new ArrayDeque<>();

	private WatsonxAiChatApi chatApi;

	@BeforeEach
	void setUp() {
		WebClient.Builder webClientBuilder = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(this.responseBodies.remove())
				.build()));

		try (MockedConstruction<WatsonxAiAuthentication> ignored = mockConstruction(WatsonxAiAuthentication.class,
				(authentication, context) -> when(authentication.getAccessToken()).thenReturn("test-token"))) {
			this.chatApi = new WatsonxAiChatApi("https://us-south.ml.cloud.ibm.com", "/ml/v1/text/chat",
					"/ml/v1/text/chat_stream", "2024-05-31", "test-project-id", null, "test-api-key",
					RestClient.builder(), webClientBuilder, response -> false);
		}
	}

	@Test
	void textStreamIsNotMergedWhileAnotherStreamIsInsideToolCall() {
		Sinks.Many<DataBuffer> toolCallBody = Sinks.many().unicast().onBackpressureBuffer();
		this.responseBodies.add(toolCallBody.asFlux());
		this.responseBodies.add(sseBody(TEXT_HELLO, TEXT_WORLD, "[DONE]"));

		List<WatsonxAiChatStream> toolCallChunks = new CopyOnWriteArrayList<>();
		this.chatApi.stream(request()).subscribe(toolCallChunks::add);

		// The first stream is now in the middle of a tool call
		toolCallBody.tryEmitNext(sseEvent(TOOL_CALL_START));

		List<WatsonxAiChatStream> textChunks = this.chatApi.stream(request()).collectList().block();

		assertEquals(List.of("Hello", " world"), contents(textChunks),
				"Text chunks of one stream must not be merged because another stream is inside a tool call");

		toolCallBody.tryEmitNext(sseEvent(TOOL_CALL_END));
		toolCallBody.tryEmitNext(sseEvent("[DONE]"));
		toolCallBody.tryEmitComplete();

		assertEquals(1, toolCallChunks.size());
		assertEquals("{\"city\":\"Manila\"}",
				toolCallChunks.get(0).choices().get(0).delta().toolCalls().get(0).function().arguments());
	}

	@Test
	void textStreamIsNotMergedAfterPreviousStreamWasCancelledInsideToolCall() {
		Sinks.Many<DataBuffer> toolCallBody = Sinks.many().unicast().onBackpressureBuffer();
		this.responseBodies.add(toolCallBody.asFlux());
		this.responseBodies.add(sseBody(TEXT_HELLO, TEXT_WORLD, "[DONE]"));

		Disposable toolCallStream = this.chatApi.stream(request()).subscribe();
		toolCallBody.tryEmitNext(sseEvent(TOOL_CALL_START));

		// The client gives up before the tool call finishes (timeout, user cancels, ...)
		toolCallStream.dispose();

		List<WatsonxAiChatStream> textChunks = this.chatApi.stream(request()).collectList().block();

		assertEquals(List.of("Hello", " world"), contents(textChunks),
				"A stream cancelled in the middle of a tool call must not affect the next stream");
	}

	private static WatsonxAiChatRequest request() {
		return WatsonxAiChatRequest.builder()
			.model("ibm/granite-3-3-8b-instruct")
			.messages(List.of(new TextChatMessage("Hello", null)))
			.build();
	}

	private static List<String> contents(List<WatsonxAiChatStream> chunks) {
		return chunks.stream().map(chunk -> chunk.choices().get(0).delta().content()).toList();
	}

	private static Flux<DataBuffer> sseBody(String... data) {
		return Flux.fromArray(data).map(WatsonxAiChatApiStreamTest::sseEvent);
	}

	private static DataBuffer sseEvent(String data) {
		return DefaultDataBufferFactory.sharedInstance
			.wrap(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
	}

}
