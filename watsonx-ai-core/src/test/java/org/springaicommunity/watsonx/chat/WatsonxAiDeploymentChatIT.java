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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springaicommunity.watsonx.auth.StubWatsonxAiAuthentication;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tests chat through a watsonx.ai deployment: with a {@code deploymentId}, requests go to
 * {@code /ml/v1/deployments/{id}/text/chat(_stream)} and contain only the messages, as
 * watsonx.ai rejects any other field there. Response bodies are shaped like real
 * watsonx.ai deployment responses.
 *
 * @author Ana Katrina Inguengan
 */
@ExtendWith(StubWatsonxAiAuthentication.class)
class WatsonxAiDeploymentChatIT {

	private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";

	private static final String VERSION = "2024-10-17";

	private static final String DEPLOYMENT_ID = "01a12498-c4b5-753e-8a31-8d377c2b6d84";

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final String CHAT_RESPONSE = """
			{
			  "id": "chatcmpl-1",
			  "object": "chat.completion",
			  "model_id": "ibm/granite-4-h-small",
			  "model": "ibm/granite-4-h-small",
			  "choices": [
			    { "index": 0, "message": { "role": "assistant", "content": "ping" }, "finish_reason": "stop" }
			  ],
			  "created": 1791523268,
			  "model_version": "4.0.0",
			  "created_at": "2026-10-10T08:01:08.909Z",
			  "usage": { "completion_tokens": 2, "prompt_tokens": 30, "total_tokens": 32 }
			}
			""";

	private static final String STREAM_CHUNK = """
			{"id":"chatcmpl-2","object":"chat.completion.chunk","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"finish_reason":"stop","delta":{"content":"ping"}}],"created":1791523268,"model_version":"4.0.0","created_at":"2026-10-10T08:01:09.909Z"}""";

	private RestClient.Builder restClientBuilder;

	private MockRestServiceServer mockServer;

	private final List<String> streamRequests = new CopyOnWriteArrayList<>();

	private WatsonxAiChatApi api;

	private WatsonxAiChatModel chatModel;

	@BeforeEach
	void setUp() {
		this.restClientBuilder = RestClient.builder();
		this.mockServer = MockRestServiceServer.bindTo(this.restClientBuilder).build();

		WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(request -> {
			this.streamRequests.add(request.url().getPath() + "?" + request.url().getQuery());
			return Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(Flux.just(STREAM_CHUNK, "[DONE]")
					.map(data -> DefaultDataBufferFactory.sharedInstance
						.wrap(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8))))
				.build());
		});

		ResponseErrorHandler errorHandler = response -> response.getStatusCode().isError();

		this.api = new WatsonxAiChatApi(BASE_URL, "/ml/v1/text/chat", "/ml/v1/text/chat_stream", VERSION, null,
				"test-space-id", "test-api-key", this.restClientBuilder, webClientBuilder, errorHandler);

		this.chatModel = chatModel(WatsonxAiChatOptions.builder()
			.model("ibm/granite-4-h-small")
			.temperature(0.7)
			.topP(1.0)
			.maxTokens(1024)
			.build());
	}

	private WatsonxAiChatModel chatModel(WatsonxAiChatOptions defaultOptions) {
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(this.api)
			.options(defaultOptions)
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
			.build();
	}

	/** The request body must contain nothing but the messages. */
	private static RequestMatcher onlyMessagesInBody() {
		return request -> {
			String body = ((MockClientHttpRequest) request).getBodyAsString();
			Map<String, Object> json = JSON_HELPER.fromJsonToMap(body);
			assertThat(json).containsOnlyKeys("messages");
		};
	}

	/** The request body must contain these values. */
	private static RequestMatcher bodyContains(Map<String, Object> values) {
		return request -> {
			String body = ((MockClientHttpRequest) request).getBodyAsString();
			assertThat(JSON_HELPER.fromJsonToMap(body)).containsAllEntriesOf(values);
		};
	}

	@Test
	void deploymentIdFromOptionsCallsTheDeployment() {
		this.mockServer
			.expect(requestTo(BASE_URL + "/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/chat?version=" + VERSION))
			.andExpect(method(HttpMethod.POST))
			.andExpect(onlyMessagesInBody())
			.andRespond(withSuccess(CHAT_RESPONSE, MediaType.APPLICATION_JSON));

		String text = this.chatModel
			.call(new Prompt("Reply with the single word: ping",
					this.chatModel.getOptions().mutate().combineWith(deploymentOptions()).build()))
			.getResult()
			.getOutput()
			.getText();

		assertThat(text).isEqualTo("ping");
		this.mockServer.verify();
	}

	@Test
	void perRequestDeploymentIdThroughChatClient() {
		this.mockServer
			.expect(requestTo(BASE_URL + "/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/chat?version=" + VERSION))
			.andExpect(onlyMessagesInBody())
			.andRespond(withSuccess(CHAT_RESPONSE, MediaType.APPLICATION_JSON));

		String text = ChatClient.create(this.chatModel)
			.prompt("Reply with the single word: ping")
			.options(deploymentOptions())
			.call()
			.content();

		assertThat(text).isEqualTo("ping");
		this.mockServer.verify();
	}

	@Test
	void withoutDeploymentIdTheModelIsCalledDirectly() {
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andRespond(withSuccess(CHAT_RESPONSE, MediaType.APPLICATION_JSON));

		this.chatModel.call(new Prompt("Reply with the single word: ping"));

		this.mockServer.verify();
	}

	@Test
	void streamWithDeploymentIdCallsTheDeploymentStream() {
		List<String> texts = this.chatModel
			.stream(new Prompt("Reply with the single word: ping",
					this.chatModel.getOptions().mutate().combineWith(deploymentOptions()).build()))
			.map(response -> response.getResult().getOutput().getText())
			.collectList()
			.block();

		assertThat(texts).containsExactly("ping");
		assertThat(this.streamRequests)
			.containsExactly("/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/chat_stream?version=" + VERSION);
	}

	@Test
	void defaultDeploymentIdAppliesWhenTheCallerPassesOwnOptions() {
		WatsonxAiChatModel deploymentChatModel = chatModel(
				WatsonxAiChatOptions.builder().deploymentId(DEPLOYMENT_ID).build());
		this.mockServer
			.expect(requestTo(BASE_URL + "/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/chat?version=" + VERSION))
			.andExpect(onlyMessagesInBody())
			.andRespond(withSuccess(CHAT_RESPONSE, MediaType.APPLICATION_JSON));

		deploymentChatModel.call(new Prompt("Reply with the single word: ping",
				WatsonxAiChatOptions.builder().temperature(0.2).build()));

		this.mockServer.verify();
	}

	@Test
	void streamWithDefaultDeploymentIdAndOwnOptionsCallsTheDeploymentStream() {
		WatsonxAiChatModel deploymentChatModel = chatModel(
				WatsonxAiChatOptions.builder().deploymentId(DEPLOYMENT_ID).build());

		deploymentChatModel
			.stream(new Prompt("Reply with the single word: ping",
					WatsonxAiChatOptions.builder().temperature(0.2).build()))
			.collectList()
			.block();

		assertThat(this.streamRequests)
			.containsExactly("/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/chat_stream?version=" + VERSION);
	}

	@Test
	void ownOptionsWithoutModelGetTheDefaultModel() {
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andExpect(
					bodyContains(Map.of("model_id", "ibm/granite-4-h-small", "temperature", 0.2, "max_tokens", 1024)))
			.andRespond(withSuccess(CHAT_RESPONSE, MediaType.APPLICATION_JSON));

		this.chatModel.call(new Prompt("Reply with the single word: ping",
				WatsonxAiChatOptions.builder().temperature(0.2).build()));

		this.mockServer.verify();
	}

	@Test
	void genericChatOptionsGetTheDefaultModel() {
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andExpect(bodyContains(Map.of("model_id", "ibm/granite-4-h-small", "temperature", 0.2)))
			.andRespond(withSuccess(CHAT_RESPONSE, MediaType.APPLICATION_JSON));

		this.chatModel
			.call(new Prompt("Reply with the single word: ping", ChatOptions.builder().temperature(0.2).build()));

		this.mockServer.verify();
	}

	@Test
	void ownOptionsDoNotChangeTheDefaults() {
		this.mockServer
			.expect(requestTo(BASE_URL + "/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/chat?version=" + VERSION))
			.andRespond(withSuccess(CHAT_RESPONSE, MediaType.APPLICATION_JSON));
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andExpect(bodyContains(Map.of("model_id", "ibm/granite-4-h-small", "temperature", 0.7)))
			.andRespond(withSuccess(CHAT_RESPONSE, MediaType.APPLICATION_JSON));

		this.chatModel.call(new Prompt("Reply with the single word: ping",
				WatsonxAiChatOptions.builder().deploymentId(DEPLOYMENT_ID).temperature(0.2).build()));
		this.chatModel
			.call(new Prompt("Reply with the single word: ping", WatsonxAiChatOptions.builder().maxTokens(10).build()));

		this.mockServer.verify();
		assertThat(this.chatModel.getOptions().getDeploymentId()).isNull();
		assertThat(this.chatModel.getOptions().getTemperature()).isEqualTo(0.7);
	}

	private static WatsonxAiChatOptions.Builder deploymentOptions() {
		return WatsonxAiChatOptions.builder().deploymentId(DEPLOYMENT_ID);
	}

}
