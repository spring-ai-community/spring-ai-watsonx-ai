---
sidebar_position: 3
---

# Upgrade Notes

This page lists the changes you may need to act on when you upgrade Spring AI watsonx.ai. Each entry explains who is affected and how to migrate. The full list of changes in a release is in the [GitHub release notes](https://github.com/spring-ai-community/spring-ai-watsonx-ai/releases).

## Upgrading to 1.2.0

1.2.0 backports bug fixes from 2.x to the 1.x line. Most of them need no change on your side, but a few change a default, a request sent to watsonx.ai, or a public API.

### Default models replaced

IBM withdrew the models that were used as defaults, so the defaults now point to supported models:

| Property | 1.1.x default | 1.2.0 default |
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

### `WatsonxAiChatRequest.toolChoice()` returns a single tool

watsonx.ai accepts one tool in `tool_choice` and rejects a list. `WatsonxAiChatRequest` now holds a single `TextChatToolChoiceTool`.

#### Impact

This affects you only if you build a `WatsonxAiChatRequest` yourself or read its tool choice. Applications that use `ChatClient` or `WatsonxAiChatModel` with `WatsonxAiChatOptions` are not affected.

- `toolChoice()` returns `TextChatToolChoiceTool` instead of `List<TextChatToolChoiceTool>`. Code that calls it must be recompiled, and code that treats the result as a list no longer compiles.
- `Builder.toolChoice(List<TextChatToolChoiceTool>)` is deprecated for removal. It still works with a list of at most one tool, and rejects a longer list.

#### Migration

```java
// Before
TextChatToolChoiceTool tool = request.toolChoice().get(0);
WatsonxAiChatRequest.builder().toolChoice(List.of(tool));

// After
TextChatToolChoiceTool tool = request.toolChoice();
WatsonxAiChatRequest.builder().toolChoice(tool);
```

### Chat options you don't set are no longer sent

Some models reject chat fields they don't support. For example, `mistral-large-2512` answers `400 extra_forbidden` for `include_reasoning` and `max_completion_tokens`. So 1.2.0 stops sending values you didn't set:

| Field | 1.1.x | 1.2.0 |
|---|---|---|
| `include_reasoning` | always sent as `true` | sent only when set |
| `max_completion_tokens` | `1024`, from the auto-configured default options | sent only when set |
| `logprobs` | `false`, from the auto-configured default options | sent only when set |

#### Impact

When you don't set these options, watsonx.ai applies its own defaults. Responses can then be longer than 1024 tokens, or leave out reasoning content, depending on the model. `WatsonxAiChatOptions.isIncludeReasoning()` still returns `true` when the option is unset.

#### Migration

If you relied on the old values, set them explicitly:

```properties
spring.ai.watsonx.ai.chat.options.include-reasoning=true
spring.ai.watsonx.ai.chat.options.max-completion-tokens=1024
```

### `guided-json` and `chat-template-kwargs` work

In 1.1.x, setting `guidedJson` or `chatTemplateKwargs` made every chat call fail with a `MismatchedInputException`. They are now sent to watsonx.ai. No migration is needed, but if you removed them to work around the failure, you can set them again.

### Text extraction goes through project or space storage

The watsonx.ai Text Extraction API reads documents from the storage of a project or space. It doesn't accept an uploaded file. `WatsonxAiTextExtractionModel` now uploads the resource to that storage, runs the extraction job, polls it until it ends, downloads the result, and then deletes the files and the job.

#### Impact

- Extracting text from a resource needs a project or space ID with storage, and a task credential. See [Text Extraction](./text-extraction/) for the prerequisites.
- `WatsonxAiTextExtractionApi.extract()` throws an `IllegalArgumentException` when the request has a `resource`. It only accepts requests that reference documents already in storage.
- Extracting from a resource supports exactly one requested output: `md` (the default), `plain_text`, `html`, `assembly` or `docling_doc`.
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

The options now match the parameters of the Text Extraction API. The old ones are deprecated and still work:

| Deprecated | Replacement |
|---|---|
| `outputFormats` | `requestedOutputs` |
| `enableOcr` (`Boolean`) | `ocrMode` (`"enabled"`, `"disabled"`, ...) |
| `model` | none: the API has no model parameter, so the value was never used |

`mode` is new.

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

### Additional chat properties are sent, and can't replace an option

In 1.1.x, the additional properties set in `WatsonxAiChatOptions` were silently dropped. They are now sent as top-level fields of the chat request.

#### Impact

- Additional properties you set before, and that had no effect, now reach watsonx.ai. A model that doesn't support one of them can reject the request.
- A key that's the name of a field the request already has, such as `max_tokens` or `tool_choice`, is rejected with an `IllegalArgumentException`, so a field is never sent twice.

#### Migration

Remove additional properties you don't need. For a field that has its own option, set the option instead:

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
