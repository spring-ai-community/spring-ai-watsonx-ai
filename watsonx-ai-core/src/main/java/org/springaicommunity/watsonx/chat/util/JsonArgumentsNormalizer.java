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

/**
 * Utility class for normalizing JSON arguments from different model formats. Different models
 * (Mistral, Granite, etc.) format JSON tool call arguments differently, and this utility ensures
 * consistent parsing across all formats.
 *
 * @author Tristan Mahinay
 * @since 1.0.2
 */
public final class JsonArgumentsNormalizer {

  private JsonArgumentsNormalizer() {
    throw new IllegalStateException("JsonArgumentsNormalizer is an utility class");
  }

  /**
   * Normalizes JSON arguments from different model formats.
   *
   * @param jsonArgs the JSON arguments string
   * @return normalized JSON string
   */
  public static String normalize(String jsonArgs) {
    if (jsonArgs == null || jsonArgs.isEmpty()) {
      return jsonArgs;
    }

    String normalized = jsonArgs.trim();

    // Check if this is a double-encoded JSON string (starts and ends with quotes)
    // Example: "\"{\\n  \\\"location\\\": \\\"Boston\\\"\\n}\""
    if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 2) {
      // Remove outer quotes
      normalized = normalized.substring(1, normalized.length() - 1);

      // Only unescape if it looks like escaped JSON (contains \\")
      if (normalized.contains("\\\"") || normalized.contains("\\n")) {
        // Unescape the JSON in the correct order to avoid double-unescaping
        // Must replace \\\\ first, then other escape sequences
        normalized =
            normalized
                .replace("\\\\", "\u0000") // Temporary placeholder for escaped backslash
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\u0000", "\\"); // Restore escaped backslash
      }
    }

    // Remove carriage returns and normalize line breaks
    // This handles Granite's pretty-printed format with \r\n
    normalized = normalized.replace("\r\n", "\n").replace("\r", "\n");

    return normalized;
  }
}
