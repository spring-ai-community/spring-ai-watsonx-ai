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

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
import org.springaicommunity.watsonx.chat.WatsonxAiChatRequest;
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse;
import org.springaicommunity.watsonx.chat.message.TextChatMessage;
import org.springframework.ai.util.JsonHelper;
import org.springframework.beans.factory.annotation.Value;
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
	 * Requests an IAM token from a local server that stands in for IBM IAM, so the native
	 * image checks that the IBM Cloud SDK can read token responses (Gson, which needs
	 * reflection hints) without credentials or network access.
	 */
	@Bean
	ApplicationRunner verifyIamTokenParsing() {
		return args -> {
			HttpServer iamServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			iamServer.createContext("/", exchange -> {
				long now = System.currentTimeMillis() / 1000;
				byte[] body = """
						{"access_token":"smoke-access-token","refresh_token":"smoke-refresh-token",\
						"token_type":"Bearer","expires_in":3600,"expiration":%d}""".formatted(now + 3600)
					.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, body.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(body);
				}
			});
			iamServer.start();
			try {
				String token = new IamAuthenticator.Builder().apikey("smoke-api-key")
					.url("http://localhost:" + iamServer.getAddress().getPort())
					.build()
					.getToken();
				if (!"smoke-access-token".equals(token)) {
					throw new IllegalStateException("IAM token response was not parsed, got access token: " + token);
				}
			}
			finally {
				iamServer.stop(0);
			}
			System.out.println("Verified IAM token parsing");
		};
	}

	/**
	 * Calls the real watsonx.ai chat API, synchronously and streaming, so the native
	 * image also exercises IAM authentication and the HTTP clients. Opt in at runtime
	 * with {@code watsonx.smoke.live=true}; a property condition on the bean would be
	 * fixed at native build time.
	 */
	@Bean
	ApplicationRunner verifyWatsonxLiveChat(WatsonxAiChatModel chatModel,
			@Value("${watsonx.smoke.live:false}") boolean live) {
		return args -> {
			if (!live) {
				System.out.println("Skipped live watsonx.ai check (watsonx.smoke.live=false)");
				return;
			}
			String prompt = "Reply with the single word: pong";

			String reply = chatModel.call(prompt);
			if (reply == null || reply.isBlank()) {
				throw new IllegalStateException("Empty watsonx.ai chat reply");
			}

			String streamed = chatModel.stream(prompt).collect(Collectors.joining()).block();
			if (streamed == null || streamed.isBlank()) {
				throw new IllegalStateException("Empty watsonx.ai streamed reply");
			}

			System.out.println("Verified live watsonx.ai chat: call=" + reply.strip() + ", stream=" + streamed.strip());
		};
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
