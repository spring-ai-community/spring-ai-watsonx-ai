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

package org.springaicommunity.watsonx.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * The AI guardrails that watsonx.ai applies to a chat request, sent as its
 * {@code moderations} field: hate, abuse and profanity (HAP), personally identifiable
 * information (PII) and Granite Guardian.
 *
 * <p>
 * A flagged input is not sent to the model: the response has no answer, and its
 * generation finishes in the
 * {@link org.springframework.ai.chat.metadata.FinishReasonCategory#FILTERED FILTERED}
 * category. A flagged output is returned as the model wrote it and only reported. The
 * detections are in the {@code moderations} key of the response metadata. See
 * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-hap.html">
 * Filtering foundation model content with AI guardrails</a>.
 *
 * <pre>{@code
 * WatsonxAiChatModerations.builder().hapInput(0.5).hapOutput(0.5).piiInput(true).build();
 * }</pre>
 *
 * <p>
 * Read from JSON, the fields that these classes don't model, such as {@code mask} or
 * {@code input_ranges}, are kept and sent as they were given.
 *
 * @author Federico Mariani
 * @since 2.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatsonxAiChatModerations extends ChatModerationOtherFields {

	/** Hate, abuse and profanity, on the input and on the output. */
	@JsonProperty("hap")
	private Hap hap;

	/** Personally identifiable information, on the input and on the output. */
	@JsonProperty("pii")
	private Pii pii;

	/** Granite Guardian (beta), on the input only. */
	@JsonProperty("granite_guardian")
	private GraniteGuardian graniteGuardian;

	public WatsonxAiChatModerations() {
	}

	public static Builder builder() {
		return new Builder();
	}

	public Hap getHap() {
		return this.hap;
	}

	public void setHap(Hap hap) {
		this.hap = hap;
	}

	public Pii getPii() {
		return this.pii;
	}

	public void setPii(Pii pii) {
		this.pii = pii;
	}

	public GraniteGuardian getGraniteGuardian() {
		return this.graniteGuardian;
	}

	public void setGraniteGuardian(GraniteGuardian graniteGuardian) {
		this.graniteGuardian = graniteGuardian;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WatsonxAiChatModerations that)) {
			return false;
		}
		return Objects.equals(this.hap, that.hap) && Objects.equals(this.pii, that.pii)
				&& Objects.equals(this.graniteGuardian, that.graniteGuardian) && sameOtherFields(that);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.hap, this.pii, this.graniteGuardian, otherFields());
	}

	/**
	 * A deep copy, so that options derived from other options don't share their
	 * moderations.
	 */
	WatsonxAiChatModerations copy() {
		WatsonxAiChatModerations copy = withOtherFieldsOf(new WatsonxAiChatModerations());
		if (this.hap != null) {
			copy.hap = this.hap.withOtherFieldsOf(new Hap());
			copy.hap.input = TextModeration.copy(this.hap.input);
			copy.hap.output = TextModeration.copy(this.hap.output);
		}
		if (this.pii != null) {
			copy.pii = this.pii.withOtherFieldsOf(new Pii());
			copy.pii.input = TextModerationWithoutThreshold.copy(this.pii.input);
			copy.pii.output = TextModerationWithoutThreshold.copy(this.pii.output);
		}
		if (this.graniteGuardian != null) {
			copy.graniteGuardian = this.graniteGuardian.withOtherFieldsOf(new GraniteGuardian());
			copy.graniteGuardian.input = TextModeration.copy(this.graniteGuardian.input);
		}
		return copy;
	}

	@Override
	public String toString() {
		return "WatsonxAiChatModerations{hap=" + this.hap + ", pii=" + this.pii + ", graniteGuardian="
				+ this.graniteGuardian + ", otherFields=" + otherFields() + "}";
	}

	/**
	 * A moderation of the input or of the output, with a threshold. The moderation is
	 * enabled when it is present and {@code enabled} is not {@code false}.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TextModeration extends ChatModerationOtherFields {

		/**
		 * Whether the moderation is applied. watsonx.ai treats a missing value as true.
		 */
		@JsonProperty("enabled")
		private Boolean enabled;

		/**
		 * The score, from 0 to 1, from which the text is flagged. A lower value flags
		 * more text. watsonx.ai uses its own default when it's not set.
		 */
		@JsonProperty("threshold")
		private Double threshold;

		public TextModeration() {
		}

		public TextModeration(Boolean enabled, Double threshold) {
			this.enabled = enabled;
			this.threshold = threshold;
		}

		static TextModeration copy(TextModeration moderation) {
			return (moderation != null)
					? moderation.withOtherFieldsOf(new TextModeration(moderation.enabled, moderation.threshold)) : null;
		}

		public Boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public Double getThreshold() {
			return this.threshold;
		}

		public void setThreshold(Double threshold) {
			this.threshold = threshold;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof TextModeration that)) {
				return false;
			}
			return Objects.equals(this.enabled, that.enabled) && Objects.equals(this.threshold, that.threshold)
					&& sameOtherFields(that);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.enabled, this.threshold, otherFields());
		}

		@Override
		public String toString() {
			return "TextModeration{enabled=" + this.enabled + ", threshold=" + this.threshold + "}";
		}

	}

	/**
	 * A moderation of the input or of the output that has no threshold, such as PII:
	 * watsonx.ai sets its sensitivity.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TextModerationWithoutThreshold extends ChatModerationOtherFields {

		/**
		 * Whether the moderation is applied. watsonx.ai treats a missing value as true.
		 */
		@JsonProperty("enabled")
		private Boolean enabled;

		public TextModerationWithoutThreshold() {
		}

		public TextModerationWithoutThreshold(Boolean enabled) {
			this.enabled = enabled;
		}

		static TextModerationWithoutThreshold copy(TextModerationWithoutThreshold moderation) {
			return (moderation != null)
					? moderation.withOtherFieldsOf(new TextModerationWithoutThreshold(moderation.enabled)) : null;
		}

		public Boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			return (o instanceof TextModerationWithoutThreshold that) && Objects.equals(this.enabled, that.enabled)
					&& sameOtherFields(that);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.enabled, otherFields());
		}

		@Override
		public String toString() {
			return "TextModerationWithoutThreshold{enabled=" + this.enabled + "}";
		}

	}

	/** The hate, abuse and profanity (HAP) moderation. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Hap extends ChatModerationOtherFields {

		@JsonProperty("input")
		private TextModeration input;

		@JsonProperty("output")
		private TextModeration output;

		public TextModeration getInput() {
			return this.input;
		}

		public void setInput(TextModeration input) {
			this.input = input;
		}

		public TextModeration getOutput() {
			return this.output;
		}

		public void setOutput(TextModeration output) {
			this.output = output;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			return (o instanceof Hap that) && Objects.equals(this.input, that.input)
					&& Objects.equals(this.output, that.output) && sameOtherFields(that);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.input, this.output, otherFields());
		}

		@Override
		public String toString() {
			return "Hap{input=" + this.input + ", output=" + this.output + "}";
		}

	}

	/** The personally identifiable information (PII) moderation. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Pii extends ChatModerationOtherFields {

		@JsonProperty("input")
		private TextModerationWithoutThreshold input;

		@JsonProperty("output")
		private TextModerationWithoutThreshold output;

		public TextModerationWithoutThreshold getInput() {
			return this.input;
		}

		public void setInput(TextModerationWithoutThreshold input) {
			this.input = input;
		}

		public TextModerationWithoutThreshold getOutput() {
			return this.output;
		}

		public void setOutput(TextModerationWithoutThreshold output) {
			this.output = output;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			return (o instanceof Pii that) && Objects.equals(this.input, that.input)
					&& Objects.equals(this.output, that.output) && sameOtherFields(that);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.input, this.output, otherFields());
		}

		@Override
		public String toString() {
			return "Pii{input=" + this.input + ", output=" + this.output + "}";
		}

	}

	/**
	 * The Granite Guardian moderation (beta), on the input only. Granite Guardian reads
	 * the whole conversation, so once a flagged message is in the history every later
	 * request is flagged too.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class GraniteGuardian extends ChatModerationOtherFields {

		@JsonProperty("input")
		private TextModeration input;

		public TextModeration getInput() {
			return this.input;
		}

		public void setInput(TextModeration input) {
			this.input = input;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			return (o instanceof GraniteGuardian that) && Objects.equals(this.input, that.input)
					&& sameOtherFields(that);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.input, otherFields());
		}

		@Override
		public String toString() {
			return "GraniteGuardian{input=" + this.input + "}";
		}

	}

	public static final class Builder {

		private final WatsonxAiChatModerations moderations = new WatsonxAiChatModerations();

		private Builder() {
		}

		/**
		 * Flags input with hate, abuse or profanity, so it's not sent to the model.
		 * @param threshold the score, from 0 to 1, from which the input is flagged, or
		 * {@code null} for the watsonx.ai default
		 * @return this builder
		 */
		public Builder hapInput(Double threshold) {
			hap().setInput(new TextModeration(true, threshold));
			return this;
		}

		/**
		 * Reports hate, abuse or profanity in the output.
		 * @param threshold the score, from 0 to 1, from which the output is flagged, or
		 * {@code null} for the watsonx.ai default
		 * @return this builder
		 */
		public Builder hapOutput(Double threshold) {
			hap().setOutput(new TextModeration(true, threshold));
			return this;
		}

		/**
		 * Flags input with personally identifiable information, such as a phone number,
		 * so it's not sent to the model.
		 * @param enabled whether the moderation is applied
		 * @return this builder
		 */
		public Builder piiInput(boolean enabled) {
			pii().setInput(new TextModerationWithoutThreshold(enabled));
			return this;
		}

		/**
		 * Reports personally identifiable information in the output.
		 * @param enabled whether the moderation is applied
		 * @return this builder
		 */
		public Builder piiOutput(boolean enabled) {
			pii().setOutput(new TextModerationWithoutThreshold(enabled));
			return this;
		}

		/**
		 * Flags harmful input with Granite Guardian (beta), such as a jailbreak or
		 * violence, so it's not sent to the model.
		 * @param threshold the score, from 0 to 1, from which the input is flagged, or
		 * {@code null} for the watsonx.ai default
		 * @return this builder
		 */
		public Builder graniteGuardianInput(Double threshold) {
			if (this.moderations.graniteGuardian == null) {
				this.moderations.graniteGuardian = new GraniteGuardian();
			}
			this.moderations.graniteGuardian.setInput(new TextModeration(true, threshold));
			return this;
		}

		public WatsonxAiChatModerations build() {
			return this.moderations;
		}

		private Hap hap() {
			if (this.moderations.hap == null) {
				this.moderations.hap = new Hap();
			}
			return this.moderations.hap;
		}

		private Pii pii() {
			if (this.moderations.pii == null) {
				this.moderations.pii = new Pii();
			}
			return this.moderations.pii;
		}

	}

}
