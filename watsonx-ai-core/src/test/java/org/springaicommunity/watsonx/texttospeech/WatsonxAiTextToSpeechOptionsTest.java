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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WatsonxAiTextToSpeechOptions}.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
class WatsonxAiTextToSpeechOptionsTest {

	@Test
	void testBuilderAndGetters() {
		WatsonxAiTextToSpeechOptions options = WatsonxAiTextToSpeechOptions.builder()
			.model("tts-model")
			.voice("alloy")
			.speed(1.5)
			.responseFormat("mp3")
			.instructions("speak gently")
			.build();

		assertThat(options.getModel()).isEqualTo("tts-model");
		assertThat(options.getVoice()).isEqualTo("alloy");
		assertThat(options.getSpeed()).isEqualTo(1.5);
		assertThat(options.getResponseFormat()).isEqualTo("mp3");
		assertThat(options.getFormat()).isEqualTo("mp3");
		assertThat(options.getInstructions()).isEqualTo("speak gently");
	}

	@Test
	void testFormatAlias() {
		WatsonxAiTextToSpeechOptions options = WatsonxAiTextToSpeechOptions.builder().format("wav").build();

		assertThat(options.getFormat()).isEqualTo("wav");
		assertThat(options.getResponseFormat()).isEqualTo("wav");
	}

	@Test
	void testSetters() {
		WatsonxAiTextToSpeechOptions options = new WatsonxAiTextToSpeechOptions();
		options.setModel("model-2");
		options.setVoice("echo");
		options.setSpeed(0.8);
		options.setResponseFormat("flac");
		options.setInstructions("instruction");

		assertThat(options.getModel()).isEqualTo("model-2");
		assertThat(options.getVoice()).isEqualTo("echo");
		assertThat(options.getSpeed()).isEqualTo(0.8);
		assertThat(options.getResponseFormat()).isEqualTo("flac");
		assertThat(options.getFormat()).isEqualTo("flac");
		assertThat(options.getInstructions()).isEqualTo("instruction");
	}

	@Test
	void testFromOptions() {
		WatsonxAiTextToSpeechOptions original = WatsonxAiTextToSpeechOptions.builder()
			.model("model-1")
			.voice("voice-1")
			.speed(1.0)
			.responseFormat("aac")
			.instructions("instruction-1")
			.build();

		WatsonxAiTextToSpeechOptions copy = WatsonxAiTextToSpeechOptions.fromOptions(original);

		assertThat(copy).isEqualTo(original);
		assertThat(copy.hashCode()).isEqualTo(original.hashCode());
		assertThat(WatsonxAiTextToSpeechOptions.fromOptions(null)).isNull();
	}

	@Test
	void testEqualsAndHashCode() {
		WatsonxAiTextToSpeechOptions opt1 = WatsonxAiTextToSpeechOptions.builder()
			.model("model-1")
			.voice("voice-1")
			.speed(1.0)
			.responseFormat("mp3")
			.instructions("instruct")
			.build();

		WatsonxAiTextToSpeechOptions opt2 = WatsonxAiTextToSpeechOptions.builder()
			.model("model-1")
			.voice("voice-1")
			.speed(1.0)
			.responseFormat("mp3")
			.instructions("instruct")
			.build();

		WatsonxAiTextToSpeechOptions opt3 = WatsonxAiTextToSpeechOptions.builder()
			.model("model-2")
			.voice("voice-2")
			.speed(1.5)
			.responseFormat("wav")
			.instructions("instruct2")
			.build();

		assertThat(opt1).isEqualTo(opt2);
		assertThat(opt1.hashCode()).isEqualTo(opt2.hashCode());
		assertThat(opt1).isNotEqualTo(opt3);
		assertThat(opt1).isNotEqualTo(null);
		assertThat(opt1).isNotEqualTo(new Object());
	}

	@Test
	void testToString() {
		WatsonxAiTextToSpeechOptions options = WatsonxAiTextToSpeechOptions.builder()
			.model("model-1")
			.voice("voice-1")
			.build();

		assertThat(options.toString()).contains("model='model-1'").contains("voice='voice-1'");
	}

}
