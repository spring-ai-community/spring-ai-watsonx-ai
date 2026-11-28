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

package org.springaicommunity.watsonx.evaluation;

import java.util.List;
import java.util.function.Consumer;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Client of the watsonx.ai detection API on context documents
 * ({@code POST /ml/v1/text/detection/context}: context relevance and faithfulness) and on
 * generated text ({@code POST /ml/v1/text/detection/generated}: answer relevance).
 *
 * <p>
 * These detectors need a watsonx.governance service instance: with a governance instance
 * ID, it's sent in the {@code x-governance-instance-id} header and the request has no
 * project or space, as watsonx.ai requires. Without one, the project or space is sent,
 * and watsonx.ai may answer that the detector is not accessible for that authorization.
 *
 * @author Federico Mariani
 * @since 2.2.0
 */
public class WatsonxAiDetectionApi {

	/** The header that carries the watsonx.governance service instance ID. */
	public static final String GOVERNANCE_INSTANCE_ID_HEADER = "x-governance-instance-id";

	public static final String DEFAULT_CONTEXT_ENDPOINT = "/ml/v1/text/detection/context";

	public static final String DEFAULT_GENERATED_ENDPOINT = "/ml/v1/text/detection/generated";

	public static final String DEFAULT_VERSION = "2025-10-01";

	private final RestClient restClient;

	private final WatsonxAiAuthentication watsonxAiAuthentication;

	private final String contextEndpoint;

	private final String generatedEndpoint;

	private final String version;

	private final String projectId;

	private final String spaceId;

	private final String governanceInstanceId;

	/**
	 * Creates the client.
	 * @param baseUrl the watsonx.ai URL, such as {@code https://eu-de.ml.cloud.ibm.com}
	 * @param contextEndpoint the path of the detection on context documents
	 * @param generatedEndpoint the path of the detection on generated text
	 * @param version the API version date, in {@code YYYY-MM-DD} format
	 * @param projectId the project ID, sent when there is no governance instance ID
	 * @param spaceId the space ID, sent when there is no governance instance ID
	 * @param governanceInstanceId the watsonx.governance service instance ID (its GUID),
	 * or {@code null}
	 * @param apiKey the IBM Cloud API key
	 * @param restClientBuilder the builder of the REST client, which is cloned and left
	 * unchanged
	 * @param responseErrorHandler the handler of error responses
	 */
	public WatsonxAiDetectionApi(final String baseUrl, final String contextEndpoint, final String generatedEndpoint,
			final String version, final String projectId, final String spaceId, final String governanceInstanceId,
			final String apiKey, final RestClient.Builder restClientBuilder,
			final ResponseErrorHandler responseErrorHandler) {
		Assert.hasText(baseUrl, "baseUrl must not be empty");
		Assert.hasText(contextEndpoint, "contextEndpoint must not be empty");
		Assert.hasText(generatedEndpoint, "generatedEndpoint must not be empty");
		Assert.hasText(version, "version must not be empty");
		Assert.notNull(restClientBuilder, "restClientBuilder must not be null");
		Assert.notNull(responseErrorHandler, "responseErrorHandler must not be null");

		this.contextEndpoint = contextEndpoint;
		this.generatedEndpoint = generatedEndpoint;
		this.version = version;
		this.projectId = projectId;
		this.spaceId = spaceId;
		this.governanceInstanceId = StringUtils.hasText(governanceInstanceId) ? governanceInstanceId : null;
		this.watsonxAiAuthentication = new WatsonxAiAuthentication(apiKey);

		final Consumer<HttpHeaders> defaultHeaders = headers -> {
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		};

		// A clone, so that other clients built from the caller's builder keep their own
		// base URL and defaults
		this.restClient = restClientBuilder.clone()
			.baseUrl(baseUrl)
			.defaultStatusHandler(responseErrorHandler)
			.defaultHeaders(defaultHeaders)
			.build();
	}

	/**
	 * Runs detectors on a text and its context documents, such as context relevance and
	 * faithfulness.
	 * @param request the request
	 * @return the detections
	 */
	public ResponseEntity<WatsonxAiDetectionResponse> detectContext(final WatsonxAiDetectionContextRequest request) {
		Assert.notNull(request, "request must not be null");

		return this.restClient.post()
			.uri(uriBuilder -> uriBuilder.path(this.contextEndpoint).queryParam("version", this.version).build())
			.headers(this::addAuthorization)
			.body(scoped(request))
			.retrieve()
			.toEntity(WatsonxAiDetectionResponse.class);
	}

	/**
	 * Runs detectors on a prompt and the text generated for it, such as answer relevance.
	 * @param request the request
	 * @return the detections
	 */
	public ResponseEntity<WatsonxAiDetectionResponse> detectGenerated(
			final WatsonxAiDetectionGeneratedRequest request) {
		Assert.notNull(request, "request must not be null");

		return this.restClient.post()
			.uri(uriBuilder -> uriBuilder.path(this.generatedEndpoint).queryParam("version", this.version).build())
			.headers(this::addAuthorization)
			.body(scoped(request))
			.retrieve()
			.toEntity(WatsonxAiDetectionResponse.class);
	}

	/**
	 * @return the watsonx.governance service instance ID, or {@code null}
	 */
	public String getGovernanceInstanceId() {
		return this.governanceInstanceId;
	}

	// Per request rather than as a default header, so that the governance instance
	// doesn't leak into other clients built from the same RestClient.Builder
	private void addAuthorization(HttpHeaders headers) {
		headers.setBearerAuth(this.watsonxAiAuthentication.getAccessToken());
		if (this.governanceInstanceId != null) {
			headers.set(GOVERNANCE_INSTANCE_ID_HEADER, this.governanceInstanceId);
		}
	}

	// With a governance instance, watsonx.ai rejects a project or a space in the body
	private WatsonxAiDetectionContextRequest scoped(WatsonxAiDetectionContextRequest request) {
		return (this.governanceInstanceId != null) ? request.withScope(null, null)
				: request.withScope(this.projectId, this.spaceId);
	}

	private WatsonxAiDetectionGeneratedRequest scoped(WatsonxAiDetectionGeneratedRequest request) {
		return (this.governanceInstanceId != null) ? request.withScope(null, null)
				: request.withScope(this.projectId, this.spaceId);
	}

}
