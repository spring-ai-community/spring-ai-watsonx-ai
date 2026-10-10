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

package org.springaicommunity.watsonx.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.chat.WatsonxAiChatApi;
import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingApi;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingModel;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingOptions;
import org.springaicommunity.watsonx.embedding.WatsonxAiEmbeddingResponse;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankApi;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankModel;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankOptions;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.JsonHelper;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

/**
 * Tests that the warnings watsonx.ai returns in {@code system.warnings} are logged once
 * per model, and kept in the response metadata where the response has metadata. The
 * watsonx.ai APIs are mocked, so no network call is made.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiWarningsTest {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

	private final Logger logger = (Logger) LoggerFactory.getLogger(WatsonxAiWarnings.class);

	// The logged warnings are remembered for the whole JVM, so every test uses its own
	// model ID
	private final String model = "test/model-" + UUID.randomUUID();

	@BeforeEach
	void captureLogs() {
		this.logs.start();
		this.logger.addAppender(this.logs);
	}

	@AfterEach
	void stopCapturingLogs() {
		this.logger.detachAppender(this.logs);
	}

	@Test
	void eachWarningIsLoggedOncePerModel() {
		WatsonxAiWarnings.log(this.model, "deprecation_warning", "Deprecated.");
		WatsonxAiWarnings.log(this.model, "deprecation_warning", "Deprecated.");
		WatsonxAiWarnings.log(this.model, "disclaimer_warning", "Third-party license.");
		WatsonxAiWarnings.log(this.model + "-other", "deprecation_warning", "Deprecated.");
		WatsonxAiWarnings.log(this.model, "empty_warning", " ");

		assertThat(this.logs.list).extracting(ILoggingEvent::getLevel).containsOnly(Level.WARN);
		assertThat(this.logs.list).extracting(ILoggingEvent::getFormattedMessage)
			.containsExactly("watsonx.ai warning for model '" + this.model + "' (deprecation_warning): Deprecated.",
					"watsonx.ai warning for model '" + this.model + "' (disclaimer_warning): Third-party license.",
					"watsonx.ai warning for model '" + this.model + "-other' (deprecation_warning): Deprecated.");
	}

	@Test
	void chatCallLogsWarningsAndKeepsThemInMetadata() {
		WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);
		when(chatApi.chat(any())).thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson(
				"""
						{"id":"chat-1","model_id":"%s","choices":[{"index":0,"message":{"role":"assistant","content":"Hi"},"finish_reason":"stop"}],
						 "system":{"warnings":[{"id":"deprecation_warning","message":"%s"}]}}"""
					.formatted(this.model, deprecation()),
				WatsonxAiChatResponse.class)));

		ChatResponse response = chatModel(chatApi).call(new Prompt("Hi"));

		assertThat(response.getMetadata().<List<?>>get("warnings")).hasSize(1);
		assertThat(warningsLogged()).containsExactly(deprecationLog());
	}

	@Test
	void chatStreamLogsWarnings() {
		WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);
		when(chatApi.stream(any())).thenReturn(Flux.just(
				JSON_HELPER.fromJson(
						"""
								{"id":"chat-1","model_id":"%s","choices":[{"index":0,"delta":{"role":"assistant","content":"Hi"}}],
								 "system":{"warnings":[{"id":"deprecation_warning","message":"%s"}]}}"""
							.formatted(this.model, deprecation()),
						WatsonxAiChatStream.class),
				JSON_HELPER.fromJson(
						"""
								{"id":"chat-1","model_id":"%s","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":"stop"}]}"""
							.formatted(this.model),
						WatsonxAiChatStream.class)));

		chatModel(chatApi).stream(new Prompt("Hi")).collectList().block();

		assertThat(warningsLogged()).containsExactly(deprecationLog());
	}

	@Test
	void embeddingLogsWarningsAndKeepsThemInMetadata() {
		WatsonxAiEmbeddingApi embeddingApi = mock(WatsonxAiEmbeddingApi.class);
		when(embeddingApi.embed(any())).thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson("""
				{"model_id":"%s","results":[{"embedding":[0.1,0.2]}],"input_token_count":1,
				 "system":{"warnings":[{"id":"deprecation_warning","message":"%s"}]}}""".formatted(this.model,
				deprecation()), WatsonxAiEmbeddingResponse.class)));

		EmbeddingResponse response = new WatsonxAiEmbeddingModel(embeddingApi,
				WatsonxAiEmbeddingOptions.builder().model(this.model).build(), ObservationRegistry.NOOP,
				RetryUtils.DEFAULT_RETRY_TEMPLATE)
			.embedForResponse(List.of("hi"));

		assertThat(response.getMetadata().<List<?>>get("warnings")).hasSize(1);
		assertThat(warningsLogged()).containsExactly(deprecationLog());
	}

	@Test
	void rerankLogsWarnings() {
		WatsonxAiRerankApi rerankApi = mock(WatsonxAiRerankApi.class);
		when(rerankApi.rerank(any())).thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson("""
				{"model_id":"%s","results":[{"index":0,"score":0.9}],
				 "system":{"warnings":[{"id":"deprecation_warning","message":"%s"}]}}""".formatted(this.model,
				deprecation()), WatsonxAiRerankResponse.class)));

		new WatsonxAiRerankModel(rerankApi, WatsonxAiRerankOptions.builder().model(this.model).build(),
				ObservationRegistry.NOOP, RetryUtils.DEFAULT_RETRY_TEMPLATE)
			.rerank("hi", List.of("hello"));

		assertThat(warningsLogged()).containsExactly(deprecationLog());
	}

	private String deprecation() {
		return "Model '" + this.model
				+ "' is in deprecated state from 2026-05-05. It will be in withdrawn state from 2027-01-12.";
	}

	private String deprecationLog() {
		return "watsonx.ai warning for model '" + this.model + "' (deprecation_warning): " + deprecation();
	}

	private List<String> warningsLogged() {
		return this.logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
	}

	private static WatsonxAiChatModel chatModel(WatsonxAiChatApi chatApi) {
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(chatApi)
			.options(WatsonxAiChatOptions.builder().model("test/model").build())
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.build();
	}

}
