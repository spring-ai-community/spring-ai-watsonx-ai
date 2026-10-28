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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.JsonHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * Tests that a deployment request has no system message: watsonx.ai rejects one there
 * ({@code Deployment text chat request cannot contain 'system' role in 'messages'}), so
 * its text goes into the first user message.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatModelDeploymentSystemMessagesTest {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final String DEPLOYMENT_ID = "01a12498-c4b5-753e-8a31-8d377c2b6d84";

	private final WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);

	@Test
	void systemMessageGoesIntoTheFirstUserMessage() {
		List<Message> messages = WatsonxAiChatModel
			.withoutSystemMessages(List.of(new SystemMessage("Answer in French."), new UserMessage("Hi")));

		assertThat(messages).singleElement().satisfies(message -> {
			assertThat(message).isInstanceOf(UserMessage.class);
			assertThat(message.getText()).isEqualTo("Answer in French.\n\nHi");
		});
	}

	@Test
	void systemMessageAfterTheUserMessageGoesIntoIt() {
		List<Message> messages = WatsonxAiChatModel
			.withoutSystemMessages(List.of(new UserMessage("Hi"), new SystemMessage("Answer in French.")));

		assertThat(messages).extracting(Message::getText).containsExactly("Answer in French.\n\nHi");
	}

	@Test
	void systemMessagesAreJoinedInOrder() {
		List<Message> messages = WatsonxAiChatModel.withoutSystemMessages(
				List.of(new SystemMessage("Be terse."), new SystemMessage("Answer in French."), new UserMessage("Hi")));

		assertThat(messages).extracting(Message::getText).containsExactly("Be terse.\n\nAnswer in French.\n\nHi");
	}

	@Test
	void onlyTheFirstUserMessageChanges() {
		List<Message> messages = WatsonxAiChatModel
			.withoutSystemMessages(List.of(new SystemMessage("Answer in French."), new UserMessage("Hi"),
					AssistantMessage.builder().content("Bonjour !").build(),
					new UserMessage("What is the capital of Japan?")));

		assertThat(messages).extracting(Message::getText)
			.containsExactly("Answer in French.\n\nHi", "Bonjour !", "What is the capital of Japan?");
		assertThat(messages).extracting(Message::getMessageType).doesNotContain(MessageType.SYSTEM);
	}

	@Test
	void userMessageKeepsItsMedia() {
		Media image = Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data(new byte[] { 1, 2, 3 }).build();
		List<Message> messages = WatsonxAiChatModel.withoutSystemMessages(List.of(new SystemMessage("Be terse."),
				UserMessage.builder().text("What is in the image?").media(image).build()));

		assertThat(messages).singleElement().isInstanceOfSatisfying(UserMessage.class, message -> {
			assertThat(message.getText()).isEqualTo("Be terse.\n\nWhat is in the image?");
			assertThat(message.getMedia()).containsExactly(image);
		});
	}

	@Test
	void withoutUserMessageTheSystemTextBecomesOne() {
		List<Message> messages = WatsonxAiChatModel.withoutSystemMessages(
				List.of(AssistantMessage.builder().content("Hello!").build(), new SystemMessage("Answer in French.")));

		assertThat(messages).extracting(Message::getText).containsExactly("Hello!", "Answer in French.");
		assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
	}

	@Test
	void blankSystemMessageIsDropped() {
		List<Message> messages = WatsonxAiChatModel
			.withoutSystemMessages(List.of(new SystemMessage(" "), new UserMessage("Hi")));

		assertThat(messages).extracting(Message::getText).containsExactly("Hi");
	}

	@Test
	void withoutSystemMessageNothingChanges() {
		List<Message> original = List.of(new UserMessage("Hi"));

		assertThat(WatsonxAiChatModel.withoutSystemMessages(original)).isSameAs(original);
	}

	@Test
	void deploymentCallHasNoSystemRole() {
		mockCall();

		ChatClient.builder(chatModel(WatsonxAiChatOptions.builder().deploymentId(DEPLOYMENT_ID).build()))
			.defaultSystem("Answer in French.")
			.build()
			.prompt("What is the capital of Japan?")
			.call()
			.content();

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).deploymentChat(any(), request.capture());
		assertThat(sentMessages(request.getValue()))
			.containsExactly(Map.of("role", "user", "content", "Answer in French.\n\nWhat is the capital of Japan?"));
	}

	@Test
	void deploymentStreamHasNoSystemRole() {
		when(this.chatApi.deploymentStream(any(), any())).thenReturn(Flux.just(JSON_HELPER.fromJson(
				"""
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"delta":{"role":"assistant","content":"Tokyo."},"finish_reason":"stop"}]}""",
				WatsonxAiChatStream.class)));

		chatModel(WatsonxAiChatOptions.builder().deploymentId(DEPLOYMENT_ID).build())
			.stream(new Prompt(
					List.of(new SystemMessage("Answer in French."), new UserMessage("What is the capital of Japan?"))))
			.collectList()
			.block();

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).deploymentStream(any(), request.capture());
		assertThat(sentMessages(request.getValue()))
			.containsExactly(Map.of("role", "user", "content", "Answer in French.\n\nWhat is the capital of Japan?"));
	}

	@Test
	void modelCallKeepsTheSystemRole() {
		when(this.chatApi.chat(any())).thenReturn(ResponseEntity.ok(textResponse()));

		chatModel(WatsonxAiChatOptions.builder().model("test/model").build()).call(new Prompt(
				List.of(new SystemMessage("Answer in French."), new UserMessage("What is the capital of Japan?"))));

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(this.chatApi).chat(request.capture());
		assertThat(sentMessages(request.getValue())).containsExactly(
				Map.of("role", "system", "content", "Answer in French."),
				Map.of("role", "user", "content", "What is the capital of Japan?"));
	}

	private void mockCall() {
		when(this.chatApi.deploymentChat(any(), any())).thenReturn(ResponseEntity.ok(textResponse()));
	}

	private static WatsonxAiChatResponse textResponse() {
		return JSON_HELPER.fromJson(
				"""
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"message":{"role":"assistant","content":"Tokyo."},"finish_reason":"stop"}]}""",
				WatsonxAiChatResponse.class);
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> sentMessages(WatsonxAiChatRequest request) {
		return (List<Map<String, Object>>) JSON_HELPER.fromJsonToMap(JSON_HELPER.toJson(request)).get("messages");
	}

	private WatsonxAiChatModel chatModel(WatsonxAiChatOptions options) {
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(this.chatApi)
			.options(options)
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
			.build();
	}

}
