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
import org.springframework.util.Assert;

/**
 * Request for the watsonx.ai gateway text-to-speech API.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WatsonxAiTextToSpeechRequest {

	public static final int MAX_INPUT_LENGTH = 4096;

	public static final double MIN_SPEED = 0.25;

	public static final double MAX_SPEED = 4.0;

	@JsonProperty("input")
	private String input;

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

	@JsonProperty("project_id")
	private String projectId;

	@JsonProperty("space_id")
	private String spaceId;

	public WatsonxAiTextToSpeechRequest() {
	}

	private WatsonxAiTextToSpeechRequest(Builder builder) {
		Assert.hasText(builder.input, "input must not be empty");
		Assert.isTrue(builder.input.length() <= MAX_INPUT_LENGTH,
				"input must not exceed " + MAX_INPUT_LENGTH + " characters");
		if (builder.speed != null) {
			Assert.isTrue(builder.speed >= MIN_SPEED && builder.speed <= MAX_SPEED,
					"speed must be between " + MIN_SPEED + " and " + MAX_SPEED);
		}

		this.input = builder.input;
		this.model = builder.model;
		this.voice = builder.voice;
		this.speed = builder.speed;
		this.responseFormat = builder.responseFormat;
		this.instructions = builder.instructions;
		this.projectId = builder.projectId;
		this.spaceId = builder.spaceId;
	}

	public String input() {
		return this.input;
	}

	public String model() {
		return this.model;
	}

	public String voice() {
		return this.voice;
	}

	public Double speed() {
		return this.speed;
	}

	public String responseFormat() {
		return this.responseFormat;
	}

	public String instructions() {
		return this.instructions;
	}

	public String projectId() {
		return this.projectId;
	}

	public String spaceId() {
		return this.spaceId;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().input(this.input)
			.model(this.model)
			.voice(this.voice)
			.speed(this.speed)
			.responseFormat(this.responseFormat)
			.instructions(this.instructions)
			.projectId(this.projectId)
			.spaceId(this.spaceId);
	}

	public static final class Builder {

		private String input;

		private String model;

		private String voice;

		private Double speed;

		private String responseFormat;

		private String instructions;

		private String projectId;

		private String spaceId;

		private Builder() {
		}

		public Builder input(String input) {
			this.input = input;
			return this;
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

		public Builder responseFormat(String responseFormat) {
			this.responseFormat = responseFormat;
			return this;
		}

		public Builder instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		public Builder projectId(String projectId) {
			this.projectId = projectId;
			return this;
		}

		public Builder spaceId(String spaceId) {
			this.spaceId = spaceId;
			return this;
		}

		public WatsonxAiTextToSpeechRequest build() {
			return new WatsonxAiTextToSpeechRequest(this);
		}

	}

}
