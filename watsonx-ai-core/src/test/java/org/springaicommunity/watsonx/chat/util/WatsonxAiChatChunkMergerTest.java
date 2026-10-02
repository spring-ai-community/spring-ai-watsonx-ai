/*
 * Copyright 2026 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream.TextChatFunctionCall;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream.TextChatResultChoiceStream;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream.TextChatResultDelta;
import org.springaicommunity.watsonx.chat.WatsonxAiChatStream.TextChatToolCallStream;

/**
 * JUnit 5 test class for {@link WatsonxAiChatChunkMerger}. Streamed tool call chunks are
 * merged the same way {@code WatsonxAiChatApi.stream()} does, starting from an empty
 * chunk.
 *
 * @author Ana Katrina Inguengan
 */
class WatsonxAiChatChunkMergerTest {

	private final WatsonxAiChatChunkMerger merger = new WatsonxAiChatChunkMerger();

	@Test
	void detectsToolCallChunksAndTheirFinish() {
		WatsonxAiChatStream toolCall = chunk(null, toolCall(0, "call-1", "getWeather", "{}"));
		WatsonxAiChatStream finish = chunk("tool_calls", toolCall(0, null, null, ""));
		WatsonxAiChatStream text = textChunk("Hello");

		assertTrue(this.merger.isStreamingToolFunctionCall(toolCall));
		assertFalse(this.merger.isStreamingToolFunctionCall(text));
		assertTrue(this.merger.isStreamingToolFunctionCallFinish(finish));
		assertFalse(this.merger.isStreamingToolFunctionCallFinish(toolCall));
	}

	@Test
	void appendsArgumentFragmentsOfOneToolCall() {
		List<TextChatToolCallStream> toolCalls = mergeToolCalls(
				chunk(null, toolCall(0, "call-1", "getWeather", "{\"city\":")),
				chunk(null, toolCall(0, null, null, "\"Manila\"")), chunk("tool_calls", toolCall(0, null, null, "}")));

		assertEquals(1, toolCalls.size());
		assertToolCall(toolCalls.get(0), "call-1", "getWeather", "{\"city\":\"Manila\"}");
	}

	@Test
	void keepsToolCallSentInOneChunk() {
		// Some models send the whole tool call at once, then a separate finish chunk
		List<TextChatToolCallStream> toolCalls = mergeToolCalls(
				chunk(null, toolCall(0, "call-1", "getWeather", "{\"city\":\"Manila\"}")), chunk("tool_calls"));

		assertEquals(1, toolCalls.size());
		assertToolCall(toolCalls.get(0), "call-1", "getWeather", "{\"city\":\"Manila\"}");
	}

	@Test
	void keepsToolCallsStreamedOneAfterAnother() {
		List<TextChatToolCallStream> toolCalls = mergeToolCalls(
				chunk(null, toolCall(0, "call-1", "getWeather", "{\"city\":")),
				chunk(null, toolCall(0, null, null, "\"Manila\"}")),
				chunk(null, toolCall(1, "call-2", "getTime", "{\"zone\":")),
				chunk("tool_calls", toolCall(1, null, null, "\"Asia/Manila\"}")));

		assertEquals(2, toolCalls.size());
		assertToolCall(toolCalls.get(0), "call-1", "getWeather", "{\"city\":\"Manila\"}");
		assertToolCall(toolCalls.get(1), "call-2", "getTime", "{\"zone\":\"Asia/Manila\"}");
	}

	@Test
	void keepsAllToolCallsOfOneChunk() {
		// The second chunk finishes the first tool call and carries the whole second one
		List<TextChatToolCallStream> toolCalls = mergeToolCalls(
				chunk(null, toolCall(0, "call-1", "getWeather", "{\"city\":")),
				chunk("tool_calls", toolCall(0, null, null, "\"Manila\"}"),
						toolCall(1, "call-2", "getTime", "{\"zone\":\"Asia/Manila\"}")));

		assertEquals(2, toolCalls.size(), "Every tool call of a chunk must be kept, not only the first");
		assertToolCall(toolCalls.get(0), "call-1", "getWeather", "{\"city\":\"Manila\"}");
		assertToolCall(toolCalls.get(1), "call-2", "getTime", "{\"zone\":\"Asia/Manila\"}");
	}

	@Test
	void appendsFragmentThatLooksLikeCompleteJsonObject() {
		List<TextChatToolCallStream> toolCalls = mergeToolCalls(
				chunk(null, toolCall(0, "call-1", "getWeather", "{\"location\":")),
				chunk(null, toolCall(0, null, null, "{\"lat\":14.6}")),
				chunk("tool_calls", toolCall(0, null, null, "}")));

		assertToolCall(toolCalls.get(0), "call-1", "getWeather", "{\"location\":{\"lat\":14.6}}");
	}

	@Test
	void appendsFragmentsThatRepeatTheToolCallId() {
		List<TextChatToolCallStream> toolCalls = mergeToolCalls(
				chunk(null, toolCall(0, "call-1", "getWeather", "{\"city\":")),
				chunk("tool_calls", toolCall(0, "call-1", null, "\"Manila\"}")));

		assertEquals(1, toolCalls.size());
		assertToolCall(toolCalls.get(0), "call-1", "getWeather", "{\"city\":\"Manila\"}");
	}

	@Test
	void keepsEveryChoice() {
		WatsonxAiChatStream merged = mergeAll(
				new WatsonxAiChatStream("chat-1", "ibm/granite-3-3-8b-instruct", null,
						List.of(choice(0, null, toolCall(0, "call-1", "getWeather", "{\"city\":\"Manila\"}")),
								choice(1, null, toolCall(0, "call-2", "getWeather", "{\"city\":\"Zurich\"}"))),
						null, null, null, null),
				new WatsonxAiChatStream("chat-1", "ibm/granite-3-3-8b-instruct", null,
						List.of(choice(0, "tool_calls"), choice(1, "tool_calls")), null, null, null, null));

		assertEquals(2, merged.choices().size(), "Every choice must be kept, not only the first");
		assertToolCall(merged.choices().get(1).delta().toolCalls().get(0), "call-2", "getWeather",
				"{\"city\":\"Zurich\"}");
	}

	private List<TextChatToolCallStream> mergeToolCalls(WatsonxAiChatStream... chunks) {
		WatsonxAiChatStream merged = mergeAll(chunks);
		assertEquals(1, merged.choices().size());
		assertEquals("tool_calls", merged.choices().get(0).finishReason());
		return merged.choices().get(0).delta().toolCalls();
	}

	/** Merges the chunks like {@code WatsonxAiChatApi.stream()} reduces a window. */
	private WatsonxAiChatStream mergeAll(WatsonxAiChatStream... chunks) {
		WatsonxAiChatStream merged = new WatsonxAiChatStream(null, null, null, null, null, null, null, null);
		for (WatsonxAiChatStream chunk : chunks) {
			merged = this.merger.merge(merged, chunk);
		}
		return merged;
	}

	private static void assertToolCall(TextChatToolCallStream toolCall, String id, String name, String arguments) {
		assertEquals(id, toolCall.id());
		assertEquals(name, toolCall.function().name());
		assertEquals(arguments, toolCall.function().arguments());
	}

	private static WatsonxAiChatStream chunk(String finishReason, TextChatToolCallStream... toolCalls) {
		return new WatsonxAiChatStream("chat-1", "ibm/granite-3-3-8b-instruct", null,
				List.of(choice(0, finishReason, toolCalls)), null, null, null, null);
	}

	private static WatsonxAiChatStream textChunk(String content) {
		return new WatsonxAiChatStream("chat-1", "ibm/granite-3-3-8b-instruct", null, List
			.of(new TextChatResultChoiceStream(0, new TextChatResultDelta(null, content, null, null), null, null)),
				null, null, null, null);
	}

	private static TextChatResultChoiceStream choice(int index, String finishReason,
			TextChatToolCallStream... toolCalls) {
		return new TextChatResultChoiceStream(index, new TextChatResultDelta(null, null, null, List.of(toolCalls)),
				finishReason, null);
	}

	private static TextChatToolCallStream toolCall(int index, String id, String name, String arguments) {
		return new TextChatToolCallStream(index, id, (id != null) ? ToolType.FUNCTION : null,
				new TextChatFunctionCall(name, arguments));
	}

}
