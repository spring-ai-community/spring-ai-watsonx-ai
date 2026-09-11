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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.springframework.ai.audio.tts.TextToSpeechOptions;

/**
 * Text-to-speech options for watsonx.ai.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatsonxAiTextToSpeechOptions implements TextToSpeechOptions {

	@JsonProperty("model")
	private String model;

	@JsonProperty("voice")
	private String voice;

	@JsonProperty("speed")
	private Double speed;

	@JsonProperty("response_format")
	private String responseFormat;

	@JsonProperty("instructions")
	private String instructions;

	public WatsonxAiTextToSpeechOptions() {
	}

	public WatsonxAiTextToSpeechOptions(String model, String voice, Double speed, String responseFormat,
			String instructions) {
		this.model = model;
		this.voice = voice;
		this.speed = speed;
		this.responseFormat = responseFormat;
		this.instructions = instructions;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static WatsonxAiTextToSpeechOptions fromOptions(WatsonxAiTextToSpeechOptions from) {
		if (from == null) {
			return null;
		}
		return WatsonxAiTextToSpeechOptions.builder()
			.model(from.getModel())
			.voice(from.getVoice())
			.speed(from.getSpeed())
			.responseFormat(from.getResponseFormat())
			.instructions(from.getInstructions())
			.build();
	}

	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public String getVoice() {
		return this.voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

	@Override
	public Double getSpeed() {
		return this.speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	@Override
	public String getFormat() {
		return this.responseFormat;
	}

	public String getResponseFormat() {
		return this.responseFormat;
	}

	public void setResponseFormat(String responseFormat) {
		this.responseFormat = responseFormat;
	}

	public String getInstructions() {
		return this.instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WatsonxAiTextToSpeechOptions that = (WatsonxAiTextToSpeechOptions) o;
		return Objects.equals(this.model, that.model) && Objects.equals(this.voice, that.voice)
				&& Objects.equals(this.speed, that.speed) && Objects.equals(this.responseFormat, that.responseFormat)
				&& Objects.equals(this.instructions, that.instructions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.model, this.voice, this.speed, this.responseFormat, this.instructions);
	}

	@Override
	public String toString() {
		return "WatsonxAiTextToSpeechOptions{" + "model='" + this.model + '\'' + ", voice='" + this.voice + '\''
				+ ", speed=" + this.speed + ", responseFormat='" + this.responseFormat + '\'' + ", instructions='"
				+ this.instructions + '\'' + '}';
	}

	public static final class Builder {

		private String model;

		private String voice;

		private Double speed;

		private String responseFormat;

		private String instructions;

		private Builder() {
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder voice(String voice) {
			this.voice = voice;
			return this;
		}

		public Builder speed(Double speed) {
			this.speed = speed;
			return this;
		}

		public Builder format(String format) {
			this.responseFormat = format;
			return this;
		}

		public Builder responseFormat(String responseFormat) {
			this.responseFormat = responseFormat;
			return this;
		}

		public Builder instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		public WatsonxAiTextToSpeechOptions build() {
			return new WatsonxAiTextToSpeechOptions(this.model, this.voice, this.speed, this.responseFormat,
					this.instructions);
		}

	}

}
