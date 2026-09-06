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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springaicommunity.watsonx.textextraction.observation.TextExtractionModelObservationConvention;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;

/**
 * JUnit 5 test class for {@link WatsonxAiTextExtractionModel} functionality.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
class WatsonxAiTextExtractionModelTest {

	@Mock
	private WatsonxAiTextExtractionApi textExtractionApi;

	@Mock
	private RetryTemplate retryTemplate;

	private WatsonxAiTextExtractionModel extractionModel;

	private WatsonxAiTextExtractionOptions defaultOptions;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		defaultOptions = WatsonxAiTextExtractionOptions.builder()
			.model("ibm-doc-extract")
			.outputFormats(List.of("markdown", "text"))
			.languages(List.of("en"))
			.enableOcr(true)
			.build();

		extractionModel = new WatsonxAiTextExtractionModel(textExtractionApi, defaultOptions, ObservationRegistry.NOOP,
				retryTemplate);

		doAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			org.springframework.core.retry.Retryable<Object> retryable = (org.springframework.core.retry.Retryable<Object>) invocation
				.getArgument(0);
			try {
				return retryable.execute();
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}).when(retryTemplate).execute(any());
	}

	@Nested
	class ConstructorTests {

		@Test
		void constructorWithValidParameters() {
			assertNotNull(extractionModel);
			assertEquals(defaultOptions, extractionModel.getDefaultOptions());
		}

		@Test
		void constructorWithNullApiThrowsException() {
			assertThrows(IllegalArgumentException.class, () -> new WatsonxAiTextExtractionModel(null, defaultOptions,
					ObservationRegistry.NOOP, retryTemplate), "WatsonxAiTextExtractionApi must not be null");
		}

		@Test
		void constructorWithNullOptionsThrowsException() {
			assertThrows(
					IllegalArgumentException.class, () -> new WatsonxAiTextExtractionModel(textExtractionApi, null,
							ObservationRegistry.NOOP, retryTemplate),
					"WatsonxAiTextExtractionOptions must not be null");
		}

		@Test
		void constructorWithNullObservationRegistryThrowsException() {
			assertThrows(IllegalArgumentException.class,
					() -> new WatsonxAiTextExtractionModel(textExtractionApi, defaultOptions, null, retryTemplate),
					"ObservationRegistry must not be null");
		}

		@Test
		void constructorWithNullRetryTemplateThrowsException() {
			assertThrows(IllegalArgumentException.class, () -> new WatsonxAiTextExtractionModel(textExtractionApi,
					defaultOptions, ObservationRegistry.NOOP, null), "RetryTemplate must not be null");
		}

	}

	@Nested
	class ExtractMethodTests {

		@Test
		void extractWithValidRequest() {
			WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
				.documentReference(WatsonxAiTextExtractionRequest.DocumentReference.ofContainer("test.pdf"))
				.build();

			WatsonxAiTextExtractionResponse mockResponse = WatsonxAiTextExtractionResponse.builder()
				.text("Extracted document content")
				.metadata(new WatsonxAiTextExtractionResponse.ExtractionMetadata("ext-123", LocalDateTime.now(),
						LocalDateTime.now()))
				.build();

			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(mockResponse));

			WatsonxAiTextExtractionResponse response = extractionModel.extract(request);

			assertNotNull(response);
			assertEquals("Extracted document content", response.getText());
			assertEquals("ext-123", response.getId());
			verify(textExtractionApi, times(1)).extract(any(WatsonxAiTextExtractionRequest.class));
		}

		@Test
		void extractWithResource() {
			Resource resource = new ByteArrayResource("sample file content".getBytes(StandardCharsets.UTF_8),
					"sample.txt");

			WatsonxAiTextExtractionResponse mockResponse = WatsonxAiTextExtractionResponse.builder()
				.text("Extracted from sample.txt")
				.build();

			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(mockResponse));

			WatsonxAiTextExtractionResponse response = extractionModel.extract(resource);

			assertNotNull(response);
			assertEquals("Extracted from sample.txt", response.getText());
		}

		@Test
		void extractWithBytes() {
			byte[] bytes = "document content".getBytes(StandardCharsets.UTF_8);

			WatsonxAiTextExtractionResponse mockResponse = WatsonxAiTextExtractionResponse.builder()
				.text("Extracted from bytes")
				.build();

			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(mockResponse));

			WatsonxAiTextExtractionResponse response = extractionModel.extract(bytes, "test.pdf");

			assertNotNull(response);
			assertEquals("Extracted from bytes", response.getText());
		}

		@Test
		void extractWithNullRequestThrowsException() {
			assertThrows(IllegalArgumentException.class,
					() -> extractionModel.extract((WatsonxAiTextExtractionRequest) null));
		}

		@Test
		void extractWithNullResourceThrowsException() {
			assertThrows(IllegalArgumentException.class, () -> extractionModel.extract((Resource) null));
		}

		@Test
		void extractWithNullBytesThrowsException() {
			assertThrows(IllegalArgumentException.class, () -> extractionModel.extract((byte[]) null));
		}

		@Test
		void extractWithRuntimeOptionsMergesCorrectly() {
			Resource resource = new ByteArrayResource("data".getBytes(), "data.pdf");

			WatsonxAiTextExtractionOptions runtimeOptions = WatsonxAiTextExtractionOptions.builder()
				.outputFormats(List.of("json"))
				.languages(List.of("fr"))
				.enableOcr(false)
				.build();

			WatsonxAiTextExtractionResponse mockResponse = WatsonxAiTextExtractionResponse.builder()
				.text("json text")
				.build();

			ArgumentCaptor<WatsonxAiTextExtractionRequest> captor = ArgumentCaptor
				.forClass(WatsonxAiTextExtractionRequest.class);
			when(textExtractionApi.extract(captor.capture())).thenReturn(ResponseEntity.ok(mockResponse));

			WatsonxAiTextExtractionResponse response = extractionModel.extract(resource, runtimeOptions);

			assertNotNull(response);
			WatsonxAiTextExtractionRequest captured = captor.getValue();
			assertNotNull(captured.parameters());
			assertEquals(List.of("json"), captured.parameters().outputFormats());
			assertEquals(List.of("fr"), captured.parameters().languages());
			assertEquals(false, captured.parameters().enableOcr());
			assertEquals("ibm-doc-extract", captured.parameters().model());
		}

		@Test
		void extractToDocumentsWithPages() {
			Resource resource = new ByteArrayResource("content".getBytes(), "report.pdf") {
				@Override
				public String getFilename() {
					return "report.pdf";
				}
			};

			WatsonxAiTextExtractionResponse.ExtractedPage page1 = new WatsonxAiTextExtractionResponse.ExtractedPage(1,
					"Page 1 content", List.of(), Map.of("section", "Introduction"));
			WatsonxAiTextExtractionResponse.ExtractedPage page2 = new WatsonxAiTextExtractionResponse.ExtractedPage(2,
					"Page 2 content", List.of(), Map.of("section", "Conclusion"));

			WatsonxAiTextExtractionResponse mockResponse = WatsonxAiTextExtractionResponse.builder()
				.pages(List.of(page1, page2))
				.build();

			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(mockResponse));

			List<Document> documents = extractionModel.extractToDocuments(resource);

			assertNotNull(documents);
			assertEquals(2, documents.size());
			assertEquals("Page 1 content", documents.get(0).getText());
			assertEquals(1, documents.get(0).getMetadata().get("page_number"));
			assertEquals("report.pdf", documents.get(0).getMetadata().get("document_name"));
			assertEquals("Introduction", documents.get(0).getMetadata().get("section"));
			assertEquals("Page 2 content", documents.get(1).getText());
			assertEquals(2, documents.get(1).getMetadata().get("page_number"));
		}

		@Test
		void extractToDocumentsWithSingleText() {
			Resource resource = new ByteArrayResource("content".getBytes(), "notes.txt") {
				@Override
				public String getFilename() {
					return "notes.txt";
				}
			};

			WatsonxAiTextExtractionResponse mockResponse = WatsonxAiTextExtractionResponse.builder()
				.text("Entire notes content")
				.build();

			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(mockResponse));

			List<Document> documents = extractionModel.extractToDocuments(resource);

			assertNotNull(documents);
			assertEquals(1, documents.size());
			assertEquals("Entire notes content", documents.get(0).getText());
			assertEquals("notes.txt", documents.get(0).getMetadata().get("document_name"));
		}

		@Test
		void setObservationConvention() {
			TextExtractionModelObservationConvention customConvention = mock(
					TextExtractionModelObservationConvention.class);
			assertDoesNotThrow(() -> extractionModel.setObservationConvention(customConvention));
		}

	}

}
