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

package org.springaicommunity.watsonx.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 test class for WatsonxAiModerationOptions functionality and configuration. Tests the
 * moderation options builder pattern and configuration validation.
 *
 * @author Tristan Mahinay
 * @since 1.0.2
 */
class WatsonxAiModerationOptionsTest {

  @Nested
  class BuilderTests {

    @Test
    void optionsBuilderWithThreshold() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().hap(0.8f).pii(0.9f).graniteGuardian(0.6f).build();

      assertNotNull(options);
      assertNotNull(options.getHap());
      assertEquals(0.8f, options.getHap().threshold());
      assertNotNull(options.getPii());
      assertEquals(0.9f, options.getPii().threshold());
      assertNotNull(options.getGraniteGuardian());
      assertEquals(0.6f, options.getGraniteGuardian().threshold());
    }

    @Test
    void detectorConfigEnabled() {
      WatsonxAiModerationRequest.DetectorConfig config =
          WatsonxAiModerationRequest.DetectorConfig.enabled();

      assertNotNull(config);
      assertNull(config.threshold());
    }

    @Test
    void detectorConfigWithThreshold() {
      WatsonxAiModerationRequest.DetectorConfig config =
          WatsonxAiModerationRequest.DetectorConfig.of(0.85f);

      assertNotNull(config);
      assertEquals(0.85f, config.threshold());
    }

    @Test
    void toDetectorsConversion() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().hap(0.75f).pii(0.85f).graniteGuardian(0.6f).build();

      WatsonxAiModerationRequest.Detectors detectors = options.toDetectors();

      assertNotNull(detectors);
      assertNotNull(detectors.hap());
      assertNotNull(detectors.pii());
      assertNotNull(detectors.graniteGuardian());
    }
  }

  @Nested
  class CopyMethodTests {

    @Test
    void copyCreatesSeparateInstance() {
      WatsonxAiModerationOptions original =
          WatsonxAiModerationOptions.builder()
              .model("granite_guardian")
              .hap(0.75f)
              .pii(0.85f)
              .graniteGuardian(0.6f)
              .build();

      WatsonxAiModerationOptions copy = original.copy();

      assertNotNull(copy);
      assertEquals(original.getModel(), copy.getModel());
      assertEquals(original.getHap(), copy.getHap());
      assertEquals(original.getPii(), copy.getPii());
      assertEquals(original.getGraniteGuardian(), copy.getGraniteGuardian());
      assertNotEquals(System.identityHashCode(original), System.identityHashCode(copy));
    }

    @Test
    void copyWithNullFieldsHandledCorrectly() {
      WatsonxAiModerationOptions original =
          WatsonxAiModerationOptions.builder().model("test-model").build();

      WatsonxAiModerationOptions copy = original.copy();

      assertNotNull(copy);
      assertEquals(original.getModel(), copy.getModel());
      assertNull(copy.getHap());
      assertNull(copy.getPii());
      assertNull(copy.getGraniteGuardian());
    }
  }

  @Nested
  class ToStringMethodTests {

    @Test
    void toStringReturnsJsonRepresentation() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder()
              .model("granite_guardian")
              .hap(0.75f)
              .pii(0.85f)
              .build();

      String result = options.toString();

      assertNotNull(result);
      assertEquals(true, result.startsWith("WatsonxAiModerationOptions: "));
    }

    @Test
    void toStringWithMinimalOptions() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().model("test-model").build();

      String result = options.toString();

      assertNotNull(result);
      assertEquals(true, result.startsWith("WatsonxAiModerationOptions: "));
    }
  }

  @Nested
  class EqualsMethodTests {

    @Test
    void equalsReturnsTrueForSameInstance() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().model("test-model").hap(0.75f).build();

      assertEquals(options, options);
    }

    @Test
    void equalsReturnsTrueForEqualOptions() {
      WatsonxAiModerationOptions options1 =
          WatsonxAiModerationOptions.builder()
              .model("granite_guardian")
              .hap(0.75f)
              .pii(0.85f)
              .build();

      WatsonxAiModerationOptions options2 =
          WatsonxAiModerationOptions.builder()
              .model("granite_guardian")
              .hap(0.75f)
              .pii(0.85f)
              .build();

      assertEquals(options1, options2);
    }

    @Test
    void equalsReturnsFalseForDifferentModel() {
      WatsonxAiModerationOptions options1 =
          WatsonxAiModerationOptions.builder().model("model1").hap(0.75f).build();

      WatsonxAiModerationOptions options2 =
          WatsonxAiModerationOptions.builder().model("model2").hap(0.75f).build();

      assertNotEquals(options1, options2);
    }

    @Test
    void equalsReturnsFalseForDifferentHap() {
      WatsonxAiModerationOptions options1 =
          WatsonxAiModerationOptions.builder().model("test-model").hap(0.75f).build();

      WatsonxAiModerationOptions options2 =
          WatsonxAiModerationOptions.builder().model("test-model").hap(0.85f).build();

      assertNotEquals(options1, options2);
    }

    @Test
    void equalsReturnsFalseForNull() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().model("test-model").build();

      assertNotEquals(options, null);
    }

    @Test
    void equalsReturnsFalseForDifferentClass() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().model("test-model").build();

      assertNotEquals(options, "string");
    }

    @Test
    void equalsHandlesNullFields() {
      WatsonxAiModerationOptions options1 =
          WatsonxAiModerationOptions.builder().model("test-model").build();

      WatsonxAiModerationOptions options2 =
          WatsonxAiModerationOptions.builder().model("test-model").build();

      assertEquals(options1, options2);
    }
  }

  @Nested
  class HashCodeMethodTests {

    @Test
    void hashCodeConsistentForSameInstance() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().model("test-model").hap(0.75f).pii(0.85f).build();

      int hashCode1 = options.hashCode();
      int hashCode2 = options.hashCode();

      assertEquals(hashCode1, hashCode2);
    }

    @Test
    void hashCodeSameForEqualOptions() {
      WatsonxAiModerationOptions options1 =
          WatsonxAiModerationOptions.builder()
              .model("granite_guardian")
              .hap(0.75f)
              .pii(0.85f)
              .build();

      WatsonxAiModerationOptions options2 =
          WatsonxAiModerationOptions.builder()
              .model("granite_guardian")
              .hap(0.75f)
              .pii(0.85f)
              .build();

      assertEquals(options1.hashCode(), options2.hashCode());
    }

    @Test
    void hashCodeDifferentForDifferentOptions() {
      WatsonxAiModerationOptions options1 =
          WatsonxAiModerationOptions.builder().model("model1").hap(0.75f).build();

      WatsonxAiModerationOptions options2 =
          WatsonxAiModerationOptions.builder().model("model2").hap(0.75f).build();

      assertNotEquals(options1.hashCode(), options2.hashCode());
    }

    @Test
    void hashCodeHandlesNullFields() {
      WatsonxAiModerationOptions options =
          WatsonxAiModerationOptions.builder().model("test-model").build();

      assertNotNull(options.hashCode());
    }
  }
}
