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

package org.springaicommunity.watsonx.chat;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps the fields of a chat moderation that its class doesn't model, such as
 * {@code mask} or {@code input_ranges}, when it's read from JSON, and writes them back,
 * so they reach watsonx.ai as they were given. The API allows other fields at every level
 * of {@code moderations}.
 *
 * @author Federico Mariani
 * @since 2.2.0
 */
abstract class ChatModerationOtherFields {

	@JsonIgnore
	private final Map<String, Object> otherFields = new LinkedHashMap<>();

	@JsonAnySetter
	void otherField(String name, Object value) {
		this.otherFields.put(name, value);
	}

	@JsonAnyGetter
	Map<String, Object> otherFields() {
		return this.otherFields;
	}

	boolean sameOtherFields(ChatModerationOtherFields other) {
		return this.otherFields.equals(other.otherFields);
	}

}
