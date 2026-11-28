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

package org.springaicommunity.watsonx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationApi;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationModel;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationOptions;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationRequest;
import org.springaicommunity.watsonx.moderation.WatsonxAiModerationResponse;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionApi;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionModel;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionOptions;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionRequest;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;

/**
 * Tests that prompts, the text to moderate and the answers of the model are logged at
 * DEBUG level only, as Spring AI logs prompts, because they can carry sensitive data.
 *
 * @author Federico Mariani
 */
class PayloadLoggingTest {

	private static final String SECRET = "my card number is 4111 1111 1111 1111";

	@Test
	void promptWithoutChatCompletionIsLoggedAtDebugOnly() {
		WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);
		when(chatApi.chat(any())).thenReturn(ResponseEntity.ok().build());

		assertLoggedAtDebugOnly(WatsonxAiChatModel.class, () -> chatModel(chatApi).call(new Prompt(SECRET)),
				"No chat completion returned for prompt");
	}

	@Test
	void promptWithoutChoicesIsLoggedAtDebugOnly() {
		WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);
		when(chatApi.chat(any())).thenReturn(ResponseEntity
			.ok(new WatsonxAiChatResponse("chat-1", "test/model", null, null, null, null, null, null)));

		assertLoggedAtDebugOnly(WatsonxAiChatModel.class, () -> chatModel(chatApi).call(new Prompt(SECRET)),
				"No choices returned for prompt");
	}

	@Test
	void moderationRequestWithoutResponseIsLoggedAtDebugOnly() {
		WatsonxAiModerationApi moderationApi = mock(WatsonxAiModerationApi.class);
		when(moderationApi.moderate(any())).thenReturn(ResponseEntity.ok().build());

		List<ILoggingEvent> events = capture(WatsonxAiModerationModel.class,
				() -> moderationModel(moderationApi).call(new ModerationPrompt(SECRET)));

		assertThat(events).noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.INFO));
		assertThat(events).filteredOn(event -> event.getLevel() == Level.DEBUG)
			.extracting(ILoggingEvent::getFormattedMessage)
			.anyMatch(message -> message.startsWith("No moderation response returned for input")
					&& message.contains(SECRET));
	}

	// An unexpected answer is still a warning, without the text of the model. With a
	// score of 0, the answer is logged by this warning only
	@Test
	void unexpectedGraniteGuardianAnswerIsAWarningWithoutTheAnswer() {
		WatsonxAiModerationApi moderationApi = mock(WatsonxAiModerationApi.class);
		when(moderationApi.moderate(any())).thenReturn(ResponseEntity.ok(new WatsonxAiModerationResponse(List
			.of(new WatsonxAiModerationResponse.Detection(0, 10, "Hello", "risk", "Sure, " + SECRET, 0.0f, null)))));

		List<ILoggingEvent> events = capture(WatsonxAiModerationModel.class,
				() -> moderationModel(moderationApi).call(new ModerationPrompt("Hello")));

		assertThat(events).filteredOn(event -> event.getLevel() == Level.WARN)
			.singleElement()
			.satisfies(event -> assertThat(event.getFormattedMessage()).startsWith("Unexpected Granite Guardian answer")
				.doesNotContain("Sure"));
		assertThat(events).filteredOn(event -> event.getLevel() == Level.DEBUG)
			.extracting(ILoggingEvent::getFormattedMessage)
			.anyMatch(message -> message.startsWith("Unexpected Granite Guardian answer: ")
					&& message.contains("Sure, my card number"));
	}

	// The message of the exception has the body of the error response
	@Test
	void failedCleanupOfATextExtractionIsAWarningWithoutTheErrorBody() {
		WatsonxAiTextExtractionApi textExtractionApi = mock(WatsonxAiTextExtractionApi.class);
		when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
			.thenReturn(ResponseEntity.ok(job("submitted")));
		when(textExtractionApi.getExtraction("job-1")).thenReturn(ResponseEntity.ok(job("completed")));
		when(textExtractionApi.downloadFile(anyString())).thenReturn("text");
		doThrow(new NonTransientAiException("403 - " + SECRET)).when(textExtractionApi).deleteFile(anyString());
		when(textExtractionApi.deleteExtraction("job-1")).thenThrow(new NonTransientAiException("404 - " + SECRET));
		WatsonxAiTextExtractionModel model = new WatsonxAiTextExtractionModel(textExtractionApi,
				WatsonxAiTextExtractionOptions.builder().build(), ObservationRegistry.NOOP,
				RetryUtils.DEFAULT_RETRY_TEMPLATE);

		List<ILoggingEvent> events = capture(WatsonxAiTextExtractionModel.class,
				() -> model.extract("pdf".getBytes(StandardCharsets.UTF_8), SECRET + ".pdf"));

		assertThat(events).filteredOn(event -> event.getLevel() == Level.WARN).hasSize(3).allSatisfy(event -> {
			assertThat(event.getFormattedMessage()).startsWith("Failed to delete").doesNotContain(SECRET);
			assertThat(event.getThrowableProxy()).isNull();
		});
		assertThat(events).filteredOn(event -> event.getLevel() == Level.DEBUG)
			.filteredOn(event -> event.getThrowableProxy() != null)
			.hasSize(3)
			.allSatisfy(event -> assertThat(event.getThrowableProxy().getMessage()).contains(SECRET));
	}

	private static WatsonxAiTextExtractionResponse job(String status) {
		return WatsonxAiTextExtractionResponse.builder()
			.metadata(new WatsonxAiTextExtractionResponse.ExtractionMetadata("job-1", null, null))
			.entity(new WatsonxAiTextExtractionResponse.ExtractionEntity(null, null, null,
					new WatsonxAiTextExtractionResponse.ExtractionResults(status, 1, null)))
			.build();
	}

	private static void assertLoggedAtDebugOnly(Class<?> type, Runnable action, String message) {
		List<ILoggingEvent> events = capture(type, action);

		assertThat(events).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.INFO))
			.extracting(ILoggingEvent::getFormattedMessage)
			.noneMatch(logged -> logged.contains(SECRET));
		assertThat(events).filteredOn(event -> event.getLevel() == Level.DEBUG)
			.extracting(ILoggingEvent::getFormattedMessage)
			.anyMatch(logged -> logged.startsWith(message) && logged.contains(SECRET));
	}

	private static List<ILoggingEvent> capture(Class<?> type, Runnable action) {
		Logger logger = (Logger) LoggerFactory.getLogger(type);
		Level originalLevel = logger.getLevel();
		ListAppender<ILoggingEvent> logs = new ListAppender<>();
		logs.start();
		logger.setLevel(Level.DEBUG);
		logger.addAppender(logs);
		try {
			action.run();
		}
		finally {
			logger.detachAppender(logs);
			logger.setLevel(originalLevel);
		}
		return logs.list;
	}

	private static WatsonxAiChatModel chatModel(WatsonxAiChatApi chatApi) {
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(chatApi)
			.options(WatsonxAiChatOptions.builder().model("test/model").build())
			.observationRegistry(ObservationRegistry.NOOP)
			.build();
	}

	private static WatsonxAiModerationModel moderationModel(WatsonxAiModerationApi moderationApi) {
		return WatsonxAiModerationModel.builder()
			.watsonxAiModerationApi(moderationApi)
			.defaultOptions(WatsonxAiModerationOptions.builder()
				.graniteGuardian(WatsonxAiModerationRequest.DetectorConfig.of(0.5f))
				.build())
			.retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
			.build();
	}

}
