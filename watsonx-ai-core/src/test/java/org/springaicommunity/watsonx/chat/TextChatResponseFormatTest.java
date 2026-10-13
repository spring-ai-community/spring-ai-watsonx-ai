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
import static org.mockito.Mockito.times;
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

/**
 * Tests the JSON that {@link TextChatResponseFormat} sends. watsonx.ai requires
 * {@code response_format.json_schema.name}.
 *
 * @author Ana Katrina Inguengan
 */
class TextChatResponseFormatTest {

	private static final String SCHEMA = """
			{"type":"object","properties":{"name":{"type":"string"},"age":{"type":"integer"}},"required":["name","age"]}""";

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static Map<String, Object> toMap(TextChatResponseFormat format) {
		return JSON_HELPER.fromJsonToMap(JSON_HELPER.toJson(format));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> jsonSchemaOf(TextChatResponseFormat format) {
		return (Map<String, Object>) toMap(format).get("json_schema");
	}

	@Test
	void builderWithSchemaSendsDefaultName() {
		TextChatResponseFormat format = TextChatResponseFormat.builder().schema(SCHEMA).build();

		assertThat(toMap(format)).containsEntry("type", "json_schema");
		assertThat(jsonSchemaOf(format)).containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME)
			.containsKey("schema")
			.doesNotContainKey("strict");
	}

	@Test
	void builderWithNameAndStrictSendsThem() {
		TextChatResponseFormat format = TextChatResponseFormat.builder()
			.schema(SCHEMA)
			.name("person")
			.strict(true)
			.build();

		assertThat(jsonSchemaOf(format)).containsEntry("name", "person").containsEntry("strict", true);
	}

	@Test
	void constructorAndSetterSendDefaultName() {
		assertThat(jsonSchemaOf(new TextChatResponseFormat(TextChatResponseFormat.Type.JSON_SCHEMA, SCHEMA)))
			.containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME);

		TextChatResponseFormat format = new TextChatResponseFormat(TextChatResponseFormat.Type.JSON_SCHEMA);
		format.setSchema(SCHEMA);
		assertThat(jsonSchemaOf(format)).containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME);
	}

	@Test
	void jsonSchemaBuilderWithoutNameUsesDefaultName() {
		TextChatResponseFormat.JsonSchema jsonSchema = TextChatResponseFormat.JsonSchema.builder()
			.schema(SCHEMA)
			.build();

		assertThat(jsonSchema.getName()).isEqualTo(TextChatResponseFormat.JsonSchema.DEFAULT_NAME);
	}

	@Test
	void noArgJsonSchemaWithSettersSendsDefaultName() {
		TextChatResponseFormat.JsonSchema jsonSchema = new TextChatResponseFormat.JsonSchema();
		jsonSchema.setSchema(JSON_HELPER.fromJsonToMap(SCHEMA));
		TextChatResponseFormat format = new TextChatResponseFormat(TextChatResponseFormat.Type.JSON_SCHEMA);
		format.setJsonSchema(jsonSchema);

		assertThat(jsonSchemaOf(format)).containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME);
	}

	@Test
	void jsonWithoutNameSendsDefaultName() {
		TextChatResponseFormat format = JSON_HELPER.fromJson(
				"{\"type\":\"json_schema\",\"json_schema\":{\"schema\":" + SCHEMA + "}}", TextChatResponseFormat.class);

		assertThat(jsonSchemaOf(format)).containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME);
	}

	@Test
	void setNameNullFallsBackToDefaultName() {
		TextChatResponseFormat format = TextChatResponseFormat.builder().schema(SCHEMA).name("person").build();
		format.getJsonSchema().setName(null);

		assertThat(jsonSchemaOf(format)).containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME);
	}

	@Test
	void setSchemaKeepsNameAndStrict() {
		TextChatResponseFormat format = TextChatResponseFormat.builder()
			.schema(SCHEMA)
			.name("person")
			.strict(true)
			.build();
		format.setSchema(SCHEMA);

		assertThat(jsonSchemaOf(format)).containsEntry("name", "person").containsEntry("strict", true);
	}

	// ChatClient builds each request from the chat model's options with mutate(), so a
	// default response format must keep its name request after request
	@Test
	void defaultResponseFormatThroughChatClientSendsNameEveryRequest() {
		WatsonxAiChatApi chatApi = chatApi();
		ChatClient chatClient = ChatClient
			.create(chatModel(chatApi, WatsonxAiChatOptions.builder().responseFormat(setterBuiltFormat()).build()));

		chatClient.prompt("Who are you?").call().content();
		chatClient.prompt("Who are you, again?").call().content();

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(chatApi, times(2)).chat(request.capture());
		assertThat(request.getAllValues()).allSatisfy(r -> assertThat(jsonSchemaOf(r.responseFormat()))
			.containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME));
	}

	@Test
	void perRequestResponseFormatSendsName() {
		WatsonxAiChatApi chatApi = chatApi();
		WatsonxAiChatModel chatModel = chatModel(chatApi, WatsonxAiChatOptions.builder().build());

		chatModel.call(new Prompt("Who are you?",
				WatsonxAiChatOptions.builder().model("test/model").responseFormat(setterBuiltFormat()).build()));

		ArgumentCaptor<WatsonxAiChatRequest> request = ArgumentCaptor.forClass(WatsonxAiChatRequest.class);
		verify(chatApi).chat(request.capture());
		assertThat(jsonSchemaOf(request.getValue().responseFormat())).containsEntry("name",
				TextChatResponseFormat.JsonSchema.DEFAULT_NAME);
	}

	/**
	 * Built like Spring Boot property binding builds it: no-argument constructors and
	 * setters.
	 */
	private static TextChatResponseFormat setterBuiltFormat() {
		TextChatResponseFormat.JsonSchema jsonSchema = new TextChatResponseFormat.JsonSchema();
		jsonSchema.setSchema(JSON_HELPER.fromJsonToMap(SCHEMA));
		TextChatResponseFormat format = new TextChatResponseFormat();
		format.setType(TextChatResponseFormat.Type.JSON_SCHEMA);
		format.setJsonSchema(jsonSchema);
		return format;
	}

	private static WatsonxAiChatApi chatApi() {
		WatsonxAiChatApi chatApi = mock(WatsonxAiChatApi.class);
		when(chatApi.chat(any())).thenReturn(ResponseEntity.ok(JSON_HELPER.fromJson(
				"""
						{"id":"chat-1","model_id":"test/model","choices":[{"index":0,"message":{"role":"assistant","content":"{\\"name\\":\\"pong\\",\\"age\\":1}"},"finish_reason":"stop"}]}""",
				WatsonxAiChatResponse.class)));
		return chatApi;
	}

	private static WatsonxAiChatModel chatModel(WatsonxAiChatApi chatApi, WatsonxAiChatOptions defaultOptions) {
		defaultOptions.setModel("test/model");
		return WatsonxAiChatModel.builder()
			.watsonxAiChatApi(chatApi)
			.options(defaultOptions)
			.observationRegistry(ObservationRegistry.NOOP)
			.toolCallingManager(ToolCallingManager.builder().build())
			.build();
	}

	@Test
	void jsonObjectHasNoJsonSchema() {
		assertThat(toMap(TextChatResponseFormat.jsonObject())).containsEntry("type", "json_object")
			.doesNotContainKey("json_schema");
	}

}
