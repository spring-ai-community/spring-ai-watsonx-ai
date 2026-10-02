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

package org.springaicommunity.watsonx.chat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 test class for {@link JsonArgumentsNormalizer}.
 *
 * @author Ana Katrina Inguengan
 */
class JsonArgumentsNormalizerTest {

	@Test
	void keepsNullAndEmptyArguments() {
		assertNull(JsonArgumentsNormalizer.normalize(null));
		assertEquals("", JsonArgumentsNormalizer.normalize(""));
	}

	@Test
	void keepsPlainJsonObject() {
		assertEquals("{\"city\":\"Manila\"}", JsonArgumentsNormalizer.normalize("{\"city\":\"Manila\"}"));
	}

	@Test
	void trimsSurroundingWhitespace() {
		assertEquals("{\"city\":\"Manila\"}", JsonArgumentsNormalizer.normalize("  {\"city\":\"Manila\"}\n"));
	}

	@Test
	void unwrapsDoubleEncodedJson() {
		// The JSON object arrives as a JSON string: "{\"city\": \"Manila\"}"
		assertEquals("{\"city\": \"Manila\"}", JsonArgumentsNormalizer.normalize("\"{\\\"city\\\": \\\"Manila\\\"}\""));
	}

	@Test
	void unwrapsDoubleEncodedPrettyPrintedJson() {
		// "{\n \"city\": \"Manila\"\n}"
		assertEquals("{\n  \"city\": \"Manila\"\n}",
				JsonArgumentsNormalizer.normalize("\"{\\n  \\\"city\\\": \\\"Manila\\\"\\n}\""));
	}

	@Test
	void keepsEscapesInsideValuesOfDoubleEncodedJson() {
		// Inner JSON: {"quote":"say \"hi\"","path":"C:\\temp"}
		String doubleEncoded = "\"{\\\"quote\\\":\\\"say \\\\\\\"hi\\\\\\\"\\\",\\\"path\\\":\\\"C:\\\\\\\\temp\\\"}\"";

		assertEquals("{\"quote\":\"say \\\"hi\\\"\",\"path\":\"C:\\\\temp\"}",
				JsonArgumentsNormalizer.normalize(doubleEncoded));
	}

	@Test
	void normalizesWindowsLineBreaks() {
		assertEquals("{\n  \"city\": \"Manila\"\n}",
				JsonArgumentsNormalizer.normalize("{\r\n  \"city\": \"Manila\"\r\n}"));
	}

}
