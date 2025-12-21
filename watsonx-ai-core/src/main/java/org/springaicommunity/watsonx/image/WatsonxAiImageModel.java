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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link ImageModel} implementation that provides access to watsonx.ai supported image generation
 * models.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
 */
public class WatsonxAiImageModel implements ImageModel {

  private static final Logger logger = LoggerFactory.getLogger(WatsonxAiImageModel.class);

  private final WatsonxAiImageApi watsonxAiImageApi;
  private final WatsonxAiImageOptions defaultOptions;
  private final RetryTemplate retryTemplate;

  public WatsonxAiImageModel(
      WatsonxAiImageApi watsonxAiImageApi,
      WatsonxAiImageOptions defaultOptions,
      RetryTemplate retryTemplate) {
    Assert.notNull(watsonxAiImageApi, "WatsonxAiImageApi must not be null");
    Assert.notNull(defaultOptions, "WatsonxAiImageOptions must not be null");
    Assert.notNull(retryTemplate, "RetryTemplate must not be null");
    this.watsonxAiImageApi = watsonxAiImageApi;
    this.defaultOptions = defaultOptions;
    this.retryTemplate = retryTemplate;
  }

  @Override
  public ImageResponse call(ImagePrompt imagePrompt) {
    Assert.notNull(imagePrompt, "ImagePrompt must not be null");
    Assert.notEmpty(imagePrompt.getInstructions(), "ImagePrompt instructions must not be empty");

    return this.retryTemplate.execute(
        ctx -> {
          String prompt = imagePrompt.getInstructions().get(0).getText();

          WatsonxAiImageOptions mergedOptions = mergeOptions(imagePrompt.getOptions());

          WatsonxAiImageRequest.ImageParameters parameters = createImageParameters(mergedOptions);

          WatsonxAiImageRequest request =
              WatsonxAiImageRequest.builder()
                  .input(prompt)
                  .model(mergedOptions.getModel())
                  .parameters(parameters)
                  .build();

          ResponseEntity<WatsonxAiImageResponse> response =
              this.watsonxAiImageApi.generateImage(request);

          return toImageResponse(response.getBody());
        });
  }

  private WatsonxAiImageOptions mergeOptions(ImageOptions runtimeOptions) {
    WatsonxAiImageOptions mergedOptions = WatsonxAiImageOptions.fromOptions(this.defaultOptions);

    if (runtimeOptions != null) {
      mergedOptions =
          ModelOptionsUtils.merge(runtimeOptions, mergedOptions, WatsonxAiImageOptions.class);
    }

    return mergedOptions;
  }

  private WatsonxAiImageRequest.ImageParameters createImageParameters(
      WatsonxAiImageOptions options) {
    return WatsonxAiImageRequest.ImageParameters.builder()
        .n(options.getN())
        .width(options.getWidth())
        .height(options.getHeight())
        .responseFormat(options.getResponseFormat())
        .style(options.getStyle())
        .build();
  }

  private ImageResponse toImageResponse(WatsonxAiImageResponse watsonxResponse) {
    if (watsonxResponse == null || CollectionUtils.isEmpty(watsonxResponse.results())) {
      logger.warn("No image results returned from watsonx.ai");
      return new ImageResponse(List.of());
    }

    List<ImageGeneration> imageGenerations =
        watsonxResponse.results().stream()
            .map(
                result -> {
                  Image image = new Image(result.image(), result.revisedPrompt());
                  return new ImageGeneration(image);
                })
            .toList();

    return new ImageResponse(imageGenerations);
  }

  public WatsonxAiImageOptions getDefaultOptions() {
    return WatsonxAiImageOptions.fromOptions(this.defaultOptions);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private WatsonxAiImageApi watsonxAiImageApi;
    private WatsonxAiImageOptions options = WatsonxAiImageOptions.builder().build();
    private RetryTemplate retryTemplate;

    private Builder() {}

    public Builder watsonxAiImageApi(WatsonxAiImageApi watsonxAiImageApi) {
      this.watsonxAiImageApi = watsonxAiImageApi;
      return this;
    }

    public Builder options(WatsonxAiImageOptions options) {
      this.options = options;
      return this;
    }

    public Builder retryTemplate(RetryTemplate retryTemplate) {
      this.retryTemplate = retryTemplate;
      return this;
    }

    public WatsonxAiImageModel build() {
      return new WatsonxAiImageModel(this.watsonxAiImageApi, this.options, this.retryTemplate);
    }
  }
}
