---
sidebar_position: 8
---

# Deployment Generation

`WatsonxAiDeploymentGenerationModel` calls a watsonx.ai **text generation deployment**: a deployed **freeform** or **structured** prompt template, as saved from watsonx.ai's Prompt Lab. The prompt lives in watsonx.ai; your application sends the values of the template's variables and gets the generated text back.

A deployed **chat** prompt template is different: chat with it through `WatsonxAiChatModel` and a `deployment-id`, see [Chat](../chat/index.md#deployments).

## Why it isn't a ChatModel

A text generation deployment doesn't take messages. watsonx.ai rejects any `input` in the request (`'input' cannot be specified in the request body`): the only thing the request carries is the template's variables, plus generation parameters. As a Spring AI `ChatModel`, the prompt's messages would have to be dropped or forced into one variable, and `ChatClient`, advisors and chat memory would seem to work without having any effect. So this is a watsonx.ai-specific model, like `WatsonxAiRerankModel`.

## Setup

The model is auto-configured with the [connection properties](../configuration.md). A deployment belongs to its space, so `project-id` and `space-id` aren't needed for it.

```properties
spring.ai.watsonx.ai.api-key=${WATSONX_AI_API_KEY}
spring.ai.watsonx.ai.base-url=https://us-south.ml.cloud.ibm.com
spring.ai.watsonx.ai.deployment-generation.options.deployment-id=<deployment ID or serving name>
```

| Property | Default | Description |
| :------- | :------ | :---------- |
| `spring.ai.watsonx.ai.deployment-generation.enabled` | `true` | Enable or disable the auto-configuration |
| `spring.ai.watsonx.ai.deployment-generation.generation-endpoint` | `/ml/v1/deployments/{deployment_id}/text/generation` | The text generation endpoint |
| `spring.ai.watsonx.ai.deployment-generation.stream-endpoint` | `/ml/v1/deployments/{deployment_id}/text/generation_stream` | The streaming endpoint |
| `spring.ai.watsonx.ai.deployment-generation.version` | `2024-10-17` | The API version date |
| `spring.ai.watsonx.ai.deployment-generation.options.deployment-id` | - | The deployment ID or serving name; can also be set per call |
| `spring.ai.watsonx.ai.deployment-generation.options.max-new-tokens` | - | The maximum number of tokens to generate |
| `spring.ai.watsonx.ai.deployment-generation.options.min-new-tokens` | - | The minimum number of tokens to generate |
| `spring.ai.watsonx.ai.deployment-generation.options.decoding-method` | - | `greedy` or `sample` |
| `spring.ai.watsonx.ai.deployment-generation.options.temperature` | - | The sampling temperature |
| `spring.ai.watsonx.ai.deployment-generation.options.top-p` | - | The cumulative probability of the tokens to sample from |
| `spring.ai.watsonx.ai.deployment-generation.options.top-k` | - | The number of most likely tokens to sample from |
| `spring.ai.watsonx.ai.deployment-generation.options.repetition-penalty` | - | The penalty for repeated tokens |
| `spring.ai.watsonx.ai.deployment-generation.options.random-seed` | - | The seed for sampling |
| `spring.ai.watsonx.ai.deployment-generation.options.stop-sequences` | - | Sequences that stop the generation |
| `spring.ai.watsonx.ai.deployment-generation.options.time-limit` | - | The time limit, in milliseconds |

A generation parameter that isn't set isn't sent, so the prompt template's own value applies.

## Generating text

For a template with a `{question}` variable:

```java
@Autowired
WatsonxAiDeploymentGenerationModel generationModel;

WatsonxAiDeploymentGenerationResponse response = generationModel
	.generate(Map.of("question", "What is the capital of Japan?"));

String text = response.generatedText(); // " Tokyo."
```

`generatedText()` is the text of the first result. The response also has the token counts and the `stop_reason` (such as `eos_token` or `max_tokens`), in `response.results()`.

Options passed to a call take precedence over the configured defaults, for example another deployment:

```java
generationModel.generate(Map.of("question", "What is the capital of Italy?"),
		WatsonxAiDeploymentGenerationOptions.builder()
			.deploymentId("other-deployment")
			.maxNewTokens(5)
			.build());
```

## Streaming

`stream(...)` returns one response per chunk of text, with `stop_reason` `not_finished` until the last chunk:

```java
String text = generationModel.stream(Map.of("question", "What is the capital of Japan?"))
	.map(WatsonxAiDeploymentGenerationResponse::generatedText)
	.collect(Collectors.joining())
	.block();
```

## Notes

- **Prompt variables are required.** watsonx.ai doesn't apply a template's `default_value`: without variables, the model gets the template's raw `{variable}` placeholders. The model logs a warning when no variables are passed.
- **Unknown variables fail:** watsonx.ai answers `400 Unexpected additional template variable <name>`.
- **Warnings** from watsonx.ai, such as a deprecated template parameter, are logged at DEBUG by `WatsonxAiWarnings`, as for the other models.
- **Not covered:** deployments that expect an `input` field instead of prompt variables. This model only sends prompt variables and generation parameters.
