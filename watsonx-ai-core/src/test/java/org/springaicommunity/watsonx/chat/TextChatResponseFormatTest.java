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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.util.JsonHelper;

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
	void jsonObjectHasNoJsonSchema() {
		assertThat(toMap(TextChatResponseFormat.jsonObject())).containsEntry("type", "json_object")
			.doesNotContainKey("json_schema");
	}

}
