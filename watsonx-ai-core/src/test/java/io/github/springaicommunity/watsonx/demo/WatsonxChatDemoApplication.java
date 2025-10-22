/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.springaicommunity.watsonx.demo;

import io.github.springaicommunity.watsonx.chat.WatsonxAiChatOptions;
import java.util.List;

/**
 * Demo application to showcase WatsonX AI Chat functionality and configuration options. This
 * demonstrates the chat model capabilities without requiring actual API connectivity.
 */
public class WatsonxChatDemoApplication {

  public static void main(String[] args) {
    System.out.println("=== WatsonX AI Chat Demo Application ===\n");

    // Demo 1: Basic Chat Options Configuration
    demonstrateBasicChatOptions();

    // Demo 2: Advanced Chat Options
    demonstrateAdvancedChatOptions();

    // Demo 3: Model Selection and Parameters
    demonstrateModelSelection();

    // Demo 4: Token and Generation Control
    demonstrateTokenControl();

    // Demo 5: Chat Behavior Settings
    demonstrateChatBehavior();

    System.out.println("=== Demo Completed Successfully ===");
  }

  private static void demonstrateBasicChatOptions() {
    System.out.println("1. Basic Chat Options Configuration");
    System.out.println("--------------------------------------");

    WatsonxAiChatOptions basic =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-3-8b-instruct")
            .temperature(0.7)
            .topP(1.0)
            .maxTokens(1024)
            .build();

    System.out.println("Created basic chat options:");
    System.out.println("   - Model: " + basic.getModel());
    System.out.println("   - Temperature: " + basic.getTemperature());
    System.out.println("   - Top P: " + basic.getTopP());
    System.out.println("   - Max Tokens: " + basic.getMaxTokens());
    System.out.println();
  }

  private static void demonstrateAdvancedChatOptions() {
    System.out.println("2. Advanced Chat Options");
    System.out.println("-----------------------------");

    WatsonxAiChatOptions advanced =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-8b-instruct")
            .temperature(0.3)
            .topP(0.9)
            .maxTokens(2048)
            .presencePenalty(0.1)
            .stopSequences(List.of("END", "STOP", "###"))
            .seed(42)
            .n(1)
            .build();

    System.out.println("Created advanced chat options:");
    System.out.println("   - Model: " + advanced.getModel());
    System.out.println(
        "   - Temperature: " + advanced.getTemperature() + " (Lower for more focused responses)");
    System.out.println("   - Top P: " + advanced.getTopP() + " (Nucleus sampling)");
    System.out.println("   - Max Tokens: " + advanced.getMaxTokens());
    System.out.println(
        "   - Presence Penalty: " + advanced.getPresencePenalty() + " (Reduces repetition)");
    System.out.println("   - Stop Sequences: " + advanced.getStopSequences());
    System.out.println("   - Seed: " + advanced.getSeed() + " (For reproducible results)");
    System.out.println("   - Number of Completions: " + advanced.getN());
    System.out.println();
  }

  private static void demonstrateModelSelection() {
    System.out.println("3. Model Selection Examples");
    System.out.println("-------------------------------");

    // Different IBM Granite models
    String[] models = {
      "ibm/granite-3-3-8b-instruct", "ibm/granite-3-8b-instruct", "ibm/granite-3-2b-instruct"
    };

    for (String model : models) {
      WatsonxAiChatOptions modelOptions =
          WatsonxAiChatOptions.builder().model(model).temperature(0.7).build();

      System.out.println("Model configuration: " + model);
      System.out.println("   - Suitable for various chat applications");
    }
    System.out.println();
  }

  private static void demonstrateTokenControl() {
    System.out.println("4. Token and Generation Control");
    System.out.println("-----------------------------------");

    // Short responses
    WatsonxAiChatOptions shortResponse =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-3-8b-instruct")
            .maxTokens(256)
            .temperature(0.5)
            .build();

    // Long responses
    WatsonxAiChatOptions longResponse =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-3-8b-instruct")
            .maxTokens(4096)
            .temperature(0.7)
            .build();

    System.out.println("Short Response Configuration:");
    System.out.println("   - Max Tokens: " + shortResponse.getMaxTokens() + " (Brief answers)");
    System.out.println("   - Temperature: " + shortResponse.getTemperature());

    System.out.println("Long Response Configuration:");
    System.out.println(
        "   - Max Tokens: " + longResponse.getMaxTokens() + " (Detailed explanations)");
    System.out.println("   - Temperature: " + longResponse.getTemperature());
    System.out.println();
  }

  private static void demonstrateChatBehavior() {
    System.out.println("5. Chat Behavior Settings");
    System.out.println("-----------------------------");

    // Creative configuration
    WatsonxAiChatOptions creative =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-3-8b-instruct")
            .temperature(0.9)
            .topP(0.95)
            .presencePenalty(0.2)
            .build();

    // Focused configuration
    WatsonxAiChatOptions focused =
        WatsonxAiChatOptions.builder()
            .model("ibm/granite-3-3-8b-instruct")
            .temperature(0.2)
            .topP(0.7)
            .presencePenalty(0.0)
            .build();

    System.out.println("Creative Chat Configuration:");
    System.out.println("   - Temperature: " + creative.getTemperature() + " (High creativity)");
    System.out.println("   - Top P: " + creative.getTopP() + " (Diverse word choices)");
    System.out.println(
        "   - Presence Penalty: " + creative.getPresencePenalty() + " (Encourages new topics)");

    System.out.println("Focused Chat Configuration:");
    System.out.println("   - Temperature: " + focused.getTemperature() + " (Very focused)");
    System.out.println("   - Top P: " + focused.getTopP() + " (Conservative word choices)");
    System.out.println(
        "   - Presence Penalty: " + focused.getPresencePenalty() + " (No repetition penalty)");
    System.out.println();
  }
}
