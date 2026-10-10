/*
 * Copyright 2025-2026 the original author or authors.
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

package org.springaicommunity.watsonx.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.util.StringUtils;

/**
 * Connection properties to use watsonx.ai Services.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
@ConfigurationProperties(WatsonxAiConnectionProperties.CONFIG_PREFIX)
public final class WatsonxAiConnectionProperties {

	public static final String CONFIG_PREFIX = "spring.ai.watsonx.ai";

	static final String PROJECT_ID_PROPERTY = CONFIG_PREFIX + ".project-id";

	static final String SPACE_ID_PROPERTY = CONFIG_PREFIX + ".space-id";

	private String baseUrl = "https://us-south.ml.cloud.ibm.com";

	private String apiKey;

	private String projectId;

	private String spaceId;

	public String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return this.baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Returns the project ID, or {@code null} when it is not set or blank.
	 * @return the project ID
	 */
	public String getProjectId() {
		return StringUtils.hasText(this.projectId) ? this.projectId : null;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * Returns the space ID, or {@code null} when it is not set or blank.
	 * @return the space ID
	 */
	public String getSpaceId() {
		return StringUtils.hasText(this.spaceId) ? this.spaceId : null;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	/**
	 * Checks that exactly one of {@code project-id} and {@code space-id} is set, as
	 * watsonx.ai requires on every request. Blank values count as not set.
	 * @throws MutuallyExclusiveConfigurationPropertiesException if both are set
	 * @throws InvalidConfigurationPropertyValueException if neither is set
	 * @since 2.1.0
	 */
	public void validateProjectOrSpaceId() {
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn(entries -> {
			entries.put(PROJECT_ID_PROPERTY, getProjectId());
			entries.put(SPACE_ID_PROPERTY, getSpaceId());
		});
		if (getProjectId() == null && getSpaceId() == null) {
			throw new InvalidConfigurationPropertyValueException(PROJECT_ID_PROPERTY, null,
					"watsonx.ai needs a project or a space. Set " + PROJECT_ID_PROPERTY + " or " + SPACE_ID_PROPERTY
							+ ".");
		}
	}

}
