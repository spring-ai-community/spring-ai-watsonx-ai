package io.github.springaicommunity.watsonx.embedding;

import com.ibm.cloud.sdk.core.http.Response;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.Assert;

public class WatsonxAiEmbeddingApi {

  @Retryable(
      retryFor = Exception.class,
      maxAttempts = 3,
      backoff = @Backoff(random = true, delay = 1200, maxDelay = 7000, multiplier = 2.5))
  public Response<WatsonxAiEmbeddingResponse> getEmbeddings(WatsonxAiEmbeddingRequest request) {
    Assert.notNull(request, "The request is required.");

    return null; // Placeholder return statement
  }
}
