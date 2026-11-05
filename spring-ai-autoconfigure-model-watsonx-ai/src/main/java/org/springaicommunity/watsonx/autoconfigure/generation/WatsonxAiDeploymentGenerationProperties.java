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

package org.springaicommunity.watsonx.autoconfigure.generation;

import org.springaicommunity.watsonx.generation.WatsonxAiDeploymentGenerationOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for watsonx.ai text generation deployments, such as a deployed
 * freeform or structured prompt template.
 *
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
@ConfigurationProperties(WatsonxAiDeploymentGenerationProperties.CONFIG_PREFIX)
public class WatsonxAiDeploymentGenerationProperties {

	public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai.deployment-generation";

	/** Enable or disable the watsonx.ai deployment generation auto-configuration. */
	private boolean enabled = true;

	/** The endpoint for text generation, with a {deployment_id} placeholder. */
	private String generationEndpoint = "/ml/v1/deployments/{deployment_id}/text/generation";

	/** The endpoint for streaming text generation, with a {deployment_id} placeholder. */
	private String streamEndpoint = "/ml/v1/deployments/{deployment_id}/text/generation_stream";

	/**
	 * API version date to use, in YYYY-MM-DD format. Example: 2024-10-17. See the
	 * <a href="https://cloud.ibm.com/apidocs/watsonx-ai#api-versioning">watsonx.ai API
	 * versioning</a>
	 */
	private String version = "2024-10-17";

	/**
	 * The default options, such as the deployment ID. These can be overridden by passing
	 * options in the request.
	 */
	@NestedConfigurationProperty
	private WatsonxAiDeploymentGenerationOptions options = new WatsonxAiDeploymentGenerationOptions();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getGenerationEndpoint() {
		return this.generationEndpoint;
	}

	public void setGenerationEndpoint(String generationEndpoint) {
		this.generationEndpoint = generationEndpoint;
	}

	public String getStreamEndpoint() {
		return this.streamEndpoint;
	}

	public void setStreamEndpoint(String streamEndpoint) {
		this.streamEndpoint = streamEndpoint;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public WatsonxAiDeploymentGenerationOptions getOptions() {
		return this.options;
	}

	public void setOptions(WatsonxAiDeploymentGenerationOptions options) {
		this.options = options;
	}

}
