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
            .model("sdxl/stable-diffusion-xl-v1-0")
            .n(2)
            .width(1024)
            .height(1024)
            .responseFormat("b64_json")
            .style("photographic")
            .build();

    assertEquals("sdxl/stable-diffusion-xl-v1-0", options.getModel());
    assertEquals(2, options.getN());
    assertEquals(1024, options.getWidth());
    assertEquals(1024, options.getHeight());
    assertEquals("b64_json", options.getResponseFormat());
    assertEquals("photographic", options.getStyle());
  }

  @Test
  void testBuilderWithMinimalOptions() {
    WatsonxAiImageOptions options = WatsonxAiImageOptions.builder().model("test-model").build();

    assertEquals("test-model", options.getModel());
    assertNull(options.getN());
    assertNull(options.getWidth());
    assertNull(options.getHeight());
    assertNull(options.getResponseFormat());
    assertNull(options.getStyle());
  }

  @Test
  void testSettersAndGetters() {
    WatsonxAiImageOptions options = new WatsonxAiImageOptions();

    options.setModel("custom-model");
    options.setN(3);
    options.setWidth(512);
    options.setHeight(768);
    options.setResponseFormat("url");
    options.setStyle("artistic");

    assertEquals("custom-model", options.getModel());
    assertEquals(3, options.getN());
    assertEquals(512, options.getWidth());
    assertEquals(768, options.getHeight());
    assertEquals("url", options.getResponseFormat());
    assertEquals("artistic", options.getStyle());
  }

  @Test
  void testToBuilder() {
    WatsonxAiImageOptions original =
        WatsonxAiImageOptions.builder()
            .model("original-model")
            .n(1)
            .width(1024)
            .height(1024)
            .responseFormat("b64_json")
            .style("photographic")
            .build();

    WatsonxAiImageOptions copy = original.toBuilder().build();

    assertEquals(original.getModel(), copy.getModel());
    assertEquals(original.getN(), copy.getN());
    assertEquals(original.getWidth(), copy.getWidth());
    assertEquals(original.getHeight(), copy.getHeight());
    assertEquals(original.getResponseFormat(), copy.getResponseFormat());
    assertEquals(original.getStyle(), copy.getStyle());
  }

  @Test
  void testToBuilderWithModifications() {
    WatsonxAiImageOptions original =
        WatsonxAiImageOptions.builder()
            .model("original-model")
            .n(1)
            .width(1024)
            .height(1024)
            .build();

    WatsonxAiImageOptions modified =
        original.toBuilder().model("modified-model").n(2).width(512).height(512).build();

    assertEquals("modified-model", modified.getModel());
    assertEquals(2, modified.getN());
    assertEquals(512, modified.getWidth());
    assertEquals(512, modified.getHeight());

    // Original should remain unchanged
    assertEquals("original-model", original.getModel());
    assertEquals(1, original.getN());
  }

  @Test
  void testFromOptions() {
    WatsonxAiImageOptions original =
        WatsonxAiImageOptions.builder()
            .model("test-model")
            .n(2)
            .width(768)
            .height(768)
            .responseFormat("url")
            .style("cinematic")
            .build();

    WatsonxAiImageOptions copy = WatsonxAiImageOptions.fromOptions(original);

    assertEquals(original.getModel(), copy.getModel());
    assertEquals(original.getN(), copy.getN());
    assertEquals(original.getWidth(), copy.getWidth());
    assertEquals(original.getHeight(), copy.getHeight());
    assertEquals(original.getResponseFormat(), copy.getResponseFormat());
    assertEquals(original.getStyle(), copy.getStyle());

    // Verify they are different instances
    assertNotSame(original, copy);
  }

  @Test
  void testBuilderChaining() {
    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("model1")
            .model("model2") // Should override
            .n(1)
            .n(3) // Should override
            .width(512)
            .width(1024) // Should override
            .build();

    assertEquals("model2", options.getModel());
    assertEquals(3, options.getN());
    assertEquals(1024, options.getWidth());
  }

  @Test
  void testWithNullValues() {
    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model(null)
            .n(null)
            .width(null)
            .height(null)
            .responseFormat(null)
            .style(null)
            .build();

    assertNull(options.getModel());
    assertNull(options.getN());
    assertNull(options.getWidth());
    assertNull(options.getHeight());
    assertNull(options.getResponseFormat());
    assertNull(options.getStyle());
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
  void testMultipleImageGeneration() {
    WatsonxAiImageOptions options = WatsonxAiImageOptions.builder().n(5).build();
    assertEquals(5, options.getN());

    options = WatsonxAiImageOptions.builder().n(1).build();
    assertEquals(1, options.getN());
  }

  @Test
  void testResponseFormats() {
    WatsonxAiImageOptions b64 = WatsonxAiImageOptions.builder().responseFormat("b64_json").build();
    assertEquals("b64_json", b64.getResponseFormat());

    WatsonxAiImageOptions url = WatsonxAiImageOptions.builder().responseFormat("url").build();
    assertEquals("url", url.getResponseFormat());
  }

  @Test
  void testStylePresets() {
    String[] styles = {"photographic", "artistic", "cinematic", "anime", "digital-art"};

    for (String style : styles) {
      WatsonxAiImageOptions options = WatsonxAiImageOptions.builder().style(style).build();
      assertEquals(style, options.getStyle());
    }
  }

  @Test
  void testModelNames() {
    String[] models = {
      "sdxl/stable-diffusion-xl-v1-0", "custom-model-1", "custom-model-2", "test-model"
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
            .model("sdxl/stable-diffusion-xl-v1-0")
            .n(4)
            .width(1024)
            .height(768)
            .responseFormat("b64_json")
            .style("photographic")
            .build();

    assertNotNull(options);
    assertAll(
        "All options should be set correctly",
        () -> assertEquals("sdxl/stable-diffusion-xl-v1-0", options.getModel()),
        () -> assertEquals(4, options.getN()),
        () -> assertEquals(1024, options.getWidth()),
        () -> assertEquals(768, options.getHeight()),
        () -> assertEquals("b64_json", options.getResponseFormat()),
        () -> assertEquals("photographic", options.getStyle()));
  }

  @Test
  void testEmptyBuilder() {
    WatsonxAiImageOptions options = WatsonxAiImageOptions.builder().build();

    assertNotNull(options);
    assertNull(options.getModel());
    assertNull(options.getN());
    assertNull(options.getWidth());
    assertNull(options.getHeight());
    assertNull(options.getResponseFormat());
    assertNull(options.getStyle());
  }
}
