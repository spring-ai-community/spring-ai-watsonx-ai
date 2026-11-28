---
sidebar_position: 6
---

# Text to Speech Models

Watsonx.ai Text to Speech models provide high-quality synthetic speech synthesis from text input through the watsonx.ai model gateway (`POST /ml/gateway/v1/audio/speech`). This capability allows developers to convert text into natural-sounding audio in real-time using Spring AI's standard `TextToSpeechModel` and `StreamingTextToSpeechModel` interfaces.

## Overview

The watsonx.ai Text to Speech integration provides:

- **Standard Spring AI Interfaces**: Implements `org.springframework.ai.audio.tts.TextToSpeechModel` and `StreamingTextToSpeechModel`.
- **Synchronous Synthesis**: Generate full audio files (`byte[]`) synchronously via `call(prompt)` or `call(text)`.
- **Streaming Synthesis**: Stream audio byte chunks in real-time via `Flux<TextToSpeechResponse>` or `Flux<byte[]>`.
- **Customizable Voices & Speed**: Configure voice selection, speed rate (0.25 to 4.0), and model-dependent voice instructions.
- **Multiple Audio Formats**: Supports `mp3`, `opus`, `aac`, `flac`, `pcm`, and `wav`.

## Supported Audio Formats

| Format | Description |
| :--- | :--- |
| `mp3` | Standard compressed MP3 format (default for broad compatibility) |
| `opus` | Low-latency audio codec optimized for interactive voice streaming |
| `aac` | Advanced Audio Coding for high-quality audio compression |
| `flac` | Free Lossless Audio Codec for uncompressed audio quality |
| `pcm` | Raw uncompressed PCM audio bytes |
| `wav` | Standard Waveform uncompressed audio container |

## Configuration Properties

### Text to Speech Properties

The prefix `spring.ai.watsonx.ai.text-to-speech` is used for configuring the Watsonx.ai text-to-speech model.

| Property | Description | Default |
| :--- | :--- | :--- |
| `spring.ai.watsonx.ai.text-to-speech.enabled` | Enable or disable the text-to-speech auto-configuration | `true` |
| `spring.ai.watsonx.ai.text-to-speech.speech-endpoint` | The text-to-speech gateway API endpoint | `/ml/gateway/v1/audio/speech` |
| `spring.ai.watsonx.ai.text-to-speech.version` | API version date in YYYY-MM-DD format | `2024-03-14` |
| `spring.ai.watsonx.ai.text-to-speech.options.model` | Default TTS model selection | - |
| `spring.ai.watsonx.ai.text-to-speech.options.voice` | Default voice selection | - |
| `spring.ai.watsonx.ai.text-to-speech.options.speed` | Speech speed between 0.25 and 4.0 | - |
| `spring.ai.watsonx.ai.text-to-speech.options.response-format` | Audio output format (`mp3`, `opus`, `aac`, `flac`, `pcm`, `wav`) | - |
| `spring.ai.watsonx.ai.text-to-speech.options.instructions` | Voice instructions (model-dependent) | - |

Example `application.yml`:

```yaml
spring:
  ai:
    watsonx:
      ai:
        api-key: ${WATSONX_AI_API_KEY}
        base-url: https://us-south.ml.cloud.ibm.com
        project-id: ${WATSONX_AI_PROJECT_ID}
        text-to-speech:
          enabled: true
          options:
            model: ibm/granite-speech
            voice: en-US_AllisonV3Voice
            response-format: mp3
            speed: 1.0
```

## Usage Examples

### Synchronous Speech Generation

You can inject `WatsonxAiTextToSpeechModel` (or the Spring AI interface `TextToSpeechModel`) into your beans and generate speech synchronously:

```java
@RestController
public class SpeechController {

    private final TextToSpeechModel textToSpeechModel;

    public SpeechController(TextToSpeechModel textToSpeechModel) {
        this.textToSpeechModel = textToSpeechModel;
    }

    @PostMapping(value = "/synthesize", produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesizeSpeech(@RequestBody String text) {
        byte[] audioData = textToSpeechModel.call(text);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audioData);
    }
}
```

### Using Prompts and Runtime Options

You can specify per-request options using `WatsonxAiTextToSpeechOptions` and `TextToSpeechPrompt`:

```java
WatsonxAiTextToSpeechOptions options = WatsonxAiTextToSpeechOptions.builder()
        .voice("en-US_MichaelV3Voice")
        .speed(1.25)
        .responseFormat("wav")
        .instructions("Speak clearly and in a cheerful tone")
        .build();

TextToSpeechPrompt prompt = new TextToSpeechPrompt("Welcome to Spring AI Watsonx integration!", options);
TextToSpeechResponse response = textToSpeechModel.call(prompt);

byte[] audioBytes = response.getResult().getOutput();
```

### Streaming Audio Playback

For low-latency real-time applications, use the `.stream()` method to stream audio chunks:

```java
@GetMapping(value = "/stream-speech", produces = "audio/mpeg")
public Flux<byte[]> streamSpeech(@RequestParam String text) {
    return textToSpeechModel.stream(text);
}
```

Or with `TextToSpeechPrompt`:

```java
TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, options);
Flux<TextToSpeechResponse> responseFlux = textToSpeechModel.stream(prompt);

Flux<byte[]> audioChunks = responseFlux.map(response -> response.getResult().getOutput());
```
