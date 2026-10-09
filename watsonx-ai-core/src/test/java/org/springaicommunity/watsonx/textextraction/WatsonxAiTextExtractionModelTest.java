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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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

	private static final String JOB_ID = "job-1";

	@Mock
	private WatsonxAiTextExtractionApi textExtractionApi;

	@Mock
	private RetryTemplate retryTemplate;

	private WatsonxAiTextExtractionModel extractionModel;

	private WatsonxAiTextExtractionOptions defaultOptions;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		defaultOptions = WatsonxAiTextExtractionOptions.builder().languages(List.of("en")).ocrMode("enabled").build();

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

	private static WatsonxAiTextExtractionResponse job(String status) {
		return job(status, null);
	}

	private static WatsonxAiTextExtractionResponse job(String status,
			WatsonxAiTextExtractionResponse.ExtractionError error) {
		return WatsonxAiTextExtractionResponse.builder()
			.metadata(new WatsonxAiTextExtractionResponse.ExtractionMetadata(JOB_ID, null, null))
			.entity(new WatsonxAiTextExtractionResponse.ExtractionEntity(null, null, null,
					new WatsonxAiTextExtractionResponse.ExtractionResults(status, 1, error)))
			.build();
	}

	private void givenJobFinishes(String extractedText) {
		when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
			.thenReturn(ResponseEntity.ok(job("submitted")));
		when(textExtractionApi.getExtraction(JOB_ID)).thenReturn(ResponseEntity.ok(job("running")))
			.thenReturn(ResponseEntity.ok(job("completed")));
		when(textExtractionApi.downloadFile(anyString())).thenReturn(extractedText);
	}

	private static Resource namedResource(String content, String fileName) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
			@Override
			public String getFilename() {
				return fileName;
			}
		};
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
	class ExtractRequestTests {

		@Test
		void extractWithValidRequest() {
			WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
				.documentReference(WatsonxAiTextExtractionRequest.DocumentReference.ofContainer("test.pdf"))
				.build();

			WatsonxAiTextExtractionResponse mockResponse = WatsonxAiTextExtractionResponse.builder()
				.metadata(new WatsonxAiTextExtractionResponse.ExtractionMetadata("ext-123", LocalDateTime.now(),
						LocalDateTime.now()))
				.build();

			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(mockResponse));

			WatsonxAiTextExtractionResponse response = extractionModel.extract(request);

			assertNotNull(response);
			assertEquals("ext-123", response.getId());
			verify(textExtractionApi, times(1)).extract(any(WatsonxAiTextExtractionRequest.class));
			verify(textExtractionApi, never()).uploadFile(anyString(), any());
		}

		@Test
		void extractSendsApiParameterNames() {
			WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
				.documentReference(WatsonxAiTextExtractionRequest.DocumentReference.ofContainer("test.pdf"))
				.build();
			WatsonxAiTextExtractionOptions runtimeOptions = WatsonxAiTextExtractionOptions.builder()
				.requestedOutputs(List.of("plain_text"))
				.mode("high_quality")
				.build();

			ArgumentCaptor<WatsonxAiTextExtractionRequest> captor = ArgumentCaptor
				.forClass(WatsonxAiTextExtractionRequest.class);
			when(textExtractionApi.extract(captor.capture())).thenReturn(ResponseEntity.ok(job("submitted")));

			extractionModel.extract(request, runtimeOptions);

			WatsonxAiTextExtractionRequest.ExtractionParameters parameters = captor.getValue().parameters();
			assertEquals(List.of("plain_text"), parameters.requestedOutputs());
			assertEquals("high_quality", parameters.mode());
			assertEquals("enabled", parameters.ocrMode());
			assertEquals(List.of("en"), parameters.languages());
		}

		@Test
		@SuppressWarnings("deprecation")
		void deprecatedOptionsAreMappedToApiParameters() {
			WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
				.documentReference(WatsonxAiTextExtractionRequest.DocumentReference.ofContainer("test.pdf"))
				.build();
			WatsonxAiTextExtractionModel model = new WatsonxAiTextExtractionModel(textExtractionApi,
					WatsonxAiTextExtractionOptions.builder()
						.model("ignored-model")
						.outputFormats(List.of("json"))
						.enableOcr(false)
						.build(),
					ObservationRegistry.NOOP, retryTemplate);

			ArgumentCaptor<WatsonxAiTextExtractionRequest> captor = ArgumentCaptor
				.forClass(WatsonxAiTextExtractionRequest.class);
			when(textExtractionApi.extract(captor.capture())).thenReturn(ResponseEntity.ok(job("submitted")));

			model.extract(request);

			WatsonxAiTextExtractionRequest.ExtractionParameters parameters = captor.getValue().parameters();
			assertEquals(List.of("json"), parameters.requestedOutputs());
			assertEquals("disabled", parameters.ocrMode());
		}

		@Test
		void extractWithNullRequestThrowsException() {
			assertThrows(IllegalArgumentException.class,
					() -> extractionModel.extract((WatsonxAiTextExtractionRequest) null));
		}

	}

	@Nested
	class ExtractResourceTests {

		@Test
		void extractWithResourceUploadsPollsDownloadsAndDeletes() {
			givenJobFinishes("# Hello pong");

			WatsonxAiTextExtractionResponse response = extractionModel.extract(namedResource("pdf", "report.pdf"));

			assertEquals("# Hello pong", response.getText());
			assertEquals("completed", response.getStatus());

			ArgumentCaptor<String> uploadKey = ArgumentCaptor.forClass(String.class);
			verify(textExtractionApi).uploadFile(uploadKey.capture(), any(Resource.class));
			assertTrue(uploadKey.getValue().startsWith("spring-ai-text-extraction/"));
			assertTrue(uploadKey.getValue().endsWith("/report.pdf"));
			String resultsKey = uploadKey.getValue().replace("/report.pdf", "/report.md");

			ArgumentCaptor<WatsonxAiTextExtractionRequest> request = ArgumentCaptor
				.forClass(WatsonxAiTextExtractionRequest.class);
			verify(textExtractionApi).extract(request.capture());
			assertEquals("container", request.getValue().documentReference().type());
			assertEquals(uploadKey.getValue(), request.getValue().documentReference().location().get("path"));
			assertEquals(resultsKey, request.getValue().resultsReference().location().get("path"));
			assertEquals(List.of("md"), request.getValue().parameters().requestedOutputs());
			assertNull(request.getValue().resource());

			verify(textExtractionApi, times(2)).getExtraction(JOB_ID);
			verify(textExtractionApi).downloadFile(resultsKey);
			verify(textExtractionApi).deleteFile(uploadKey.getValue());
			verify(textExtractionApi).deleteFile(resultsKey);
		}

		@Test
		void extractWithBytesUsesFileName() {
			givenJobFinishes("bytes text");

			WatsonxAiTextExtractionResponse response = extractionModel.extract("data".getBytes(), "scan.png");

			assertEquals("bytes text", response.getText());
			verify(textExtractionApi).uploadFile(argThat(key -> key.endsWith("/scan.png")), any(Resource.class));
			verify(textExtractionApi).downloadFile(argThat(key -> key.endsWith("/scan.md")));
		}

		@Test
		void plainTextOutputUsesTxtResultFile() {
			givenJobFinishes("plain");

			extractionModel.extract(namedResource("pdf", "report.pdf"),
					WatsonxAiTextExtractionOptions.builder().requestedOutputs(List.of("plain_text")).build());

			verify(textExtractionApi).downloadFile(argThat(key -> key.endsWith("/report.txt")));
		}

		@Test
		void failedJobThrowsWithErrorAndDeletesFiles() {
			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(job("submitted")));
			when(textExtractionApi.getExtraction(JOB_ID)).thenReturn(ResponseEntity.ok(job("failed",
					new WatsonxAiTextExtractionResponse.ExtractionError("file_download_error", "NoSuchKey"))));

			IllegalStateException ex = assertThrows(IllegalStateException.class,
					() -> extractionModel.extract(namedResource("pdf", "report.pdf")));

			assertTrue(ex.getMessage().contains("failed"));
			assertTrue(ex.getMessage().contains("file_download_error NoSuchKey"));
			verify(textExtractionApi, never()).downloadFile(anyString());
			verify(textExtractionApi, times(2)).deleteFile(anyString());
		}

		@Test
		void timeoutCancelsJobAndDeletesFiles() {
			extractionModel.setPollTimeout(Duration.ofMillis(50));
			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(job("submitted")));
			when(textExtractionApi.getExtraction(JOB_ID)).thenReturn(ResponseEntity.ok(job("running")));

			IllegalStateException ex = assertThrows(IllegalStateException.class,
					() -> extractionModel.extract(namedResource("pdf", "report.pdf")));

			assertTrue(ex.getMessage().contains("did not finish within"));
			verify(textExtractionApi).deleteExtraction(JOB_ID);
			verify(textExtractionApi, times(2)).deleteFile(anyString());
		}

		@Test
		void pollingErrorCancelsJobAndDeletesFiles() {
			when(textExtractionApi.extract(any(WatsonxAiTextExtractionRequest.class)))
				.thenReturn(ResponseEntity.ok(job("submitted")));
			when(textExtractionApi.getExtraction(JOB_ID)).thenThrow(new IllegalStateException("HTTP 400"));

			assertThrows(IllegalStateException.class,
					() -> extractionModel.extract(namedResource("pdf", "report.pdf")));

			verify(textExtractionApi).deleteExtraction(JOB_ID);
			verify(textExtractionApi, times(2)).deleteFile(anyString());
		}

		@Test
		void deleteFilesFalseKeepsFiles() {
			extractionModel.setDeleteFiles(false);
			givenJobFinishes("kept");

			extractionModel.extract(namedResource("pdf", "report.pdf"));

			verify(textExtractionApi, never()).deleteFile(anyString());
		}

		@Test
		void failedDeleteDoesNotHideTheResult() {
			givenJobFinishes("text");
			doThrow(new IllegalStateException("403")).when(textExtractionApi).deleteFile(anyString());

			assertEquals("text", extractionModel.extract(namedResource("pdf", "report.pdf")).getText());
		}

		@Test
		void severalRequestedOutputsAreRejected() {
			WatsonxAiTextExtractionOptions options = WatsonxAiTextExtractionOptions.builder()
				.requestedOutputs(List.of("md", "json"))
				.build();

			assertThrows(IllegalArgumentException.class,
					() -> extractionModel.extract(namedResource("pdf", "report.pdf"), options));
			verify(textExtractionApi, never()).uploadFile(anyString(), any());
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
		void extractToDocumentsReturnsExtractedText() {
			givenJobFinishes("Entire notes content");

			List<Document> documents = extractionModel.extractToDocuments(namedResource("pdf", "notes.pdf"));

			assertEquals(1, documents.size());
			assertEquals("Entire notes content", documents.get(0).getText());
			assertEquals("notes.pdf", documents.get(0).getMetadata().get("document_name"));
		}

		@Test
		void setObservationConvention() {
			TextExtractionModelObservationConvention customConvention = mock(
					TextExtractionModelObservationConvention.class);
			assertDoesNotThrow(() -> extractionModel.setObservationConvention(customConvention));
		}

	}

	@Nested
	class ResponseTests {

		@Test
		void statusComesFromResults() {
			assertEquals("running", job("running").getStatus());
			assertEquals(1, job("running").getNumberPagesProcessed());
		}

		@Test
		void statusIsNullWhenAbsent() {
			assertNull(WatsonxAiTextExtractionResponse.builder().build().getStatus());
		}

	}

}
