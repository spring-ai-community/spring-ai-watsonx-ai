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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tests that {@link WatsonxAiChatApi#stream(WatsonxAiChatRequest)} fetches the access
 * token when the stream is subscribed, using a stubbed {@link WebClient} exchange and
 * authentication, so no watsonx.ai or IBM IAM call is made.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatApiStreamAuthTest {

	private static final String TEXT_CHUNK = """
			{"id":"chat-a","model_id":"ibm/granite-3-3-8b-instruct","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":"stop"}]}""";

	private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();

	private final List<String> tokenThreads = new CopyOnWriteArrayList<>();

	private WatsonxAiAuthentication authentication;

	private WatsonxAiChatApi chatApi;

	@BeforeEach
	void setUp() {
		WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(request -> {
			this.authorizationHeaders.add(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
			return Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(Flux.just(TEXT_CHUNK, "[DONE]")
					.map(data -> DefaultDataBufferFactory.sharedInstance
						.wrap(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8))))
				.build());
		});

		AtomicInteger tokenRequests = new AtomicInteger();
		try (MockedConstruction<WatsonxAiAuthentication> constructed = mockConstruction(WatsonxAiAuthentication.class,
				(authentication, context) -> when(authentication.getAccessToken()).thenAnswer(invocation -> {
					this.tokenThreads.add(Thread.currentThread().getName());
					return "token-" + tokenRequests.incrementAndGet();
				}))) {
			this.chatApi = new WatsonxAiChatApi("https://us-south.ml.cloud.ibm.com", "/ml/v1/text/chat",
					"/ml/v1/text/chat_stream", "2024-05-31", "test-project-id", null, "test-api-key",
					RestClient.builder(), webClientBuilder, response -> false);
			this.authentication = constructed.constructed().get(0);
		}
	}

	@Test
	void tokenIsNotFetchedUntilStreamIsSubscribed() {
		this.chatApi.stream(request());

		verify(this.authentication, never()).getAccessToken();
	}

	@Test
	void eachSubscriptionFetchesCurrentToken() {
		Flux<WatsonxAiChatStream> stream = this.chatApi.stream(request());

		stream.blockLast();
		stream.blockLast();

		assertThat(this.authorizationHeaders).containsExactly("Bearer token-1", "Bearer token-2");
	}

	@Test
	void tokenIsFetchedOffTheSubscribingThread() {
		this.chatApi.stream(request()).blockLast();

		assertThat(this.tokenThreads).singleElement().asString().startsWith("boundedElastic");
	}

	private static WatsonxAiChatRequest request() {
		return WatsonxAiChatRequest.builder()
			.model("ibm/granite-3-3-8b-instruct")
			.messages(List.of(new TextChatMessage("Hello", null)))
			.build();
	}

}
