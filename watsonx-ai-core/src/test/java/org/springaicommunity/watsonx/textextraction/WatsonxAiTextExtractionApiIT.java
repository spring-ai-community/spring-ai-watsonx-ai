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

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springaicommunity.watsonx.auth.StubWatsonxAiAuthentication;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Integration tests for {@link WatsonxAiTextExtractionApi} using
 * {@link MockRestServiceServer}. The response bodies are taken from real watsonx.ai
 * responses.
 *
 * @author Ana Katrina Inguengan
 * @since 2.0.0
 */
@ExtendWith(StubWatsonxAiAuthentication.class)
public class WatsonxAiTextExtractionApiIT {

	private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";

	private static final String DATAPLATFORM_URL = "https://api.dataplatform.cloud.ibm.com";

	private static final String TEXT_EXTRACTION_ENDPOINT = "/ml/v1/text/extractions";

	private static final String VERSION = "2024-10-17";

	private static final String PROJECT_ID = "test-project-id";

	private static final String SPACE_ID = "test-space-id";

	private static final String API_KEY = "test-api-key";

	private static final String COS_ENDPOINT = "https://s3.us-south.cloud-object-storage.appdomain.cloud";

	private static final String BUCKET = "test-bucket";

	private static final String STORAGE_RESPONSE = """
			{
			  "metadata": { "id": "test-space-id" },
			  "entity": {
			    "name": "test space",
			    "storage": {
			      "type": "bmcos_object_storage",
			      "properties": {
			        "bucket_name": "test-bucket",
			        "endpoint_url": "https://s3.us-south.cloud-object-storage.appdomain.cloud/"
			      }
			    }
			  }
			}
			""";

	private RestClient.Builder restClientBuilder;

	private MockRestServiceServer mockServer;

	private ResponseErrorHandler errorHandler;

	private WatsonxAiTextExtractionApi textExtractionApi;

	@BeforeEach
	void setUp() {
		this.restClientBuilder = RestClient.builder();
		this.mockServer = MockRestServiceServer.bindTo(this.restClientBuilder).build();

		this.errorHandler = response -> {
			HttpStatus.Series series = HttpStatus.Series.resolve(response.getStatusCode().value());
			return series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR;
		};

		this.textExtractionApi = new WatsonxAiTextExtractionApi(BASE_URL, TEXT_EXTRACTION_ENDPOINT, VERSION, PROJECT_ID,
				SPACE_ID, API_KEY, this.restClientBuilder, this.errorHandler);
	}

	private WatsonxAiTextExtractionApi spaceOnlyApi() {
		return new WatsonxAiTextExtractionApi(BASE_URL, TEXT_EXTRACTION_ENDPOINT, VERSION, null, SPACE_ID, API_KEY,
				this.restClientBuilder, this.errorHandler);
	}

	@Test
	void extractSendsApiParameterNamesTest() {
		String submittedResponse = """
				{
				  "metadata": {
				    "id": "0022b23f-88c8-4f02-9c4e-127047d37613",
				    "created_at": "2026-10-09T08:15:22.182Z",
				    "space_id": "test-space-id"
				  },
				  "entity": {
				    "document_reference": { "type": "container", "location": { "path": "test.pdf" } },
				    "results_reference": { "type": "container", "location": { "path": "test.md" } },
				    "parameters": { "requested_outputs": ["md"], "mode": "standard", "languages": ["latn"] },
				    "results": { "status": "submitted", "number_pages_processed": 0 }
				  }
				}
				""";

		this.mockServer.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "?version=" + VERSION))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
			.andExpect(content().json("""
					{
					  "project_id": "test-project-id",
					  "space_id": "test-space-id",
					  "document_reference": { "type": "container", "location": { "path": "test.pdf" } },
					  "results_reference": { "type": "container", "location": { "path": "test.md" } },
					  "parameters": {
					    "requested_outputs": ["md"],
					    "mode": "standard",
					    "ocr_mode": "enabled",
					    "languages": ["en"]
					  }
					}
					""", true))
			.andRespond(withSuccess(submittedResponse, MediaType.APPLICATION_JSON));

		WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
			.documentReference(WatsonxAiTextExtractionRequest.DocumentReference.ofContainer("test.pdf"))
			.resultsReference(WatsonxAiTextExtractionRequest.DocumentReference.ofContainer("test.md"))
			.parameters(new WatsonxAiTextExtractionRequest.ExtractionParameters(List.of("md"), "standard", "enabled",
					List.of("en")))
			.build();

		ResponseEntity<WatsonxAiTextExtractionResponse> response = this.textExtractionApi.extract(request);

		assertNotNull(response.getBody());
		assertEquals("0022b23f-88c8-4f02-9c4e-127047d37613", response.getBody().getId());
		assertEquals("submitted", response.getBody().getStatus());
		assertEquals(0, response.getBody().getNumberPagesProcessed());

		this.mockServer.verify();
	}

	@Test
	void extractWithResourceIsRejectedTest() {
		ByteArrayResource resource = new ByteArrayResource("content".getBytes(StandardCharsets.UTF_8));
		WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder().resource(resource).build();

		assertThrows(IllegalArgumentException.class, () -> this.textExtractionApi.extract(request));

		this.mockServer.verify();
	}

	@Test
	void getCompletedExtractionTest() {
		String completedResponse = """
				{
				  "metadata": {
				    "id": "61ffa24a-84de-4cce-9470-c4ade1697352",
				    "created_at": "2026-10-09T08:22:41.120Z",
				    "modified_at": "2026-10-09T08:22:50.877Z",
				    "space_id": "test-space-id"
				  },
				  "entity": {
				    "results": { "status": "completed", "number_pages_processed": 1, "completed_at": "2026-10-09T08:22:50.877Z" }
				  }
				}
				""";

		this.mockServer
			.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "/extraction-789?version=" + VERSION
					+ "&project_id=" + PROJECT_ID + "&space_id=" + SPACE_ID))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(completedResponse, MediaType.APPLICATION_JSON));

		WatsonxAiTextExtractionResponse response = this.textExtractionApi.getExtraction("extraction-789").getBody();

		assertNotNull(response);
		assertEquals("61ffa24a-84de-4cce-9470-c4ade1697352", response.getId());
		assertEquals("completed", response.getStatus());
		assertEquals(1, response.getNumberPagesProcessed());
		assertNull(response.getError());

		this.mockServer.verify();
	}

	@Test
	void getFailedExtractionTest() {
		String failedResponse = """
				{
				  "metadata": { "id": "extraction-789", "created_at": "2026-10-09T08:15:22.182Z" },
				  "entity": {
				    "results": {
				      "status": "failed",
				      "number_pages_processed": 0,
				      "error": {
				        "code": "file_download_error",
				        "message": "failed to download file, NoSuchKey: The specified key does not exist."
				      }
				    }
				  }
				}
				""";

		this.mockServer
			.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "/extraction-789?version=" + VERSION
					+ "&project_id=" + PROJECT_ID + "&space_id=" + SPACE_ID))
			.andRespond(withSuccess(failedResponse, MediaType.APPLICATION_JSON));

		WatsonxAiTextExtractionResponse response = this.textExtractionApi.getExtraction("extraction-789").getBody();

		assertNotNull(response);
		assertEquals("failed", response.getStatus());
		assertEquals("file_download_error", response.getError().code());

		this.mockServer.verify();
	}

	@Test
	void deleteExtractionTest() {
		this.mockServer
			.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "/extraction-789?version=" + VERSION
					+ "&hard_delete=true&project_id=" + PROJECT_ID + "&space_id=" + SPACE_ID))
			.andExpect(method(HttpMethod.DELETE))
			.andRespond(withStatus(HttpStatus.NO_CONTENT));

		ResponseEntity<Void> response = this.textExtractionApi.deleteExtraction("extraction-789");

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

		this.mockServer.verify();
	}

	@Test
	void storageFileOperationsUseSpaceStorageTest() {
		WatsonxAiTextExtractionApi api = spaceOnlyApi();
		String objectUrl = COS_ENDPOINT + "/" + BUCKET + "/spring-ai-text-extraction/abc/my%20report.pdf";

		this.mockServer.expect(ExpectedCount.once(), requestTo(DATAPLATFORM_URL + "/v2/spaces/" + SPACE_ID))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
			.andRespond(withSuccess(STORAGE_RESPONSE, MediaType.APPLICATION_JSON));
		this.mockServer.expect(requestTo(objectUrl))
			.andExpect(method(HttpMethod.PUT))
			.andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
			.andExpect(content().bytes("pdf bytes".getBytes(StandardCharsets.UTF_8)))
			.andRespond(withSuccess());
		this.mockServer.expect(requestTo(objectUrl))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("Hello pong\n\n<!-- Page 1 -->", MediaType.TEXT_PLAIN));
		this.mockServer.expect(requestTo(objectUrl))
			.andExpect(method(HttpMethod.DELETE))
			.andRespond(withStatus(HttpStatus.NO_CONTENT));

		String key = "spring-ai-text-extraction/abc/my report.pdf";
		api.uploadFile(key, new ByteArrayResource("pdf bytes".getBytes(StandardCharsets.UTF_8)));
		assertEquals("Hello pong\n\n<!-- Page 1 -->", api.downloadFile(key));
		api.deleteFile(key);

		assertEquals(new WatsonxAiTextExtractionApi.Storage(BUCKET, COS_ENDPOINT), api.getStorage());
		this.mockServer.verify();
	}

	@Test
	void blankProjectIdIsNotSentTest() {
		WatsonxAiTextExtractionApi api = new WatsonxAiTextExtractionApi(BASE_URL, TEXT_EXTRACTION_ENDPOINT, VERSION, "",
				SPACE_ID, API_KEY, this.restClientBuilder, this.errorHandler);

		this.mockServer
			.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "/extraction-789?version=" + VERSION + "&space_id="
					+ SPACE_ID))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		api.getExtraction("extraction-789");

		this.mockServer.verify();
	}

	@Test
	void storageOfProjectIsPreferredTest() {
		this.mockServer.expect(requestTo(DATAPLATFORM_URL + "/v2/projects/" + PROJECT_ID))
			.andRespond(withSuccess(STORAGE_RESPONSE, MediaType.APPLICATION_JSON));

		assertEquals(BUCKET, this.textExtractionApi.getStorage().bucketName());

		this.mockServer.verify();
	}

	@Test
	void customDataplatformUrlTest() {
		WatsonxAiTextExtractionApi api = new WatsonxAiTextExtractionApi(BASE_URL, TEXT_EXTRACTION_ENDPOINT, VERSION,
				null, SPACE_ID, API_KEY, "https://dataplatform.example.com/", this.restClientBuilder,
				this.errorHandler);

		this.mockServer.expect(requestTo("https://dataplatform.example.com/v2/spaces/" + SPACE_ID))
			.andRespond(withSuccess(STORAGE_RESPONSE, MediaType.APPLICATION_JSON));

		assertEquals(BUCKET, api.getStorage().bucketName());

		this.mockServer.verify();
	}

	@Test
	void dataplatformUrlIsDerivedFromRegionTest() {
		assertEquals("https://api.dataplatform.cloud.ibm.com",
				WatsonxAiTextExtractionApi.dataplatformUrlFor("https://us-south.ml.cloud.ibm.com"));
		assertEquals("https://api.eu-de.dataplatform.cloud.ibm.com",
				WatsonxAiTextExtractionApi.dataplatformUrlFor("https://eu-de.ml.cloud.ibm.com/"));
		assertEquals("https://api.jp-tok.dataplatform.cloud.ibm.com",
				WatsonxAiTextExtractionApi.dataplatformUrlFor("https://jp-tok.ml.cloud.ibm.com"));
		assertEquals("https://api.au-syd.dai.cloud.ibm.com",
				WatsonxAiTextExtractionApi.dataplatformUrlFor("https://au-syd.ml.cloud.ibm.com"));
		assertNull(WatsonxAiTextExtractionApi.dataplatformUrlFor("https://cpd.example.com"));
	}

}
