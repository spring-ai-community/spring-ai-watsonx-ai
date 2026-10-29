---
sidebar_position: 3
---

# Upgrade Notes

This page lists the changes you may need to act on when you upgrade Spring AI watsonx.ai. Each entry explains who is affected and how to migrate. The full list of changes in a release is in the [GitHub release notes](https://github.com/spring-ai-community/spring-ai-watsonx-ai/releases).

## Upgrading to 2.1.0

2.1.0 adds chat through deployments and fixes many requests that watsonx.ai rejected or that left out options. It uses the same Spring AI (2.0.1) and Spring Boot (4.1.1) versions as 2.0.0. Most fixes need no change on your side, but a few change a default, a request sent to watsonx.ai, an exception, or a public API.

### `project-id` and `space-id` are checked at startup

watsonx.ai needs exactly one of a project and a space on every request. In 2.0.x, an application with both or neither started, and only its first request failed. Now it fails at startup ([#228](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/228)).

#### Impact

This also affects applications that start but never call watsonx.ai, such as a `@SpringBootTest` or a CI profile that only sets `api-key`, and applications that get one ID from each of two profiles.

#### Migration

Set exactly one of `spring.ai.watsonx.ai.project-id` and `spring.ai.watsonx.ai.space-id`. In a profile that doesn't call watsonx.ai, turn the watsonx.ai models off instead. See [Upgrading: project ID and space ID are checked at startup](./configuration.md#upgrading-project-id-and-space-id-are-checked-at-startup) for the error messages and every option.

### Default models replaced

IBM withdrew the models that were used as defaults, so the defaults now point to supported models ([#213](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/213)):

| Property | 2.0.0 default | 2.1.0 default |
|---|---|---|
| `spring.ai.watsonx.ai.chat.options.model` | `ibm/granite-3-3-8b-instruct` | `ibm/granite-4-h-small` |
| `spring.ai.watsonx.ai.embedding.options.model` | `ibm/slate-125m-english-rtrvr` | `ibm/granite-embedding-278m-multilingual` |
| `spring.ai.watsonx.ai.rerank.options.model` | `cross-encoder/ms-marco-minilm-l-12-v2` | `intfloat/multilingual-e5-large` |

#### Impact

Applications that don't set a model now use a different one. Chat answers and rerank scores can change.

**Embeddings are the important case:** vectors from two different models can't be compared. If your vector store holds embeddings created with the old default, new queries embedded with the new default won't match them.

#### Migration

Set the model explicitly. Either keep the one you used before, while it's still available in your region:

```properties
spring.ai.watsonx.ai.embedding.options.model=ibm/slate-125m-english-rtrvr
```

Or move to the new default and re-embed the documents in your vector store.

### Every chat option is sent

In 2.0.0, several chat options were accepted but never sent to watsonx.ai, without an error ([#236](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/236), [#239](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/239)):

- `max-completion-tokens`, `seed`, `time-limit`, `logit-bias`
- `tool-choice`, `tool-choice-option`, and tool definitions set with `tools(...)`
- `guided-choice`, `guided-regex`, `guided-grammar`, `guided-json`
- `include-reasoning`, `chat-template-kwargs`
- additional properties

#### Impact

- Values you configured for these options now take effect, and can change the answers. A model that doesn't support one of them can reject the request: for example, `mistral-large-2512` answers `400 extra_forbidden` for `include_reasoning` and `max_completion_tokens`.
- An additional property whose key is the name of a field the request already has, such as `max_tokens` or `tool_choice`, is rejected with an `IllegalArgumentException`, so a field is never sent twice.

#### Migration

Remove the options and additional properties your model doesn't accept. For a field that has its own option, set the option instead of an additional property:

```java
// Before
WatsonxAiChatOptions.builder()
    .additionalProperties(Map.of("max_tokens", 500))
    .build();

// After
WatsonxAiChatOptions.builder()
    .maxTokens(500)
    .build();
```

### Chat options you don't set are no longer sent

Some models reject chat fields they don't support, so 2.1.0 stops sending values you didn't set ([#236](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/236), [#250](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/250)):

| Field | 2.0.0 | 2.1.0 |
|---|---|---|
| `max_completion_tokens` | `1024`, from the auto-configured default options | sent only when set |
| `logprobs` | `false`, from the auto-configured default options | sent only when set |

#### Impact

When you don't set these options, watsonx.ai applies its own defaults. Responses can then be longer than 1024 tokens, depending on the model.

#### Migration

If you relied on the old limit, set it explicitly:

```properties
spring.ai.watsonx.ai.chat.options.max-completion-tokens=1024
```

### Per-request chat options override the defaults

In 2.0.0, per-request options were partly or entirely lost ([#222](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/222), [#224](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/224)):

- **With `ChatClient`**, `model`, `temperature` and `top-p` couldn't be changed per request, and `response-format`, `seed`, tools and the tool choice weren't taken from the request at all.
- **With `ChatModel.call(Prompt)` or `stream(Prompt)`**, a prompt with its own options lost every default, including the model, and watsonx.ai answered `400 Either 'model_id' or 'model' must be specified`. A plain Spring AI `ChatOptions` threw a `ClassCastException`.

Now every value set in the request overrides the default, and unset values keep the default, with `ChatClient` and with `ChatModel`.

#### Impact

Per-request options that had no effect in 2.0.0 now apply. No migration is needed.

### Streaming errors throw Spring AI exceptions

When watsonx.ai rejects a streaming request, `stream()` now throws the same exceptions as `call()`, with watsonx.ai's error message ([#230](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/230)).

#### Impact

- With the default error handler, a 4xx is a `NonTransientAiException` and a 5xx a `TransientAiException` (both in `org.springframework.ai.retry`). In 2.0.0, `stream()` threw WebClient's `WebClientResponseException`, without watsonx.ai's message.
- A custom `ResponseErrorHandler` now applies to streams too.

#### Migration

```java
// Before
chatModel.stream(prompt)
    .onErrorResume(WebClientResponseException.class, ex -> fallback());

// After
chatModel.stream(prompt)
    .onErrorResume(NonTransientAiException.class, ex -> fallback());
```

### The model's earlier answers are sent as `assistant` messages

In a conversation, the model's earlier answers (an `AssistantMessage` without tool calls) were sent with the `system` role. They're now sent as `assistant` ([#255](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/255)).

#### Impact

Multi-turn answers, for example with chat memory, can change. No migration is needed.

### A forced tool choice calls the tool once

Two problems with a tool choice that forces a tool call are fixed ([#248](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/248)):

- **A named tool choice** (`tool-choice`) returned an empty answer, because watsonx.ai ends it with `finish_reason: "stop"`. The tool call is now returned, and `ChatClient` runs the tool.
- **With `ChatClient`, `tool-choice` or `tool-choice-option=required` called the tool again and again,** up to Spring AI's limit of 40 calls per tool, because the tool choice was sent again with the tool result. It's now left out after a tool result, so the tool runs once. `auto` and `none` are still sent.

No migration is needed.

### JSON schema response formats and images work

Two kinds of chat request that watsonx.ai rejected with `400 invalid_input_argument` now work. No migration is needed.

- **A JSON schema response format** without a name. The name now defaults to `custom_schema`, and `TextChatResponseFormat.Builder` gets `name(...)` and `strict(...)` ([#220](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/220)).
- **Images and videos given as bytes**, which are now sent as `data:` URLs ([#218](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/218)).

### Chat through deployments

You can now chat with a deployed prompt template, tuned model or custom foundation model by setting `spring.ai.watsonx.ai.chat.options.deployment-id`, or `deploymentId(...)` per request ([#224](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/224), [#257](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/257)). See [Deployments](./chat/index.md#deployments).

### watsonx.ai warnings are logged

watsonx.ai sends warnings such as a model's deprecation and withdrawal dates. Chat, embeddings and rerank now log them at DEBUG, and embedding responses keep them in their metadata under `warnings`, as chat already did ([#234](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/234)). To be told before a model you use is withdrawn:

```properties
logging.level.org.springaicommunity.watsonx.util.WatsonxAiWarnings=DEBUG
```

### A Granite Guardian "No" is not flagged

`WatsonxAiModerationModel` flagged every Granite Guardian detection, including the ones that answered "No" ([#253](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/253)).

#### Impact

A "No" now leaves the text unflagged. A "Yes", and any answer that is neither "Yes" nor "No", is still flagged. HAP and PII detections don't change. No migration is needed.

### Text extraction goes through project or space storage

The watsonx.ai Text Extraction API reads documents from the storage of a project or space. It doesn't accept an uploaded file, so extracting a file in 2.0.0 failed with `400 invalid_content_type`. `WatsonxAiTextExtractionModel` now uploads the resource to that storage, runs the extraction job, polls it until it ends, downloads the result, and then deletes the files and the job ([#216](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/216)).

#### Impact

- Extracting text from a resource needs a project or space ID with storage, and a task credential. See [Text Extraction](./text-extraction/) for the prerequisites.
- `WatsonxAiTextExtractionApi.extract()` throws an `IllegalArgumentException` when the request has a `resource`. It only accepts requests that reference documents already in storage.
- Extracting from a resource supports exactly one requested output: `md` (the default), `plain_text`, `html`, `assembly` or `docling_doc`.
- `WatsonxAiTextExtractionResponse.getStatus()` returns the job's status, or `null` when there is none. In 2.0.0 it returned `"completed"` when it couldn't read one.
- `extract(WatsonxAiTextExtractionRequest)` on the model replaced the request's parameters with the default options. Now the request's own parameters win, and the defaults only fill in the ones it doesn't set.
- The default `spring.ai.watsonx.ai.text-extraction.version` changed from `2024-05-31` to `2024-10-17`.
- The new properties `poll-timeout` (default `5m`), `delete-files` (default `true`) and `dataplatform-url` (derived from the base URL for IBM Cloud regions) control the new flow.

#### Migration

If you called `WatsonxAiTextExtractionApi.extract()` with a resource, call `WatsonxAiTextExtractionModel` instead:

```java
// Before
watsonxAiTextExtractionApi.extract(
    WatsonxAiTextExtractionRequest.builder().resource(resource).build());

// After
WatsonxAiTextExtractionResponse response = watsonxAiTextExtractionModel.extract(resource);
String text = response.getText();
```

Code that already used `WatsonxAiTextExtractionModel` keeps working without changes, once the project or space storage and the task credential are in place.

### Text extraction options renamed

The options now match the parameters of the Text Extraction API ([#216](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/216)). The old ones are deprecated and still work:

| Deprecated | Replacement |
|---|---|
| `outputFormats` (`output-formats`) | `requestedOutputs` (`requested-outputs`) |
| `enableOcr` (`enable-ocr`), a `Boolean` | `ocrMode` (`ocr-mode`): `"enabled"`, `"disabled"` or `"forced"` |
| `model` | none: the API has no model parameter, so the value was never used |

`mode` (`standard` or `high_quality`) is new.

#### Migration

```java
// Before
WatsonxAiTextExtractionOptions.builder()
    .outputFormats(List.of("md"))
    .enableOcr(true)
    .build();

// After
WatsonxAiTextExtractionOptions.builder()
    .requestedOutputs(List.of("md"))
    .ocrMode("enabled")
    .build();
```

### `ExtractionParameters` has the API's parameters

`WatsonxAiTextExtractionRequest.ExtractionParameters` now has the components the Text Extraction API accepts ([#216](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/216)).

#### Impact

This affects you only if you create `ExtractionParameters` yourself or read its components. `ExtractionParameters.of(options)` is unchanged, and maps the deprecated options.

#### Migration

```java
// Before
new ExtractionParameters(model, outputFormats, languages, enableOcr);

// After
new ExtractionParameters(requestedOutputs, mode, ocrMode, languages);
```

### `WatsonxAiChatRequest.toolChoice()` returns a single tool

watsonx.ai accepts one tool in `tool_choice` and rejects a list. `WatsonxAiChatRequest` now holds a single `TextChatToolChoiceTool` ([#236](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/236)).

#### Impact

This affects you only if you build a `WatsonxAiChatRequest` yourself or read its tool choice. Applications that use `ChatClient` or `WatsonxAiChatModel` with `WatsonxAiChatOptions` are not affected.

- `toolChoice()` returns `TextChatToolChoiceTool` instead of `List<TextChatToolChoiceTool>`. Code that calls it must be recompiled, and code that treats the result as a list no longer compiles.
- `Builder.toolChoice(List<TextChatToolChoiceTool>)` is deprecated for removal. It still works with a list of at most one tool, and rejects a longer list.
- Because of the two overloads, `toolChoice(null)` no longer compiles. Cast the argument: `toolChoice((TextChatToolChoiceTool) null)`.

#### Migration

```java
// Before
TextChatToolChoiceTool tool = request.toolChoice().get(0);
WatsonxAiChatRequest.builder().toolChoice(List.of(tool));

// After
TextChatToolChoiceTool tool = request.toolChoice();
WatsonxAiChatRequest.builder().toolChoice(tool);
```

### Native images work without workarounds

Native executables built with 2.0.0 sent chat requests as `{}` and authenticated with `Bearer null` ([#199](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/199), [#211](https://github.com/spring-ai-community/spring-ai-watsonx-ai/pull/211)).

#### Migration

If you worked around that, remove:

- runtime hints that register `com.ibm.cloud.sdk.core.security.IamToken` for reflection
- `@ImportRuntimeHints(WatsonxAiRuntimeHints.class)`
- a `spring-ai-rag` dependency added only so that AOT processing works
