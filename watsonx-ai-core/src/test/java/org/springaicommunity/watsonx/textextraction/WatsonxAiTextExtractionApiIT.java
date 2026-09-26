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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Integration tests for {@link WatsonxAiTextExtractionApi} using
 * {@link MockRestServiceServer}.
 *
 * @author Ana Katrina Inguengan
 * @since 2.0.0
 */
public class WatsonxAiTextExtractionApiIT {

	private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";

	private static final String TEXT_EXTRACTION_ENDPOINT = "/ml/v1/text/extractions";

	private static final String VERSION = "2024-05-31";

	private static final String PROJECT_ID = "test-project-id";

	private static final String SPACE_ID = "test-space-id";

	private static final String API_KEY = "test-api-key";

	private MockRestServiceServer mockServer;

	private WatsonxAiTextExtractionApi textExtractionApi;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		this.mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

		ResponseErrorHandler errorHandler = response -> {
			HttpStatus.Series series = HttpStatus.Series.resolve(response.getStatusCode().value());
			return series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR;
		};

		this.textExtractionApi = new WatsonxAiTextExtractionApi(BASE_URL, TEXT_EXTRACTION_ENDPOINT, VERSION, PROJECT_ID,
				SPACE_ID, API_KEY, restClientBuilder, errorHandler);
	}

	@Test
	void extractWithDocumentReferenceTest() {
		String jsonResponse = """
				{
				  "metadata": {
				    "id": "extraction-123",
				    "created_at": "2026-01-15T10:30:00",
				    "modified_at": "2026-01-15T10:31:00"
				  },
				  "entity": {
				    "status": {
				      "state": "completed",
				      "message": "Extraction completed"
				    }
				  },
				  "text": "Extracted document content"
				}
				""";

		this.mockServer.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "?version=" + VERSION))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
			.andExpect(content().json("""
					{
					  "project_id": "test-project-id",
					  "space_id": "test-space-id",
					  "document_reference": {
					    "type": "container",
					    "location": {
					      "path": "test.pdf"
					    }
					  },
					  "parameters": {
					    "model_id": "ibm-doc-extract",
					    "output_formats": ["text"],
					    "languages": ["en"],
					    "enable_ocr": true
					  }
					}
					"""))
			.andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

		WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
			.documentReference(WatsonxAiTextExtractionRequest.DocumentReference.ofContainer("test.pdf"))
			.parameters(new WatsonxAiTextExtractionRequest.ExtractionParameters("ibm-doc-extract", List.of("text"),
					List.of("en"), true))
			.build();

		ResponseEntity<WatsonxAiTextExtractionResponse> response = this.textExtractionApi.extract(request);

		assertNotNull(response.getBody());
		assertEquals("extraction-123", response.getBody().getId());
		assertEquals("completed", response.getBody().getStatus());
		assertEquals("Extracted document content", response.getBody().getText());

		this.mockServer.verify();
	}

	@Test
	void extractWithResourceTest() {
		String jsonResponse = """
				{
				  "metadata": {
				    "id": "extraction-456",
				    "created_at": "2026-01-15T10:30:00",
				    "modified_at": "2026-01-15T10:31:00"
				  },
				  "text": "Extracted file content"
				}
				""";

		this.mockServer.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "?version=" + VERSION))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.MULTIPART_FORM_DATA_VALUE)))
			.andExpect(content().string(containsString("sample.txt")))
			.andExpect(content().string(containsString("sample file content")))
			.andExpect(content().string(containsString("project_id")))
			.andExpect(content().string(containsString(PROJECT_ID)))
			.andExpect(content().string(containsString("space_id")))
			.andExpect(content().string(containsString(SPACE_ID)))
			.andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

		ByteArrayResource resource = new ByteArrayResource("sample file content".getBytes(StandardCharsets.UTF_8)) {
			@Override
			public String getFilename() {
				return "sample.txt";
			}
		};

		WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder().resource(resource).build();

		ResponseEntity<WatsonxAiTextExtractionResponse> response = this.textExtractionApi.extract(request);

		assertNotNull(response.getBody());
		assertEquals("extraction-456", response.getBody().getId());
		assertEquals("Extracted file content", response.getBody().getText());

		this.mockServer.verify();
	}

	@Test
	void getExtractionTest() {
		String jsonResponse = """
				{
				  "metadata": {
				    "id": "extraction-789",
				    "created_at": "2026-01-15T10:30:00",
				    "modified_at": "2026-01-15T10:31:00"
				  },
				  "entity": {
				    "status": {
				      "state": "completed",
				      "message": "Extraction completed"
				    }
				  },
				  "pages": [
				    {
				      "page_number": 1,
				      "text": "First page"
				    },
				    {
				      "page_number": 2,
				      "text": "Second page"
				    }
				  ]
				}
				""";

		this.mockServer
			.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "/extraction-789?version=" + VERSION
					+ "&project_id=" + PROJECT_ID + "&space_id=" + SPACE_ID))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

		ResponseEntity<WatsonxAiTextExtractionResponse> response = this.textExtractionApi
			.getExtraction("extraction-789");

		assertNotNull(response.getBody());
		assertEquals("extraction-789", response.getBody().getId());
		assertEquals("completed", response.getBody().getStatus());
		assertEquals("First page\n\nSecond page", response.getBody().getText());
		assertNotNull(response.getBody().pages());
		assertEquals(2, response.getBody().pages().size());

		this.mockServer.verify();
	}

	@Test
	void deleteExtractionTest() {
		this.mockServer
			.expect(requestTo(BASE_URL + TEXT_EXTRACTION_ENDPOINT + "/extraction-789?version=" + VERSION
					+ "&project_id=" + PROJECT_ID + "&space_id=" + SPACE_ID))
			.andExpect(method(HttpMethod.DELETE))
			.andRespond(withStatus(HttpStatus.NO_CONTENT));

		ResponseEntity<Void> response = this.textExtractionApi.deleteExtraction("extraction-789");

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

		this.mockServer.verify();
	}

}
