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

package org.springaicommunity.watsonx.autoconfigure.texttospeech;

import org.springaicommunity.watsonx.texttospeech.WatsonxAiTextToSpeechApi;
import org.springaicommunity.watsonx.texttospeech.WatsonxAiTextToSpeechOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for watsonx.ai Text to Speech Model.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
@ConfigurationProperties(WatsonxAiTextToSpeechProperties.CONFIG_PREFIX)
public class WatsonxAiTextToSpeechProperties {

	public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.text-to-speech";

	/** Enable or disable the watsonx.ai text-to-speech auto-configuration. */
	private boolean enabled = true;

	/** The endpoint for the text-to-speech gateway API. */
	private String speechEndpoint = WatsonxAiTextToSpeechApi.DEFAULT_SPEECH_ENDPOINT;

	/**
	 * API version date to use, in YYYY-MM-DD format. Example: 2024-03-14. See the
	 * <a href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API
	 * versioning</a>
	 */
	private String version = WatsonxAiTextToSpeechApi.DEFAULT_VERSION;

	/**
	 * The default options to use when calling the watsonx.ai Text to Speech API. These
	 * can be overridden by passing options in the request.
	 */
	@NestedConfigurationProperty
	private WatsonxAiTextToSpeechOptions options = WatsonxAiTextToSpeechOptions.builder().build();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getSpeechEndpoint() {
		return this.speechEndpoint;
	}

	public void setSpeechEndpoint(String speechEndpoint) {
		this.speechEndpoint = speechEndpoint;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public WatsonxAiTextToSpeechOptions getOptions() {
		return this.options;
	}

	public void setOptions(WatsonxAiTextToSpeechOptions options) {
		this.options = options;
	}

}
