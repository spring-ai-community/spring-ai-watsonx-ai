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

package org.springaicommunity.watsonx.textextraction;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.util.JsonHelper;

/**
 * Options for watsonx.ai Text Extraction API. Configuration options that can be passed to
 * control document extraction behavior.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatsonxAiTextExtractionOptions {

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	@JsonProperty("requested_outputs")
	private List<String> requestedOutputs;

	@JsonProperty("mode")
	private String mode;

	@JsonProperty("ocr_mode")
	private String ocrMode;

	@JsonProperty("languages")
	private List<String> languages;

	@JsonProperty("model_id")
	private String model;

	@JsonProperty("output_formats")
	private List<String> outputFormats;

	@JsonProperty("enable_ocr")
	private Boolean enableOcr;

	public WatsonxAiTextExtractionOptions() {
	}

	private WatsonxAiTextExtractionOptions(Builder builder) {
		this.requestedOutputs = builder.requestedOutputs;
		this.mode = builder.mode;
		this.ocrMode = builder.ocrMode;
		this.languages = builder.languages;
		this.model = builder.model;
		this.outputFormats = builder.outputFormats;
		this.enableOcr = builder.enableOcr;
	}

	/**
	 * The outputs to extract, for example {@code md}, {@code plain_text}, {@code json} or
	 * {@code html}.
	 * @return the requested outputs
	 */
	public List<String> getRequestedOutputs() {
		return requestedOutputs;
	}

	public void setRequestedOutputs(List<String> requestedOutputs) {
		this.requestedOutputs = requestedOutputs;
	}

	/**
	 * The extraction mode, {@code standard} or {@code high_quality}.
	 * @return the mode
	 */
	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * The OCR mode, {@code disabled}, {@code enabled} or {@code forced}.
	 * @return the OCR mode
	 */
	public String getOcrMode() {
		return ocrMode;
	}

	public void setOcrMode(String ocrMode) {
		this.ocrMode = ocrMode;
	}

	public List<String> getLanguages() {
		return languages;
	}

	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	/**
	 * @return the model
	 * @deprecated the watsonx.ai Text Extraction API has no model parameter, so this
	 * value is not sent
	 */
	@Deprecated(since = "2.0.1")
	public String getModel() {
		return model;
	}

	/**
	 * @param model the model
	 * @deprecated the watsonx.ai Text Extraction API has no model parameter, so this
	 * value is not sent
	 */
	@Deprecated(since = "2.0.1")
	public void setModel(String model) {
		this.model = model;
	}

	/**
	 * @return the output formats
	 * @deprecated use {@link #getRequestedOutputs()}
	 */
	@Deprecated(since = "2.0.1")
	public List<String> getOutputFormats() {
		return outputFormats;
	}

	/**
	 * @param outputFormats the output formats
	 * @deprecated use {@link #setRequestedOutputs(List)}
	 */
	@Deprecated(since = "2.0.1")
	public void setOutputFormats(List<String> outputFormats) {
		this.outputFormats = outputFormats;
	}

	/**
	 * @return whether OCR is enabled
	 * @deprecated use {@link #getOcrMode()}
	 */
	@Deprecated(since = "2.0.1")
	public Boolean getEnableOcr() {
		return enableOcr;
	}

	/**
	 * @param enableOcr whether OCR is enabled
	 * @deprecated use {@link #setOcrMode(String)}
	 */
	@Deprecated(since = "2.0.1")
	public void setEnableOcr(Boolean enableOcr) {
		this.enableOcr = enableOcr;
	}

	/**
	 * The requested outputs to send: {@link #getRequestedOutputs()}, or the deprecated
	 * {@link #getOutputFormats()} when it is not set.
	 * @return the effective requested outputs, or {@code null}
	 */
	@SuppressWarnings("deprecation")
	public List<String> effectiveRequestedOutputs() {
		return (this.requestedOutputs != null) ? this.requestedOutputs : this.outputFormats;
	}

	/**
	 * The OCR mode to send: {@link #getOcrMode()}, or the deprecated
	 * {@link #getEnableOcr()} mapped to {@code enabled} or {@code disabled} when it is
	 * not set.
	 * @return the effective OCR mode, or {@code null}
	 */
	@SuppressWarnings("deprecation")
	public String effectiveOcrMode() {
		if (this.ocrMode != null) {
			return this.ocrMode;
		}
		if (this.enableOcr != null) {
			return this.enableOcr ? "enabled" : "disabled";
		}
		return null;
	}

	public static Builder builder() {
		return new Builder();
	}

	@SuppressWarnings("deprecation")
	public Builder toBuilder() {
		return new Builder()
			.requestedOutputs(this.requestedOutputs != null ? new ArrayList<>(this.requestedOutputs) : null)
			.mode(this.mode)
			.ocrMode(this.ocrMode)
			.languages(this.languages != null ? new ArrayList<>(this.languages) : null)
			.model(this.model)
			.outputFormats(this.outputFormats != null ? new ArrayList<>(this.outputFormats) : null)
			.enableOcr(this.enableOcr);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WatsonxAiTextExtractionOptions that = (WatsonxAiTextExtractionOptions) o;
		return Objects.equals(requestedOutputs, that.requestedOutputs) && Objects.equals(mode, that.mode)
				&& Objects.equals(ocrMode, that.ocrMode) && Objects.equals(languages, that.languages)
				&& Objects.equals(model, that.model) && Objects.equals(outputFormats, that.outputFormats)
				&& Objects.equals(enableOcr, that.enableOcr);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestedOutputs, mode, ocrMode, languages, model, outputFormats, enableOcr);
	}

	@Override
	public String toString() {
		return JSON_HELPER.toJson(this);
	}

	public static final class Builder {

		private List<String> requestedOutputs;

		private String mode;

		private String ocrMode;

		private List<String> languages;

		private String model;

		private List<String> outputFormats;

		private Boolean enableOcr;

		private Builder() {
		}

		public Builder requestedOutputs(List<String> requestedOutputs) {
			this.requestedOutputs = requestedOutputs;
			return this;
		}

		public Builder mode(String mode) {
			this.mode = mode;
			return this;
		}

		public Builder ocrMode(String ocrMode) {
			this.ocrMode = ocrMode;
			return this;
		}

		public Builder languages(List<String> languages) {
			this.languages = languages;
			return this;
		}

		/**
		 * @param model the model
		 * @return this builder
		 * @deprecated the watsonx.ai Text Extraction API has no model parameter, so this
		 * value is not sent
		 */
		@Deprecated(since = "2.0.1")
		public Builder model(String model) {
			this.model = model;
			return this;
		}

		/**
		 * @param outputFormats the output formats
		 * @return this builder
		 * @deprecated use {@link #requestedOutputs(List)}
		 */
		@Deprecated(since = "2.0.1")
		public Builder outputFormats(List<String> outputFormats) {
			this.outputFormats = outputFormats;
			return this;
		}

		/**
		 * @param enableOcr whether OCR is enabled
		 * @return this builder
		 * @deprecated use {@link #ocrMode(String)}
		 */
		@Deprecated(since = "2.0.1")
		public Builder enableOcr(Boolean enableOcr) {
			this.enableOcr = enableOcr;
			return this;
		}

		public WatsonxAiTextExtractionOptions build() {
			return new WatsonxAiTextExtractionOptions(this);
		}

	}

}
