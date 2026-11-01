---
sidebar_position: 1
---

# Chat Models

Watsonx.ai Chat Models provide powerful conversational AI capabilities for building intelligent applications. The Spring AI Watsonx.ai integration supports various foundation models available in IBM's Watsonx.ai platform.

## Supported Models

The following foundation models are supported:

- **IBM Granite models** - IBM's enterprise-focused language models
- **Meta Llama models** - Meta's open-source language models
- **Mistral AI models** - Mistral's efficient language models
- **Other foundation models** available in your Watsonx.ai deployment

## Auto-configuration

Spring AI provides Spring Boot auto-configuration for the Watsonx.ai Chat Model. To enable it, add the following dependency to your project's Maven `pom.xml` file:

:::tip
Check [Maven Central](https://central.sonatype.com/search?namespace=org.springaicommunity&name=spring-ai-starter-model-watsonx-ai) for the latest version.
:::

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-starter-model-watsonx-ai</artifactId>
    <version>2.1.0</version>
</dependency>
```

Or to your Gradle `build.gradle` build file:

```groovy
dependencies {
    implementation 'org.springaicommunity:spring-ai-starter-model-watsonx-ai:2.1.0'
}
```

## Configuration Properties

The prefix `spring.ai.watsonx.ai.chat` is used as the property prefix that lets you configure the connection to Watsonx.ai.

| Property                                                 | Default                  | Required | Description                               |
| :------------------------------------------------------- | :----------------------- | :------- | :---------------------------------------- |
| `spring.ai.watsonx.ai.api-key`                           |                          | true     | Your Watsonx.ai API key                   |
| `spring.ai.watsonx.ai.base-url`                          |                          | true     | Your Watsonx.ai service URL               |
| `spring.ai.watsonx.ai.project-id`                        |                          | one of   | Your Watsonx.ai project ID. Set either this or `space-id`, not both |
| `spring.ai.watsonx.ai.space-id`                          |                          | one of   | Your Watsonx.ai deployment space ID. Set either this or `project-id`, not both |
| `spring.ai.watsonx.ai.chat.text-endpoint`                | `/ml/v1/text/chat`       | false    | The chat endpoint                         |
| `spring.ai.watsonx.ai.chat.stream-endpoint`              | `/ml/v1/text/chat_stream`| false    | The streaming chat endpoint               |
| `spring.ai.watsonx.ai.chat.version`                      | `2024-10-17`             | false    | API version date, in `YYYY-MM-DD` format  |
| `spring.ai.watsonx.ai.chat.options.model`                | `ibm/granite-4-h-small`  | false    | The model to use for chat completions     |
| `spring.ai.watsonx.ai.chat.options.temperature`          | `0.7`                    | false    | Controls randomness in the response       |
| `spring.ai.watsonx.ai.chat.options.top-p`                | `1.0`                    | false    | Controls diversity via nucleus sampling   |
| `spring.ai.watsonx.ai.chat.options.presence-penalty`     | `0.0`                    | false    | Penalty for tokens that already appear in the text |
| `spring.ai.watsonx.ai.chat.options.n`                    | `1`                      | false    | Number of chat completion choices to generate |
| `spring.ai.watsonx.ai.chat.options.max-completion-tokens`|                          | false    | Maximum number of tokens to generate      |
| `spring.ai.watsonx.ai.chat.options.deployment-id`        |                          | false    | Chat through a watsonx.ai deployment instead of a model. See [Deployments](#deployments) |

The other options in [WatsonxAiChatOptions](#watsonxaichatoptions) can be set the same way, for example `spring.ai.watsonx.ai.chat.options.seed` or `spring.ai.watsonx.ai.chat.options.reasoning-effort`.

:::tip
All properties prefixed with `spring.ai.watsonx.ai.chat.options` can be overridden at runtime by adding a request specific [runtime options](#runtime-options) to the `Prompt` call.
:::

## Runtime Options

The `WatsonxAiChatOptions.java` provides model configurations, such as the model to use, the temperature, max tokens, etc.

On start-up, the default options can be configured with the `WatsonxAiChatModel(api, options)` constructor or the `spring.ai.watsonx.ai.chat.options.*` properties.

At runtime you can override the default options by adding new ones, using the `WatsonxAiChatOptions.Builder`, to a `Prompt` call. For example to override the default temperature for a specific request:

```java
ChatResponse response = chatModel.call(
    new Prompt(
        "Generate the names of 5 famous pirates.",
        WatsonxAiChatOptions.builder()
            .temperature(0.4)
            .build()
    ));
```

:::tip
In addition to the model specific `WatsonxAiChatOptions` you can use a portable [ChatOptions](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_chatclient_api) instance, created with the `ChatOptionsBuilder#builder()`.
:::

## Sample Controller

```java
@RestController
public class ChatController {

    private final WatsonxAiChatModel chatModel;

    public ChatController(WatsonxAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/ai/generate")
    public Map generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Map.of("generation", chatModel.call(message));
    }

    @GetMapping("/ai/generateStream")
    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return chatModel.stream(prompt);
    }
}
```

## Deployments

To chat with a model deployed in a watsonx.ai deployment space, such as a deployed prompt template or tuned model, set the deployment ID or serving name. Requests then go to `/ml/v1/deployments/{id}/text/chat` (and `chat_stream` for streaming).

For every request:

```properties
spring.ai.watsonx.ai.chat.options.deployment-id=<deployment ID or serving name>
```

Or per request, with `ChatClient`:

```java
String answer = ChatClient.create(chatModel)
    .prompt("Hello")
    .options(WatsonxAiChatOptions.builder().deploymentId("<deployment ID or serving name>"))
    .call()
    .content();
```

The deployment decides the model and its parameters. watsonx.ai only accepts the messages, tools and tool choice (`tool-choice-option`, `tool-choice`) for a deployment, so the other options (`model`, `temperature`, `max-tokens`, `response-format`, ...) are not sent. Tool calling and streaming work as usual.

:::note
For a deployed chat prompt template, watsonx.ai applies some of its generation parameters (such as `max_new_tokens` and `stop_sequences`), but not its `instruction` or `chat_items`. Send a system message yourself if you need one, for example with `ChatClient.builder(chatModel).defaultSystem(...)`. watsonx.ai rejects the `system` role in a deployment request, so the text of the system messages is put at the start of the first user message.
:::

## Manual Configuration

The `WatsonxAiChatModel` implements the `ChatModel` and `StreamingChatModel` and uses the [Low-level WatsonxAiChatApi](#low-level-watsonxaichatapi) to connect to the Watsonx.ai service.

Add the `spring-ai-watsonx-ai-core` dependency to your project's Maven `pom.xml` file:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>watsonx-ai-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

:::tip
Refer to the [Getting Started](../getting-started/index.md) guide for information about adding dependencies to your build file.
:::

Next, create a `WatsonxAiChatModel` and use it for text generations:

```java
var watsonxAiApi = new WatsonxAiChatApi(baseUrl, "/ml/v1/text/chat", "/ml/v1/text/chat_stream",
    "2024-10-17", projectId, null, apiKey,
    RestClient.builder(), WebClient.builder(), responseErrorHandler);

var chatModel = WatsonxAiChatModel.builder()
    .watsonxAiChatApi(watsonxAiApi)
    .options(WatsonxAiChatOptions.builder()
        .model("ibm/granite-4-h-small")
        .temperature(0.4)
        .maxCompletionTokens(200)
        .build())
    .build();

ChatResponse response = chatModel.call(
    new Prompt("Generate the names of 5 famous pirates."));

// Or with streaming
Flux<ChatResponse> response = chatModel.stream(
    new Prompt("Generate the names of 5 famous pirates."));
```

The `WatsonxAiChatOptions` provides the configuration information for the chat requests. The `WatsonxAiChatOptions.Builder` is fluent options builder.

## Low-level WatsonxAiChatApi

The `WatsonxAiChatApi` provides is a lightweight Java client on top of Watsonx.ai [Chat Completions API](https://cloud.ibm.com/apidocs/watsonx-ai).

Here is a simple snippet showing how to use the api programmatically:

```java
WatsonxAiChatApi watsonxAiApi = new WatsonxAiChatApi(baseUrl, "/ml/v1/text/chat", "/ml/v1/text/chat_stream",
    "2024-10-17", projectId, null, apiKey,
    RestClient.builder(), WebClient.builder(), responseErrorHandler);

WatsonxAiChatRequest request = WatsonxAiChatRequest.builder()
    .model("ibm/granite-4-h-small")
    .messages(List.of(new TextChatMessage(
        "Tell me about 3 famous pirates from the Golden Age of Piracy and why they were famous.", null)))
    .temperature(0.8)
    .maxCompletionTokens(300)
    .build();

WatsonxAiChatResponse response = watsonxAiApi.chat(request).getBody();

// Or stream it
Flux<WatsonxAiChatStream> stream = watsonxAiApi.stream(request);
```

Follow the `WatsonxAiChatApi.java`'s JavaDoc for further information.

## WatsonxAiChatOptions

The `WatsonxAiChatOptions` class provides various options for configuring chat requests:

| Option                | Default                 | Description                                                                       |
| :-------------------- | :---------------------- | :-------------------------------------------------------------------------------- |
| `model`               | `ibm/granite-4-h-small` | The foundation model to use for chat completions                                  |
| `deploymentId`        | `null`                  | Chat through a watsonx.ai deployment. See [Deployments](#deployments)             |
| `temperature`         | `0.7`                   | Controls randomness in the response. Higher values make output more random        |
| `topP`                | `1.0`                   | Controls diversity via nucleus sampling. Lower values focus on more likely tokens |
| `maxCompletionTokens` | `null`                  | Maximum number of tokens to generate in the completion                            |
| `maxTokens`           | `null`                  | Deprecated by watsonx.ai in favor of `maxCompletionTokens`, which wins if both are set |
| `n`                   | `1`                     | Number of chat completion choices to generate. You are charged for all of them    |
| `stopSequences`       | `[]`                    | List of strings that will stop generation when encountered                        |
| `presencePenalty`     | `0.0`                   | Penalty for new tokens based on their presence in the text so far (−2 to 2)       |
| `frequencyPenalty`    | `null`                  | Penalty for new tokens based on their frequency in the text so far (−2 to 2)      |
| `seed`                | `null`                  | Set the same value to get repeatable results                                      |
| `logitBias`           | `null`                  | Map of token ID to a bias that makes the token more or less likely                |
| `timeLimit`           | `null`                  | Time limit in milliseconds. If generation isn't done by then, the text so far is returned with the `time_limit` finish reason. Must be greater than 0 |
| `logprobs`            | `null`                  | When `true`, returns log probabilities of the output tokens                       |
| `topLogprobs`         | `null`                  | Number of most likely tokens (0–20) to return at each position. Requires `logprobs: true` |
| `responseFormat`      | `null`                  | Force JSON output. See [Structured Output](#structured-output)                    |
| `guidedChoice`, `guidedRegex`, `guidedGrammar`, `guidedJson` | `null` | Constrain the output. See [Guided Decoding](#guided-decoding)        |
| `includeReasoning`, `reasoningEffort` | `null`  | Control the model's reasoning. See [Reasoning](#reasoning)                        |
| `chatTemplateKwargs`  | `null`                  | Extra arguments for the model's chat template, as a map or a JSON string          |
| `tools`, `toolChoiceOption`, `toolChoice`, `toolCallbacks`, `toolContext` | `null` | Tool calling. See [Function Calling](#function-calling) |
| `additionalProperties` | none                   | Request fields without an option of their own. See below                          |

:::note
watsonx.ai has no `top_k` or `repetition_penalty` chat option. `getTopK()` returns `null` and logs a warning. To send `repetition_penalty`, use [`additionalProperty`](#parameters-without-their-own-option).
:::

### Parameters Without Their Own Option

For a watsonx.ai chat parameter that has no option of its own, such as `repetition_penalty`, use `additionalProperty(...)` or `additionalProperties(...)`. Each one is sent as a top-level field of the request, with camelCase keys turned into snake_case:

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .model("ibm/granite-4-h-small")
    .additionalProperty("repetition_penalty", 2.0)
    .build();
```

A key that is the name of a field with its own option, such as `max_completion_tokens`, is rejected with an `IllegalArgumentException`: set that option instead. watsonx.ai accepts unknown fields without an error or a warning, so check the spelling of the parameter name.

## Structured Output

Set `responseFormat` to make the model answer with JSON. `TextChatResponseFormat.jsonObject()` guarantees valid JSON. To also follow a schema, build a `JSON_SCHEMA` format:

```java
String schema = """
    {"type":"object","properties":{"city":{"type":"string"},"temperature":{"type":"number"}},
     "required":["city","temperature"]}
    """;

WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .responseFormat(TextChatResponseFormat.builder()
        .schema(schema)
        .name("weather")     // defaults to "custom_schema"
        .strict(true)
        .build())
    .build();

String json = ChatClient.create(chatModel)
    .prompt("What's the weather like in Paris? Return it as JSON.")
    .options(options)
    .call()
    .content();
```

## Guided Decoding

Guided decoding constrains the tokens the model can generate. Use one of these options per request:

| Option          | Constrains the output to                                  |
| :-------------- | :-------------------------------------------------------- |
| `guidedChoice`  | One of the strings in a list                              |
| `guidedRegex`   | Text that matches a regular expression                    |
| `guidedGrammar` | Text that matches a context-free grammar                  |
| `guidedJson`    | JSON that follows a JSON schema, as a map or a JSON string |

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .guidedChoice(List.of("positive", "negative", "neutral"))
    .build();

String sentiment = chatModel.call(new Prompt("Classify the sentiment: 'I love this product!'", options))
    .getResult().getOutput().getText();
```

:::note
Guided decoding depends on the model. If the model doesn't support it, watsonx.ai returns an error.
:::

## Reasoning

For models that reason before they answer, `reasoningEffort` sets how much (`low`, `medium` or `high`): less effort gives faster answers, fewer tokens and a shorter `reasoning_content`. `includeReasoning` (the API default is `true`) decides whether `reasoning_content` is returned.

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .model("openai/gpt-oss-120b")
    .reasoningEffort("low")
    .includeReasoning(false)
    .build();
```

`chatTemplateKwargs` passes extra arguments to the model's chat template, for models that read them there:

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .chatTemplateKwargs(Map.of("enable_thinking", false))
    .build();
```

## Multimodal Input

Attach Spring AI `Media` to a `UserMessage` to send images, video or audio to a multimodal model. `WatsonxAiChatModel` maps the media by its MIME type:

| MIME type                | Sent as                                                        |
| :----------------------- | :------------------------------------------------------------- |
| `image/*`                | `image_url`, with a `data:` URL for bytes or the URL you give  |
| `video/*`                | `video_url`, with a `data:` URL for bytes or the URL you give  |
| `audio/mp3`, `audio/wav` | `input_audio`, as base64 data, with the matching audio format  |

Any other type throws an `IllegalArgumentException`.

```java
UserMessage message = UserMessage.builder()
    .text("What is in this picture?")
    .media(new Media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("/photo.png")))
    .build();

ChatResponse response = chatModel.call(new Prompt(message,
    WatsonxAiChatOptions.builder().model("meta-llama/llama-4-maverick-17b-128e-instruct-fp8").build()));
```

:::tip
Use a model that accepts the input type. Check the [supported models](https://www.ibm.com/watsonx/developer/get-started/models) for the ones that take images, video or audio.
:::

## Response Metadata

`ChatResponse.getMetadata()` carries the watsonx.ai response ID, model, usage, and these extra keys:

| Key             | Value                                                               |
| :-------------- | :------------------------------------------------------------------ |
| `created`       | When watsonx.ai created the response                                |
| `model_version` | The version of the model                                            |
| `warnings`      | Warnings from watsonx.ai, such as a model deprecation, when present |

The generation metadata has the finish reason, and the `logprobs` when you [enable them](#log-probabilities). watsonx.ai repeats a warning in every response, so it is logged at `DEBUG` level only. To see them:

```properties
logging.level.org.springaicommunity.watsonx.util.WatsonxAiWarnings=DEBUG
```


## Log Probabilities

Log probabilities (logprobs) allow you to inspect how confident the model was in each generated token, and what the alternative token choices were. When enabled, every token in the response is annotated with its log probability and, optionally, the top-N alternative tokens at that position.

### Enabling Log Probabilities

Set `logprobs` to `true` in your `WatsonxAiChatOptions` to receive log probability data:

```java
ChatResponse response = chatModel.call(
    new Prompt(
        "Tell me a fun fact.",
        WatsonxAiChatOptions.builder()
            .logProbs(true)
            .build()
    ));
```

To also receive the top-N alternative tokens at each position, set `topLogprobs` (0–20). `logprobs` must be `true` when `topLogprobs` is provided:

```java
ChatResponse response = chatModel.call(
    new Prompt(
        "Tell me a fun fact.",
        WatsonxAiChatOptions.builder()
            .logProbs(true)
            .topLogprobs(5)
            .build()
    ));
```

### Accessing Log Probability Metadata

Log probability data is stored in the generation metadata under the key `"logprobs"` as a `TextChatLogProbs` object:

```java
import org.springaicommunity.watsonx.chat.WatsonxAiChatResponse.TextChatLogProbs;

ChatResponse response = chatModel.call(prompt);

TextChatLogProbs logprobs =
    (TextChatLogProbs) response.getResult().getMetadata().get("logprobs");

// Iterate over each generated token
for (var tokenInfo : logprobs.content()) {
    System.out.printf("Token: %-15s logprob: %.4f%n",
        tokenInfo.token(), tokenInfo.logprob());

    // Top alternative tokens (populated when topLogprobs > 0)
    if (tokenInfo.topLogprobs() != null) {
        for (var alt : tokenInfo.topLogprobs()) {
            System.out.printf("  alt: %-15s logprob: %.4f%n",
                alt.token(), alt.logprob());
        }
    }
}
```

Log probabilities are equally available for streaming responses — the data is carried on each `ChatResponse` chunk and accumulated through the stream merger.

### Log Probability Data Model

The following types represent the logprobs payload returned by the API:

| Type                     | Field          | Type                          | Description                                             |
| :----------------------- | :------------- | :---------------------------- | :------------------------------------------------------ |
| `TextChatLogProbs`       | `content`      | `List<TextChatLogProbsContent>` | Per-token log probability data for the generated text |
| `TextChatLogProbs`       | `refusal`      | `List<TextChatLogProbsContent>` | Per-token log probability data for any refusal text   |
| `TextChatLogProbsContent`| `token`        | `String`                      | The generated token string                              |
| `TextChatLogProbsContent`| `logprob`      | `Double`                      | Log probability of this token (range: −∞ to 0)         |
| `TextChatLogProbsContent`| `bytes`        | `List<Integer>`               | UTF-8 byte values of the token                          |
| `TextChatLogProbsContent`| `topLogprobs`  | `List<TextChatTopLogProbs>`   | Alternative tokens at this position                     |
| `TextChatTopLogProbs`    | `token`        | `String`                      | Alternative token string                                |
| `TextChatTopLogProbs`    | `logprob`      | `Double`                      | Log probability of the alternative token                |
| `TextChatTopLogProbs`    | `bytes`        | `List<Integer>`               | UTF-8 byte values of the alternative token              |

### Validation Rules

- Setting `logprobs` to `false` when `topLogprobs` is already set throws an `IllegalArgumentException`.
- Setting `topLogprobs` when `logprobs` is `false` throws an `IllegalArgumentException`.
- `topLogprobs` may be set independently of `logprobs` when `logprobs` is `null` (i.e., not yet configured); the model will validate the combination at request time.

## Function Calling

You can register custom Java functions with the `WatsonxAiChatModel` and have the Watsonx.ai model intelligently choose to output a JSON object containing arguments to call one or many of the registered functions. This allows you to connect the LLM capabilities with external tools and APIs.

Function calling works by:

1. **Defining tools** - You register functions that the model can call
2. **Model decision** - The model analyzes the user's request and decides which tool(s) to invoke
3. **Tool execution** - Spring AI executes the selected tools with the model-provided arguments
4. **Response generation** - The model uses the tool results to generate a final response

### Registering Functions as Spring Beans

The simplest approach is to annotate the methods of a class with `@Tool` and pass an instance of it to `ChatClient`:

```java
@Component
public class WeatherTools {

    public enum Unit { C, F }
    public record Response(double temp, Unit unit, String location) {}

    @Tool(description = "Get the weather in a location")
    public Response currentWeather(String location, Unit unit) {
        return new Response(30.0, unit, location);
    }
}
```

```java
@RestController
public class WeatherController {

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;

    public WeatherController(WatsonxAiChatModel chatModel, WeatherTools weatherTools) {
        this.chatClient = ChatClient.create(chatModel);
        this.weatherTools = weatherTools;
    }

    @GetMapping("/ai/weather")
    public String weather(String location) {
        return chatClient.prompt()
            .user("What's the weather like in " + location + "?")
            .tools(weatherTools)
            .call()
            .content();
    }
}
```

To call `WatsonxAiChatModel` directly, wrap the function in a `ToolCallback` and set it on the options, as in the next section.

### Using ToolCallback for Programmatic Tool Registration

For more programmatic control, use `ToolCallback` and `FunctionToolCallback`. This approach is useful when you need to create tools dynamically at runtime or wrap existing functions:

```java
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import java.util.function.Function;

public class WeatherRequest {
    private String city;
    private String unit;

    // Constructors, getters, setters
}

public class WeatherResponse {
    private double temperature;
    private String unit;
    private String description;

    // Constructors, getters, setters
}

// Define your function
Function<WeatherRequest, WeatherResponse> weatherFunction =
    request -> new WeatherResponse(22.0, request.getUnit(), "Sunny");

// Create a ToolCallback
ToolCallback weatherToolCallback = FunctionToolCallback.builder(
        "getCurrentWeather",      // Tool name
        weatherFunction           // The function to execute
    )
    .description("Get the current weather for a given city")
    .inputType(WeatherRequest.class)
    .build();
```

### Using Tools with ChatClient

The `ChatClient` API provides a fluent interface for working with tools:

```java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(WatsonxAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @GetMapping("/ai/weather")
    public String getWeather(@RequestParam String city) {
        // Using ToolCallback
        Function<WeatherRequest, WeatherResponse> weatherFn =
            req -> new WeatherResponse(22.0, req.getUnit(), "Sunny");

        ToolCallback weatherTool = FunctionToolCallback.builder(
                "getCurrentWeather",
                weatherFn
            )
            .description("Get current weather for a city")
            .inputType(WeatherRequest.class)
            .build();

        return chatClient.prompt()
            .user("What's the weather in " + city + "?")
            .toolCallbacks(weatherTool)  // Use toolCallbacks() for ToolCallback
            .call()
            .content();
    }
}
```

#### Choosing Tools per Request

`.tools(...)` takes objects with `@Tool` methods, and `.toolCallbacks(...)` takes `ToolCallback`s. Pass only the ones a request needs:

```java
@GetMapping("/ai/calculate")
public String calculate(@RequestParam String question) {
    return chatClient.prompt()
        .user(question)
        .tools(weatherTools, calculatorTools)
        .call()
        .content();
}
```

This approach allows you to:

- Control which tools are available for each request
- Combine tools from several classes
- Mix `.tools(...)` and `.toolCallbacks(...)` in the same request

### Forcing a Tool Call

Set `toolChoiceOption` to `"required"` to make the model call a tool, `"none"` to stop it from calling one, or `"auto"` (the default) to let it decide. To force one specific tool, set `toolChoice`:

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .toolChoice(new WatsonxAiChatRequest.TextChatToolChoiceTool(ToolType.FUNCTION,
        new WatsonxAiChatRequest.TextChatToolChoiceFunction("getCurrentWeather")))
    .build();
```

:::warning[Upgrading to 2.1.0]
watsonx.ai accepts one tool in `tool_choice`, so `toolChoice` now takes a single `TextChatToolChoiceTool` instead of a `List<TextChatToolChoiceTool>`. The `List` overload of `WatsonxAiChatRequest.Builder#toolChoice` is deprecated for removal: pass the tool itself.
:::

### Using Multiple Tools

The model can intelligently call multiple tools in a single conversation:

```java
// Create multiple tool callbacks
ToolCallback weatherTool = FunctionToolCallback.builder(
        "getCurrentWeather",
        weatherFunction
    )
    .description("Get weather information")
    .inputType(WeatherRequest.class)
    .build();

ToolCallback dateTool = FunctionToolCallback.builder(
        "getCurrentDate",
        (Void input) -> LocalDate.now().toString()
    )
    .description("Get the current date")
    .build();

String response = chatClient.prompt()
    .user("What's the weather today in Paris?")
    .toolCallbacks(weatherTool, dateTool)  // Multiple tools
    .call()
    .content();
```

The model will:

1. Recognize it needs both date and weather information
2. Call `getCurrentDate()` to get the date
3. Call `getCurrentWeather()` to get weather data
4. Combine the results into a coherent response

### Combining Tools with Response Formats

You can combine tool calling with structured output formats like JSON:

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .model("ibm/granite-3-2b-instruct")
    .responseFormat(TextChatResponseFormat.jsonObject())
    .build();

String response = chatClient.prompt()
    .user("What's the weather like in Paris? Return as JSON.")
    .toolCallbacks(weatherToolCallback)
    .options(options)
    .call()
    .content();

// Response will be structured JSON:
// {"city":"Paris","temperature":15.0,"unit":"C","condition":"Cloudy"}
```

### Best Practices for Function Calling

#### Provide Clear Descriptions

Clear descriptions help the model understand when to use each tool:

```java
ToolCallback tool = FunctionToolCallback.builder("getCurrentWeather", weatherFunction)
    .description("Get the current weather for a specific city. " +
                 "Requires city name and temperature unit (C or F).")
    .inputType(WeatherRequest.class)
    .build();
```

#### Validate Tool Inputs

Always validate inputs to handle edge cases:

```java
Function<WeatherRequest, WeatherResponse> weatherFunction = request -> {
    if (request.getCity() == null || request.getCity().isBlank()) {
        throw new IllegalArgumentException("City name is required");
    }

    if (!List.of("C", "F").contains(request.getUnit())) {
        throw new IllegalArgumentException("Unit must be C or F");
    }

    // Implementation
    return weatherService.getWeather(request.getCity(), request.getUnit());
};
```

#### Handle Errors Gracefully

Implement proper error handling in your tools:

```java
Function<WeatherRequest, WeatherResponse> weatherFunction = request -> {
    try {
        return weatherService.getWeather(request.getCity(), request.getUnit());
    } catch (Exception e) {
        logger.error("Failed to get weather", e);
        return new WeatherResponse(0.0, request.getUnit(), "Weather data unavailable");
    }
};
```

### Advanced Usage: Dynamic Tool Registration

Create tools dynamically based on runtime conditions:

```java
@Service
public class DynamicToolService {

    public List<ToolCallback> createToolsForUser(String userId) {
        List<ToolCallback> tools = new ArrayList<>();

        // Add tools based on user permissions
        if (hasPermission(userId, "weather")) {
            tools.add(createWeatherTool());
        }

        if (hasPermission(userId, "calendar")) {
            tools.add(createCalendarTool());
        }

        return tools;
    }

    private ToolCallback createWeatherTool() {
        return FunctionToolCallback.builder(
                "getCurrentWeather",
                weatherFunction
            )
            .description("Get weather information")
            .inputType(WeatherRequest.class)
            .build();
    }
}
```

### Tool Execution with Streaming

Tools work seamlessly with streaming responses:

```java
Flux<ChatResponse> stream = chatClient.prompt()
    .user("What's the weather in multiple cities?")
    .toolCallbacks(weatherToolCallback)
    .stream()
    .chatResponse();

stream.subscribe(response -> {
    System.out.println(response.getResult().getOutput().getText());
});
```

### Tool Definitions Without a Callback

`WatsonxAiChatOptions.builder().tools(...)` sends tool definitions (`WatsonxAiChatRequest.TextChatParameterTool`) that have no `ToolCallback`. They're sent together with the tools from tool callbacks; for the same name, the tool callback wins. Use them when your code runs the tools itself: call the chat model directly, and read the tool calls from the response:

```java
WatsonxAiChatOptions options = WatsonxAiChatOptions.builder()
    .model("ibm/granite-4-h-small")
    .tools(List.of(new WatsonxAiChatRequest.TextChatParameterTool(ToolType.FUNCTION,
        new WatsonxAiChatRequest.TextChatParameterFunction("get_weather", "Get the current weather for a city",
            Map.of("type", "object", "properties", Map.of("city", Map.of("type", "string")), "required", List.of("city"))))))
    .build();

ChatResponse response = chatModel.call(new Prompt("What is the weather in Manila?", options));
List<AssistantMessage.ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
```

With Spring AI's automatic tool execution (for example through `ChatClient`), a tool call needs a `ToolCallback` to run: Spring AI fails with `No ToolCallback found for tool name` when the model calls a tool that has none.
