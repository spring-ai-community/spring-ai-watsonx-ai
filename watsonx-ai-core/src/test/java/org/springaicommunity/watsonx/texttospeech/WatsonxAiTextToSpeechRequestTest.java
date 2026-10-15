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

import org.junit.jupiter.api.Test;
import org.springframework.ai.util.JsonHelper;

/**
 * Unit tests for {@link WatsonxAiTextToSpeechRequest}.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
class WatsonxAiTextToSpeechRequestTest {

	private final JsonHelper jsonHelper = new JsonHelper();

	@Test
	void createValidRequest() {
		WatsonxAiTextToSpeechRequest request = WatsonxAiTextToSpeechRequest.builder()
			.input("Hello, world!")
			.model("ibm/granite-speech")
			.voice("en-US_AllisonV3Voice")
			.speed(1.25)
			.responseFormat("mp3")
			.instructions("Speak in a friendly tone")
			.projectId("test-project-id")
			.spaceId("test-space-id")
			.build();

		assertThat(request.input()).isEqualTo("Hello, world!");
		assertThat(request.model()).isEqualTo("ibm/granite-speech");
		assertThat(request.voice()).isEqualTo("en-US_AllisonV3Voice");
		assertThat(request.speed()).isEqualTo(1.25);
		assertThat(request.responseFormat()).isEqualTo("mp3");
		assertThat(request.instructions()).isEqualTo("Speak in a friendly tone");
		assertThat(request.projectId()).isEqualTo("test-project-id");
		assertThat(request.spaceId()).isEqualTo("test-space-id");
	}

	@Test
	void emptyInputThrowsException() {
		assertThatThrownBy(() -> WatsonxAiTextToSpeechRequest.builder().input("").build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("input must not be empty");

		assertThatThrownBy(() -> WatsonxAiTextToSpeechRequest.builder().input(null).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("input must not be empty");
	}

	@Test
	void inputExceedingMaxLengthThrowsException() {
		String longInput = "a".repeat(4097);
		assertThatThrownBy(() -> WatsonxAiTextToSpeechRequest.builder().input(longInput).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("input must not exceed 4096 characters");
	}

	@Test
	void inputAtMaxLengthSucceeds() {
		String maxInput = "a".repeat(4096);
		WatsonxAiTextToSpeechRequest request = WatsonxAiTextToSpeechRequest.builder().input(maxInput).build();
		assertThat(request.input()).hasSize(4096);
	}

	@Test
	void speedOutOfRangeThrowsException() {
		assertThatThrownBy(() -> WatsonxAiTextToSpeechRequest.builder().input("test").speed(0.24).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("speed must be between 0.25 and 4.0");

		assertThatThrownBy(() -> WatsonxAiTextToSpeechRequest.builder().input("test").speed(4.01).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("speed must be between 0.25 and 4.0");
	}

	@Test
	void speedBoundaryValuesSucceed() {
		WatsonxAiTextToSpeechRequest reqMin = WatsonxAiTextToSpeechRequest.builder().input("test").speed(0.25).build();
		assertThat(reqMin.speed()).isEqualTo(0.25);

		WatsonxAiTextToSpeechRequest reqMax = WatsonxAiTextToSpeechRequest.builder().input("test").speed(4.0).build();
		assertThat(reqMax.speed()).isEqualTo(4.0);
	}

	@Test
	void toBuilderCopiesAllFields() {
		WatsonxAiTextToSpeechRequest original = WatsonxAiTextToSpeechRequest.builder()
			.input("Speech text")
			.model("tts-model")
			.voice("alloy")
			.speed(1.0)
			.responseFormat("wav")
			.instructions("slow")
			.projectId("proj-1")
			.spaceId("space-1")
			.build();

		WatsonxAiTextToSpeechRequest copy = original.toBuilder().build();

		assertThat(copy.input()).isEqualTo(original.input());
		assertThat(copy.model()).isEqualTo(original.model());
		assertThat(copy.voice()).isEqualTo(original.voice());
		assertThat(copy.speed()).isEqualTo(original.speed());
		assertThat(copy.responseFormat()).isEqualTo(original.responseFormat());
		assertThat(copy.instructions()).isEqualTo(original.instructions());
		assertThat(copy.projectId()).isEqualTo(original.projectId());
		assertThat(copy.spaceId()).isEqualTo(original.spaceId());
	}

	@Test
	void jsonSerialization() {
		WatsonxAiTextToSpeechRequest request = WatsonxAiTextToSpeechRequest.builder()
			.input("Test speech")
			.model("model-1")
			.voice("voice-1")
			.speed(1.5)
			.responseFormat("opus")
			.instructions("instruct-1")
			.projectId("proj-1")
			.build();

		String json = this.jsonHelper.toJson(request);

		assertThat(json).contains("\"input\":\"Test speech\"");
		assertThat(json).contains("\"model\":\"model-1\"");
		assertThat(json).contains("\"voice\":\"voice-1\"");
		assertThat(json).contains("\"speed\":1.5");
		assertThat(json).contains("\"response_format\":\"opus\"");
		assertThat(json).contains("\"instructions\":\"instruct-1\"");
		assertThat(json).contains("\"project_id\":\"proj-1\"");
		assertThat(json).doesNotContain("space_id");
	}

}
