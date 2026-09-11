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
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * JUnit 5 test class for {@link WatsonxAiDocumentReader}.
 *
 * @author Arnab Nandy
 * @since 1.2.0
 */
class WatsonxAiDocumentReaderTest {

	@Mock
	private WatsonxAiTextExtractionModel textExtractionModel;

	private Resource resource;

	private WatsonxAiTextExtractionOptions options;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		resource = new ByteArrayResource("test document".getBytes(), "test.pdf");
		options = WatsonxAiTextExtractionOptions.builder().enableOcr(true).build();
	}

	@Nested
	class ConstructorTests {

		@Test
		void constructorWithValidArguments() {
			WatsonxAiDocumentReader reader = new WatsonxAiDocumentReader(textExtractionModel, resource);
			assertNotNull(reader);
			assertInstanceOf(org.springframework.ai.document.DocumentReader.class, reader);

			WatsonxAiDocumentReader readerWithOptions = new WatsonxAiDocumentReader(textExtractionModel, resource,
					options);
			assertNotNull(readerWithOptions);
		}

		@Test
		void constructorWithNullModelThrowsException() {
			assertThrows(IllegalArgumentException.class, () -> new WatsonxAiDocumentReader(null, resource));
		}

		@Test
		void constructorWithNullResourceThrowsException() {
			assertThrows(IllegalArgumentException.class, () -> new WatsonxAiDocumentReader(textExtractionModel, null));
		}

	}

	@Nested
	class ReadTests {

		@Test
		void readExtractsDocumentsSuccessfully() {
			List<Document> expectedDocs = List.of(
					Document.builder().text("Page 1").metadata(Map.of("page_number", 1)).build(),
					Document.builder().text("Page 2").metadata(Map.of("page_number", 2)).build());

			when(textExtractionModel.extractToDocuments(resource, options)).thenReturn(expectedDocs);

			WatsonxAiDocumentReader reader = new WatsonxAiDocumentReader(textExtractionModel, resource, options);
			List<Document> result = reader.read();

			assertNotNull(result);
			assertEquals(2, result.size());
			assertEquals("Page 1", result.get(0).getText());
			assertEquals("Page 2", result.get(1).getText());
			verify(textExtractionModel, times(1)).extractToDocuments(resource, options);
		}

		@Test
		void getDelegatesToRead() {
			List<Document> expectedDocs = List.of(Document.builder().text("Content").build());
			when(textExtractionModel.extractToDocuments(resource, null)).thenReturn(expectedDocs);

			WatsonxAiDocumentReader reader = new WatsonxAiDocumentReader(textExtractionModel, resource);
			List<Document> result = reader.get();

			assertNotNull(result);
			assertEquals(1, result.size());
			assertEquals("Content", result.get(0).getText());
		}

	}

}
