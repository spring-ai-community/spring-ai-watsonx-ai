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

package org.springaicommunity.watsonx.autoconfigure.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.chat.TextChatResponseFormat;
import org.springframework.ai.util.JsonHelper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Tests that a JSON schema response format bound from
 * {@code spring.ai.watsonx.ai.chat.options.response-format.*} is sent with a name, which
 * watsonx.ai requires.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatPropertiesTest {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final String PREFIX = "spring.ai.watsonx.ai.chat.options.response-format.";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(PropertiesConfiguration.class);

	@Test
	void jsonSchemaPropertiesSendDefaultName() {
		this.contextRunner
			.withPropertyValues(PREFIX + "type=json_schema", PREFIX + "json-schema.schema.type=object",
					PREFIX + "json-schema.schema.properties.name.type=string")
			.run(context -> assertThat(jsonSchemaOf(context.getBean(WatsonxAiChatProperties.class)))
				.containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME)
				.containsKey("schema"));
	}

	@Test
	void jsonSchemaPropertiesWithNameAndStrictSendThem() {
		this.contextRunner
			.withPropertyValues(PREFIX + "type=json_schema", PREFIX + "json-schema.name=person",
					PREFIX + "json-schema.strict=true", PREFIX + "json-schema.schema.type=object")
			.run(context -> assertThat(jsonSchemaOf(context.getBean(WatsonxAiChatProperties.class)))
				.containsEntry("name", "person")
				.containsEntry("strict", true));
	}

	@Test
	void schemaStringPropertySendsDefaultName() {
		this.contextRunner
			.withPropertyValues(PREFIX + "type=json_schema",
					PREFIX + "schema={\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}")
			.run(context -> assertThat(jsonSchemaOf(context.getBean(WatsonxAiChatProperties.class)))
				.containsEntry("name", TextChatResponseFormat.JsonSchema.DEFAULT_NAME)
				.containsKey("schema"));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> jsonSchemaOf(WatsonxAiChatProperties properties) {
		String json = JSON_HELPER.toJson(properties.getOptions().getResponseFormat());
		return (Map<String, Object>) JSON_HELPER.fromJsonToMap(json).get("json_schema");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WatsonxAiChatProperties.class)
	static class PropertiesConfiguration {

	}

}
