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

package io.github.springaicommunity.watsonx.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.micrometer.observation.ObservationRegistry;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Test class for WatsonxAiChatModel multimodality support. This test demonstrates integration with
 * Spring AI's multimodal APIs for handling text, image, and video inputs.
 *
 * <p>Based on watsonx.ai API documentation:
 * https://cloud.ibm.com/apidocs/watsonx-ai-cp/watsonx-ai-cp-2.2.1#text-chat
 *
 * @author Federico Mariani
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiChatClientMultimodalityIT {

  private RestClient.Builder restClientBuilder;

  private MockRestServiceServer mockServer;

  private WatsonxAiChatApi watsonxAiChatApi;

  private WatsonxAiChatModel chatModel;

  private ChatClient chatClient;

  private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";
  private static final String TEXT_ENDPOINT = "/ml/v1/text/chat";
  private static final String STREAM_ENDPOINT = "/ml/v1/text/chat_stream";
  private static final String VERSION = "2024-03-19";
  private static final String PROJECT_ID = "test-project-id";
  private static final String API_KEY = "test-api-key";

  // Sample base64-encoded data for testing (simple string for all media types)
  private static final String SAMPLE_MEDIA_BASE64 = "dGVzdA==";

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    // Create a simple ResponseErrorHandler for testing
    ResponseErrorHandler errorHandler =
        response -> {
          HttpStatus.Series series = HttpStatus.Series.resolve(response.getStatusCode().value());
          return (series == HttpStatus.Series.CLIENT_ERROR
              || series == HttpStatus.Series.SERVER_ERROR);
        };

    // Initialize the WatsonxAiChatApi
    watsonxAiChatApi =
        new WatsonxAiChatApi(
            BASE_URL,
            TEXT_ENDPOINT,
            STREAM_ENDPOINT,
            VERSION,
            PROJECT_ID,
            API_KEY,
            restClientBuilder,
            WebClient.builder(),
            errorHandler);

    // Create default options for the chat model with a vision-capable model
    WatsonxAiChatOptions defaultOptions =
        WatsonxAiChatOptions.builder()
            .model("meta-llama/llama-3-2-90b-vision-instruct")
            .temperature(0.7)
            .topP(1.0)
            .maxTokens(1024)
            .presencePenalty(0.0)
            .stopSequences(List.of())
            .logProbs(false)
            .n(1)
            .build();

    // Initialize the chat model
    chatModel =
        new WatsonxAiChatModel(
            watsonxAiChatApi,
            defaultOptions,
            ObservationRegistry.NOOP,
            ToolCallingManager.builder().build(),
            new DefaultToolExecutionEligibilityPredicate(),
            RetryUtils.DEFAULT_RETRY_TEMPLATE);

    // Initialize the ChatClient with the chat model
    chatClient = ChatClient.builder(chatModel).build();
  }

  @Test
  void chatClientWithImageUsingChatClientBuilderTest() {
    // Mock response for image analysis
    String jsonResponse =
        """
        {
          "id": "cmpl-img-002",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The image shows a red square on a white background."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create image resource
    byte[] imageBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource = new ByteArrayResource(imageBytes);

    // Use ChatClient builder API
    String response =
        chatClient
            .prompt()
            .user(
                u ->
                    u.text("What do you see in this image?")
                        .media(MimeTypeUtils.IMAGE_PNG, imageResource))
            .call()
            .content();

    // Verify the response
    assertNotNull(response);
    assertEquals("The image shows a red square on a white background.", response);

    mockServer.verify();
  }

  @Test
  void chatClientWithMultipleImagesTest() {
    // Mock response for multiple images analysis
    String jsonResponse =
        """
        {
          "id": "cmpl-img-multi-003",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The first image shows a cat, and the second image shows a dog. Both are common household pets."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create multiple image resources
    byte[] imageBytes1 = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    byte[] imageBytes2 = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource1 = new ByteArrayResource(imageBytes1);
    Resource imageResource2 = new ByteArrayResource(imageBytes2);

    // Create user message with multiple images
    UserMessage userMessage =
        UserMessage.builder()
            .media(
                new Media(MimeTypeUtils.IMAGE_PNG, imageResource1),
                new Media(MimeTypeUtils.IMAGE_JPEG, imageResource2))
            .text("Compare these two images")
            .build();

    Prompt prompt = new Prompt(userMessage);
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(
        response.getResult().getOutput().getText().contains("first image")
            && response.getResult().getOutput().getText().contains("second image"));

    mockServer.verify();
  }

  @Test
  void chatClientWithVideoUsingChatClientBuilderTest() {
    // Mock response for video analysis
    String jsonResponse =
        """
        {
          "id": "cmpl-video-002",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The video contains approximately 5 seconds of footage showing a beach scene."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create video resource
    byte[] videoBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource videoResource = new ByteArrayResource(videoBytes);

    // Use ChatClient builder API with video
    String response =
        chatClient
            .prompt()
            .user(
                u ->
                    u.text("How long is this video?")
                        .media(MimeTypeUtils.parseMimeType("video/mp4"), videoResource))
            .call()
            .content();

    // Verify the response
    assertNotNull(response);
    assertEquals(
        "The video contains approximately 5 seconds of footage showing a beach scene.", response);

    mockServer.verify();
  }

  @Test
  void chatClientWithMixedTextImageAndVideoTest() {
    // Mock response for mixed content analysis
    String jsonResponse =
        """
        {
          "id": "cmpl-mixed-001",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The image shows a product photo, and the video demonstrates the product in use. Both highlight the product's key features effectively."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create image and video resources
    byte[] imageBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    byte[] videoBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource = new ByteArrayResource(imageBytes);
    Resource videoResource = new ByteArrayResource(videoBytes);

    // Create user message with both image and video
    UserMessage userMessage =
        UserMessage.builder()
            .text("Compare the product image with the product demo video")
            .media(
                new Media(MimeTypeUtils.IMAGE_PNG, imageResource),
                new Media(MimeTypeUtils.parseMimeType("video/mp4"), videoResource))
            .build();

    Prompt prompt = new Prompt(userMessage);
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(response.getResult().getOutput().getText().contains("image"));
    assertTrue(response.getResult().getOutput().getText().contains("video"));

    mockServer.verify();
  }

  @Test
  void chatClientWithImageAndCustomOptionsTest() {
    // Mock response for image with custom options
    String jsonResponse =
        """
        {
          "id": "cmpl-img-opts-001",
          "model_id": "meta-llama/llama-3-2-11b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Detailed analysis: The image contains various geometric shapes arranged in a specific pattern."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create custom options with different model
    WatsonxAiChatOptions customOptions =
        WatsonxAiChatOptions.builder()
            .model("meta-llama/llama-3-2-11b-vision-instruct")
            .temperature(0.3)
            .maxTokens(512)
            .build();

    // Create image resource
    byte[] imageBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource = new ByteArrayResource(imageBytes);

    // Use ChatClient with custom options
    String response =
        chatClient
            .prompt()
            .user(
                u ->
                    u.text("Provide a detailed analysis of this image")
                        .media(MimeTypeUtils.IMAGE_PNG, imageResource))
            .options(customOptions)
            .call()
            .content();

    // Verify the response
    assertNotNull(response);
    assertTrue(response.contains("Detailed analysis"));

    mockServer.verify();
  }

  @Test
  void chatClientWithImageDifferentFormatsTest() {
    // Mock response for different image formats
    String jsonResponse =
        """
        {
          "id": "cmpl-img-formats-001",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "I can analyze images in various formats including PNG, JPEG, and WebP."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Test with JPEG format
    byte[] imageBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource = new ByteArrayResource(imageBytes);

    UserMessage userMessage =
        UserMessage.builder()
            .text("Analyze this JPEG image")
            .media(new Media(MimeTypeUtils.IMAGE_JPEG, imageResource))
            .build();

    Prompt prompt = new Prompt(userMessage);
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(response.getResult().getOutput().getText().contains("image"));

    mockServer.verify();
  }

  @Test
  void chatClientWithImageDetailParameterTest() {
    // Mock response for image with detail parameter
    String jsonResponse =
        """
        {
          "id": "cmpl-img-detail-001",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "High-detail analysis: The image contains fine details including textures, patterns, and subtle color variations."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create image resource
    byte[] imageBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource = new ByteArrayResource(imageBytes);

    // Note: The detail parameter should be supported via Media metadata in future Spring AI
    // versions
    // For now, we create a standard media object
    UserMessage userMessage =
        UserMessage.builder()
            .text("Provide a high-detail analysis")
            .media(new Media(MimeTypeUtils.IMAGE_PNG, imageResource))
            .build();

    Prompt prompt = new Prompt(userMessage);
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(response.getResult().getOutput().getText().contains("detail"));

    mockServer.verify();
  }

  @Test
  void chatClientWithMultipleMessagesAndImagesTest() {
    // Mock response for conversational multimodal interaction
    String jsonResponse =
        """
        {
          "id": "cmpl-conv-img-001",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Based on the image you shared earlier and this new one, I can see a clear progression in the design."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create image resources for multi-turn conversation
    byte[] imageBytes1 = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    byte[] imageBytes2 = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource1 = new ByteArrayResource(imageBytes1);
    Resource imageResource2 = new ByteArrayResource(imageBytes2);

    // Create first user message with image
    UserMessage firstMessage =
        UserMessage.builder()
            .text("Look at this design")
            .media(new Media(MimeTypeUtils.IMAGE_PNG, imageResource1))
            .build();

    // Create second user message with another image
    UserMessage secondMessage =
        UserMessage.builder()
            .text("Now compare it with this updated version")
            .media(new Media(MimeTypeUtils.IMAGE_PNG, imageResource2))
            .build();

    // Note: In a real scenario, you would include the assistant's response between messages
    // For testing purposes, we're simplifying this
    Prompt prompt = new Prompt(List.of(firstMessage, secondMessage));
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(response.getResult().getOutput().getText().contains("image"));

    mockServer.verify();
  }

  @Test
  void chatClientWithWAVAudioTest() {
    // Mock response for WAV audio analysis
    String jsonResponse =
        """
        {
          "id": "cmpl-audio-wav-001",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The WAV audio file contains background music with no speech. The audio is approximately 2 seconds long."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create WAV audio resource from base64
    byte[] audioBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource audioResource = new ByteArrayResource(audioBytes);

    // Create user message with WAV audio
    UserMessage userMessage =
        UserMessage.builder()
            .text("What is in this audio file?")
            .media(new Media(MimeTypeUtils.parseMimeType("audio/wav"), audioResource))
            .build();

    Prompt prompt = new Prompt(userMessage);
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(response.getResult().getOutput().getText().contains("audio"));

    mockServer.verify();
  }

  @Test
  void chatClientWithMP3AudioUsingChatClientBuilderTest() {
    // Mock response for MP3 audio using ChatClient builder
    String jsonResponse =
        """
        {
          "id": "cmpl-audio-mp3-002",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The speaker in the audio is discussing technology trends. The tone is professional and informative."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create MP3 audio resource
    byte[] audioBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource audioResource = new ByteArrayResource(audioBytes);

    // Use ChatClient builder API with MP3 audio
    String response =
        chatClient
            .prompt()
            .user(
                u ->
                    u.text("What is the speaker talking about?")
                        .media(MimeTypeUtils.parseMimeType("audio/mp3"), audioResource))
            .call()
            .content();

    // Verify the response
    assertNotNull(response);
    assertTrue(response.contains("speaker") || response.contains("audio"));

    mockServer.verify();
  }

  @Test
  void chatClientWithMultipleAudioFilesTest() {
    // Mock response for multiple audio files analysis
    String jsonResponse =
        """
        {
          "id": "cmpl-audio-multi-001",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The first audio is in MP3 format and contains speech. The second audio is in WAV format and contains music. Both are short clips."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create multiple audio resources
    byte[] mp3Bytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    byte[] wavBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource mp3Resource = new ByteArrayResource(mp3Bytes);
    Resource wavResource = new ByteArrayResource(wavBytes);

    // Create user message with multiple audio files
    UserMessage userMessage =
        UserMessage.builder()
            .text("Compare these two audio files")
            .media(
                new Media(MimeTypeUtils.parseMimeType("audio/mp3"), mp3Resource),
                new Media(MimeTypeUtils.parseMimeType("audio/wav"), wavResource))
            .build();

    Prompt prompt = new Prompt(userMessage);
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(response.getResult().getOutput().getText().contains("audio"));

    mockServer.verify();
  }

  @Test
  void chatClientWithMixedImageAndAudioTest() {
    // Mock response for mixed image and audio analysis
    String jsonResponse =
        """
        {
          "id": "cmpl-mixed-img-audio-001",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "The image shows a presentation slide about AI, and the audio contains the presenter explaining the concepts shown in the slide."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create image and audio resources
    byte[] imageBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    byte[] audioBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource = new ByteArrayResource(imageBytes);
    Resource audioResource = new ByteArrayResource(audioBytes);

    // Create user message with both image and audio
    UserMessage userMessage =
        UserMessage.builder()
            .text("Does the audio content match what's shown in the image?")
            .media(
                new Media(MimeTypeUtils.IMAGE_PNG, imageResource),
                new Media(MimeTypeUtils.parseMimeType("audio/mp3"), audioResource))
            .build();

    Prompt prompt = new Prompt(userMessage);
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    assertTrue(response.getResult().getOutput().getText().contains("image"));
    assertTrue(response.getResult().getOutput().getText().contains("audio"));

    mockServer.verify();
  }

  @Test
  void chatClientWithAllMediaTypesTest() {
    // Mock response for all media types (text, image, audio, video)
    String jsonResponse =
        """
        {
          "id": "cmpl-all-media-001",
          "model_id": "meta-llama/llama-3-2-90b-vision-instruct",
          "created": 1689958352,
          "created_at": "2023-07-21T16:52:32.190Z",
          "model_version": "1.0.0",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Analysis complete: The image shows the product, the video demonstrates its usage, and the audio provides detailed specifications. All three media types complement each other well."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + TEXT_ENDPOINT + "?version=" + VERSION))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // Create all types of media resources
    byte[] imageBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    byte[] videoBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    byte[] audioBytes = Base64.getDecoder().decode(SAMPLE_MEDIA_BASE64);
    Resource imageResource = new ByteArrayResource(imageBytes);
    Resource videoResource = new ByteArrayResource(videoBytes);
    Resource audioResource = new ByteArrayResource(audioBytes);

    // Create user message with image, video, and audio
    UserMessage userMessage =
        UserMessage.builder()
            .text("Analyze this product presentation with image, video, and audio commentary")
            .media(
                new Media(MimeTypeUtils.IMAGE_PNG, imageResource),
                new Media(MimeTypeUtils.parseMimeType("video/mp4"), videoResource),
                new Media(MimeTypeUtils.parseMimeType("audio/mp3"), audioResource))
            .build();

    Prompt prompt = new Prompt(userMessage);
    ChatResponse response = chatModel.call(prompt);

    // Verify the response
    assertNotNull(response);
    assertNotNull(response.getResult());
    String responseText = response.getResult().getOutput().getText();
    assertTrue(responseText.contains("image"));
    assertTrue(responseText.contains("video"));
    assertTrue(responseText.contains("audio"));

    mockServer.verify();
  }
}
