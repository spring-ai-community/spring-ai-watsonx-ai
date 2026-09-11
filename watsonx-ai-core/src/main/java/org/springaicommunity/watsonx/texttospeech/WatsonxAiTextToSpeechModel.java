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

package org.springaicommunity.watsonx.texttospeech;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

/**
 * {@link TextToSpeechModel} and
 * {@link org.springframework.ai.audio.tts.StreamingTextToSpeechModel} implementation
 * providing access to watsonx.ai text-to-speech services via the model gateway.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public class WatsonxAiTextToSpeechModel implements TextToSpeechModel {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiTextToSpeechModel.class);

	private final WatsonxAiTextToSpeechApi watsonxAiTextToSpeechApi;

	private final WatsonxAiTextToSpeechOptions options;

	private final RetryTemplate retryTemplate;

	public WatsonxAiTextToSpeechModel(WatsonxAiTextToSpeechApi watsonxAiTextToSpeechApi) {
		this(watsonxAiTextToSpeechApi, WatsonxAiTextToSpeechOptions.builder().build(),
				RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public WatsonxAiTextToSpeechModel(WatsonxAiTextToSpeechApi watsonxAiTextToSpeechApi,
			WatsonxAiTextToSpeechOptions options) {
		this(watsonxAiTextToSpeechApi, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public WatsonxAiTextToSpeechModel(WatsonxAiTextToSpeechApi watsonxAiTextToSpeechApi,
			WatsonxAiTextToSpeechOptions options, RetryTemplate retryTemplate) {
		Assert.notNull(watsonxAiTextToSpeechApi, "WatsonxAiTextToSpeechApi must not be null");
		Assert.notNull(options, "WatsonxAiTextToSpeechOptions must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");

		this.watsonxAiTextToSpeechApi = watsonxAiTextToSpeechApi;
		this.options = options;
		this.retryTemplate = retryTemplate;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
		Assert.notNull(prompt, "TextToSpeechPrompt must not be null");
		Assert.notNull(prompt.getInstructions(), "Instructions must not be null");
		Assert.hasText(prompt.getInstructions().getText(), "Prompt must contain text to convert to speech");

		WatsonxAiTextToSpeechRequest request = createRequest(prompt);

		byte[] audioData = RetryUtils.execute(this.retryTemplate, () -> {
			ResponseEntity<byte[]> response = this.watsonxAiTextToSpeechApi.textToSpeech(request);
			if (response == null || response.getBody() == null) {
				if (logger.isWarnEnabled()) {
					logger.warn("No speech response returned for request: {}", request);
				}
				return new byte[0];
			}
			return response.getBody();
		});

		return new TextToSpeechResponse(List.of(new Speech(audioData)));
	}

	@Override
	public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
		Assert.notNull(prompt, "TextToSpeechPrompt must not be null");
		Assert.notNull(prompt.getInstructions(), "Instructions must not be null");
		Assert.hasText(prompt.getInstructions().getText(), "Prompt must contain text to convert to speech");

		WatsonxAiTextToSpeechRequest request = createRequest(prompt);

		return RetryUtils.execute(this.retryTemplate, () -> this.watsonxAiTextToSpeechApi.stream(request)
			.map(bytes -> new TextToSpeechResponse(List.of(new Speech(bytes)))));
	}

	private WatsonxAiTextToSpeechRequest createRequest(TextToSpeechPrompt prompt) {
		WatsonxAiTextToSpeechOptions mergedOptions = mergeOptions(prompt);

		return WatsonxAiTextToSpeechRequest.builder()
			.input(prompt.getInstructions().getText())
			.model(mergedOptions.getModel())
			.voice(mergedOptions.getVoice())
			.speed(mergedOptions.getSpeed())
			.responseFormat(mergedOptions.getResponseFormat())
			.instructions(mergedOptions.getInstructions())
			.build();
	}

	private WatsonxAiTextToSpeechOptions mergeOptions(TextToSpeechPrompt prompt) {
		WatsonxAiTextToSpeechOptions runtimeOptions = null;
		TextToSpeechOptions promptOptions = prompt.getOptions();

		if (promptOptions instanceof WatsonxAiTextToSpeechOptions watsonxOptions) {
			runtimeOptions = watsonxOptions;
		}
		else if (promptOptions != null) {
			runtimeOptions = WatsonxAiTextToSpeechOptions.builder()
				.model(promptOptions.getModel())
				.voice(promptOptions.getVoice())
				.speed(promptOptions.getSpeed())
				.format(promptOptions.getFormat())
				.build();
		}

		return (runtimeOptions != null) ? merge(runtimeOptions, this.options) : this.options;
	}

	private WatsonxAiTextToSpeechOptions merge(WatsonxAiTextToSpeechOptions runtime,
			WatsonxAiTextToSpeechOptions defaults) {
		return WatsonxAiTextToSpeechOptions.builder()
			.model(getOrDefault(runtime.getModel(), defaults.getModel()))
			.voice(getOrDefault(runtime.getVoice(), defaults.getVoice()))
			.speed(getOrDefault(runtime.getSpeed(), defaults.getSpeed()))
			.responseFormat(getOrDefault(runtime.getResponseFormat(), defaults.getResponseFormat()))
			.instructions(getOrDefault(runtime.getInstructions(), defaults.getInstructions()))
			.build();
	}

	private <T> T getOrDefault(T runtimeValue, T defaultValue) {
		return runtimeValue != null ? runtimeValue : defaultValue;
	}

	@Override
	public WatsonxAiTextToSpeechOptions getOptions() {
		return this.options;
	}

	/**
	 * @deprecated use {@link #getOptions()} instead.
	 */
	@Deprecated(forRemoval = true)
	@Override
	@SuppressWarnings("removal")
	public WatsonxAiTextToSpeechOptions getDefaultOptions() {
		return this.options;
	}

	public static final class Builder {

		private WatsonxAiTextToSpeechApi watsonxAiTextToSpeechApi;

		private WatsonxAiTextToSpeechOptions options = WatsonxAiTextToSpeechOptions.builder().build();

		private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

		private Builder() {
		}

		public Builder watsonxAiTextToSpeechApi(WatsonxAiTextToSpeechApi watsonxAiTextToSpeechApi) {
			this.watsonxAiTextToSpeechApi = watsonxAiTextToSpeechApi;
			return this;
		}

		public Builder options(WatsonxAiTextToSpeechOptions options) {
			this.options = options;
			return this;
		}

		public Builder retryTemplate(RetryTemplate retryTemplate) {
			this.retryTemplate = retryTemplate;
			return this;
		}

		public WatsonxAiTextToSpeechModel build() {
			Assert.notNull(this.watsonxAiTextToSpeechApi, "WatsonxAiTextToSpeechApi must not be null");
			Assert.notNull(this.options, "WatsonxAiTextToSpeechOptions must not be null");
			Assert.notNull(this.retryTemplate, "RetryTemplate must not be null");
			return new WatsonxAiTextToSpeechModel(this.watsonxAiTextToSpeechApi, this.options, this.retryTemplate);
		}

	}

}
