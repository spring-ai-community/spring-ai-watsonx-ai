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

package org.springaicommunity.watsonx.chat.util;

import java.util.ArrayList;
import java.util.List;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream.TextChatFunctionCall;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream.TextChatResultChoiceStream;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream.TextChatResultDelta;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream.TextChatToolCallStream;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Helper class to support streaming function calling. It can merge the streamed
 * WatsonxAiChatResponse chunks in case of function calling messages.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public class WatsonxAiChatChunkMerger {

  /**
   * Checks if the chunk contains a streaming tool function call.
   *
   * @param chunk the chat response chunk
   * @return true if the chunk contains a streaming tool function call
   */
  public boolean isStreamingToolFunctionCall(WatsonxAiChatStream chunk) {
    if (chunk == null || CollectionUtils.isEmpty(chunk.choices())) {
      return false;
    }

    for (TextChatResultChoiceStream choice : chunk.choices()) {
      if (choice != null
          && choice.delta() != null
          && !CollectionUtils.isEmpty(choice.delta().toolCalls())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the chunk indicates the end of a streaming tool function call.
   *
   * @param chunk the chat response chunk
   * @return true if the chunk indicates the end of a streaming tool function call
   */
  public boolean isStreamingToolFunctionCallFinish(WatsonxAiChatStream chunk) {
    if (chunk == null || CollectionUtils.isEmpty(chunk.choices())) {
      return false;
    }

    for (TextChatResultChoiceStream choice : chunk.choices()) {
      if (choice != null && "tool_calls".equals(choice.finishReason())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Merges two chat response chunks.
   *
   * @param previous the previous chunk
   * @param current the current chunk
   * @return the merged chunk
   */
  public WatsonxAiChatStream merge(WatsonxAiChatStream previous, WatsonxAiChatStream current) {
    if (previous == null) {
      return current;
    }

    if (current == null) {
      return previous;
    }

    String id = (current.id() != null ? current.id() : previous.id());
    Integer created = (current.created() != null ? current.created() : previous.created());
    String model = (current.model() != null ? current.model() : previous.model());
    String modelVersion =
        (current.modelVersion() != null ? current.modelVersion() : previous.modelVersion());

    TextChatResultChoiceStream previousChoice0 =
        (CollectionUtils.isEmpty(previous.choices()) ? null : previous.choices().get(0));
    TextChatResultChoiceStream currentChoice0 =
        (CollectionUtils.isEmpty(current.choices()) ? null : current.choices().get(0));

    TextChatResultChoiceStream choice = merge(previousChoice0, currentChoice0);
    List<TextChatResultChoiceStream> mergedChoices = (choice == null) ? List.of() : List.of(choice);

    return new WatsonxAiChatStream(
        id,
        model,
        created,
        mergedChoices,
        modelVersion,
        current.createdAt() != null ? current.createdAt() : previous.createdAt(),
        current.usage() != null ? current.usage() : previous.usage(),
        current.system() != null ? current.system() : previous.system());
  }

  private TextChatResultChoiceStream merge(
      TextChatResultChoiceStream previous, TextChatResultChoiceStream current) {
    if (previous == null) {
      return current;
    }

    if (current == null) {
      return previous;
    }

    String finishReason =
        (current.finishReason() != null ? current.finishReason() : previous.finishReason());
    Integer index = (current.index() != null ? current.index() : previous.index());

    TextChatResultDelta delta = merge(previous.delta(), current.delta());

    return new TextChatResultChoiceStream(index, delta, finishReason);
  }

  private TextChatResultDelta merge(TextChatResultDelta previous, TextChatResultDelta current) {
    if (previous == null) {
      return current;
    }

    String content =
        (current != null && current.content() != null)
            ? current.content()
            : (previous.content() != null ? previous.content() : "");

    ChatRole role = (current != null && current.role() != null) ? current.role() : previous.role();
    String refusal =
        (current != null && current.refusal() != null) ? current.refusal() : previous.refusal();

    List<TextChatToolCallStream> toolCalls = new ArrayList<>();

    // Handle tool calls merging
    TextChatToolCallStream lastPreviousToolCall = null;
    if (previous.toolCalls() != null && !previous.toolCalls().isEmpty()) {
      lastPreviousToolCall = previous.toolCalls().get(previous.toolCalls().size() - 1);

      // Add all but last tool call from previous
      if (previous.toolCalls().size() > 1) {
        toolCalls.addAll(previous.toolCalls().subList(0, previous.toolCalls().size() - 1));
      }
    }

    if (current != null && current.toolCalls() != null && !current.toolCalls().isEmpty()) {
      TextChatToolCallStream currentToolCall = current.toolCalls().get(0);

      if (StringUtils.hasText(currentToolCall.id())) {
        // If new tool call has ID, add both previous and current
        if (lastPreviousToolCall != null) {
          toolCalls.add(lastPreviousToolCall);
        }
        toolCalls.add(currentToolCall);
      } else {
        // Otherwise merge them
        toolCalls.add(merge(lastPreviousToolCall, currentToolCall));
      }
    } else if (lastPreviousToolCall != null) {
      // No current tool calls, keep the last previous one
      toolCalls.add(lastPreviousToolCall);
    }

    return new TextChatResultDelta(role, content, refusal, toolCalls);
  }

  private TextChatToolCallStream merge(
      TextChatToolCallStream previous, TextChatToolCallStream current) {
    if (previous == null) {
      return current;
    }

    Integer index =
        (current != null && current.index() != null) ? current.index() : previous.index();
    String id =
        (current != null && StringUtils.hasText(current.id())) ? current.id() : previous.id();
    ToolType type = (current != null && current.type() != null) ? current.type() : previous.type();

    TextChatFunctionCall function =
        merge(previous.function(), (current != null) ? current.function() : null);

    return new TextChatToolCallStream(index, id, type, function);
  }

  private TextChatFunctionCall merge(TextChatFunctionCall previous, TextChatFunctionCall current) {
    if (previous == null) {
      return current;
    }

    String name =
        (current != null && StringUtils.hasText(current.name())) ? current.name() : previous.name();

    StringBuilder arguments = new StringBuilder();
    if (previous.arguments() != null) {
      arguments.append(previous.arguments());
    }

    if (current != null && current.arguments() != null) {
      arguments.append(current.arguments());
    }

    return new TextChatFunctionCall(name, arguments.toString());
  }
}
