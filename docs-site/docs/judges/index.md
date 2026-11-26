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

The evaluators are off unless enabled, and then need a governance instance:

```properties
spring.ai.watsonx.ai.api-key=${WATSONX_AI_API_KEY}
spring.ai.watsonx.ai.base-url=https://eu-de.ml.cloud.ibm.com
# For the chat and other models of the starter; the evaluators don't send it
spring.ai.watsonx.ai.project-id=${WATSONX_AI_PROJECT_ID}
spring.ai.watsonx.ai.evaluation.enabled=true
spring.ai.watsonx.ai.evaluation.governance-instance-id=${WATSONX_AI_GOVERNANCE_INSTANCE_ID}
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

## Manual Configuration

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

Evaluator faithfulness = WatsonxAiFaithfulnessEvaluator.builder()
    .detectionApi(detectionApi)
    .threshold(0.7)
    .build();

EvaluationResponse evaluation = faithfulness.evaluate(new EvaluationRequest(documents, answer));
```

`WatsonxAiDetectionApi` can also be called directly, with `detectContext(...)` and `detectGenerated(...)`.

## Limitations

- watsonx.ai uses Granite Guardian for the three detectors. The dedicated models that the API names as the `method` (`context_relevance_model`, `faithfulness_model`, `answer_relevance_model`) are not deployed in every region (they answer `404 model_not_found` in Frankfurt), so the evaluators use the default.
- The detectors judge English text.
- Each evaluation is one call to watsonx.ai.
