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

package org.springaicommunity.watsonx.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WatsonxAiAuthentication} against a local server that stands in for the
 * IAM token endpoint.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiAuthenticationTest {

	private static final int THREADS = 8;

	private final AtomicInteger tokenRequests = new AtomicInteger();

	private HttpServer iamServer;

	private WatsonxAiAuthentication authentication;

	@BeforeEach
	void setUp() throws IOException {
		this.iamServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		this.iamServer.createContext("/", exchange -> {
			int request = this.tokenRequests.incrementAndGet();
			try {
				// Keep the request in flight long enough for the other threads to arrive.
				Thread.sleep(200);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			long now = System.currentTimeMillis() / 1000;
			byte[] body = """
					{"access_token":"token-%d","refresh_token":"refresh","token_type":"Bearer",\
					"expires_in":3600,"expiration":%d}""".formatted(request, now + 3600)
				.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		this.iamServer.setExecutor(Executors.newFixedThreadPool(THREADS));
		this.iamServer.start();

		IamAuthenticator iamAuthenticator = new IamAuthenticator.Builder().apikey("test-api-key")
			.url("http://localhost:" + this.iamServer.getAddress().getPort())
			.build();
		this.authentication = new WatsonxAiAuthentication(iamAuthenticator);
	}

	@AfterEach
	void tearDown() {
		this.iamServer.stop(0);
	}

	@Test
	void concurrentCallersShareOneTokenRequest() throws Exception {
		ExecutorService callers = Executors.newFixedThreadPool(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<String>> tokens = IntStream.range(0, THREADS).mapToObj(i -> callers.submit(() -> {
				start.await();
				return this.authentication.getAccessToken();
			})).toList();
			start.countDown();

			for (Future<String> token : tokens) {
				assertThat(token.get(10, TimeUnit.SECONDS)).isEqualTo("token-1");
			}
		}
		finally {
			callers.shutdownNow();
		}

		assertThat(this.tokenRequests).hasValue(1);
	}

	@Test
	void reusesCachedTokenUntilItNeedsRefresh() {
		assertThat(this.authentication.getAccessToken()).isEqualTo("token-1");
		assertThat(this.authentication.getAccessToken()).isEqualTo("token-1");

		assertThat(this.tokenRequests).hasValue(1);
	}

}
