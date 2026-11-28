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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.audio.tts.DefaultTextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechMessage;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

/**
 * Unit tests for {@link WatsonxAiTextToSpeechModel}.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
class WatsonxAiTextToSpeechModelTest {

	private WatsonxAiTextToSpeechApi textToSpeechApi;

	private WatsonxAiTextToSpeechOptions defaultOptions;

	private WatsonxAiTextToSpeechModel speechModel;

	@BeforeEach
	void setUp() {
		this.textToSpeechApi = mock(WatsonxAiTextToSpeechApi.class);
		this.defaultOptions = WatsonxAiTextToSpeechOptions.builder()
			.model("ibm/granite-speech")
			.voice("en-US_AllisonV3Voice")
			.speed(1.0)
			.responseFormat("mp3")
			.build();

		this.speechModel = new WatsonxAiTextToSpeechModel(this.textToSpeechApi, this.defaultOptions,
				RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	@Nested
	class ConstructorTests {

		@Test
		void validConstructor() {
			assertThat(speechModel).isNotNull();
			assertThat(speechModel.getOptions()).isEqualTo(defaultOptions);
			assertThat(speechModel.getDefaultOptions()).isEqualTo(defaultOptions);
		}

		@Test
		void constructorWithNullApiThrowsException() {
			assertThatThrownBy(
					() -> new WatsonxAiTextToSpeechModel(null, defaultOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("WatsonxAiTextToSpeechApi must not be null");
		}

		@Test
		void constructorWithNullOptionsThrowsException() {
			assertThatThrownBy(
					() -> new WatsonxAiTextToSpeechModel(textToSpeechApi, null, RetryUtils.DEFAULT_RETRY_TEMPLATE))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("WatsonxAiTextToSpeechOptions must not be null");
		}

		@Test
		void constructorWithNullRetryTemplateThrowsException() {
			assertThatThrownBy(() -> new WatsonxAiTextToSpeechModel(textToSpeechApi, defaultOptions, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("RetryTemplate must not be null");
		}

		@Test
		void builderCreatesModel() {
			WatsonxAiTextToSpeechModel model = WatsonxAiTextToSpeechModel.builder()
				.watsonxAiTextToSpeechApi(textToSpeechApi)
				.options(defaultOptions)
				.retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
				.build();

			assertThat(model).isNotNull();
			assertThat(model.getOptions()).isEqualTo(defaultOptions);
		}

	}

	@Nested
	class CallTests {

		@Test
		void callWithValidPrompt() {
			byte[] expectedAudio = new byte[] { 1, 2, 3, 4, 5 };
			when(textToSpeechApi.textToSpeech(any())).thenReturn(ResponseEntity.ok(expectedAudio));

			TextToSpeechPrompt prompt = new TextToSpeechPrompt("Hello, watsonx!");
			TextToSpeechResponse response = speechModel.call(prompt);

			assertThat(response).isNotNull();
			assertThat(response.getResults()).hasSize(1);
			assertThat(response.getResult().getOutput()).isEqualTo(expectedAudio);

			ArgumentCaptor<WatsonxAiTextToSpeechRequest> captor = ArgumentCaptor
				.forClass(WatsonxAiTextToSpeechRequest.class);
			verify(textToSpeechApi).textToSpeech(captor.capture());

			WatsonxAiTextToSpeechRequest capturedRequest = captor.getValue();
			assertThat(capturedRequest.input()).isEqualTo("Hello, watsonx!");
			assertThat(capturedRequest.model()).isEqualTo("ibm/granite-speech");
			assertThat(capturedRequest.voice()).isEqualTo("en-US_AllisonV3Voice");
			assertThat(capturedRequest.speed()).isEqualTo(1.0);
			assertThat(capturedRequest.responseFormat()).isEqualTo("mp3");
		}

		@Test
		void callWithStringConvenienceMethod() {
			byte[] expectedAudio = new byte[] { 10, 20, 30 };
			when(textToSpeechApi.textToSpeech(any())).thenReturn(ResponseEntity.ok(expectedAudio));

			byte[] audio = speechModel.call("Short test");
			assertThat(audio).isEqualTo(expectedAudio);
		}

		@Test
		void callWithNullPromptThrowsException() {
			assertThatThrownBy(() -> speechModel.call((TextToSpeechPrompt) null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("TextToSpeechPrompt must not be null");
		}

		@Test
		void callWithNullInstructionsThrowsException() {
			TextToSpeechPrompt prompt = new TextToSpeechPrompt((TextToSpeechMessage) null);
			assertThatThrownBy(() -> speechModel.call(prompt)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Instructions must not be null");
		}

		@Test
		void callWithEmptyTextThrowsException() {
			TextToSpeechPrompt prompt = new TextToSpeechPrompt("");
			assertThatThrownBy(() -> speechModel.call(prompt)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Prompt must contain text to convert to speech");
		}

		@Test
		void callWithEmptyResponseBodyReturnsEmptyByteArray() {
			when(textToSpeechApi.textToSpeech(any())).thenReturn(ResponseEntity.ok(null));

			TextToSpeechPrompt prompt = new TextToSpeechPrompt("Silence expected");
			TextToSpeechResponse response = speechModel.call(prompt);

			assertThat(response.getResult().getOutput()).isEmpty();
		}

		@Test
		void callWithRuntimeWatsonxOptionsOverridesDefaults() {
			byte[] expectedAudio = new byte[] { 42 };
			when(textToSpeechApi.textToSpeech(any())).thenReturn(ResponseEntity.ok(expectedAudio));

			WatsonxAiTextToSpeechOptions runtimeOptions = WatsonxAiTextToSpeechOptions.builder()
				.voice("en-US_MichaelV3Voice")
				.speed(1.5)
				.responseFormat("wav")
				.instructions("Speak slowly and clearly")
				.build();

			TextToSpeechPrompt prompt = new TextToSpeechPrompt("Customized speech", runtimeOptions);
			TextToSpeechResponse response = speechModel.call(prompt);

			assertThat(response.getResult().getOutput()).isEqualTo(expectedAudio);

			ArgumentCaptor<WatsonxAiTextToSpeechRequest> captor = ArgumentCaptor
				.forClass(WatsonxAiTextToSpeechRequest.class);
			verify(textToSpeechApi).textToSpeech(captor.capture());

			WatsonxAiTextToSpeechRequest captured = captor.getValue();
			assertThat(captured.input()).isEqualTo("Customized speech");
			assertThat(captured.model()).isEqualTo("ibm/granite-speech"); // inherited
																			// from
																			// default
			assertThat(captured.voice()).isEqualTo("en-US_MichaelV3Voice"); // overridden
			assertThat(captured.speed()).isEqualTo(1.5); // overridden
			assertThat(captured.responseFormat()).isEqualTo("wav"); // overridden
			assertThat(captured.instructions()).isEqualTo("Speak slowly and clearly");
		}

		@Test
		void callWithGenericTextToSpeechOptions() {
			byte[] expectedAudio = new byte[] { 7 };
			when(textToSpeechApi.textToSpeech(any())).thenReturn(ResponseEntity.ok(expectedAudio));

			DefaultTextToSpeechOptions genericOptions = DefaultTextToSpeechOptions.builder()
				.model("generic-model")
				.voice("generic-voice")
				.speed(2.0)
				.format("flac")
				.build();

			TextToSpeechPrompt prompt = new TextToSpeechPrompt("Generic options speech", genericOptions);
			TextToSpeechResponse response = speechModel.call(prompt);

			assertThat(response.getResult().getOutput()).isEqualTo(expectedAudio);

			ArgumentCaptor<WatsonxAiTextToSpeechRequest> captor = ArgumentCaptor
				.forClass(WatsonxAiTextToSpeechRequest.class);
			verify(textToSpeechApi).textToSpeech(captor.capture());

			WatsonxAiTextToSpeechRequest captured = captor.getValue();
			assertThat(captured.model()).isEqualTo("generic-model");
			assertThat(captured.voice()).isEqualTo("generic-voice");
			assertThat(captured.speed()).isEqualTo(2.0);
			assertThat(captured.responseFormat()).isEqualTo("flac");
		}

	}

	@Nested
	class StreamTests {

		@Test
		void streamWithValidPrompt() {
			byte[] chunk1 = new byte[] { 1, 2 };
			byte[] chunk2 = new byte[] { 3, 4, 5 };
			when(textToSpeechApi.stream(any())).thenReturn(Flux.just(chunk1, chunk2));

			TextToSpeechPrompt prompt = new TextToSpeechPrompt("Streaming test");
			List<TextToSpeechResponse> responses = speechModel.stream(prompt).collectList().block();
			assertThat(responses).hasSize(2);
			assertThat(responses.get(0).getResult().getOutput()).isEqualTo(chunk1);
			assertThat(responses.get(1).getResult().getOutput()).isEqualTo(chunk2);
		}

		@Test
		void streamConvenienceMethods() {
			byte[] chunk = new byte[] { 99 };
			when(textToSpeechApi.stream(any())).thenReturn(Flux.just(chunk));

			List<byte[]> chunks1 = speechModel.stream("Stream text").collectList().block();
			assertThat(chunks1).containsExactly(chunk);

			List<byte[]> chunks2 = speechModel.stream("Stream text with options", defaultOptions).collectList().block();
			assertThat(chunks2).containsExactly(chunk);
		}

		@Test
		void streamWithNullPromptThrowsException() {
			assertThatThrownBy(() -> speechModel.stream((TextToSpeechPrompt) null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("TextToSpeechPrompt must not be null");
		}

	}

}
