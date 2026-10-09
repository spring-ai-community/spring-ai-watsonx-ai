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

package org.springaicommunity.watsonx.autoconfigure.textextraction;

import java.time.Duration;
import org.springaicommunity.watsonx.textextraction.WatsonxAiTextExtractionOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for watsonx.ai Text Extraction Model.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@ConfigurationProperties(WatsonxAiTextExtractionProperties.CONFIG_PREFIX)
public class WatsonxAiTextExtractionProperties {

	public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.text-extraction";

	/** Enable or disable the watsonx.ai text extraction auto-configuration. */
	private boolean enabled = true;

	/** The endpoint for the text extraction API. */
	private String textExtractionEndpoint = "/ml/v1/text/extractions";

	/**
	 * API version date to use, in YYYY-MM-DD format. Example: 2024-10-17. See the
	 * <a href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API
	 * versioning</a>
	 */
	private String version = "2024-10-17";

	/**
	 * The data platform API URL used to look up the storage of the project or space, for
	 * example https://api.dataplatform.cloud.ibm.com. Derived from the base URL when not
	 * set.
	 */
	private String dataplatformUrl;

	/** How long to wait for an extraction job to finish. */
	private Duration pollTimeout = Duration.ofMinutes(5);

	/**
	 * Whether to delete the uploaded document and the extraction result from storage
	 * after extracting text from a resource.
	 */
	private boolean deleteFiles = true;

	/**
	 * The default options to use when calling the watsonx.ai Text Extraction API. These
	 * can be overridden by passing options in the request.
	 */
	@NestedConfigurationProperty
	private WatsonxAiTextExtractionOptions options = WatsonxAiTextExtractionOptions.builder().build();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getTextExtractionEndpoint() {
		return textExtractionEndpoint;
	}

	public void setTextExtractionEndpoint(String textExtractionEndpoint) {
		this.textExtractionEndpoint = textExtractionEndpoint;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDataplatformUrl() {
		return dataplatformUrl;
	}

	public void setDataplatformUrl(String dataplatformUrl) {
		this.dataplatformUrl = dataplatformUrl;
	}

	public Duration getPollTimeout() {
		return pollTimeout;
	}

	public void setPollTimeout(Duration pollTimeout) {
		this.pollTimeout = pollTimeout;
	}

	public boolean isDeleteFiles() {
		return deleteFiles;
	}

	public void setDeleteFiles(boolean deleteFiles) {
		this.deleteFiles = deleteFiles;
	}

	public WatsonxAiTextExtractionOptions getOptions() {
		return options;
	}

	public void setOptions(WatsonxAiTextExtractionOptions options) {
		this.options = options;
	}

}
