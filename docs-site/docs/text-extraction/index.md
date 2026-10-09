---
sidebar_position: 5
---

# Text Extraction Models

Watsonx.ai Text Extraction Models provide document processing capabilities to extract structured and unstructured text, tables, and metadata from complex business documents (such as PDFs, DOCX, PPTX, HTML, and images). This capability eliminates the need for manual parsing and third-party extraction tools, seamlessly feeding extracted documents directly into Spring AI embeddings, vector databases, and RAG pipelines.

## Overview

Document extraction is the foundational first step in enterprise document Q&A and Retrieval-Augmented Generation (RAG) workflows. Watsonx.ai Text Extraction converts complex documents into clean markdown or text, preserving table structure and reading order.

The watsonx.ai text extraction integration provides:

- **Standalone Extraction**: Direct API access via `WatsonxAiTextExtractionModel` for extracting text from files, byte arrays, Spring `Resource` objects, or Cloud Object Storage references.
- **Spring AI DocumentReader Integration**: `WatsonxAiDocumentReader` implementation bridging extracted documents directly into Spring AI's ETL and RAG pipelines.
- **Enterprise Observability**: Out-of-the-box Micrometer observation metrics (`gen_ai.client.operation`) tracking extraction jobs and page metrics.
- **Custom Request Control**: Fine-grained control over the extraction request via `WatsonxAiTextExtractionRequest`, including COS document references and output destinations.

## How It Works

watsonx.ai extracts text asynchronously from a document in Cloud Object Storage. When you pass a file (bytes or a Spring `Resource`), `WatsonxAiTextExtractionModel`:

1. Uploads it to the storage bucket of the configured project or deployment space (`spring.ai.watsonx.ai.project-id` or `space-id`).
2. Starts an extraction job that reads it from there and writes the result next to it.
3. Polls the job until it completes or fails (up to `poll-timeout`).
4. Downloads the result and returns it as the response text.
5. Deletes the uploaded file and the result from storage (unless `delete-files` is `false`).

### Prerequisites

- A project or deployment space with associated storage, and the API key's user or service ID must be allowed to write to it.
- A **task credential** for that user or service ID. watsonx.ai uses it to run the extraction job, and rejects the request with `missing_task_credentials` without it. See [Managing task credentials](https://dataplatform.cloud.ibm.com/docs/content/wsj/manage-data/task-credentials.html?context=wx).

## Supported Document Formats

The watsonx.ai Text Extraction service supports various formats:

- **PDF** (`.pdf`) — Complex multi-page documents, scanned files, and forms
- **Word** (`.docx`, `.doc`) — Office documents
- **PowerPoint** (`.pptx`) — Slide decks and presentations
- **Images** (`.png`, `.jpg`, `.jpeg`, `.tiff`) — Scanned documents and receipts with OCR
- **HTML** (`.html`) — Web pages and formatted reports

## Configuration Properties

The prefix `spring.ai.watsonx.ai.text-extraction` is used as the property prefix for configuring the Watsonx.ai text extraction model.

| Property | Description | Default |
| :--- | :--- | :--- |
| `spring.ai.watsonx.ai.text-extraction.enabled` | Enable or disable text extraction auto-configuration | `true` |
| `spring.ai.watsonx.ai.text-extraction.text-extraction-endpoint` | The text extraction API endpoint | `/ml/v1/text/extractions` |
| `spring.ai.watsonx.ai.text-extraction.version` | API version date in YYYY-MM-DD format | `2024-10-17` |
| `spring.ai.watsonx.ai.text-extraction.poll-timeout` | How long to wait for an extraction job to finish | `5m` |
| `spring.ai.watsonx.ai.text-extraction.delete-files` | Delete the uploaded document and the result from storage after extracting a file | `true` |
| `spring.ai.watsonx.ai.text-extraction.dataplatform-url` | Data platform API URL used to look up the project or space storage. Derived from the base URL for IBM Cloud regions | — |
| `spring.ai.watsonx.ai.text-extraction.options.requested-outputs` | Output to extract — `md`, `plain_text`, `json` or `html`. Extracting a file supports exactly one | `md` |
| `spring.ai.watsonx.ai.text-extraction.options.mode` | Extraction mode — `standard` or `high_quality` | — |
| `spring.ai.watsonx.ai.text-extraction.options.ocr-mode` | OCR mode — `disabled`, `enabled` or `forced` | — |
| `spring.ai.watsonx.ai.text-extraction.options.languages` | Document languages — e.g. `en`, `es`, `fr` | — |
| `spring.ai.watsonx.ai.text-extraction.options.output-formats` | **Deprecated**, use `requested-outputs` | — |
| `spring.ai.watsonx.ai.text-extraction.options.enable-ocr` | **Deprecated**, use `ocr-mode` (`true` maps to `enabled`, `false` to `disabled`) | — |
| `spring.ai.watsonx.ai.text-extraction.options.model` | **Deprecated**, not sent: the API has no model parameter | — |

## Dependency

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-starter-model-watsonx-ai</artifactId>
    <version>2.0.0</version>
</dependency>
```

```groovy
implementation 'org.springaicommunity:spring-ai-starter-model-watsonx-ai:2.0.0'
```

---

## Extracting from a File Upload

The simplest usage — extract text from raw bytes passed directly to the model:

```java
@RestController
public class DocumentController {

    private final WatsonxAiTextExtractionModel textExtractionModel;

    public DocumentController(WatsonxAiTextExtractionModel textExtractionModel) {
        this.textExtractionModel = textExtractionModel;
    }

    @PostMapping("/extract")
    public String extractText(@RequestParam("file") MultipartFile file) throws IOException {
        WatsonxAiTextExtractionResponse response = textExtractionModel.extract(
            file.getBytes(),
            file.getOriginalFilename()
        );
        return response.getText();
    }
}
```

## Extracting from a Spring Resource

Any Spring `Resource` (classpath, filesystem, URL) can be passed directly:

```java
Resource resource = new FileSystemResource("/path/to/document.pdf");
WatsonxAiTextExtractionResponse response = textExtractionModel.extract(resource);
String text = response.getText();
```

## Runtime Options

Override default extraction options at call time using `WatsonxAiTextExtractionOptions`:

```java
WatsonxAiTextExtractionOptions options = WatsonxAiTextExtractionOptions.builder()
    .requestedOutputs(List.of("plain_text"))
    .languages(List.of("en"))
    .ocrMode("enabled")
    .build();

WatsonxAiTextExtractionResponse response = textExtractionModel.extract(resource, options);
```

---

## Custom Extraction Request

For advanced scenarios, build a `WatsonxAiTextExtractionRequest` directly. This gives you full control over the request, including specifying Cloud Object Storage (COS) document references, output destinations, and project/space scope.

A request with a document reference only **starts** the extraction job: the response contains the job ID and status (`submitted`), and watsonx.ai writes the result to the `resultsReference` location. Use `WatsonxAiTextExtractionApi.getExtraction(id)` to follow the job. A request with document bytes runs the full flow described in [How It Works](#how-it-works).

### From Raw Bytes with Options

```java
WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
    .documentBytes(file.getBytes(), file.getOriginalFilename())
    .build();

WatsonxAiTextExtractionOptions options = WatsonxAiTextExtractionOptions.builder()
    .requestedOutputs(List.of("md"))
    .ocrMode("enabled")
    .build();

WatsonxAiTextExtractionResponse response = textExtractionModel.extract(request, options);
```

### From a Cloud Object Storage Connection Asset

Use `DocumentReference.ofConnectionAsset(...)` to point to a file stored in IBM Cloud Object Storage via a connection asset:

```java
WatsonxAiTextExtractionRequest.DocumentReference docRef =
    WatsonxAiTextExtractionRequest.DocumentReference.ofConnectionAsset(
        "my-connection-id",   // COS connection asset ID
        "my-bucket",          // bucket name
        "contracts/report.pdf" // file path within the bucket
    );

WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
    .documentReference(docRef)
    .build();

WatsonxAiTextExtractionResponse response = textExtractionModel.extract(request, null);
```

### From a Container (Project/Space Storage)

Use `DocumentReference.ofContainer(...)` to reference a file already uploaded to the watsonx.ai project or space container:

```java
WatsonxAiTextExtractionRequest.DocumentReference docRef =
    WatsonxAiTextExtractionRequest.DocumentReference.ofContainer("documents/report.pdf");

WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
    .documentReference(docRef)
    .build();

WatsonxAiTextExtractionResponse response = textExtractionModel.extract(request, null);
```

### With an Output Results Reference

You can store extraction results back to COS by specifying a `resultsReference`:

```java
WatsonxAiTextExtractionRequest.DocumentReference inputRef =
    WatsonxAiTextExtractionRequest.DocumentReference.ofConnectionAsset(
        "my-connection-id", "my-bucket", "input/report.pdf");

WatsonxAiTextExtractionRequest.DocumentReference outputRef =
    WatsonxAiTextExtractionRequest.DocumentReference.ofConnectionAsset(
        "my-connection-id", "my-bucket", "output/report-extracted/");

WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
    .documentReference(inputRef)
    .resultsReference(outputRef)
    .build();

WatsonxAiTextExtractionResponse response = textExtractionModel.extract(request, null);
```

---

## Working with the Response

`WatsonxAiTextExtractionResponse` describes the extraction job. When you extract a file, it also contains the extracted text, in the requested output format (Markdown by default).

### Get the Text

```java
String text = response.getText();
```

### Check the Job

```java
String id      = response.getId();                    // watsonx.ai extraction job ID
String status  = response.getStatus();                // "submitted", "running", "completed", "failed", ...
Integer pages  = response.getNumberPagesProcessed();  // pages processed so far
var error      = response.getError();                 // code and message of a failed job
```

---

## Spring AI Document Pipeline Integration

### Using `extractToDocuments`

The `extractToDocuments` convenience method extracts a file and returns the text as a Spring AI `Document`:

```java
List<Document> documents = textExtractionModel.extractToDocuments(resource);
```

The `Document` carries the `document_name` metadata: the filename of the source resource. Split it further with a Spring AI `DocumentTransformer`, such as `TokenTextSplitter`, before adding it to a vector store.

### Using `WatsonxAiDocumentReader`

`WatsonxAiDocumentReader` implements Spring AI's `DocumentReader` interface for direct integration into ETL and RAG pipelines:

```java
@Service
public class DocumentIngestionService {

    private final WatsonxAiTextExtractionModel textExtractionModel;
    private final VectorStore vectorStore;

    public DocumentIngestionService(WatsonxAiTextExtractionModel textExtractionModel,
                                    VectorStore vectorStore) {
        this.textExtractionModel = textExtractionModel;
        this.vectorStore = vectorStore;
    }

    public void ingestDocument(Resource documentResource) {
        WatsonxAiDocumentReader reader = new WatsonxAiDocumentReader(textExtractionModel, documentResource);
        List<Document> documents = reader.read();
        vectorStore.add(documents);
    }
}
```

---

## Enterprise Document Q&A Pipeline Example

A complete pipeline connecting Text Extraction, Embeddings, Vector Store, and Chat:

```java
@RestController
@RequestMapping("/api/documents")
public class DocumentQaController {

    private final WatsonxAiTextExtractionModel textExtractionModel;
    private final VectorStore vectorStore;
    private final WatsonxAiChatModel chatModel;

    public DocumentQaController(WatsonxAiTextExtractionModel textExtractionModel,
                                 VectorStore vectorStore,
                                 WatsonxAiChatModel chatModel) {
        this.textExtractionModel = textExtractionModel;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadAndIndex(@RequestParam("file") MultipartFile file) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() { return file.getOriginalFilename(); }
        };

        // 1. Extract text using watsonx Text Extraction
        List<Document> documents = textExtractionModel.extractToDocuments(resource);

        // 2. Index in vector store (embeddings generated automatically)
        vectorStore.add(documents);

        return ResponseEntity.ok("Extracted and indexed " + documents.size() + " documents.");
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        List<Document> context = vectorStore.similaritySearch(question);
        String contextText = context.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n"));

        return chatModel.call("Context:\n" + contextText + "\n\nQuestion: " + question);
    }
}
```

---

## Observability

`WatsonxAiTextExtractionModel` emits Micrometer observations under the `gen_ai.client.operation` meter name. You can customise the observation convention by injecting a custom `TextExtractionModelObservationConvention`:

```java
textExtractionModel.setObservationConvention(new MyCustomObservationConvention());
```
