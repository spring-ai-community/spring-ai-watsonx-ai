---
sidebar_position: 5
---

# Text Extraction Models

Watsonx.ai Text Extraction Models provide document processing capabilities to extract structured and unstructured text, tables, and metadata from complex business documents (such as PDFs, DOCX, PPTX, HTML, and images). This capability eliminates the need for manual parsing and third-party extraction tools, seamlessly feeding extracted documents directly into Spring AI embeddings, vector databases, and RAG pipelines.

## Overview

Document extraction is the foundational first step in enterprise document Q&A and Retrieval-Augmented Generation (RAG) workflows. Watsonx.ai Text Extraction converts complex documents into clean markdown or text, preserving table structure and reading order.

The watsonx.ai text extraction integration provides:

- **Standalone Extraction**: Direct API access via `WatsonxAiTextExtractionModel` for extracting text from files, byte arrays, or Cloud Object Storage references.
- **Spring AI DocumentReader Integration**: `WatsonxAiDocumentReader` implementation bridging extracted documents directly into Spring AI's ETL and RAG pipelines.
- **Enterprise Observability**: Out-of-the-box Micrometer observation metrics (`gen_ai.client.operation`) tracking extraction jobs and page metrics.

## Supported Document Formats

The watsonx.ai Text Extraction service supports various formats:

- **PDF** (`.pdf`) - Complex multi-page documents, scanned files, and forms
- **Word** (`.docx`, `.doc`) - Office documents
- **PowerPoint** (`.pptx`) - Slide decks and presentations
- **Images** (`.png`, `.jpg`, `.jpeg`, `.tiff`) - Scanned documents and receipts with OCR
- **HTML** (`.html`) - Web pages and formatted reports

## Configuration Properties

### Text Extraction Properties

The prefix `spring.ai.watsonx.ai.text-extraction` is used as the property prefix for configuring the Watsonx.ai text extraction model.

| Property                                                             | Description                                               | Default                 |
| :------------------------------------------------------------------- | :-------------------------------------------------------- | :---------------------- |
| `spring.ai.watsonx.ai.text-extraction.enabled`                       | Enable or disable the text extraction auto-configuration  | true                    |
| `spring.ai.watsonx.ai.text-extraction.text-extraction-endpoint`      | The text extraction API endpoint                          | /ml/v1/text/extractions |
| `spring.ai.watsonx.ai.text-extraction.version`                       | API version date in YYYY-MM-DD format                     | 2024-05-31              |
| `spring.ai.watsonx.ai.text-extraction.options.model`                 | Model ID to use for extraction (optional)                | -                       |
| `spring.ai.watsonx.ai.text-extraction.options.output-formats`        | Output formats (e.g., markdown, text, json, html)        | -                       |
| `spring.ai.watsonx.ai.text-extraction.options.languages`             | Document languages (e.g., en, es, fr)                     | -                       |
| `spring.ai.watsonx.ai.text-extraction.options.enable-ocr`           | Whether to enable Optical Character Recognition (OCR)     | -                       |

## Standalone Text Extraction

The `WatsonxAiTextExtractionModel` provides direct access to document text extraction:

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

### Runtime Options

You can specify runtime extraction options using `WatsonxAiTextExtractionOptions`:

```java
WatsonxAiTextExtractionOptions options = WatsonxAiTextExtractionOptions.builder()
    .outputFormats(List.of("markdown"))
    .languages(List.of("en"))
    .enableOcr(true)
    .build();

WatsonxAiTextExtractionResponse response = textExtractionModel.extract(resource, options);
```

## Spring AI Document Pipeline Integration

### Using WatsonxAiDocumentReader

The `WatsonxAiDocumentReader` implements `Supplier<List<Document>>`, allowing you to ingest documents straight into Spring AI vector databases and embedding models:

```java
@Service
public class DocumentIngestionService {

    private final WatsonxAiTextExtractionModel textExtractionModel;
    private final VectorStore vectorStore;

    public DocumentIngestionService(WatsonxAiTextExtractionModel textExtractionModel, VectorStore vectorStore) {
        this.textExtractionModel = textExtractionModel;
        this.vectorStore = vectorStore;
    }

    public void ingestDocument(Resource documentResource) {
        // Read document using watsonx text extraction
        WatsonxAiDocumentReader reader = new WatsonxAiDocumentReader(textExtractionModel, documentResource);
        List<Document> documents = reader.read();

        // Feed directly into Spring AI Vector Store
        vectorStore.add(documents);
    }
}
```

### Direct Conversion to Spring AI Documents

You can also use the `extractToDocuments` convenience method on `WatsonxAiTextExtractionModel`:

```java
List<Document> documents = textExtractionModel.extractToDocuments(resource);
```

Each page or section is automatically converted into a `Document` with metadata including:
- `document_name`: Filename of the source resource
- `page_number`: Page index within the extracted document
- Custom metadata extracted by watsonx.ai

## Enterprise Document Q&A Pipeline Example

Here is how Text Extraction connects seamlessly with Chat and Embedding models in a complete enterprise pipeline:

```java
@RestController
@RequestMapping("/api/documents")
public class DocumentQaController {

    private final WatsonxAiTextExtractionModel textExtractionModel;
    private final WatsonxAiEmbeddingModel embeddingModel;
    private final WatsonxAiChatModel chatModel;
    private final VectorStore vectorStore;

    public DocumentQaController(
            WatsonxAiTextExtractionModel textExtractionModel,
            WatsonxAiEmbeddingModel embeddingModel,
            WatsonxAiChatModel chatModel,
            VectorStore vectorStore) {
        this.textExtractionModel = textExtractionModel;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadAndIndex(@RequestParam("file") MultipartFile file) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes(), file.getOriginalFilename());

        // 1. Extract text using watsonx Text Extraction
        List<Document> documents = textExtractionModel.extractToDocuments(resource);

        // 2. Index in vector store (automatically generates embeddings via WatsonxAiEmbeddingModel)
        vectorStore.add(documents);

        return ResponseEntity.ok("Successfully extracted and indexed " + documents.size() + " pages.");
    }
}
```
