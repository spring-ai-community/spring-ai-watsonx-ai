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
| `spring.ai.watsonx.ai.text-extraction.version` | API version date in YYYY-MM-DD format | `2024-05-31` |
| `spring.ai.watsonx.ai.text-extraction.options.model` | Model ID to use for extraction (optional) | — |
| `spring.ai.watsonx.ai.text-extraction.options.output-formats` | Output formats — `markdown`, `text`, `json`, `html` | — |
| `spring.ai.watsonx.ai.text-extraction.options.languages` | Document languages — e.g. `en`, `es`, `fr` | — |
| `spring.ai.watsonx.ai.text-extraction.options.enable-ocr` | Whether to enable Optical Character Recognition (OCR) | — |

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
    .outputFormats(List.of("markdown"))
    .languages(List.of("en"))
    .enableOcr(true)
    .build();

WatsonxAiTextExtractionResponse response = textExtractionModel.extract(resource, options);
```

---

## Custom Extraction Request

For advanced scenarios, build a `WatsonxAiTextExtractionRequest` directly. This gives you full control over the request, including specifying Cloud Object Storage (COS) document references, output destinations, and project/space scope.

### From Raw Bytes with Options

```java
WatsonxAiTextExtractionRequest request = WatsonxAiTextExtractionRequest.builder()
    .documentBytes(file.getBytes(), file.getOriginalFilename())
    .build();

WatsonxAiTextExtractionOptions options = WatsonxAiTextExtractionOptions.builder()
    .outputFormats(List.of("markdown"))
    .enableOcr(true)
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

`WatsonxAiTextExtractionResponse` exposes the full extraction result including per-page content, tables, and metadata.

### Get Full Text

```java
String fullText = response.getText(); // concatenates all pages with "\n\n" separator
```

### Iterate Pages

Each `ExtractedPage` contains:
- `pageNumber()` — 1-based page index
- `text()` — extracted text for that page
- `tables()` — list of table structures extracted from the page
- `metadata()` — page-level metadata map

```java
for (WatsonxAiTextExtractionResponse.ExtractedPage page : response.pages()) {
    System.out.println("Page " + page.pageNumber() + ": " + page.text());

    if (page.tables() != null) {
        page.tables().forEach(table -> System.out.println("Table: " + table));
    }
}
```

### Check Extraction Status

```java
String status = response.getStatus(); // "completed", "failed", etc.
String id     = response.getId();     // watsonx.ai extraction job ID
```

---

## Spring AI Document Pipeline Integration

### Using `extractToDocuments`

The `extractToDocuments` convenience method converts the response directly into Spring AI `Document` objects, one per page:

```java
List<Document> documents = textExtractionModel.extractToDocuments(resource);
```

Each `Document` carries metadata:
- `document_name` — filename of the source resource
- `page_number` — page index within the extracted document
- Any additional metadata returned by watsonx.ai

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

        return ResponseEntity.ok("Extracted and indexed " + documents.size() + " pages.");
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
