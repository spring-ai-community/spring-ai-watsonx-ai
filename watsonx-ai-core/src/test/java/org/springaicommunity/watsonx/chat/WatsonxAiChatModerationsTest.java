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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.util.JsonHelper;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

/**
 * Tests that the {@code moderations} option is sent to watsonx.ai as its
 * {@code moderations} field, and how it combines with other options and with an
 * additional property of the same name. The chat API is mocked.
 *
 * @author Federico Mariani
 */
class WatsonxAiChatModerationsTest {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final String ANSWER = """
			{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"message":{"role":"assistant","content":"yes"},"finish_reason":"stop"}]}""";

	private static final String ANSWER_CHUNK = """
			{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"delta":{"role":"assistant","content":"yes"},"finish_reason":"stop"}]}""";

	private final WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);

	@Test
	void builderWritesTheModerationsOfTheApi() {
		WatsonxAiChatModerations moderations = WatsonxAiChatModerations.builder()
			.hapInput(0.5)
			.hapOutput(0.7)
			.piiInput(true)
			.piiOutput(false)
			.graniteGuardianInput(0.6)
			.build();

		assertThat(JSON_HELPER.fromJsonToMap(JSON_HELPER.toJson(moderations))).isEqualTo(Map.of("hap",
				Map.of("input", Map.of("enabled", true, "threshold", 0.5), "output",
						Map.of("enabled", true, "threshold", 0.7)),
				"pii", Map.of("input", Map.of("enabled", true), "output", Map.of("enabled", false)), "granite_guardian",
				Map.of("input", Map.of("enabled", true, "threshold", 0.6))));
	}

	@Test
	void builderLeavesOutTheModerationsAndThresholdsNotSet() {
		WatsonxAiChatModerations moderations = WatsonxAiChatModerations.builder().hapInput(null).build();

		assertThat(JSON_HELPER.toJson(moderations)).isEqualTo("{\"hap\":{\"input\":{\"enabled\":true}}}");
	}

	@Test
	void callAndStreamSendTheModerations() {
		mockCallAndStream();
		WatsonxAiChatModerations moderations = WatsonxAiChatModerations.builder().hapInput(0.5).build();
		WatsonxAiChatOptions options = WatsonxAiChatOptions.builder().moderations(moderations).build();

		chatModel(defaultOptions()).call(new Prompt("Hi", options));
		chatModel(defaultOptions()).stream(new Prompt("Hi", options)).collectList().block();

		assertThat(sentCall().moderations()).isEqualTo(moderations);
		assertThat(sentStream().moderations()).isEqualTo(moderations);
		assertThat(JSON_HELPER.toJson(sentCall()))
			.contains("\"moderations\":{\"hap\":{\"input\":{\"enabled\":true,\"threshold\":0.5}}}");
	}

	@Test
	void moderationsAreNotSentWhenNotSet() {
		mockCallAndStream();

		chatModel(defaultOptions()).call(new Prompt("Hi"));

		assertThat(JSON_HELPER.toJson(sentCall())).doesNotContain("moderations");
	}

	// The spec of the deployment chat request has no moderations field
	@Test
	void deploymentRequestDoesNotSendTheModerations() {
		when(this.chatApi.deploymentChat(eq("my-deployment"), any()))
			.thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson(ANSWER, WatsonxAiChatResponse.class)));

		chatModel(defaultOptions()).call(new Prompt("Hi",
				WatsonxAiChatOptions.builder()
					.deploymentId("my-deployment")
					.moderations(WatsonxAiChatModerations.builder().hapInput(0.5).build())
					.build()));

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).deploymentChat(eq("my-deployment"), request.capture());
		assertThat(JSON_HELPER.toJson(request.getValue())).doesNotContain("moderations");
	}

	// Before the option existed, moderations could only be sent as an additional
	// property, and that still works
	@Test
	void additionalPropertyNamedModerationsIsStillSent() {
		mockCallAndStream();
		Map<String, Object> moderations = Map.of("pii", Map.of("input", Map.of("enabled", true)));

		chatModel(defaultOptions()).call(new Prompt("Hi",
				WatsonxAiChatOptions.builder().additionalProperty("moderations", moderations).build()));

		assertThat(sentCall().moderations()).isNull();
		assertThat(JSON_HELPER.fromJsonToMap(JSON_HELPER.toJson(sentCall()))).containsEntry("moderations", moderations);
	}

	@Test
	void moderationsOptionWinsOverAnAdditionalPropertyNamedModerations() {
		mockCallAndStream();
		WatsonxAiChatModerations moderations = WatsonxAiChatModerations.builder().hapInput(0.5).build();

		chatModel(defaultOptions()).call(new Prompt("Hi",
				WatsonxAiChatOptions.builder()
					.moderations(moderations)
					.additionalProperty("moderations", Map.of("pii", Map.of("input", Map.of("enabled", true))))
					.additionalProperty("repetition_penalty", 2.0)
					.build()));

		String json = JSON_HELPER.toJson(sentCall());
		assertThat(json).containsOnlyOnce("\"moderations\"")
			.contains("\"moderations\":{\"hap\":{\"input\":{\"enabled\":true,\"threshold\":0.5}}}")
			.doesNotContain("pii")
			.contains("\"repetition_penalty\":2.0");
	}

	// WatsonxAiChatApi adds the project or space with toBuilder()
	@Test
	void moderationsSurviveToBuilder() {
		WatsonxAiChatModerations moderations = WatsonxAiChatModerations.builder().piiInput(true).build();

		WatsonxAiChatRequest request = WatsonxAiChatRequest.builder()
			.moderations(moderations)
			.additionalProperties(Map.of("moderations", Map.of("hap", Map.of())))
			.build()
			.toBuilder()
			.spaceId("test-space")
			.build();

		assertThat(request.moderations()).isEqualTo(moderations);
		assertThat(request.additionalProperties()).doesNotContainKey("moderations");
	}

	@Test
	void requestModerationsReplaceTheDefaultOnes() {
		mockCallAndStream();
		WatsonxAiChatModerations defaults = WatsonxAiChatModerations.builder().hapInput(0.5).build();
		WatsonxAiChatModerations perRequest = WatsonxAiChatModerations.builder().piiInput(true).build();
		ChatClient chatClient = ChatClient.create(chatModel(defaultOptions().mutate().moderations(defaults).build()));

		chatClient.prompt("Hi").options(WatsonxAiChatOptions.builder().moderations(perRequest)).call().content();

		assertThat(sentCall().moderations()).isEqualTo(perRequest);
	}

	@Test
	void chatClientSendsTheDefaultModerations() {
		mockCallAndStream();
		WatsonxAiChatModerations defaults = WatsonxAiChatModerations.builder().hapInput(0.5).build();
		ChatClient chatClient = ChatClient.create(chatModel(defaultOptions().mutate().moderations(defaults).build()));

		chatClient.prompt("Hi").options(WatsonxAiChatOptions.builder().temperature(0.1)).call().content();

		assertThat(sentCall().moderations()).isEqualTo(defaults);
	}

	// Options read from JSON, such as the moderations that could only be sent as an
	// additional property before, keep the fields that the classes don't model
	@Test
	void moderationsReadFromJsonKeepTheFieldsTheyDontModel() {
		mockCallAndStream();
		String moderations = """
				{"pii":{"output":{"enabled":true,"x":1},"mask":{"remove_entity_value":true}},"input_ranges":[{"start":0,"end":2}]}""";
		WatsonxAiChatOptions options = JSON_HELPER
			.fromJson("{\"model_id\":\"test/model\",\"moderations\":" + moderations + "}", WatsonxAiChatOptions.class);

		chatModel(defaultOptions()).call(new Prompt("Hi", options));

		assertThat(JSON_HELPER.fromJsonToMap(JSON_HELPER.toJson(sentCall())).get("moderations"))
			.isEqualTo(JSON_HELPER.fromJsonToMap(moderations));
	}

	@Test
	void optionsWriteOnlyTheModerationsOptionWhenAnAdditionalPropertyHasTheSameName() {
		WatsonxAiChatModerations moderations = WatsonxAiChatModerations.builder().hapInput(0.5).build();
		WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
			.moderations(moderations)
			.additionalProperty("moderations", Map.of("pii", Map.of("input", Map.of("enabled", true))))
			.build();

		// The raw additional properties are written under "additional" as before; the
		// top-level fields have the moderations option only
		String json = JSON_HELPER.toJson(options);
		String rawAdditional = "\"additional\":{\"moderations\":{\"pii\":{\"input\":{\"enabled\":true}}}}";
		assertThat(json).contains(rawAdditional);
		assertThat(json.replace(rawAdditional, "")).containsOnlyOnce("\"moderations\"").doesNotContain("pii");
		assertThat(options.getAdditionalProperties()).doesNotContainKey("moderations");
		assertThat(options.toMap().get("moderations"))
			.isEqualTo(JSON_HELPER.fromJsonToMap(JSON_HELPER.toJson(moderations)));
	}

	@Test
	void optionsWithOnlyAnAdditionalPropertyNamedModerationsWriteItOnce() {
		WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
			.additionalProperty("moderations", Map.of("pii", Map.of("input", Map.of("enabled", true))))
			.build();

		String json = JSON_HELPER.toJson(options);
		String rawAdditional = "\"additional\":{\"moderations\":{\"pii\":{\"input\":{\"enabled\":true}}}}";
		assertThat(json.replace(rawAdditional, "")).containsOnlyOnce("\"moderations\"")
			.doesNotContain("\"moderations\":null");
	}

	@Test
	void copyIsDeepAndKeepsTheValuesOfTheFieldsNotModeled() {
		WatsonxAiChatModerations moderations = JSON_HELPER.fromJson(
				"""
						{"hap":{"input":{"threshold":0.5,"x":{"y":[1,2]}},"output":{"enabled":true}},"pii":{"input":{"enabled":true},
						 "mask":{"remove_entity_value":true}},"granite_guardian":{"input":{"threshold":0.6}},"input_ranges":[{"start":0,"end":2}]}""",
				WatsonxAiChatModerations.class);

		WatsonxAiChatModerations copy = moderations.copy();

		assertThat(copy).isEqualTo(moderations).isNotSameAs(moderations);
		assertThat(copy.getHap()).isNotSameAs(moderations.getHap());
		assertThat(copy.getHap().getInput()).isNotSameAs(moderations.getHap().getInput());
		assertThat(copy.getPii().getInput()).isNotSameAs(moderations.getPii().getInput());
		assertThat(copy.getGraniteGuardian().getInput()).isNotSameAs(moderations.getGraniteGuardian().getInput());
		assertThat(copy.otherFields().get("input_ranges")).isNotSameAs(moderations.otherFields().get("input_ranges"));
		assertThat(copy.getPii().otherFields().get("mask")).isNotSameAs(moderations.getPii().otherFields().get("mask"));
		assertThat(JSON_HELPER.toJson(copy)).isEqualTo(JSON_HELPER.toJson(moderations));
	}

	// ChatClient derives the options of every request from the defaults with mutate()
	@Test
	void derivedOptionsDontShareTheModerations() {
		WatsonxAiChatOptions defaults = WatsonxAiChatOptions.builder()
			.moderations(WatsonxAiChatModerations.builder().piiInput(true).build())
			.build();

		WatsonxAiChatOptions derived = defaults.mutate().build();
		derived.getModerations().getPii().getInput().setEnabled(false);
		WatsonxAiChatOptions cloned = defaults.mutate().clone().build();
		cloned.getModerations().setHap(new WatsonxAiChatModerations.Hap());

		assertThat(defaults.getModerations().getPii().getInput().getEnabled()).isTrue();
		assertThat(defaults.getModerations().getHap()).isNull();
	}

	@Test
	void optionsCompareTheModerations() {
		WatsonxAiChatOptions hap = WatsonxAiChatOptions.builder()
			.moderations(WatsonxAiChatModerations.builder().hapInput(0.5).build())
			.build();

		assertThat(hap.mutate().build()).isEqualTo(hap).hasSameHashCodeAs(hap);
		assertThat(hap).isNotEqualTo(WatsonxAiChatOptions.builder()
			.moderations(WatsonxAiChatModerations.builder().hapInput(0.6).build())
			.build());
	}

	private void mockCallAndStream() {
		when(this.chatApi.chat(any()))
			.thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson(ANSWER, WatsonxAiChatResponse.class)));
		when(this.chatApi.stream(any()))
			.thenReturn(Flux.just(JSON_HELPER.fromJson(ANSWER_CHUNK, WatsonxAiChatStream.class)));
	}

	private WatsonxAiChatRequest sentCall() {
		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).chat(request.capture());
		return request.getValue();
	}

	private WatsonxAiChatRequest sentStream() {
		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).stream(request.capture());
		return request.getValue();
	}

	private static WatsonxAiChatOptions defaultOptions() {
		return WatsonxAiChatOptions.builder().model("test/model").build();
	}

	private WatsonxAiChatModel chatModel(WatsonxAiChatOptions defaultOptions) {
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(this.chatApi)
			.options(defaultOptions)
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.build();
	}

}
