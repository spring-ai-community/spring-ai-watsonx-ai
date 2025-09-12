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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

/** */
public class WatsonxAiChatApi {
  private static final Log logger = LogFactory.getLog(WatsonxAiChatApi.class);

  public Flux<WatsonxAiChatResponse> chat(WatsonxAiChatRequest watsonxAiChatRequest) {
    Assert.notNull(watsonxAiChatRequest, WATSONX_REQUEST_CANNOT_BE_NULL);

    if (this.token.needsRefresh()) {
      this.token = this.iamAuthenticator.requestToken();
    }

    return this.webClient
        .post()
        .uri(this.streamEndpoint)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token.getAccessToken())
        .bodyValue(watsonxAiChatRequest.withProjectId(this.projectId))
        .retrieve()
        .bodyToFlux(WatsonxAiChatResponse.class)
        .handle(
            (data, sink) -> {
              if (logger.isTraceEnabled()) {
                logger.trace(data);
              }
              sink.next(data);
            });
  }
}
