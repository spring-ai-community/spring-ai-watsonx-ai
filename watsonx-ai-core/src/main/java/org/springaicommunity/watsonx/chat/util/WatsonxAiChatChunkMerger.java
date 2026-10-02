/*
 * Copyright 2025-2026 the original author or authors.
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	 * @param chunk the chat response chunk
	 * @return true if the chunk contains a streaming tool function call
	 */
	public boolean isStreamingToolFunctionCall(WatsonxAiChatStream chunk) {
		if (chunk == null || CollectionUtils.isEmpty(chunk.choices())) {
			return false;
		}

		for (TextChatResultChoiceStream choice : chunk.choices()) {
			if (choice != null && choice.delta() != null && !CollectionUtils.isEmpty(choice.delta().toolCalls())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the chunk indicates the end of a streaming tool function call.
	 * @param chunk the chat response chunk
	 * @return true if the chunk indicates the end of a streaming tool function call
	 */
	public boolean isStreamingToolFunctionCallFinish(WatsonxAiChatStream chunk) {
		if (chunk == null || CollectionUtils.isEmpty(chunk.choices())) {
			return false;
		}

		for (TextChatResultChoiceStream choice : chunk.choices()) {
			if (choice != null && ChatFinishReason.TOOL_CALLS.name().toLowerCase().equals(choice.finishReason())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Merges two chat response chunks.
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
		String modelVersion = (current.modelVersion() != null ? current.modelVersion() : previous.modelVersion());

		List<TextChatResultChoiceStream> mergedChoices = mergeChoices(previous.choices(), current.choices());

		return new WatsonxAiChatStream(id, model, created, mergedChoices, modelVersion,
				current.createdAt() != null ? current.createdAt() : previous.createdAt(),
				current.usage() != null ? current.usage() : previous.usage(),
				current.system() != null ? current.system() : previous.system());
	}

	private List<TextChatResultChoiceStream> mergeChoices(List<TextChatResultChoiceStream> previous,
			List<TextChatResultChoiceStream> current) {
		// Choices are matched by their index, so every choice is kept
		Map<Integer, TextChatResultChoiceStream> choices = new LinkedHashMap<>();
		for (List<TextChatResultChoiceStream> chunkChoices : Arrays.asList(previous, current)) {
			if (CollectionUtils.isEmpty(chunkChoices)) {
				continue;
			}
			for (TextChatResultChoiceStream choice : chunkChoices) {
				if (choice != null) {
					choices.merge((choice.index() != null) ? choice.index() : 0, choice, this::merge);
				}
			}
		}
		return List.copyOf(choices.values());
	}

	private TextChatResultChoiceStream merge(TextChatResultChoiceStream previous, TextChatResultChoiceStream current) {
		if (previous == null) {
			return current;
		}

		if (current == null) {
			return previous;
		}

		String finishReason = (current.finishReason() != null ? current.finishReason() : previous.finishReason());
		Integer index = (current.index() != null ? current.index() : previous.index());

		TextChatResultDelta delta = merge(previous.delta(), current.delta());

		return new TextChatResultChoiceStream(index, delta, finishReason,
				current.logprobs() != null ? current.logprobs() : previous.logprobs());
	}

	private TextChatResultDelta merge(TextChatResultDelta previous, TextChatResultDelta current) {
		if (previous == null) {
			return current;
		}

		String content = (current != null && current.content() != null) ? current.content()
				: (previous.content() != null ? previous.content() : "");

		ChatRole role = (current != null && current.role() != null) ? current.role() : previous.role();
		String refusal = (current != null && current.refusal() != null) ? current.refusal() : previous.refusal();

		List<TextChatToolCallStream> toolCalls = mergeToolCalls(previous.toolCalls(),
				(current != null) ? current.toolCalls() : null);

		return new TextChatResultDelta(role, content, refusal, toolCalls);
	}

	private List<TextChatToolCallStream> mergeToolCalls(List<TextChatToolCallStream> previous,
			List<TextChatToolCallStream> current) {
		List<TextChatToolCallStream> toolCalls = new ArrayList<>((previous != null) ? previous : List.of());
		if (current == null) {
			return toolCalls;
		}

		// Every tool call fragment of the chunk is added to the tool call it belongs to
		for (TextChatToolCallStream fragment : current) {
			if (fragment == null) {
				continue;
			}
			int position = findToolCall(toolCalls, fragment);
			if (position < 0) {
				toolCalls.add(fragment);
			}
			else {
				toolCalls.set(position, merge(toolCalls.get(position), fragment));
			}
		}
		return toolCalls;
	}

	/**
	 * Finds the tool call a streamed fragment belongs to: the one with the same index,
	 * else the one with the same id. A fragment with neither belongs to the last tool
	 * call.
	 * @return the position of the tool call, or -1 if the fragment starts a new one
	 */
	private int findToolCall(List<TextChatToolCallStream> toolCalls, TextChatToolCallStream fragment) {
		for (int i = 0; i < toolCalls.size(); i++) {
			if (fragment.index() != null && fragment.index().equals(toolCalls.get(i).index())) {
				return i;
			}
		}
		if (StringUtils.hasText(fragment.id())) {
			for (int i = 0; i < toolCalls.size(); i++) {
				if (fragment.id().equals(toolCalls.get(i).id())) {
					return i;
				}
			}
			return -1;
		}
		return (fragment.index() == null) ? toolCalls.size() - 1 : -1;
	}

	private TextChatToolCallStream merge(TextChatToolCallStream previous, TextChatToolCallStream current) {
		if (previous == null) {
			return current;
		}

		Integer index = (current != null && current.index() != null) ? current.index() : previous.index();
		String id = (current != null && StringUtils.hasText(current.id())) ? current.id() : previous.id();
		ToolType type = (current != null && current.type() != null) ? current.type() : previous.type();

		TextChatFunctionCall function = merge(previous.function(), (current != null) ? current.function() : null);

		return new TextChatToolCallStream(index, id, type, function);
	}

	private TextChatFunctionCall merge(TextChatFunctionCall previous, TextChatFunctionCall current) {
		if (previous == null) {
			return current;
		}

		String name = (current != null && StringUtils.hasText(current.name())) ? current.name() : previous.name();

		// The streaming API sends the JSON arguments in fragments that need to be
		// concatenated. A fragment is never a replacement, even if it looks like a
		// complete JSON object on its own (e.g. a nested object).
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
