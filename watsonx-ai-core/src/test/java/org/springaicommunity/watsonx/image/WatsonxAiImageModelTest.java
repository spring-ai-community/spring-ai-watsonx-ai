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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;

/**
 * JUnit 5 test class for WatsonxAiImageModel functionality. Tests image generation operations using
 * mocking for external dependencies.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
class WatsonxAiImageModelTest {

  @Mock private WatsonxAiImageApi watsonxAiImageApi;

  @Mock private RetryTemplate retryTemplate;

  private WatsonxAiImageModel imageModel;
  private WatsonxAiImageOptions defaultOptions;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    defaultOptions =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(1024)
            .height(1024)
            .build();

    imageModel = new WatsonxAiImageModel(watsonxAiImageApi, defaultOptions, retryTemplate);

    // Mock retry template to execute directly without retry logic
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              org.springframework.retry.RetryCallback<Object, Exception> callback =
                  (org.springframework.retry.RetryCallback<Object, Exception>)
                      invocation.getArgument(0);
              return callback.doWithRetry(null);
            })
        .when(retryTemplate)
        .execute(any());
  }

  @Nested
  class ConstructorTests {

    @Test
    void constructorWithValidParameters() {
      assertNotNull(imageModel);
      assertEquals(defaultOptions.getModel(), imageModel.getDefaultOptions().getModel());
    }

    @Test
    void constructorWithNullApiThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WatsonxAiImageModel(null, defaultOptions, retryTemplate),
          "WatsonxAiImageApi must not be null");
    }

    @Test
    void constructorWithNullOptionsThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WatsonxAiImageModel(watsonxAiImageApi, null, retryTemplate),
          "WatsonxAiImageOptions must not be null");
    }

    @Test
    void constructorWithNullRetryTemplateThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WatsonxAiImageModel(watsonxAiImageApi, defaultOptions, null),
          "RetryTemplate must not be null");
    }
  }

  @Nested
  class CallMethodTests {

    @Test
    void callWithValidPrompt() {
      ImagePrompt prompt = new ImagePrompt("A beautiful sunset over mountains");

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult(
              "base64encodedimage123", 42L, "A stunning sunset over majestic mountains");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse(
              "meta-llama/llama-3-2-11b-vision-instruct", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertEquals(1, response.getResults().size());
      assertEquals("base64encodedimage123", response.getResult().getOutput().getUrl());

      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }

    @Test
    void callWithNullPromptThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> imageModel.call(null),
          "ImagePrompt must not be null");
    }

    @Test
    void callWithEmptyInstructionsThrowsException() {
      ImagePrompt prompt = new ImagePrompt(List.of());

      assertThrows(
          IllegalArgumentException.class,
          () -> imageModel.call(prompt),
          "ImagePrompt instructions must not be empty");
    }

    @Test
    void callWithMultipleImages() {
      WatsonxAiImageOptions options =
          WatsonxAiImageOptions.builder()
              .model("meta-llama/llama-3-2-11b-vision-instruct")
              .width(512)
              .height(512)
              .build();

      ImagePrompt prompt = new ImagePrompt("A futuristic city", options);

      WatsonxAiImageResponse.ImageResult result1 =
          new WatsonxAiImageResponse.ImageResult("image1base64", 100L, "Futuristic cityscape 1");
      WatsonxAiImageResponse.ImageResult result2 =
          new WatsonxAiImageResponse.ImageResult("image2base64", 101L, "Futuristic cityscape 2");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse(
              "meta-llama/llama-3-2-11b-vision-instruct",
              LocalDateTime.now(),
              List.of(result1, result2));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertEquals(2, response.getResults().size());
      assertEquals("image1base64", response.getResults().get(0).getOutput().getUrl());
      assertEquals("image2base64", response.getResults().get(1).getOutput().getUrl());

      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }

    @Test
    void callWithCustomOptions() {
      WatsonxAiImageOptions customOptions =
          WatsonxAiImageOptions.builder().model("custom-model").width(768).height(768).build();

      ImagePrompt prompt = new ImagePrompt("A serene lake", customOptions);

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult("customimagedata", 200L, "A peaceful serene lake");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse("custom-model", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertEquals(1, response.getResults().size());
      assertEquals("customimagedata", response.getResult().getOutput().getUrl());

      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }
  }

  @Nested
  class OptionsMergingTests {

    @Test
    void mergeOptionsWithRuntimeModel() {
      WatsonxAiImageOptions runtimeOptions =
          WatsonxAiImageOptions.builder().model("runtime-model").build();

      ImagePrompt prompt = new ImagePrompt("Test prompt", runtimeOptions);

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult("imagedata", 1L, "Test image");
      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse("runtime-model", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }

    @Test
    void mergeOptionsWithAllParameters() {
      WatsonxAiImageOptions runtimeOptions =
          WatsonxAiImageOptions.builder().model("test-model").width(512).height(512).build();

      ImagePrompt prompt = new ImagePrompt("Complex prompt", runtimeOptions);

      WatsonxAiImageResponse.ImageResult result1 =
          new WatsonxAiImageResponse.ImageResult("img1", 10L, "Image 1");
      WatsonxAiImageResponse.ImageResult result2 =
          new WatsonxAiImageResponse.ImageResult("img2", 11L, "Image 2");
      WatsonxAiImageResponse.ImageResult result3 =
          new WatsonxAiImageResponse.ImageResult("img3", 12L, "Image 3");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse(
              "test-model", LocalDateTime.now(), List.of(result1, result2, result3));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertEquals(3, response.getResults().size());

      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }
  }

  @Nested
  class ResponseHandlingTests {

    @Test
    void handleNullResponse() {
      ImagePrompt prompt = new ImagePrompt("Test prompt");

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(null));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertTrue(response.getResults().isEmpty());
    }

    @Test
    void handleResponseWithNullResults() {
      ImagePrompt prompt = new ImagePrompt("Test prompt");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse("test-model", LocalDateTime.now(), null);

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertTrue(response.getResults().isEmpty());
    }

    @Test
    void handleResponseWithEmptyResults() {
      ImagePrompt prompt = new ImagePrompt("Test prompt");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse("test-model", LocalDateTime.now(), List.of());

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertTrue(response.getResults().isEmpty());
    }

    @Test
    void handleResponseWithRevisedPrompt() {
      ImagePrompt prompt = new ImagePrompt("Original prompt");

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult(
              "imagedata", 50L, "Revised prompt with more detail");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse(
              "meta-llama/llama-3-2-11b-vision-instruct", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertEquals("Revised prompt with more detail", response.getResult().getOutput().getUrl());
    }
  }

  @Nested
  class DefaultOptionsTests {

    @Test
    void getDefaultOptionsReturnsCorrectOptions() {
      WatsonxAiImageOptions retrievedOptions = imageModel.getDefaultOptions();

      assertAll(
          "Default options validation",
          () ->
              assertEquals("meta-llama/llama-3-2-11b-vision-instruct", retrievedOptions.getModel()),
          () -> assertEquals(1024, retrievedOptions.getWidth()),
          () -> assertEquals(1024, retrievedOptions.getHeight()));
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void builderCreatesValidModel() {
      WatsonxAiImageModel builtModel =
          WatsonxAiImageModel.builder()
              .watsonxAiImageApi(watsonxAiImageApi)
              .options(defaultOptions)
              .retryTemplate(retryTemplate)
              .build();

      assertNotNull(builtModel);
      assertEquals(defaultOptions.getModel(), builtModel.getDefaultOptions().getModel());
    }

    @Test
    void builderWithMinimalConfiguration() {
      WatsonxAiImageOptions minimalOptions = WatsonxAiImageOptions.builder().build();

      WatsonxAiImageModel builtModel =
          WatsonxAiImageModel.builder()
              .watsonxAiImageApi(watsonxAiImageApi)
              .options(minimalOptions)
              .retryTemplate(retryTemplate)
              .build();

      assertNotNull(builtModel);
    }
  }

  @Nested
  class ModerationTests {

    @Test
    void callWithModerationOptions() {
      WatsonxAiImageRequest.TextModeration textModeration =
          new WatsonxAiImageRequest.TextModeration(true, 0.5);
      WatsonxAiImageRequest.MaskProperties maskProperties =
          new WatsonxAiImageRequest.MaskProperties(true);
      WatsonxAiImageRequest.ModerationsInputProperties hapProperties =
          new WatsonxAiImageRequest.ModerationsInputProperties(textModeration, maskProperties);
      WatsonxAiImageRequest.ModerationsInput moderations =
          WatsonxAiImageRequest.ModerationsInput.builder().hap(hapProperties).build();

      WatsonxAiImageOptions optionsWithModeration =
          WatsonxAiImageOptions.builder()
              .model("meta-llama/llama-3-2-11b-vision-instruct")
              .width(1024)
              .height(768)
              .moderations(moderations)
              .build();

      ImagePrompt prompt = new ImagePrompt("A safe image", optionsWithModeration);

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult("moderatedimage", 123L, "Safe moderated image");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse(
              "meta-llama/llama-3-2-11b-vision-instruct", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertEquals(1, response.getResults().size());
      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }

    @Test
    void callWithBothHapAndPiiModerations() {
      WatsonxAiImageRequest.ModerationsInput moderations =
          WatsonxAiImageRequest.ModerationsInput.builder()
              .hap(
                  new WatsonxAiImageRequest.ModerationsInputProperties(
                      new WatsonxAiImageRequest.TextModeration(true, 0.6),
                      new WatsonxAiImageRequest.MaskProperties(true)))
              .pii(
                  new WatsonxAiImageRequest.ModerationsInputProperties(
                      new WatsonxAiImageRequest.TextModeration(true, 0.8),
                      new WatsonxAiImageRequest.MaskProperties(false)))
              .build();

      WatsonxAiImageOptions optionsWithModeration =
          WatsonxAiImageOptions.builder()
              .model("test-model")
              .width(512)
              .height(512)
              .moderations(moderations)
              .build();

      ImagePrompt prompt = new ImagePrompt("Content with moderation", optionsWithModeration);

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult(
              "doublemoderated", 456L, "Content filtered by HAP and PII");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse("test-model", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertEquals(1, response.getResults().size());
      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }

    @Test
    void callWithDefaultOptionsHavingModerations() {
      WatsonxAiImageRequest.ModerationsInput moderations =
          WatsonxAiImageRequest.ModerationsInput.builder()
              .hap(
                  new WatsonxAiImageRequest.ModerationsInputProperties(
                      new WatsonxAiImageRequest.TextModeration(false, 0.3), null))
              .build();

      WatsonxAiImageOptions defaultOptionsWithMod =
          WatsonxAiImageOptions.builder()
              .model("meta-llama/llama-3-2-11b-vision-instruct")
              .width(1024)
              .height(1024)
              .moderations(moderations)
              .build();

      WatsonxAiImageModel modelWithModeration =
          new WatsonxAiImageModel(watsonxAiImageApi, defaultOptionsWithMod, retryTemplate);

      ImagePrompt prompt = new ImagePrompt("Default moderation test");

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult("defaultmodimage", 789L, "Default moderation");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse(
              "meta-llama/llama-3-2-11b-vision-instruct", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = modelWithModeration.call(prompt);

      assertNotNull(response);
      assertEquals(1, response.getResults().size());
      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }

    @Test
    void callWithRuntimeModerationOverridesDefault() {
      // Set up default options with moderation
      WatsonxAiImageRequest.ModerationsInput defaultModerations =
          WatsonxAiImageRequest.ModerationsInput.builder()
              .hap(
                  new WatsonxAiImageRequest.ModerationsInputProperties(
                      new WatsonxAiImageRequest.TextModeration(true, 0.5), null))
              .build();

      WatsonxAiImageOptions defaultOptionsWithMod =
          WatsonxAiImageOptions.builder()
              .model("default-model")
              .width(1024)
              .height(1024)
              .moderations(defaultModerations)
              .build();

      WatsonxAiImageModel modelWithModeration =
          new WatsonxAiImageModel(watsonxAiImageApi, defaultOptionsWithMod, retryTemplate);

      // Runtime options with different moderation
      WatsonxAiImageRequest.ModerationsInput runtimeModerations =
          WatsonxAiImageRequest.ModerationsInput.builder()
              .pii(
                  new WatsonxAiImageRequest.ModerationsInputProperties(
                      new WatsonxAiImageRequest.TextModeration(true, 0.9),
                      new WatsonxAiImageRequest.MaskProperties(true)))
              .build();

      WatsonxAiImageOptions runtimeOptions =
          WatsonxAiImageOptions.builder().moderations(runtimeModerations).build();

      ImagePrompt prompt = new ImagePrompt("Runtime override test", runtimeOptions);

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult("overrideimage", 999L, "Override moderation");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse("default-model", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = modelWithModeration.call(prompt);

      assertNotNull(response);
      assertEquals(1, response.getResults().size());
      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }

    @Test
    void callWithNullModerations() {
      WatsonxAiImageOptions optionsWithoutModeration =
          WatsonxAiImageOptions.builder()
              .model("test-model")
              .width(512)
              .height(512)
              .moderations(null)
              .build();

      ImagePrompt prompt = new ImagePrompt("No moderation", optionsWithoutModeration);

      WatsonxAiImageResponse.ImageResult result =
          new WatsonxAiImageResponse.ImageResult("nomodimage", 111L, "No moderation applied");

      WatsonxAiImageResponse mockResponse =
          new WatsonxAiImageResponse("test-model", LocalDateTime.now(), List.of(result));

      when(watsonxAiImageApi.generateImage(any(WatsonxAiImageRequest.class)))
          .thenReturn(ResponseEntity.ok(mockResponse));

      ImageResponse response = imageModel.call(prompt);

      assertNotNull(response);
      assertEquals(1, response.getResults().size());
      verify(watsonxAiImageApi, times(1)).generateImage(any(WatsonxAiImageRequest.class));
    }
  }
}
