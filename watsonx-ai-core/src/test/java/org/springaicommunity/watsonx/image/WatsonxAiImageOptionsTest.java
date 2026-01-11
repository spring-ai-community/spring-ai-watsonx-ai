/*
 * Copyright 2025 the original author or authors.
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

package org.springaicommunity.watsonx.image;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WatsonxAiImageOptions}.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
class WatsonxAiImageOptionsTest {

  @Test
  void testDefaultConstructor() {
    WatsonxAiImageOptions options = new WatsonxAiImageOptions();
    assertNotNull(options);
  }

  @Test
  void testBuilderWithAllOptions() {
    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(1024)
            .height(1024)
            .build();

    assertEquals("meta-llama/llama-3-2-11b-vision-instruct", options.getModel());
    assertEquals(1024, options.getWidth());
    assertEquals(1024, options.getHeight());
  }

  @Test
  void testBuilderWithMinimalOptions() {
    WatsonxAiImageOptions options = WatsonxAiImageOptions.builder().model("test-model").build();

    assertEquals("test-model", options.getModel());
    assertNull(options.getWidth());
    assertNull(options.getHeight());
  }

  @Test
  void testSettersAndGetters() {
    WatsonxAiImageOptions options = new WatsonxAiImageOptions();

    options.setModel("custom-model");
    options.setWidth(512);
    options.setHeight(768);

    assertEquals("custom-model", options.getModel());
    assertEquals(512, options.getWidth());
    assertEquals(768, options.getHeight());
  }

  @Test
  void testToBuilder() {
    WatsonxAiImageOptions original =
        WatsonxAiImageOptions.builder().model("original-model").width(1024).height(1024).build();

    WatsonxAiImageOptions copy = original.toBuilder().build();

    assertEquals(original.getModel(), copy.getModel());
    assertEquals(original.getWidth(), copy.getWidth());
    assertEquals(original.getHeight(), copy.getHeight());
  }

  @Test
  void testToBuilderWithModifications() {
    WatsonxAiImageOptions original =
        WatsonxAiImageOptions.builder().model("original-model").width(1024).height(1024).build();

    WatsonxAiImageOptions modified =
        original.toBuilder().model("modified-model").width(512).height(512).build();

    assertEquals("modified-model", modified.getModel());
    assertEquals(512, modified.getWidth());
    assertEquals(512, modified.getHeight());

    // Original should remain unchanged
    assertEquals("original-model", original.getModel());
    assertEquals(1024, original.getWidth());
  }

  @Test
  void testFromOptions() {
    WatsonxAiImageOptions original =
        WatsonxAiImageOptions.builder().model("test-model").width(768).height(768).build();

    WatsonxAiImageOptions copy = WatsonxAiImageOptions.fromOptions(original);

    assertEquals(original.getModel(), copy.getModel());
    assertEquals(original.getWidth(), copy.getWidth());
    assertEquals(original.getHeight(), copy.getHeight());

    // Verify they are different instances
    assertNotSame(original, copy);
  }

  @Test
  void testBuilderChaining() {
    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("model1")
            .model("model2") // Should override
            .width(512)
            .width(1024) // Should override
            .build();

    assertEquals("model2", options.getModel());
    assertEquals(1024, options.getWidth());
  }

  @Test
  void testWithNullValues() {
    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder().model(null).width(null).height(null).build();

    assertNull(options.getModel());
    assertNull(options.getWidth());
    assertNull(options.getHeight());
  }

  @Test
  void testDifferentImageSizes() {
    // Test square images
    WatsonxAiImageOptions square = WatsonxAiImageOptions.builder().width(1024).height(1024).build();
    assertEquals(1024, square.getWidth());
    assertEquals(1024, square.getHeight());

    // Test landscape
    WatsonxAiImageOptions landscape =
        WatsonxAiImageOptions.builder().width(1024).height(768).build();
    assertEquals(1024, landscape.getWidth());
    assertEquals(768, landscape.getHeight());

    // Test portrait
    WatsonxAiImageOptions portrait =
        WatsonxAiImageOptions.builder().width(768).height(1024).build();
    assertEquals(768, portrait.getWidth());
    assertEquals(1024, portrait.getHeight());
  }

  @Test
  void testModelNames() {
    String[] models = {
      "meta-llama/llama-3-2-11b-vision-instruct", "custom-model-1", "custom-model-2", "test-model"
    };

    for (String model : models) {
      WatsonxAiImageOptions options = WatsonxAiImageOptions.builder().model(model).build();
      assertEquals(model, options.getModel());
    }
  }

  @Test
  void testCompleteConfiguration() {
    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(1024)
            .height(768)
            .build();

    assertNotNull(options);
    assertAll(
        "All options should be set correctly",
        () -> assertEquals("meta-llama/llama-3-2-11b-vision-instruct", options.getModel()),
        () -> assertEquals(1024, options.getWidth()),
        () -> assertEquals(768, options.getHeight()));
  }

  @Test
  void testEmptyBuilder() {
    WatsonxAiImageOptions options = WatsonxAiImageOptions.builder().build();

    assertNotNull(options);
    assertNull(options.getModel());
    assertNull(options.getWidth());
    assertNull(options.getHeight());
  }

  @Test
  void testBuilderWithModerations() {
    WatsonxAiImageRequest.TextModeration textModeration =
        new WatsonxAiImageRequest.TextModeration(true, 0.5);
    WatsonxAiImageRequest.MaskProperties maskProperties =
        new WatsonxAiImageRequest.MaskProperties(true);
    WatsonxAiImageRequest.ModerationsInputProperties hapProperties =
        new WatsonxAiImageRequest.ModerationsInputProperties(textModeration, maskProperties);
    WatsonxAiImageRequest.ModerationsInput moderations =
        WatsonxAiImageRequest.ModerationsInput.builder().hap(hapProperties).build();

    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("test-model")
            .width(1024)
            .height(768)
            .moderations(moderations)
            .build();

    assertEquals("test-model", options.getModel());
    assertEquals(1024, options.getWidth());
    assertEquals(768, options.getHeight());
    assertNotNull(options.getModerations());
    assertEquals(moderations, options.getModerations());
  }

  @Test
  void testModerationsSetterAndGetter() {
    WatsonxAiImageOptions options = new WatsonxAiImageOptions();

    WatsonxAiImageRequest.TextModeration textModeration =
        new WatsonxAiImageRequest.TextModeration(true, 0.7);
    WatsonxAiImageRequest.ModerationsInputProperties piiProperties =
        new WatsonxAiImageRequest.ModerationsInputProperties(textModeration, null);
    WatsonxAiImageRequest.ModerationsInput moderations =
        WatsonxAiImageRequest.ModerationsInput.builder().pii(piiProperties).build();

    options.setModerations(moderations);

    assertNotNull(options.getModerations());
    assertEquals(moderations, options.getModerations());
  }

  @Test
  void testToBuilderWithModerations() {
    WatsonxAiImageRequest.ModerationsInput moderations =
        WatsonxAiImageRequest.ModerationsInput.builder()
            .hap(
                new WatsonxAiImageRequest.ModerationsInputProperties(
                    new WatsonxAiImageRequest.TextModeration(true, 0.5),
                    new WatsonxAiImageRequest.MaskProperties(true)))
            .build();

    WatsonxAiImageOptions original =
        WatsonxAiImageOptions.builder()
            .model("original-model")
            .width(1024)
            .height(1024)
            .moderations(moderations)
            .build();

    WatsonxAiImageOptions copy = original.toBuilder().build();

    assertEquals(original.getModel(), copy.getModel());
    assertEquals(original.getWidth(), copy.getWidth());
    assertEquals(original.getHeight(), copy.getHeight());
    assertEquals(original.getModerations(), copy.getModerations());
  }

  @Test
  void testFromOptionsWithModerations() {
    WatsonxAiImageRequest.ModerationsInput moderations =
        WatsonxAiImageRequest.ModerationsInput.builder()
            .hap(
                new WatsonxAiImageRequest.ModerationsInputProperties(
                    new WatsonxAiImageRequest.TextModeration(false, 0.3), null))
            .pii(
                new WatsonxAiImageRequest.ModerationsInputProperties(
                    new WatsonxAiImageRequest.TextModeration(true, 0.8),
                    new WatsonxAiImageRequest.MaskProperties(false)))
            .build();

    WatsonxAiImageOptions original =
        WatsonxAiImageOptions.builder()
            .model("test-model")
            .width(768)
            .height(768)
            .moderations(moderations)
            .build();

    WatsonxAiImageOptions copy = WatsonxAiImageOptions.fromOptions(original);

    assertEquals(original.getModel(), copy.getModel());
    assertEquals(original.getWidth(), copy.getWidth());
    assertEquals(original.getHeight(), copy.getHeight());
    assertEquals(original.getModerations(), copy.getModerations());
    assertNotSame(original, copy);
  }

  @Test
  void testModerationsWithBuilderPattern() {
    WatsonxAiImageRequest.ModerationsInput moderations =
        WatsonxAiImageRequest.ModerationsInput.builder()
            .hap(
                new WatsonxAiImageRequest.ModerationsInputProperties(
                    new WatsonxAiImageRequest.TextModeration(true, 0.6),
                    new WatsonxAiImageRequest.MaskProperties(true)))
            .pii(
                new WatsonxAiImageRequest.ModerationsInputProperties(
                    new WatsonxAiImageRequest.TextModeration(true, 0.9), null))
            .build();

    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(1024)
            .height(1024)
            .moderations(moderations)
            .build();

    assertNotNull(options.getModerations());
    assertNotNull(options.getModerations().hap());
    assertNotNull(options.getModerations().pii());
    assertTrue(options.getModerations().hap().input().enabled());
    assertEquals(0.6, options.getModerations().hap().input().threshold());
  }

  @Test
  void testNullModerations() {
    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("test-model")
            .width(512)
            .height(512)
            .moderations(null)
            .build();

    assertNull(options.getModerations());
  }
}
