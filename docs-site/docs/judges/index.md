---
sidebar_position: 4
---

# Evaluators

Spring AI watsonx.ai implements the Spring AI [`Evaluator`](https://docs.spring.io/spring-ai/reference/api/testing.html) API with the watsonx.ai detectors that judge a retrieval-augmented generation (RAG) answer. Like Spring AI's `RelevancyEvaluator` and `FactCheckingEvaluator`, they take an `EvaluationRequest` and return an `EvaluationResponse`, but the judge is a watsonx.ai detector (Granite Guardian) instead of a chat model and a prompt.

| Evaluator | watsonx.ai detector | Checks | Reads from the `EvaluationRequest` |
| :-------- | :------------------ | :----- | :--------------------------------- |
| `WatsonxAiAnswerRelevanceEvaluator` | `answer_relevance` | The answer addresses the question | user text (question), response content (answer) |
| `WatsonxAiContextRelevanceEvaluator` | `context_relevance` | The retrieved documents are relevant to the question | user text (question), documents |
| `WatsonxAiFaithfulnessEvaluator` | `faithfulness` (groundedness) | The answer is supported by the documents, so it is not a hallucination. The counterpart of `FactCheckingEvaluator` | response content (answer), documents |

The documents are sent to watsonx.ai as text, one context item per document with text (at most 100). A URL in a document is not fetched: it is judged as text.

## A watsonx.governance Instance Is Required

watsonx.ai runs these detectors only for a [watsonx.governance](https://cloud.ibm.com/catalog/services/watsonxgovernance) service instance. With a project or a space, watsonx.ai answers `401 detector_not_accessible` ("Detectors 'answer_relevance' is not accessible for authorization type 'project_id'"), which is thrown as a `NonTransientAiException`.

Set the GUID of the watsonx.governance instance in the same region as `base-url`. It is sent in the `x-governance-instance-id` header, and the request then has no project or space, even when `project-id` or `space-id` is set for the other models. To find the GUID:

```bash
ibmcloud resource service-instances --service-name aiopenscale
ibmcloud resource service-instance "<instance name>" --output json | jq -r '.[0].guid'
```

## Scores and Thresholds

Each detector returns a score from 0 to 1: the probability that the answer is relevant, that the context is relevant, or that the answer is faithful. A higher score is better. The evaluation passes when the score reaches the threshold of the evaluator, `0.5` by default.

The threshold is not sent to watsonx.ai. watsonx.ai would leave out a detection below it, and the score would be lost. Without a threshold, it labels every detection `relevant` or `factual`, whatever the score, so only the score tells the result.

The `EvaluationResponse` has:

| | |
| :-- | :-- |
| `isPass()` | Whether the score reaches the threshold |
| `getScore()` | The score of the detector |
| `getFeedback()` | The score against the threshold, for example `faithfulness score 0.01909583806991577 is below the threshold 0.7` |
| `getMetadata().get("detection")` | The `WatsonxAiDetectionResponse.Detection` that gave the score: detection type, label, detector ID (such as `granite_guardian_3_2_5b`) and evidence, when the detector gives some |
| `getMetadata().get("detections")` | Every detection of the response |
| `getMetadata().get("threshold")` | The threshold |

If watsonx.ai returns no score for the detector, the evaluation fails with a score of 0.

## Auto-configuration

There are two ways to get the evaluators: let the auto-configuration create them as beans, or [create them yourself](#creating-the-evaluators-without-auto-configuration) with their builders. Either way they need a [watsonx.governance instance](#a-watsonxgovernance-instance-is-required).

### Why the Evaluators Are Opt-in

The other watsonx.ai models of the starter are on by default, but the evaluators are off unless `spring.ai.watsonx.ai.evaluation.enabled=true`:

- They work only with a watsonx.governance instance, which an application that just chats doesn't have. Turned on by default, they would make every such application fail at startup or at its first evaluation.
- Spring AI doesn't create `Evaluator` beans either. Spring AI 2.x ships two evaluators, `RelevancyEvaluator` and `FactCheckingEvaluator` in `spring-ai-client-chat`, and the application builds them; no auto-configuration creates them, and no model module, such as OpenAI, Anthropic or Bedrock Converse, ships an evaluator.
- Evaluations usually run in tests or in an evaluation job, not in every application that has the starter.

### Auto-configured Evaluators

Turn them on and set the governance instance:

```properties
spring.ai.watsonx.ai.api-key=${WATSONX_AI_API_KEY}
spring.ai.watsonx.ai.base-url=https://eu-de.ml.cloud.ibm.com
# For the chat and other models of the starter; the evaluators don't send it
spring.ai.watsonx.ai.project-id=${WATSONX_AI_PROJECT_ID}
spring.ai.watsonx.ai.evaluation.enabled=true
# The empty default makes startup fail when the variable is not set, instead of sending "${...}"
spring.ai.watsonx.ai.evaluation.governance-instance-id=${WATSONX_AI_GOVERNANCE_INSTANCE_ID:}
spring.ai.watsonx.ai.evaluation.faithfulness.threshold=0.7
```

The other models of the starter need `project-id` or `space-id` at startup. An application that only evaluates can turn them off instead: `spring.ai.model.chat=none`, `spring.ai.model.embedding=none`, `spring.ai.model.moderation=none`, `spring.ai.watsonx.ai.rerank.enabled=false` and `spring.ai.watsonx.ai.text-extraction.enabled=false`.

| Property | Default | Description |
| :------- | :------ | :---------- |
| `spring.ai.watsonx.ai.evaluation.enabled` | `false` | Create the evaluators |
| `spring.ai.watsonx.ai.evaluation.governance-instance-id` | | The GUID of the watsonx.governance instance. Required when the evaluators are enabled: startup fails without it |
| `spring.ai.watsonx.ai.evaluation.answer-relevance.threshold` | `0.5` | The score from which an answer relevance evaluation passes |
| `spring.ai.watsonx.ai.evaluation.context-relevance.threshold` | `0.5` | The score from which a context relevance evaluation passes |
| `spring.ai.watsonx.ai.evaluation.faithfulness.threshold` | `0.5` | The score from which a faithfulness evaluation passes |
| `spring.ai.watsonx.ai.evaluation.context-endpoint` | `/ml/v1/text/detection/context` | The endpoint of context relevance and faithfulness |
| `spring.ai.watsonx.ai.evaluation.generated-endpoint` | `/ml/v1/text/detection/generated` | The endpoint of answer relevance |
| `spring.ai.watsonx.ai.evaluation.version` | `2025-10-01` | API version date, in `YYYY-MM-DD` format |

This creates a `WatsonxAiDetectionApi` and the three evaluators. Each one is replaced by a bean of the same type that the application defines, such as a `WatsonxAiDetectionApi` scoped by project for an account where watsonx.ai allows it.

## Evaluating a RAG Answer

```java
@Autowired
WatsonxAiAnswerRelevanceEvaluator answerRelevance;

@Autowired
WatsonxAiContextRelevanceEvaluator contextRelevance;

@Autowired
WatsonxAiFaithfulnessEvaluator faithfulness;

@Test
void answersFromTheDocuments() {
    String question = "What is the capital of Italy?";
    ChatResponse response = ChatClient.create(chatModel)
        .prompt(question)
        .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
        .call()
        .chatResponse();

    // The documents that the model got, as the advisor retrieved them
    List<Document> documents = response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
    EvaluationRequest request = new EvaluationRequest(question, documents,
            response.getResult().getOutput().getText());

    assertThat(contextRelevance.evaluate(request).isPass()).isTrue();
    assertThat(answerRelevance.evaluate(request).isPass()).isTrue();

    EvaluationResponse grounded = faithfulness.evaluate(request);
    assertThat(grounded.isPass()).as(grounded.getFeedback()).isTrue();
}
```

## Creating the Evaluators Without Auto-configuration

Leave `spring.ai.watsonx.ai.evaluation.enabled` unset and build the evaluators where you need them, such as in a test, like Spring AI's own evaluators. They share one `WatsonxAiDetectionApi`:

```java
WatsonxAiDetectionApi detectionApi = new WatsonxAiDetectionApi(
        "https://eu-de.ml.cloud.ibm.com",
        WatsonxAiDetectionApi.DEFAULT_CONTEXT_ENDPOINT,
        WatsonxAiDetectionApi.DEFAULT_GENERATED_ENDPOINT,
        WatsonxAiDetectionApi.DEFAULT_VERSION,
        null, null,                                   // no project or space with a governance instance
        System.getenv("WATSONX_AI_GOVERNANCE_INSTANCE_ID"),
        System.getenv("WATSONX_AI_API_KEY"),
        RestClient.builder(),
        RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);

Evaluator answerRelevance = new WatsonxAiAnswerRelevanceEvaluator(detectionApi);   // threshold 0.5
Evaluator contextRelevance = WatsonxAiContextRelevanceEvaluator.builder()
    .detectionApi(detectionApi)
    .threshold(0.6)
    .build();
Evaluator faithfulness = WatsonxAiFaithfulnessEvaluator.builder()
    .detectionApi(detectionApi)
    .threshold(0.7)
    .retryTemplate(RetryUtils.SHORT_RETRY_TEMPLATE)
    .build();

EvaluationResponse evaluation = faithfulness.evaluate(new EvaluationRequest(documents, answer));
```

`WatsonxAiDetectionApi` can also be called directly, with `detectContext(...)` and `detectGenerated(...)`.

## Without watsonx.governance: LLM-as-a-Judge

Without a watsonx.governance instance, use Spring AI's own evaluators with a watsonx.ai chat model as the judge. `RelevancyEvaluator` asks whether the answer is in line with the question and the documents, and `FactCheckingEvaluator` asks whether the documents support the answer. They need only the chat model of the starter:

```java
ChatClient.Builder judge = ChatClient.builder(chatModel)
    .defaultOptions(WatsonxAiChatOptions.builder()
        .model("ibm/granite-4-h-small")
        .temperature(0.0));

Evaluator relevancy = new RelevancyEvaluator(judge);
Evaluator factChecking = FactCheckingEvaluator.builder(judge).build();

boolean relevant = relevancy.evaluate(new EvaluationRequest(question, documents, answer)).isPass();
boolean grounded = factChecking.evaluate(new EvaluationRequest(documents, answer)).isPass();
```

`ibm/granite-4-h-small` answers these prompts with the plain `yes` or `no` that the evaluators read.

| | watsonx.ai detection evaluators | Spring AI evaluators with a watsonx.ai chat model |
| :-- | :-- | :-- |
| Needs | A watsonx.governance instance | A chat model only |
| Judge | Granite Guardian, a model trained to detect these risks | A chat model with a prompt, which you can change |
| Result | A score from 0 to 1, with a threshold you choose | Pass or fail, from a `yes` or `no` answer. `RelevancyEvaluator` gives a score of 1 or 0, `FactCheckingEvaluator` none |
| Checks | Answer relevance, context relevance and faithfulness, each on its own | Relevancy of the answer to the question and the documents, and fact checking |
| Cost | Detection calls | Chat completion tokens |

Choose the detection evaluators to rank or compare answers by a score, or to tell whether the retrieval or the answer is at fault. Choose the Spring AI evaluators when there is no watsonx.governance instance, or to judge with your own prompt.

## Limitations

- watsonx.ai uses Granite Guardian for the three detectors. The dedicated models that the API names as the `method` (`context_relevance_model`, `faithfulness_model`, `answer_relevance_model`) are not deployed in every region (they answer `404 model_not_found` in Frankfurt), so the evaluators use the default.
- The detectors judge English text.
- Each evaluation is one call to watsonx.ai.
