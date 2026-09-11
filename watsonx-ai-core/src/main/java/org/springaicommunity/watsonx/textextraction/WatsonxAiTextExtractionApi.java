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

import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * API client implementation for watsonx.ai Text Extraction API.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public class WatsonxAiTextExtractionApi {

	private static final Logger logger = LoggerFactory.getLogger(WatsonxAiTextExtractionApi.class);

	private final RestClient restClient;

	private final WatsonxAiAuthentication watsonxAiAuthentication;

	private final String textExtractionEndpoint;

	private final String projectId;

	private final String spaceId;

	private final String version;

	public WatsonxAiTextExtractionApi(final String baseUrl, final String textExtractionEndpoint, final String version,
			final String projectId, final String spaceId, final String apiKey,
			final RestClient.Builder restClientBuilder, final ResponseErrorHandler responseErrorHandler) {

		this.textExtractionEndpoint = textExtractionEndpoint;
		this.version = version;
		this.projectId = projectId;
		this.spaceId = spaceId;
		this.watsonxAiAuthentication = new WatsonxAiAuthentication(apiKey);

		final Consumer<HttpHeaders> defaultHeaders = headers -> {
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		};

		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultStatusHandler(responseErrorHandler)
			.defaultHeaders(defaultHeaders)
			.build();
	}

	/**
	 * Synchronous call to watsonx.ai Text Extraction API.
	 * @param request the watsonx.ai text extraction request
	 * @return the response entity containing the extraction response
	 */
	public ResponseEntity<WatsonxAiTextExtractionResponse> extract(final WatsonxAiTextExtractionRequest request) {
		Assert.notNull(request, "Watsonx.ai text extraction request cannot be null");

		if (request.resource() != null) {
			return extractWithResource(request);
		}

		WatsonxAiTextExtractionRequest payload = request.toBuilder()
			.projectId(this.projectId)
			.spaceId(this.spaceId)
			.build();

		return this.restClient.post()
			.uri(uriBuilder -> uriBuilder.path(this.textExtractionEndpoint).queryParam("version", this.version).build())
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.contentType(MediaType.APPLICATION_JSON)
			.body(payload)
			.retrieve()
			.toEntity(WatsonxAiTextExtractionResponse.class);
	}

	private ResponseEntity<WatsonxAiTextExtractionResponse> extractWithResource(
			final WatsonxAiTextExtractionRequest request) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", request.resource());

		if (this.projectId != null) {
			body.add("project_id", this.projectId);
		}
		if (this.spaceId != null) {
			body.add("space_id", this.spaceId);
		}
		if (request.parameters() != null) {
			body.add("parameters", request.parameters());
		}

		return this.restClient.post()
			.uri(uriBuilder -> uriBuilder.path(this.textExtractionEndpoint).queryParam("version", this.version).build())
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(body)
			.retrieve()
			.toEntity(WatsonxAiTextExtractionResponse.class);
	}

	/**
	 * Creates an asynchronous text extraction job.
	 * @param request the text extraction request
	 * @return the extraction response containing job metadata
	 */
	public ResponseEntity<WatsonxAiTextExtractionResponse> createExtraction(
			final WatsonxAiTextExtractionRequest request) {
		return extract(request);
	}

	/**
	 * Retrieves the status and result of a text extraction job.
	 * @param id the extraction job ID
	 * @return the extraction response
	 */
	public ResponseEntity<WatsonxAiTextExtractionResponse> getExtraction(final String id) {
		Assert.hasText(id, "Extraction ID cannot be null or empty");

		return this.restClient.get().uri(uriBuilder -> {
			var builder = uriBuilder.path(this.textExtractionEndpoint + "/{id}").queryParam("version", this.version);
			if (this.projectId != null) {
				builder.queryParam("project_id", this.projectId);
			}
			if (this.spaceId != null) {
				builder.queryParam("space_id", this.spaceId);
			}
			return builder.build(id);
		})
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.retrieve()
			.toEntity(WatsonxAiTextExtractionResponse.class);
	}

	/**
	 * Deletes a text extraction job.
	 * @param id the extraction job ID
	 * @return response entity
	 */
	public ResponseEntity<Void> deleteExtraction(final String id) {
		Assert.hasText(id, "Extraction ID cannot be null or empty");

		return this.restClient.delete().uri(uriBuilder -> {
			var builder = uriBuilder.path(this.textExtractionEndpoint + "/{id}").queryParam("version", this.version);
			if (this.projectId != null) {
				builder.queryParam("project_id", this.projectId);
			}
			if (this.spaceId != null) {
				builder.queryParam("space_id", this.spaceId);
			}
			return builder.build(id);
		})
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.retrieve()
			.toBodilessEntity();
	}

}
