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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.io.buffer.DataBuffer;
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
 * Replays tool call streams recorded from watsonx.ai through
 * {@link WatsonxAiChatModel#stream(Prompt)}, so no watsonx.ai or IBM IAM call is made.
 * Every model streams tool calls in its own way, and the application must get the same
 * tool calls from each. The recordings keep only the fields the client reads.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatModelRecordedStreamTest {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final String PLAN_TRIP = """
			{"stops":[{"city":"Manila","nights":2},{"city":"Cebu","nights":3}],
			 "traveler":{"name":"Ana","preferences":{"budget":1500,"tags":["beach","food"]}}}""";

	static Stream<Arguments> recordedStreams() {
		return Stream.of(
				// Streams nested arguments in fragments that miss some characters, then
				// sends the complete arguments again as the last fragment of the tool
				// call
				Arguments.of("mistral-small-3-1-24b-instruct-2503--nested", List.of(weather(14.6, 121))),
				Arguments.of("mistral-small-3-1-24b-instruct-2503--two",
						List.of(weather(14.6, 121), time("Asia/Manila"))),
				// Also streams a {} fragment in the middle of the arguments
				Arguments.of("mistral-small-3-1-24b-instruct-2503--deep", List.of(toolCall("planTrip", PLAN_TRIP))),
				// Streams the arguments in fragments that are appended
				Arguments.of("llama-4-maverick-17b-128e-instruct-fp8--two",
						List.of(weather(14.6, 121.0), time("Asia/Manila"))),
				// Sends every tool call in one chunk, together with the finish reason
				Arguments.of("mistral-large-2512--two", List.of(weather(14.6, 121.0), time("Asia/Manila"))),
				// Streams the arguments as a JSON encoded string, finished by a chunk
				// without tool calls
				Arguments.of("granite-4-h-small--two", List.of(weather(14.6, 121.0), time("Asia/Manila"))),
				// Streams pretty-printed arguments
				Arguments.of("gpt-oss-120b--nested", List.of(weather(14.6, 121.0))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("recordedStreams")
	void streamsRecordedToolCalls(String recording, List<Map<String, Object>> expectedToolCalls) throws IOException {
		List<ChatResponse> responses = chatModel(recording(recording + ".jsonl")).stream(new Prompt("Hello"))
			.collectList()
			.block();

		List<Map<String, Object>> toolCalls = new ArrayList<>();
		for (ChatResponse response : responses) {
			response.getResults()
				.forEach(generation -> generation.getOutput()
					.getToolCalls()
					.forEach(toolCall -> toolCalls.add(parsed(toolCall))));
		}

		assertEquals(expectedToolCalls, toolCalls);
	}

	private static Map<String, Object> weather(double lat, double lon) {
		return toolCall("getWeather", "{\"location\":{\"lat\":" + lat + ",\"lon\":" + lon + "}}");
	}

	private static Map<String, Object> time(String zone) {
		return toolCall("getTime", "{\"zone\":\"" + zone + "\"}");
	}

	private static Map<String, Object> toolCall(String name, String arguments) {
		return Map.of("name", name, "arguments", withComparableNumbers(JSON_HELPER.fromJsonToMap(arguments)));
	}

	private static Map<String, Object> parsed(AssistantMessage.ToolCall toolCall) {
		Object arguments;
		try {
			arguments = withComparableNumbers(JSON_HELPER.fromJsonToMap(toolCall.arguments()));
		}
		catch (RuntimeException ex) {
			// Keep the raw arguments, so a failure shows what the application got
			arguments = toolCall.arguments();
		}
		return Map.of("name", toolCall.name(), "arguments", arguments);
	}

	/** Models write 121 or 121.0 for the same number, so numbers compare by value. */
	private static Object withComparableNumbers(Object json) {
		if (json instanceof Map<?, ?> map) {
			Map<Object, Object> copy = new LinkedHashMap<>();
			map.forEach((key, value) -> copy.put(key, withComparableNumbers(value)));
			return copy;
		}
		if (json instanceof List<?> list) {
			return list.stream().map(WatsonxAiChatModelRecordedStreamTest::withComparableNumbers).toList();
		}
		if (json instanceof Number number) {
			return new BigDecimal(number.toString()).stripTrailingZeros();
		}
		return json;
	}

	private static List<String> recording(String name) throws IOException {
		try (InputStream input = WatsonxAiChatModelRecordedStreamTest.class
			.getResourceAsStream("/recorded-streams/" + name)) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
				.filter(line -> !line.isBlank())
				.toList();
		}
	}

	private static WatsonxAiChatModel chatModel(List<String> recording) {
		List<String> events = new ArrayList<>(recording);
		events.add("[DONE]");
		WebClient.Builder webClientBuilder = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(Flux.fromIterable(events).map(WatsonxAiChatModelRecordedStreamTest::sseEvent))
				.build()));

		WatsonxAiChatApi chatApi;
		try (MockedConstruction<WatsonxAiAuthentication> ignored = mockConstruction(WatsonxAiAuthentication.class,
				(authentication, context) -> when(authentication.getAccessToken()).thenReturn("test-token"))) {
			chatApi = new WatsonxAiChatApi("https://us-south.ml.cloud.ibm.com", "/ml/v1/text/chat",
					"/ml/v1/text/chat_stream", "2024-05-31", null, "test-space-id", "test-api-key",
					RestClient.builder(), webClientBuilder, response -> false);
		}

		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(chatApi)
			.options(WatsonxAiChatOptions.builder().model("test-model").build())
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
			.build();
	}

	private static DataBuffer sseEvent(String data) {
		return DefaultDataBufferFactory.sharedInstance
			.wrap(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
	}

}
