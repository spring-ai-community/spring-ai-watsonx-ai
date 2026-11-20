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

package org.springaicommunity.watsonx.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Turns an error response of a watsonx.ai stream into the same exception that a
 * synchronous call gets, by passing it to the configured {@link ResponseErrorHandler}.
 *
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
public final class WatsonxAiStreamErrors {

	private WatsonxAiStreamErrors() {
	}

	/**
	 * The exception for an error response of a stream.
	 * @param response the error response
	 * @param responseErrorHandler the handler that synchronous calls use
	 * @return the exception the handler throws, or WebClient's default one
	 */
	public static Mono<Throwable> toException(final ClientResponse response,
			final ResponseErrorHandler responseErrorHandler) {
		// If the body can't be read (connection reset, codec limit), report the status
		// anyway, as Spring's DefaultClientResponse.createException() does
		return response.bodyToMono(byte[].class)
			.onErrorReturn(ex -> !(ex instanceof Error), new byte[0])
			.defaultIfEmpty(new byte[0])
			.<Throwable>map(body -> {
				BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(response, body);
				try {
					if (responseErrorHandler.hasError(bufferedResponse)) {
						responseErrorHandler.handleError(response.request().getURI(), response.request().getMethod(),
								bufferedResponse);
					}
				}
				catch (IOException | RuntimeException ex) {
					return ex;
				}
				// The handler didn't throw: fall back to WebClient's default exception
				return WebClientResponseException.create(response.statusCode(), bufferedResponse.getStatusText(),
						response.headers().asHttpHeaders(), body, null, response.request());
			});
	}

	/**
	 * A {@link ClientHttpResponse} over a {@link ClientResponse} whose body has already
	 * been read.
	 */
	private static final class BufferedClientHttpResponse implements ClientHttpResponse {

		private final ClientResponse response;

		private final byte[] body;

		private BufferedClientHttpResponse(final ClientResponse response, final byte[] body) {
			this.response = response;
			this.body = body;
		}

		@Override
		public HttpStatusCode getStatusCode() {
			return this.response.statusCode();
		}

		@Override
		public String getStatusText() {
			HttpStatus status = HttpStatus.resolve(this.response.statusCode().value());
			return status != null ? status.getReasonPhrase() : "";
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.response.headers().asHttpHeaders();
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(this.body);
		}

		@Override
		public void close() {
		}

	}

}
