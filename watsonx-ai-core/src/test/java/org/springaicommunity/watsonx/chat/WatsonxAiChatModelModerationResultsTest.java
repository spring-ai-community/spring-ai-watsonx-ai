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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse.TextChatChoiceDetections;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse.TextChatDetectionResult;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse.TextChatDetections;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse.TextChatModerationResult;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse.TextChatModerationTextRange;
import org.springaicommunity.watsonx.chat.util.WatsonxAiChatChunkMerger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ContentFilteredException;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.metadata.PromptMetadata.PromptFilterMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Replays responses that watsonx.ai sent with chat {@code moderations}, recorded in eu-de
 * with {@code ibm/granite-4-h-small}: a request blocked by the HAP moderation of the
 * input, and an answer flagged by the HAP moderation of the output. No watsonx.ai or IBM
 * IAM call is made.
 *
 * @author Federico Mariani
 */
class WatsonxAiChatModelModerationResultsTest {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	@Test
	void callBlockedByAnInputModerationIsFiltered() throws IOException {
		ChatResponse response = callModel("hap-input-blocked.json").call(new Prompt("Hi"));

		Generation generation = response.getResult();
		assertThat(generation.getOutput().getText()).isEmpty();
		assertThat(generation.getOutput().getToolCalls()).isEmpty();
		assertThat(generation.getMetadata().getFinishReasonCategory()).isEqualTo(FinishReasonCategory.FILTERED);
		assertThat(generation.getMetadata().getContentFilters()).containsExactly("hap");
		assertThat(response.hasFinishReasonCategory(FinishReasonCategory.FILTERED)).isTrue();
		assertInputModerations(response);
	}

	@Test
	void chatClientEntityOfABlockedCallThrowsContentFilteredException() throws IOException {
		ChatClient chatClient = ChatClient.create(callModel("hap-input-blocked.json"));

		assertThatExceptionOfType(ContentFilteredException.class)
			.isThrownBy(() -> chatClient.prompt("Hi").call().entity(Joke.class))
			.satisfies(ex -> assertThat(ex.getText()).isEmpty());
	}

	// watsonx.ai sends one chunk whose choice has no delta, and no [DONE]
	@Test
	void streamBlockedByAnInputModerationIsFiltered() throws IOException {
		List<ChatResponse> responses = streamModel(recording("hap-input-blocked-stream.jsonl")).stream(new Prompt("Hi"))
			.collectList()
			.block();

		assertThat(responses).hasSize(1);
		ChatResponse response = responses.get(0);
		assertThat(response.getResults()).hasSize(1);
		Generation generation = response.getResult();
		assertThat(generation.getOutput().getText()).isEmpty();
		assertThat(generation.getMetadata().getFinishReasonCategory()).isEqualTo(FinishReasonCategory.FILTERED);
		assertThat(generation.getMetadata().getContentFilters()).containsExactly("hap");
		assertInputModerations(response);
	}

	@Test
	void chatClientStreamOfABlockedRequestCompletesWithoutText() throws IOException {
		ChatClient chatClient = ChatClient.create(streamModel(recording("hap-input-blocked-stream.jsonl")));

		List<String> content = chatClient.prompt("Hi").stream().content().collectList().block();

		assertThat(String.join("", content)).isEmpty();
	}

	// A flagged output is returned as the model wrote it: it's reported, not blocked
	@Test
	void callWithAFlaggedOutputIsCompletedAndReportsTheModeration() throws IOException {
		ChatResponse response = callModel("hap-output-flagged.json").call(new Prompt("Hi"));

		Generation generation = response.getResult();
		assertThat(generation.getOutput().getText()).isEqualTo("You are an idiot and I hate you. Have a nice day.");
		assertThat(generation.getMetadata().getFinishReason()).isEqualTo("stop");
		assertThat(generation.getMetadata().getFinishReasonCategory()).isEqualTo(FinishReasonCategory.COMPLETED);
		assertThat(generation.getMetadata().getContentFilters()).isEmpty();
		assertThat(response.getMetadata().getPromptMetadata()).isEmpty();
		assertThat(moderations(response)).containsOnlyKeys("hap");
		assertThat(moderations(response).get("hap")).singleElement().satisfies(result -> {
			assertThat(result.input()).isFalse();
			assertThat(result.entity()).isEqualTo("has_HAP");
			assertThat(result.word()).isEqualTo("You are an idiot and I hate you.");
			assertThat(result.position()).isEqualTo(new TextChatModerationTextRange(0, 32));
		});
	}

	@Test
	void streamWithAFlaggedOutputIsCompletedAndReportsTheModeration() throws IOException {
		List<String> events = new ArrayList<>(recording("hap-output-flagged-stream.jsonl"));
		events.add("[DONE]");

		List<ChatResponse> responses = streamModel(events).stream(new Prompt("Hi")).collectList().block();

		assertThat(responses.stream()
			.flatMap(response -> response.getResults().stream())
			.map(generation -> generation.getOutput().getText())
			.filter(Objects::nonNull)
			.collect(Collectors.joining())).isEqualTo("You are an idiot and I hate you. Have a nice day.");
		assertThat(responses).noneMatch(response -> response.hasFinishReasonCategory(FinishReasonCategory.FILTERED));
		assertThat(responses).filteredOn(response -> response.getMetadata().containsKey("moderations"))
			.singleElement()
			.satisfies(response -> assertThat(moderations(response).get("hap")).singleElement()
				.satisfies(result -> assertThat(result.input()).isFalse()));
		assertThat(responses.stream()
			.flatMap(response -> response.getResults().stream())
			.filter(generation -> "stop".equals(generation.getMetadata().getFinishReason()))
			.map(generation -> generation.getMetadata().getFinishReasonCategory()))
			.containsExactly(FinishReasonCategory.COMPLETED);
	}

	// Tool call chunks are merged into one: the moderation results of each are kept
	@Test
	void mergedChunksKeepTheModerationResultsOfEveryChunk() {
		TextChatModerationResult first = new TextChatModerationResult(0.8, false,
				new TextChatModerationTextRange(0, 10), "PhoneNumber", "3572865321");
		TextChatModerationResult second = new TextChatModerationResult(0.8, false,
				new TextChatModerationTextRange(0, 10), "PhoneNumber", "2132348765");
		WatsonxAiChatChunkMerger merger = new WatsonxAiChatChunkMerger();

		WatsonxAiChatStream merged = merger
			.merge(merger.merge(new WatsonxAiChatStream(null, null, null, null, null, null, null, null),
					chunk(Map.of("pii", List.of(first)))), chunk(Map.of("pii", List.of(second), "hap", List.of())));

		assertThat(merged.moderations()).containsEntry("pii", List.of(first, second)).containsEntry("hap", List.of());
	}

	@Test
	void blockingModerationsAreTheModerationsThatFlaggedTheInput() {
		TextChatModerationResult input = new TextChatModerationResult(0.8, true, null, "PhoneNumber", null);
		TextChatModerationResult output = new TextChatModerationResult(0.9, false, null, "has_HAP", null);
		Map<String, List<TextChatModerationResult>> moderations = Map.of("pii", List.of(input), "hap", List.of(output));

		assertThat(WatsonxAiChatModel.blockingModerations(true, null, moderations)).containsExactly("pii");
		// A choice with a message or a finish reason was not blocked
		assertThat(WatsonxAiChatModel.blockingModerations(false, null, moderations)).isEmpty();
		assertThat(WatsonxAiChatModel.blockingModerations(true, "stop", moderations)).isEmpty();
		// Nor was a choice without a message when no moderation flagged the input
		assertThat(WatsonxAiChatModel.blockingModerations(true, null, Map.of("hap", List.of(output)))).isEmpty();
		assertThat(WatsonxAiChatModel.blockingModerations(true, null, null)).isEmpty();
	}

	private static WatsonxAiChatStream chunk(Map<String, List<TextChatModerationResult>> moderations) {
		return new WatsonxAiChatStream("chat-1", "test/model", null, List.of(), null, null, null, null, moderations,
				null);
	}

	// detections: what watsonx.ai returns next to moderations, by choice and by message,
	// without documenting it

	private static final TextChatDetectionResult INSULT = new TextChatDetectionResult("en_syntax_slate.38m.hap", "hap",
			"has_HAP", 0.9971058964729308, "You are an idiot and I hate you.", 0, 32, null);

	@Test
	void callWithAFlaggedOutputHasTheDetectionsOfItsGeneration() throws IOException {
		ChatResponse response = callModel("hap-output-flagged.json").call(new Prompt("Hi"));

		assertThat(detections(response.getResult())).containsExactly(INSULT);
		assertThat(response.getResult().getMetadata().getFinishReasonCategory())
			.isEqualTo(FinishReasonCategory.COMPLETED);
		TextChatDetections all = response.getMetadata().get(WatsonxAiChatModel.DETECTIONS_METADATA_KEY);
		assertThat(all.output()).containsExactly(new TextChatChoiceDetections(0, List.of(INSULT)));
		assertThat(all.input()).isNull();
	}

	// watsonx.ai lists the choices of the detections in any order
	@Test
	void eachGenerationGetsTheDetectionsOfItsChoice() throws IOException {
		ChatResponse response = callModel("hap-output-flagged-n2.json").call(new Prompt("Hi"));

		assertThat(response.getResults()).hasSize(2)
			.allSatisfy(generation -> assertThat(detections(generation)).containsExactly(INSULT));
		assertThat(response.getResults())
			.allSatisfy(generation -> assertThat(generation.getMetadata().getFinishReasonCategory())
				.isEqualTo(FinishReasonCategory.COMPLETED));
	}

	// Derived from the recording: only the second choice was flagged
	@Test
	void aGenerationWithoutDetectionsGetsNone() throws IOException {
		Map<String, Object> recorded = JSON_HELPER
			.fromJsonToMap(String.join("", recording("hap-output-flagged-n2.json")));
		recorded.put("detections", Map.of("output", List
			.of(Map.of("choice_index", 1, "results", List.of(JSON_HELPER.fromJsonToMap(JSON_HELPER.toJson(INSULT)))))));

		ChatResponse response = callModelReturning(JSON_HELPER.toJson(recorded)).call(new Prompt("Hi"));

		assertThat(response.getResults().get(0).getMetadata().containsKey(WatsonxAiChatModel.DETECTIONS_METADATA_KEY))
			.isFalse();
		assertThat(detections(response.getResults().get(1))).containsExactly(INSULT);
	}

	// The chunks of the two choices are interleaved
	@Test
	void eachStreamedGenerationGetsTheDetectionsOfItsChoice() throws IOException {
		List<String> events = new ArrayList<>(recording("hap-output-flagged-n2-stream.jsonl"));
		events.add("[DONE]");

		List<ChatResponse> responses = streamModel(events).stream(new Prompt("Hi")).collectList().block();

		Map<Object, List<TextChatDetectionResult>> byChoice = new HashMap<>();
		responses.stream()
			.flatMap(response -> response.getResults().stream())
			.filter(generation -> generation.getMetadata().containsKey(WatsonxAiChatModel.DETECTIONS_METADATA_KEY))
			.forEach(generation -> byChoice
				.computeIfAbsent(generation.getOutput().getMetadata().get("index"), key -> new ArrayList<>())
				.addAll(detections(generation)));
		assertThat(byChoice).containsOnlyKeys(0, 1);
		assertThat(byChoice.values()).allSatisfy(results -> assertThat(results).containsExactly(INSULT));
	}

	@Test
	void blockedInputHasItsDetectionsByMessageInTheResponse() throws IOException {
		ChatResponse response = callModel("pii-input-blocked.json").call(new Prompt("Hi"));

		assertThat(response.getResult().getMetadata().getFinishReasonCategory())
			.isEqualTo(FinishReasonCategory.FILTERED);
		assertThat(response.getResult().getMetadata().getContentFilters()).containsExactly("pii");
		assertThat(response.getResult().getMetadata().containsKey(WatsonxAiChatModel.DETECTIONS_METADATA_KEY))
			.isFalse();
		TextChatDetections all = response.getMetadata().get(WatsonxAiChatModel.DETECTIONS_METADATA_KEY);
		assertThat(all.input()).singleElement().satisfies(message -> {
			assertThat(message.messageIndex()).isEqualTo(1);
			assertThat(message.results()).extracting(TextChatDetectionResult::text)
				.containsExactly("3572865321", "2132348765");
		});
		assertThat(response.getMetadata().getPromptMetadata()).singleElement()
			.satisfies(promptFilter -> assertThat(
					promptFilter.<Map<String, List<TextChatModerationResult>>>getContentFilterMetadata())
				.isEqualTo(moderations(response)));
	}

	@Test
	void graniteGuardianDetectionsKeepTheirMetadata() throws IOException {
		ChatResponse response = callModel("granite-guardian-input-blocked.json").call(new Prompt("Hi"));

		assertThat(response.getResult().getMetadata().getContentFilters()).containsExactly("granite_guardian");
		TextChatDetections all = response.getMetadata().get(WatsonxAiChatModel.DETECTIONS_METADATA_KEY);
		assertThat(all.input().get(0).results()).singleElement().satisfies(result -> {
			assertThat(result.detectionType()).isEqualTo("risk");
			assertThat(result.detection()).isEqualTo("Yes");
			assertThat(result.metadata()).containsEntry("confidence", "High");
		});
	}

	// watsonx.ai doesn't document detections: without them, the moderations are mapped
	// as before
	@Test
	void withoutDetectionsTheModerationsAreMappedAsBefore() throws IOException {
		for (String recording : List.of("hap-input-blocked.json", "hap-output-flagged.json")) {
			Map<String, Object> recorded = JSON_HELPER.fromJsonToMap(String.join("", recording(recording)));
			recorded.remove("detections");

			ChatResponse response = callModelReturning(JSON_HELPER.toJson(recorded)).call(new Prompt("Hi"));

			assertThat(response.getMetadata().containsKey(WatsonxAiChatModel.DETECTIONS_METADATA_KEY)).isFalse();
			assertThat(response.getResult().getMetadata().containsKey(WatsonxAiChatModel.DETECTIONS_METADATA_KEY))
				.isFalse();
			assertThat(moderations(response)).containsOnlyKeys("hap");
		}
		Map<String, Object> blocked = JSON_HELPER.fromJsonToMap(String.join("", recording("hap-input-blocked.json")));
		blocked.remove("detections");
		assertThat(callModelReturning(JSON_HELPER.toJson(blocked)).call(new Prompt("Hi"))
			.getResult()
			.getMetadata()
			.getFinishReasonCategory()).isEqualTo(FinishReasonCategory.FILTERED);
	}

	@Test
	void incompleteDetectionsAreTolerated() {
		TextChatDetections detections = JSON_HELPER.fromJson("""
				{"output":[null,{"results":null},{"results":[null,{"detection_type":"hap","text":"a"}]},
				 {"choice_index":1}],"input":[null,{"message_index":0}]}""", TextChatDetections.class);

		assertThat(WatsonxAiChatModel.outputDetections(detections, 0)).extracting(TextChatDetectionResult::text)
			.containsExactly("a");
		assertThat(WatsonxAiChatModel.outputDetections(detections, 1)).isEmpty();
		assertThat(WatsonxAiChatModel.outputDetections(null, 0)).isEmpty();
		assertThat(WatsonxAiChatModel.outputDetections(new TextChatDetections(null, null), 0)).isEmpty();
	}

	@Test
	void mergedChunksKeepTheDetectionsOfEveryChunk() {
		TextChatChoiceDetections first = new TextChatChoiceDetections(0, List.of(INSULT));
		TextChatChoiceDetections second = new TextChatChoiceDetections(0, List.of());
		WatsonxAiChatChunkMerger merger = new WatsonxAiChatChunkMerger();

		WatsonxAiChatStream merged = merger.merge(
				merger.merge(new WatsonxAiChatStream(null, null, null, null, null, null, null, null),
						new WatsonxAiChatStream("chat-1", null, null, List.of(), null, null, null, null, null,
								new TextChatDetections(null, List.of(first)))),
				new WatsonxAiChatStream("chat-1", null, null, List.of(), null, null, null, null, null,
						new TextChatDetections(null, List.of(second))));

		assertThat(merged.detections().output()).containsExactly(first, second);
	}

	private static List<TextChatDetectionResult> detections(Generation generation) {
		return generation.getMetadata().get(WatsonxAiChatModel.DETECTIONS_METADATA_KEY);
	}

	private static void assertInputModerations(ChatResponse response) {
		assertThat(moderations(response)).containsOnlyKeys("hap");
		assertThat(moderations(response).get("hap")).singleElement().satisfies(result -> {
			assertThat(result.input()).isTrue();
			assertThat(result.entity()).isEqualTo("has_HAP");
			assertThat(result.word()).isEqualTo("I hate you, you stupid idiot.");
			assertThat(result.score()).isGreaterThan(0.9);
		});
		// The results found in the input are the content filter metadata of the prompt
		assertThat(response.getMetadata().getPromptMetadata()).singleElement().satisfies(promptFilter -> {
			assertThat(promptFilter.getPromptIndex()).isZero();
			Map<String, List<TextChatModerationResult>> filterMetadata = promptFilter.getContentFilterMetadata();
			assertThat(filterMetadata).isEqualTo(moderations(response));
		});
		assertThat(response.getMetadata().getPromptMetadata().findByPromptIndex(0))
			.map(PromptFilterMetadata::getPromptIndex)
			.contains(0);
	}

	private static Map<String, List<TextChatModerationResult>> moderations(ChatResponse response) {
		return response.getMetadata().get(WatsonxAiChatModel.MODERATIONS_METADATA_KEY);
	}

	private static WatsonxAiChatModel callModel(String recording) throws IOException {
		return callModelReturning(String.join("", recording(recording)));
	}

	private static WatsonxAiChatModel callModelReturning(String json) {
		WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);
		when(chatApi.chat(any()))
			.thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson(json, WatsonxAiChatResponse.class)));
		return chatModel(chatApi);
	}

	private static WatsonxAiChatModel streamModel(List<String> events) {
		WebClient.Builder webClientBuilder = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(Flux.fromIterable(events).map(WatsonxAiChatModelModerationResultsTest::sseEvent))
				.build()));

		WatsonxAiChatApi chatApi;
		try (MockedConstruction<WatsonxAiAuthentication> ignored = mockConstruction(WatsonxAiAuthentication.class,
				(authentication, context) -> when(authentication.getAccessToken()).thenReturn("test-token"))) {
			chatApi = new WatsonxAiChatApi("https://eu-de.ml.cloud.ibm.com", "/ml/v1/text/chat",
					"/ml/v1/text/chat_stream", "2024-10-17", null, "test-space-id", "test-api-key",
					RestClient.builder(), webClientBuilder, response -> false);
		}
		return chatModel(chatApi);
	}

	@SuppressWarnings("removal")
	private static WatsonxAiChatModel chatModel(WatsonxAiChatApi chatApi) {
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(chatApi)
			.options(WatsonxAiChatOptions.builder()
				.model("ibm/granite-4-h-small")
				.moderations(WatsonxAiChatModerations.builder().hapInput(0.5).hapOutput(0.5).build())
				.build())
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
			.build();
	}

	private static List<String> recording(String name) throws IOException {
		try (InputStream input = WatsonxAiChatModelModerationResultsTest.class
			.getResourceAsStream("/moderations/" + name)) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
				.filter(line -> !line.isBlank())
				.toList();
		}
	}

	private static DataBuffer sseEvent(String data) {
		return DefaultDataBufferFactory.sharedInstance
			.wrap(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
	}

	record Joke(String setup, String punchline) {
	}

}
