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

package org.springaicommunity.watsonx.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springaicommunity.watsonx.auth.StubWatsonxAiAuthentication;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tests {@link WatsonxAiDeploymentGenerationModel} against a stubbed watsonx.ai text
 * generation deployment. Response bodies are shaped like real watsonx.ai responses for a
 * deployed freeform prompt template.
 *
 * @author Ana Katrina Inguengan
 */
@ExtendWith(StubWatsonxAiAuthentication.class)
class WatsonxAiDeploymentGenerationModelIT {

	private static final String BASE_URL = "https://us-south.ml.cloud.ibm.com";

	private static final String VERSION = "2024-10-17";

	private static final String DEPLOYMENT_ID = "01a13873-a61a-72e9-a075-9e3337f3f718";

	private static final JsonHelper JSON_HELPER = new JsonHelper();

	private static final String RESPONSE = """
			{
			  "model_id": "ibm/granite-4-h-small",
			  "model_version": "4.0.0",
			  "created_at": "2026-10-14T09:11:15.502Z",
			  "results": [
			    { "generated_text": " Tokyo.", "generated_token_count": 3, "input_token_count": 17, "stop_reason": "eos_token" }
			  ],
			  "system": {
			    "warnings": [ { "message": "The parameter `parameters.decoding_method` is ignored and set automatically", "id": "param_deprecation" } ]
			  }
			}
			""";

	// As watsonx.ai streams it: server-sent events with an id and an event name, and no
	// [DONE] at the end
	private static final List<String> STREAM_EVENTS = List.of(
			"id: 1\nevent: message\ndata: {\"model_id\":\"ibm/granite-4-h-small\",\"model_version\":\"4.0.0\",\"created_at\":\"2026-10-14T09:11:44.695Z\",\"results\":[{\"generated_text\":\" Tokyo\",\"generated_token_count\":1,\"input_token_count\":17,\"stop_reason\":\"not_finished\"}]}\n\n",
			"id: 2\nevent: message\ndata: {\"model_id\":\"ibm/granite-4-h-small\",\"model_version\":\"4.0.0\",\"created_at\":\"2026-10-14T09:11:44.703Z\",\"results\":[{\"generated_text\":\".\",\"generated_token_count\":2,\"input_token_count\":17,\"stop_reason\":\"not_finished\"}]}\n\n",
			"id: 3\nevent: message\ndata: {\"model_id\":\"ibm/granite-4-h-small\",\"model_version\":\"4.0.0\",\"created_at\":\"2026-10-14T09:11:44.712Z\",\"results\":[{\"generated_text\":\"\",\"generated_token_count\":3,\"input_token_count\":17,\"stop_reason\":\"eos_token\"}]}\n\n");

	private static final String UNKNOWN_VARIABLE_ERROR = """
			{"errors":[{"code":"invalid_input_argument","message":"Unexpected additional template variable nope"}],"trace":"t","status_code":400}""";

	private MockRestServiceServer mockServer;

	private final List<String> streamRequests = new CopyOnWriteArrayList<>();

	private HttpStatus streamStatus = HttpStatus.OK;

	private WatsonxAiDeploymentGenerationApi api;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		this.mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

		WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(request -> {
			this.streamRequests.add(request.url().getPath() + "?" + request.url().getQuery());
			if (this.streamStatus.isError()) {
				return Mono.just(ClientResponse.create(this.streamStatus)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.body(UNKNOWN_VARIABLE_ERROR)
					.build());
			}
			return Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
				.body(Flux.fromIterable(STREAM_EVENTS)
					.map(event -> DefaultDataBufferFactory.sharedInstance.wrap(event.getBytes(StandardCharsets.UTF_8))))
				.build());
		});

		this.api = new WatsonxAiDeploymentGenerationApi(BASE_URL, "/ml/v1/deployments/{deployment_id}/text/generation",
				"/ml/v1/deployments/{deployment_id}/text/generation_stream", VERSION, "test-api-key", restClientBuilder,
				webClientBuilder, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	private WatsonxAiDeploymentGenerationModel model(WatsonxAiDeploymentGenerationOptions defaultOptions) {
		return new WatsonxAiDeploymentGenerationModel(this.api, defaultOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	/** The JSON request body must be exactly this. */
	private static RequestMatcher body(Map<String, Object> expected) {
		return request -> assertThat(JSON_HELPER.fromJsonToMap(((MockClientHttpRequest) request).getBodyAsString()))
			.isEqualTo(expected);
	}

	@Test
	void generateSendsOnlyThePromptVariablesAndParameters() {
		this.mockServer
			.expect(requestTo(BASE_URL + "/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/generation?version=" + VERSION))
			.andExpect(method(HttpMethod.POST))
			.andExpect(body(Map.of("parameters",
					Map.of("prompt_variables", Map.of("question", "What is the capital of Japan?"), "max_new_tokens",
							3))))
			.andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

		WatsonxAiDeploymentGenerationResponse response = model(
				WatsonxAiDeploymentGenerationOptions.builder().deploymentId(DEPLOYMENT_ID).maxNewTokens(3).build())
			.generate(Map.of("question", "What is the capital of Japan?"));

		assertThat(response.generatedText()).isEqualTo(" Tokyo.");
		assertThat(response.modelId()).isEqualTo("ibm/granite-4-h-small");
		assertThat(response.results()).singleElement().satisfies(result -> {
			assertThat(result.generatedTokenCount()).isEqualTo(3);
			assertThat(result.inputTokenCount()).isEqualTo(17);
			assertThat(result.stopReason()).isEqualTo("eos_token");
		});
		assertThat(response.system().warnings()).extracting(WatsonxAiDeploymentGenerationResponse.Warning::id)
			.containsExactly("param_deprecation");
		this.mockServer.verify();
	}

	@Test
	void callOptionsTakePrecedenceOverTheDefaults() {
		this.mockServer
			.expect(requestTo(BASE_URL + "/ml/v1/deployments/other-deployment/text/generation?version=" + VERSION))
			.andExpect(body(Map.of("parameters",
					Map.of("prompt_variables", Map.of("question", "Hi"), "max_new_tokens", 20, "temperature", 0.2))))
			.andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

		WatsonxAiDeploymentGenerationModel model = model(
				WatsonxAiDeploymentGenerationOptions.builder().deploymentId(DEPLOYMENT_ID).maxNewTokens(20).build());
		model.generate(Map.of("question", "Hi"),
				WatsonxAiDeploymentGenerationOptions.builder()
					.deploymentId("other-deployment")
					.temperature(0.2)
					.build());

		this.mockServer.verify();
		assertThat(model.getDefaultOptions().getDeploymentId()).isEqualTo(DEPLOYMENT_ID);
		assertThat(model.getDefaultOptions().getTemperature()).isNull();
	}

	@Test
	void withoutPromptVariablesNoneAreSent() {
		this.mockServer
			.expect(requestTo(BASE_URL + "/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/generation?version=" + VERSION))
			.andExpect(body(Map.of("parameters", Map.of())))
			.andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

		model(WatsonxAiDeploymentGenerationOptions.builder().deploymentId(DEPLOYMENT_ID).build()).generate(Map.of());

		this.mockServer.verify();
	}

	@Test
	void generateWithoutDeploymentIdFails() {
		WatsonxAiDeploymentGenerationModel model = model(new WatsonxAiDeploymentGenerationOptions());

		assertThatIllegalArgumentException().isThrownBy(() -> model.generate(Map.of("question", "Hi")))
			.withMessageContaining("A deployment ID is required");
	}

	@Test
	void errorResponseFailsTheCall() {
		this.mockServer
			.expect(requestTo(BASE_URL + "/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/generation?version=" + VERSION))
			.andRespond(withBadRequest().body(UNKNOWN_VARIABLE_ERROR).contentType(MediaType.APPLICATION_JSON));

		WatsonxAiDeploymentGenerationModel model = model(
				WatsonxAiDeploymentGenerationOptions.builder().deploymentId(DEPLOYMENT_ID).build());

		assertThatExceptionOfType(NonTransientAiException.class).isThrownBy(() -> model.generate(Map.of("nope", "x")))
			.withMessageContaining("Unexpected additional template variable nope");
	}

	@Test
	void streamReturnsEveryChunk() {
		List<WatsonxAiDeploymentGenerationResponse> chunks = model(
				WatsonxAiDeploymentGenerationOptions.builder().deploymentId(DEPLOYMENT_ID).build())
			.stream(Map.of("question", "What is the capital of Japan?"))
			.collectList()
			.block();

		assertThat(chunks).extracting(WatsonxAiDeploymentGenerationResponse::generatedText)
			.containsExactly(" Tokyo", ".", "");
		assertThat(chunks.get(chunks.size() - 1).results().get(0).stopReason()).isEqualTo("eos_token");
		assertThat(this.streamRequests)
			.containsExactly("/ml/v1/deployments/" + DEPLOYMENT_ID + "/text/generation_stream?version=" + VERSION);
	}

	@Test
	void streamErrorFailsLikeACall() {
		this.streamStatus = HttpStatus.BAD_REQUEST;
		WatsonxAiDeploymentGenerationModel model = model(
				WatsonxAiDeploymentGenerationOptions.builder().deploymentId(DEPLOYMENT_ID).build());

		assertThatExceptionOfType(NonTransientAiException.class)
			.isThrownBy(() -> model.stream(Map.of("nope", "x")).collectList().block())
			.withMessageContaining("Unexpected additional template variable nope");
	}

	@Test
	void streamWithoutDeploymentIdFails() {
		WatsonxAiDeploymentGenerationModel model = model(new WatsonxAiDeploymentGenerationOptions());

		assertThatIllegalArgumentException().isThrownBy(() -> model.stream(Map.of("question", "Hi")))
			.withMessageContaining("A deployment ID is required");
	}

}
