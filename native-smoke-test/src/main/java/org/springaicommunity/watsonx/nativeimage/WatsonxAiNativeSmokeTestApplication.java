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

package org.springaicommunity.watsonx.nativeimage;

import java.util.List;
import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springaicommunity.watsonx.chat.WatsonxAiChatRequest;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springframework.ai.util.JsonHelper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Native-image smoke-test application for the Spring AI watsonx.ai starter.
 *
 * @author Ana Katrina Inguengan
 */
@SpringBootApplication
public class WatsonxAiNativeSmokeTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(WatsonxAiNativeSmokeTestApplication.class, args).close();
	}

	@Bean
	ApplicationRunner verifyWatsonxChatModel(WatsonxAiChatModel chatModel) {
		return args -> System.out.println("Verified Watsonx chat model: " + chatModel.getClass().getName());
	}

	/**
	 * Round-trips the chat DTOs through the same {@link JsonHelper} the chat model uses.
	 * Without reflection hints, a native image cannot see the annotated fields and
	 * serialization or deserialization fails.
	 */
	@Bean
	ApplicationRunner verifyWatsonxJsonMapping() {
		return args -> {
			JsonHelper jsonHelper = new JsonHelper();

			WatsonxAiChatRequest request = WatsonxAiChatRequest.builder()
				.model("ibm/granite-3-3-8b-instruct")
				.messages(List.of(new TextChatMessage("Hello", null)))
				.temperature(0.7)
				.build();
			String requestJson = jsonHelper.toJson(request);
			if (!requestJson.contains("\"model_id\":\"ibm/granite-3-3-8b-instruct\"")
					|| !requestJson.contains("\"messages\"")) {
				throw new IllegalStateException("Unexpected chat request JSON: " + requestJson);
			}

			WatsonxAiChatResponse response = jsonHelper.fromJson("""
					{
					  "id": "chat-1",
					  "model_id": "ibm/granite-3-3-8b-instruct",
					  "choices": [
					    {
					      "index": 0,
					      "message": { "role": "assistant", "content": "Hi" },
					      "finish_reason": "stop"
					    }
					  ],
					  "usage": { "prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2 }
					}
					""", WatsonxAiChatResponse.class);
			if (response.choices() == null || response.choices().isEmpty()
					|| !"Hi".equals(response.choices().get(0).message().content())) {
				throw new IllegalStateException("Unexpected chat response mapping: " + response);
			}

			System.out.println("Verified Watsonx JSON mapping");
		};
	}

}
