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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springaicommunity.watsonx.auth.WatsonxAiAuthentication;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * API client implementation for watsonx.ai Text Extraction API.
 * <p>
 * watsonx.ai extracts text asynchronously from a document stored in Cloud Object Storage.
 * Besides starting, fetching and deleting extraction jobs, this client can read and write
 * files in the storage of the configured project or deployment space, which is what
 * {@link WatsonxAiTextExtractionModel#extract(Resource)} uses.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
public class WatsonxAiTextExtractionApi {

	// Data platform API host per watsonx.ai region
	private static final Map<String, String> DATAPLATFORM_URLS = Map.of("us-south",
			"https://api.dataplatform.cloud.ibm.com", "eu-de", "https://api.eu-de.dataplatform.cloud.ibm.com", "eu-gb",
			"https://api.eu-gb.dataplatform.cloud.ibm.com", "jp-tok", "https://api.jp-tok.dataplatform.cloud.ibm.com",
			"au-syd", "https://api.au-syd.dai.cloud.ibm.com", "ca-tor", "https://api.ca-tor.dai.cloud.ibm.com");

	private final RestClient restClient;

	private final WatsonxAiAuthentication watsonxAiAuthentication;

	private final String textExtractionEndpoint;

	private final String projectId;

	private final String spaceId;

	private final String version;

	private final String dataplatformUrl;

	private volatile Storage storage;

	public WatsonxAiTextExtractionApi(final String baseUrl, final String textExtractionEndpoint, final String version,
			final String projectId, final String spaceId, final String apiKey,
			final RestClient.Builder restClientBuilder, final ResponseErrorHandler responseErrorHandler) {
		this(baseUrl, textExtractionEndpoint, version, projectId, spaceId, apiKey, null, restClientBuilder,
				responseErrorHandler);
	}

	/**
	 * Create a new API client.
	 * @param baseUrl the watsonx.ai base URL, for example
	 * {@code https://us-south.ml.cloud.ibm.com}
	 * @param textExtractionEndpoint the text extraction endpoint
	 * @param version the API version
	 * @param projectId the project ID, or {@code null} when a space is used
	 * @param spaceId the deployment space ID, or {@code null} when a project is used
	 * @param apiKey the IBM Cloud API key
	 * @param dataplatformUrl the data platform API URL used to look up the project or
	 * space storage, or {@code null} to derive it from the region in {@code baseUrl}
	 * @param restClientBuilder the RestClient builder
	 * @param responseErrorHandler the response error handler
	 * @since 2.0.1
	 */
	public WatsonxAiTextExtractionApi(final String baseUrl, final String textExtractionEndpoint, final String version,
			final String projectId, final String spaceId, final String apiKey, final String dataplatformUrl,
			final RestClient.Builder restClientBuilder, final ResponseErrorHandler responseErrorHandler) {

		this.textExtractionEndpoint = textExtractionEndpoint;
		this.version = version;
		// A blank ID counts as unset: watsonx.ai rejects requests that name both a
		// project and
		// a space
		this.projectId = StringUtils.hasText(projectId) ? projectId : null;
		this.spaceId = StringUtils.hasText(spaceId) ? spaceId : null;
		this.dataplatformUrl = StringUtils.hasText(dataplatformUrl) ? stripTrailingSlash(dataplatformUrl)
				: dataplatformUrlFor(baseUrl);
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
	 * Start a text extraction job for a document that is already in storage, as
	 * referenced by the request's {@code document_reference}. The job runs
	 * asynchronously: the response contains the job ID and status. Use
	 * {@link #getExtraction(String)} to follow it.
	 * @param request the watsonx.ai text extraction request
	 * @return the response entity containing the submitted job
	 */
	public ResponseEntity<WatsonxAiTextExtractionResponse> extract(final WatsonxAiTextExtractionRequest request) {
		Assert.notNull(request, "Watsonx.ai text extraction request cannot be null");
		Assert.isNull(request.resource(),
				"The watsonx.ai Text Extraction API only reads documents from storage. Use WatsonxAiTextExtractionModel"
						+ " to extract text from a resource, which uploads it first.");

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

	public ResponseEntity<WatsonxAiTextExtractionResponse> createExtraction(
			final WatsonxAiTextExtractionRequest request) {
		return extract(request);
	}

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
	 * Delete an extraction job. A running job is cancelled.
	 * @param id the extraction job ID
	 * @return the response entity
	 */
	public ResponseEntity<Void> deleteExtraction(final String id) {
		Assert.hasText(id, "Extraction ID cannot be null or empty");

		return this.restClient.delete().uri(uriBuilder -> {
			var builder = uriBuilder.path(this.textExtractionEndpoint + "/{id}")
				.queryParam("version", this.version)
				.queryParam("hard_delete", true);
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

	/**
	 * Upload a file to the storage of the configured project or space.
	 * @param key the object key, which a {@code container} document reference uses as its
	 * path
	 * @param resource the file content
	 * @since 2.0.1
	 */
	public void uploadFile(final String key, final Resource resource) {
		Assert.hasText(key, "File key cannot be null or empty");
		Assert.notNull(resource, "Resource cannot be null");

		byte[] content;
		try {
			content = resource.getContentAsByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + resource.getDescription(), ex);
		}

		this.restClient.put()
			.uri(storageUri(key))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(content)
			.retrieve()
			.toBodilessEntity();
	}

	/**
	 * Download a text file from the storage of the configured project or space.
	 * @param key the object key
	 * @return the file content, decoded as UTF-8
	 * @since 2.0.1
	 */
	public String downloadFile(final String key) {
		Assert.hasText(key, "File key cannot be null or empty");

		byte[] content = this.restClient.get()
			.uri(storageUri(key))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.accept(MediaType.ALL)
			.retrieve()
			.body(byte[].class);
		return (content != null) ? new String(content, StandardCharsets.UTF_8) : "";
	}

	/**
	 * Delete a file from the storage of the configured project or space.
	 * @param key the object key
	 * @since 2.0.1
	 */
	public void deleteFile(final String key) {
		Assert.hasText(key, "File key cannot be null or empty");

		this.restClient.delete()
			.uri(storageUri(key))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.retrieve()
			.toBodilessEntity();
	}

	/**
	 * The storage of the configured project or space, looked up once from the data
	 * platform API.
	 * @return the storage bucket and endpoint
	 * @since 2.0.1
	 */
	public Storage getStorage() {
		Storage resolved = this.storage;
		if (resolved == null) {
			resolved = resolveStorage();
			this.storage = resolved;
		}
		return resolved;
	}

	private Storage resolveStorage() {
		Assert.state(StringUtils.hasText(this.projectId) || StringUtils.hasText(this.spaceId),
				"A project ID or space ID is required to use its storage for text extraction");
		Assert.state(this.dataplatformUrl != null,
				"Cannot derive the data platform API URL from the watsonx.ai base URL. Set it explicitly"
						+ " (spring.ai.watsonx.ai.text-extraction.dataplatform-url).");

		String path = StringUtils.hasText(this.projectId) ? "/v2/projects/" + this.projectId
				: "/v2/spaces/" + this.spaceId;

		StorageContainer container = this.restClient.get()
			.uri(URI.create(this.dataplatformUrl + path))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.watsonxAiAuthentication.getAccessToken())
			.retrieve()
			.body(StorageContainer.class);

		StorageProperties properties = (container != null && container.entity() != null
				&& container.entity().storage() != null) ? container.entity().storage().properties() : null;
		Assert.state(properties != null && StringUtils.hasText(properties.bucketName())
				&& StringUtils.hasText(properties.endpointUrl()), "No storage bucket found for " + path);

		return new Storage(properties.bucketName(), stripTrailingSlash(properties.endpointUrl()));
	}

	private URI storageUri(String key) {
		Storage resolved = getStorage();
		return UriComponentsBuilder.fromUriString(resolved.endpointUrl())
			.pathSegment(resolved.bucketName())
			.pathSegment(key.split("/"))
			.encode()
			.build()
			.toUri();
	}

	/**
	 * The data platform API URL for the region of a watsonx.ai base URL, for example
	 * {@code https://api.dataplatform.cloud.ibm.com} for
	 * {@code https://us-south.ml.cloud.ibm.com}.
	 * @param baseUrl the watsonx.ai base URL
	 * @return the data platform API URL, or {@code null} when the region is unknown
	 * @since 2.0.1
	 */
	public static String dataplatformUrlFor(String baseUrl) {
		if (!StringUtils.hasText(baseUrl)) {
			return null;
		}
		String host = URI.create(baseUrl).getHost();
		if (host == null || !host.endsWith(".ml.cloud.ibm.com")) {
			return null;
		}
		return DATAPLATFORM_URLS.get(host.substring(0, host.indexOf('.')));
	}

	private static String stripTrailingSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	/**
	 * The Cloud Object Storage bucket of a project or deployment space.
	 *
	 * @param bucketName the bucket name
	 * @param endpointUrl the Cloud Object Storage endpoint URL
	 * @since 2.0.1
	 */
	public record Storage(String bucketName, String endpointUrl) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record StorageContainer(@JsonProperty("entity") StorageEntity entity) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record StorageEntity(@JsonProperty("storage") StorageDetails storage) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record StorageDetails(@JsonProperty("properties") StorageProperties properties) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record StorageProperties(@JsonProperty("bucket_name") String bucketName,
			@JsonProperty("endpoint_url") String endpointUrl) {
	}

}
