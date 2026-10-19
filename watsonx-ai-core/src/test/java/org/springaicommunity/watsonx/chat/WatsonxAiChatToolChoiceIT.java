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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springaicommunity.watsonx.auth.StubWatsonxAiAuthentication;
import org.springaicommunity.watsonx.chat.util.ToolType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
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
 * Tests a tool choice that forces a tool call. watsonx.ai answers a named tool choice
 * ({@code tool_choice}) with {@code finish_reason: "stop"}, and
 * {@code tool_choice_option: "required"} with {@code "tool_calls"}: the tool call must be
 * returned either way. After a tool result, the forcing tool choice is left out, so the
 * tool calling loop ends. Response bodies are shaped like real watsonx.ai responses.
 *
 * @author Ana Katrina Inguengan
 */
@ExtendWith(StubWatsonxAiAuthentication.class)
class WatsonxAiChatToolChoiceIT {

	private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";

	private static final String VERSION = "2024-10-17";

	private static final String CHUNK_PREFIX = "{\"id\":\"chatcmpl-1\",\"object\":\"chat.completion.chunk\",\"model_id\":\"ibm/granite-4-h-small\",\"model\":\"ibm/granite-4-h-small\",";

	private static final String CHUNK_SUFFIX = ",\"created\":1791906254,\"model_version\":\"4.0.0\",\"created_at\":\"2026-10-13T15:44:15.082Z\"}";

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final WatsonxAiChatRequest.TextChatToolChoiceTool GET_TIME = new WatsonxAiChatRequest.TextChatToolChoiceTool(
			ToolType.FUNCTION, new WatsonxAiChatRequest.TextChatToolChoiceFunction("getTime"));

	private static final String TEXT_RESPONSE = """
			{"id":"chatcmpl-2","object":"chat.completion","model_id":"ibm/granite-4-h-small","model":"ibm/granite-4-h-small","choices":[{"index":0,"message":{"role":"assistant","content":"It's 12:00 in UTC."},"finish_reason":"stop"}],"created":1791906255,"model_version":"4.0.0","created_at":"2026-10-13T15:44:15.484Z","usage":{"completion_tokens":9,"prompt_tokens":200,"total_tokens":209}}""";

	private final AtomicInteger getTimeCalls = new AtomicInteger();

	private RestClient.Builder restClientBuilder;

	private MockRestServiceServer mockServer;

	private final Queue<List<String>> streamBodies = new ArrayDeque<>();

	private WatsonxAiChatModel chatModel;

	@BeforeEach
	void setUp() {
		this.restClientBuilder = RestClient.builder();
		this.mockServer = MockRestServiceServer.bindTo(this.restClientBuilder).build();

		WebClient.Builder webClientBuilder = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(Flux.fromIterable(this.streamBodies.remove())
					.map(data -> DefaultDataBufferFactory.sharedInstance
						.wrap(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8))))
				.build()));

		ResponseErrorHandler errorHandler = response -> response.getStatusCode().isError();

		WatsonxAiChatApi api = new WatsonxAiChatApi(BASE_URL, "/ml/v1/text/chat", "/ml/v1/text/chat_stream", VERSION,
				null, "test-space-id", "test-api-key", this.restClientBuilder, webClientBuilder, errorHandler);

		this.chatModel = WatsonxAiChatModel.builder()
			.watsonxAiChatApi(api)
			.options(WatsonxAiChatOptions.builder().model("ibm/granite-4-h-small").build())
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
			.build();
	}

	@Test
	void callReturnsTheToolCallWhenTheFinishReasonIsStop() {
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andRespond(withSuccess(callResponse("stop"), MediaType.APPLICATION_JSON));

		AssistantMessage output = this.chatModel.call(new Prompt("Say hi")).getResult().getOutput();

		assertThat(output.getToolCalls()).singleElement().satisfies(toolCall -> {
			assertThat(toolCall.id()).isEqualTo("chatcmpl-tool-1");
			assertThat(toolCall.name()).isEqualTo("getTime");
			assertThat(toolCall.arguments()).contains("\"timeZone\"").contains("America/New_York");
		});
		this.mockServer.verify();
	}

	@Test
	void callReturnsTheToolCallWhenTheFinishReasonIsToolCalls() {
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andRespond(withSuccess(callResponse("tool_calls"), MediaType.APPLICATION_JSON));

		AssistantMessage output = this.chatModel.call(new Prompt("Say hi")).getResult().getOutput();

		assertThat(output.getToolCalls()).extracting(AssistantMessage.ToolCall::name).containsExactly("getTime");
		this.mockServer.verify();
	}

	@Test
	void callDropsTheToolCallWhenTheFinishReasonIsLength() {
		// The token limit cuts the arguments off partway through the JSON
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andRespond(withSuccess(callResponse("length"), MediaType.APPLICATION_JSON));

		AssistantMessage output = this.chatModel.call(new Prompt("Say hi")).getResult().getOutput();

		assertThat(output.getToolCalls()).isEmpty();
		this.mockServer.verify();
	}

	@Test
	void streamReturnsTheToolCallWhenTheFinishReasonIsStop() {
		// As watsonx.ai streams a named tool choice: the arguments in pieces, then
		// "stop" with an empty piece, then the usage without choices
		this.streamBodies.add(List.of(
				chunk("{\"index\":0,\"finish_reason\":null,\"delta\":{\"role\":\"assistant\",\"content\":\"\"}}"),
				toolCallPiece("\"id\":\"chatcmpl-tool-1\",\"type\":\"function\",", "getTime", "{", "null"),
				toolCallPiece("", "", " \\\"timeZone\\\": \\\"UTC\\\"", "null"), toolCallPiece("", "", " }", "null"),
				toolCallPiece("", "", "", "\"stop\""),
				CHUNK_PREFIX
						+ "\"choices\":[],\"usage\":{\"completion_tokens\":10,\"prompt_tokens\":178,\"total_tokens\":188}"
						+ CHUNK_SUFFIX,
				"[DONE]"));

		List<AssistantMessage.ToolCall> toolCalls = streamToolCalls();

		assertThat(toolCalls).singleElement().satisfies(toolCall -> {
			assertThat(toolCall.id()).isEqualTo("chatcmpl-tool-1");
			assertThat(toolCall.name()).isEqualTo("getTime");
			assertThat(toolCall.arguments()).isEqualToIgnoringWhitespace("{\"timeZone\":\"UTC\"}");
		});
	}

	@Test
	void streamReturnsTheToolCallWhenTheFinishReasonIsToolCalls() {
		// As watsonx.ai streams tool_choice_option "required": the arguments in pieces,
		// then "tool_calls" without a piece
		this.streamBodies.add(List.of(
				chunk("{\"index\":0,\"finish_reason\":null,\"delta\":{\"role\":\"assistant\",\"content\":\"\"}}"),
				toolCallPiece("\"id\":\"chatcmpl-tool-1\",\"type\":\"function\",", "getTime", "{\\n", "null"),
				toolCallPiece("", "", "\\t\\\"timeZone\\\": \\\"UTC\\\"\\n", "null"),
				toolCallPiece("", "", "}\\n", "null"),
				chunk("{\"index\":0,\"finish_reason\":\"tool_calls\",\"delta\":{}}"), "[DONE]"));

		List<AssistantMessage.ToolCall> toolCalls = streamToolCalls();

		assertThat(toolCalls).singleElement().satisfies(toolCall -> {
			assertThat(toolCall.name()).isEqualTo("getTime");
			assertThat(toolCall.arguments()).isEqualToIgnoringWhitespace("{\"timeZone\":\"UTC\"}");
		});
	}

	@Test
	void streamDropsTheToolCallWhenTheFinishReasonIsLength() {
		// The token limit cuts the arguments off partway through the JSON
		this.streamBodies.add(List.of(
				chunk("{\"index\":0,\"finish_reason\":null,\"delta\":{\"role\":\"assistant\",\"content\":\"\"}}"),
				toolCallPiece("\"id\":\"chatcmpl-tool-1\",\"type\":\"function\",", "getTime",
						"{ \\\"timeZone\\\": \\\"Amer", "null"),
				chunk("{\"index\":0,\"finish_reason\":\"length\",\"delta\":{}}"), "[DONE]"));

		List<AssistantMessage.ToolCall> toolCalls = streamToolCalls();

		assertThat(toolCalls).isEmpty();
	}

	@Test
	void textStreamHasNoToolCalls() {
		this.streamBodies.add(List.of(chunk("{\"index\":0,\"finish_reason\":null,\"delta\":{\"content\":\"Hi\"}}"),
				chunk("{\"index\":0,\"finish_reason\":\"stop\",\"delta\":{\"content\":\"!\"}}"), "[DONE]"));

		List<ChatResponse> responses = this.chatModel.stream(new Prompt("Say hi")).collectList().block();

		assertThat(responses).isNotNull();
		assertThat(responses.stream().map(response -> response.getResult().getOutput().getText())).containsExactly("Hi",
				"!");
		assertThat(responses)
			.allSatisfy(response -> assertThat(response.getResult().getOutput().getToolCalls()).isEmpty());
	}

	@Test
	void chatClientWithNamedToolChoiceRunsTheToolOnce() {
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andExpect(body(json -> assertThat(json).containsKey("tool_choice")))
			.andRespond(withSuccess(callResponse("stop"), MediaType.APPLICATION_JSON));
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andExpect(body(json -> assertThat(json).containsKey("tools").doesNotContainKeys("tool_choice")))
			.andRespond(withSuccess(TEXT_RESPONSE, MediaType.APPLICATION_JSON));

		String answer = ChatClient.create(this.chatModel)
			.prompt("What time is it in New York?")
			.options(WatsonxAiChatOptions.builder().toolCallbacks(getTimeTool()).toolChoice(GET_TIME))
			.call()
			.content();

		assertThat(answer).isEqualTo("It's 12:00 in UTC.");
		assertThat(this.getTimeCalls).hasValue(1);
		this.mockServer.verify();
	}

	@Test
	void chatClientWithRequiredToolChoiceRunsTheToolOnce() {
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andExpect(body(json -> assertThat(json).containsEntry("tool_choice_option", "required")))
			.andRespond(withSuccess(callResponse("tool_calls"), MediaType.APPLICATION_JSON));
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andExpect(body(json -> assertThat(json).containsKey("tools").doesNotContainKeys("tool_choice_option")))
			.andRespond(withSuccess(TEXT_RESPONSE, MediaType.APPLICATION_JSON));

		String answer = ChatClient.create(this.chatModel)
			.prompt("What time is it in New York?")
			.options(WatsonxAiChatOptions.builder().toolCallbacks(getTimeTool()).toolChoiceOption("required"))
			.call()
			.content();

		assertThat(answer).isEqualTo("It's 12:00 in UTC.");
		assertThat(this.getTimeCalls).hasValue(1);
		this.mockServer.verify();
	}

	@Test
	void toolChoiceOptionNoneIsStillSentAfterAToolResult() {
		this.mockServer.expect(requestTo(BASE_URL + "/ml/v1/text/chat?version=" + VERSION))
			.andExpect(body(json -> assertThat(json).containsEntry("tool_choice_option", "none")))
			.andRespond(withSuccess(TEXT_RESPONSE, MediaType.APPLICATION_JSON));

		this.chatModel.call(new Prompt(
				List.of(new UserMessage("What time is it in New York?"),
						AssistantMessage.builder()
							.content("")
							.toolCalls(List.of(new AssistantMessage.ToolCall("chatcmpl-tool-1", "function", "getTime",
									"{\"timeZone\":\"America/New_York\"}")))
							.build(),
						ToolResponseMessage.builder()
							.responses(List
								.of(new ToolResponseMessage.ToolResponse("chatcmpl-tool-1", "getTime", "12:00")))
							.build()),
				WatsonxAiChatOptions.builder().toolCallbacks(getTimeTool()).toolChoiceOption("none").build()));

		this.mockServer.verify();
	}

	private ToolCallback getTimeTool() {
		Function<TimeRequest, String> getTime = request -> {
			this.getTimeCalls.incrementAndGet();
			return "12:00";
		};
		return FunctionToolCallback.builder("getTime", getTime)
			.description("Get the current time in a time zone")
			.inputType(TimeRequest.class)
			.build();
	}

	record TimeRequest(String timeZone) {
	}

	/** The JSON request body must satisfy these assertions. */
	private static RequestMatcher body(Consumer<Map<String, Object>> assertions) {
		return request -> assertions
			.accept(JSON_HELPER.fromJsonToMap(((MockClientHttpRequest) request).getBodyAsString()));
	}

	private List<AssistantMessage.ToolCall> streamToolCalls() {
		List<ChatResponse> responses = this.chatModel.stream(new Prompt("Say hi")).collectList().block();
		assertThat(responses).isNotNull();
		return responses.stream()
			.map(ChatResponse::getResult)
			.filter(Objects::nonNull)
			.flatMap(result -> result.getOutput().getToolCalls().stream())
			.toList();
	}

	private static String callResponse(String finishReason) {
		return """
				{
				  "id": "chatcmpl-1",
				  "object": "chat.completion",
				  "model_id": "ibm/granite-4-h-small",
				  "model": "ibm/granite-4-h-small",
				  "choices": [
				    {
				      "index": 0,
				      "message": {
				        "role": "assistant",
				        "content": "",
				        "tool_calls": [
				          {
				            "id": "chatcmpl-tool-1",
				            "type": "function",
				            "function": { "name": "getTime", "arguments": "{ \\n  \\"timeZone\\": \\"America/New_York\\"\\n}" }
				          }
				        ]
				      },
				      "finish_reason": "%s"
				    }
				  ],
				  "created": 1791906254,
				  "model_version": "4.0.0",
				  "created_at": "2026-10-13T15:44:14.484Z",
				  "usage": { "completion_tokens": 15, "prompt_tokens": 178, "total_tokens": 193 }
				}
				"""
			.formatted(finishReason);
	}

	private static String chunk(String choice) {
		return CHUNK_PREFIX + "\"choices\":[" + choice + "]" + CHUNK_SUFFIX;
	}

	private static String toolCallPiece(String idAndType, String name, String arguments, String finishReason) {
		return chunk("{\"index\":0,\"finish_reason\":" + finishReason + ",\"delta\":{\"tool_calls\":[{\"index\":0,"
				+ idAndType + "\"function\":{\"name\":\"" + name + "\",\"arguments\":\"" + arguments + "\"}}]}}");
	}

}
