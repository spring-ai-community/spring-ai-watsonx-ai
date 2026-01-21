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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * Integration test class for WatsonxAiImageModel using Spring AI ImageModel APIs. This test
 * demonstrates integration with Spring AI's high-level ImagePrompt and ImageResponse APIs.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiImageModelIT {

  private RestClient.Builder restClientBuilder;

  private MockRestServiceServer mockServer;

  private WatsonxAiImageApi watsonxAiImageApi;

  private WatsonxAiImageModel imageModel;

  private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";
  private static final String IMAGE_ENDPOINT = "/ml/v1/text/image";
  private static final String VERSION = "2024-10-17";
  private static final String PROJECT_ID = "test-project-id";
  private static final String API_KEY = "test-api-key";

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    ResponseErrorHandler errorHandler =
        response -> {
          HttpStatus.Series series = HttpStatus.Series.resolve(response.getStatusCode().value());
          return (series == HttpStatus.Series.CLIENT_ERROR
              || series == HttpStatus.Series.SERVER_ERROR);
        };

    watsonxAiImageApi =
        new WatsonxAiImageApi(
            BASE_URL,
            IMAGE_ENDPOINT,
            VERSION,
            PROJECT_ID,
            API_KEY,
            restClientBuilder,
            errorHandler);

    WatsonxAiImageOptions defaultOptions =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(1024)
            .height(1024)
            .build();

    imageModel =
        new WatsonxAiImageModel(
            watsonxAiImageApi, defaultOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
  }

  @Test
  void imageGenerationWithSinglePromptTest() {
    String jsonResponse =
        """
        {
          "model_id": "meta-llama/llama-3-2-11b-vision-instruct",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "image": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
              "seed": 42,
              "revised_prompt": "A beautiful sunset over mountains with vibrant colors"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + IMAGE_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    ImagePrompt prompt = new ImagePrompt("A beautiful sunset over mountains");
    ImageResponse response = imageModel.call(prompt);

    assertNotNull(response);
    assertNotNull(response.getResults());
    assertEquals(1, response.getResults().size());

    String imageData = response.getResult().getOutput().getUrl();
    assertNotNull(imageData);
    assertTrue(imageData.contains("iVBORw0KGgo"));

    mockServer.verify();
  }

  @Test
  void imageGenerationWithMultipleImagesTest() {
    String jsonResponse =
        """
        {
          "model_id": "meta-llama/llama-3-2-11b-vision-instruct",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "image": "imagedata1base64",
              "seed": 100,
              "revised_prompt": "A futuristic city at night - variation 1"
            },
            {
              "image": "imagedata2base64",
              "seed": 101,
              "revised_prompt": "A futuristic city at night - variation 2"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + IMAGE_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(1024)
            .height(1024)
            .build();

    ImagePrompt prompt = new ImagePrompt("A futuristic city at night", options);
    ImageResponse response = imageModel.call(prompt);

    assertNotNull(response);
    assertNotNull(response.getResults());
    assertEquals(2, response.getResults().size());

    assertEquals("imagedata1base64", response.getResults().get(0).getOutput().getUrl());
    assertEquals("imagedata2base64", response.getResults().get(1).getOutput().getUrl());

    mockServer.verify();
  }

  @Test
  void imageGenerationWithCustomOptionsTest() {
    String jsonResponse =
        """
        {
          "model_id": "meta-llama/llama-3-2-11b-vision-instruct",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "image": "customsizeimage",
              "seed": 200,
              "revised_prompt": "A serene lake with mountains at sunrise in photographic style"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + IMAGE_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiImageOptions customOptions =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(512)
            .height(512)
            .build();

    ImagePrompt prompt = new ImagePrompt("A serene lake with mountains at sunrise", customOptions);
    ImageResponse response = imageModel.call(prompt);

    assertNotNull(response);
    assertEquals(1, response.getResults().size());

    String imageData = response.getResult().getOutput().getUrl();
    assertEquals("customsizeimage", imageData);

    mockServer.verify();
  }

  @Test
  void imageGenerationWithRevisedPromptTest() {
    String jsonResponse =
        """
        {
          "model_id": "meta-llama/llama-3-2-11b-vision-instruct",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "image": "revisedpromptimage",
              "seed": 150,
              "revised_prompt": "A majestic dragon soaring through clouds at sunset with intricate scales and vibrant colors"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + IMAGE_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    ImagePrompt prompt = new ImagePrompt("A dragon in the sky");
    ImageResponse response = imageModel.call(prompt);

    assertNotNull(response);
    assertEquals(1, response.getResults().size());

    String imageData = response.getResult().getOutput().getUrl();
    assertEquals("revisedpromptimage", imageData);

    mockServer.verify();
  }

  @Test
  void imageGenerationWithDifferentSizesTest() {
    String jsonResponse =
        """
        {
          "model_id": "meta-llama/llama-3-2-11b-vision-instruct",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "image": "landscapeimage768x512",
              "seed": 300,
              "revised_prompt": "A wide landscape view of rolling hills"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + IMAGE_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(768)
            .height(512)
            .build();

    ImagePrompt prompt = new ImagePrompt("A landscape view of rolling hills", options);
    ImageResponse response = imageModel.call(prompt);

    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertEquals("landscapeimage768x512", response.getResult().getOutput().getUrl());

    mockServer.verify();
  }

  @Test
  void imageGenerationWithComplexPromptTest() {
    String jsonResponse =
        """
        {
          "model_id": "meta-llama/llama-3-2-11b-vision-instruct",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "image": "complexsceneimage",
              "seed": 500,
              "revised_prompt": "A detailed cyberpunk cityscape at night with neon lights, flying vehicles, holographic advertisements, rain-soaked streets reflecting colorful lights, and towering skyscrapers in a futuristic setting"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + IMAGE_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    String complexPrompt =
        "A cyberpunk cityscape at night with neon lights, flying vehicles, and rain-soaked streets";
    ImagePrompt prompt = new ImagePrompt(complexPrompt);
    ImageResponse response = imageModel.call(prompt);

    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertEquals("complexsceneimage", response.getResult().getOutput().getUrl());

    mockServer.verify();
  }

  @Test
  void imageGenerationWithUrlResponseFormatTest() {
    String jsonResponse =
        """
        {
          "model_id": "meta-llama/llama-3-2-11b-vision-instruct",
          "created_at": "2024-01-15T10:30:00.000Z",
          "results": [
            {
              "image": "https://example.com/generated-image.png",
              "seed": 600,
              "revised_prompt": "A peaceful garden with flowers"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + IMAGE_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    WatsonxAiImageOptions options =
        WatsonxAiImageOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .width(1024)
            .height(1024)
            .build();

    ImagePrompt prompt = new ImagePrompt("A peaceful garden with flowers", options);
    ImageResponse response = imageModel.call(prompt);

    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    assertEquals(
        "https://example.com/generated-image.png", response.getResult().getOutput().getUrl());

    mockServer.verify();
  }
}
