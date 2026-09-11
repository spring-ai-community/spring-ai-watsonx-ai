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

	@JsonProperty("model_id")
	private String model;

	@JsonProperty("output_formats")
	private List<String> outputFormats;

	@JsonProperty("languages")
	private List<String> languages;

	@JsonProperty("enable_ocr")
	private Boolean enableOcr;

	public WatsonxAiTextExtractionOptions() {
	}

	private WatsonxAiTextExtractionOptions(Builder builder) {
		this.model = builder.model;
		this.outputFormats = builder.outputFormats;
		this.languages = builder.languages;
		this.enableOcr = builder.enableOcr;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public List<String> getOutputFormats() {
		return outputFormats;
	}

	public void setOutputFormats(List<String> outputFormats) {
		this.outputFormats = outputFormats;
	}

	public List<String> getLanguages() {
		return languages;
	}

	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	public Boolean getEnableOcr() {
		return enableOcr;
	}

	public void setEnableOcr(Boolean enableOcr) {
		this.enableOcr = enableOcr;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().model(this.model)
			.outputFormats(this.outputFormats != null ? new ArrayList<>(this.outputFormats) : null)
			.languages(this.languages != null ? new ArrayList<>(this.languages) : null)
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
		return Objects.equals(model, that.model) && Objects.equals(outputFormats, that.outputFormats)
				&& Objects.equals(languages, that.languages) && Objects.equals(enableOcr, that.enableOcr);
	}

	@Override
	public int hashCode() {
		return Objects.hash(model, outputFormats, languages, enableOcr);
	}

	@Override
	public String toString() {
		return JSON_HELPER.toJson(this);
	}

	public static final class Builder {

		private String model;

		private List<String> outputFormats;

		private List<String> languages;

		private Boolean enableOcr;

		private Builder() {
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder outputFormats(List<String> outputFormats) {
			this.outputFormats = outputFormats;
			return this;
		}

		public Builder languages(List<String> languages) {
			this.languages = languages;
			return this;
		}

		public Builder enableOcr(Boolean enableOcr) {
			this.enableOcr = enableOcr;
			return this;
		}

		public WatsonxAiTextExtractionOptions build() {
			return new WatsonxAiTextExtractionOptions(this);
		}

	}

}
