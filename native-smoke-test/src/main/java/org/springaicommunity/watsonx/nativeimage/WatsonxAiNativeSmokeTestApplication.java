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

import org.springaicommunity.watsonx.chat.WatsonxAiChatModel;
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

}
