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
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the fields of a chat moderation that its class doesn't model, such as
 * {@code mask} or {@code input_ranges}, when it's read from JSON, and writes them back,
 * so they reach watsonx.ai as they were given. The API allows other fields at every level
 * of {@code moderations}.
 *
 * <p>
 * Not abstract and annotated with {@link JsonInclude}, so that the native image hints,
 * which scan the package for such classes, give Jackson its methods.
 *
 * @author Federico Mariani
 * @since 2.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ChatModerationOtherFields {

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

	/**
	 * Copies the other fields into a copy of this object, with their maps and lists
	 * copied too and their values unchanged.
	 * @param copy the copy of this object
	 * @return the copy
	 */
	<T extends ChatModerationOtherFields> T withOtherFieldsOf(T copy) {
		this.otherFields.forEach((name, value) -> copy.otherField(name, deepCopy(value)));
		return copy;
	}

	private static Object deepCopy(Object value) {
		if (value instanceof Map<?, ?> map) {
			Map<Object, Object> copy = new LinkedHashMap<>();
			map.forEach((key, item) -> copy.put(key, deepCopy(item)));
			return copy;
		}
		if (value instanceof List<?> list) {
			List<Object> copy = new ArrayList<>(list.size());
			list.forEach(item -> copy.add(deepCopy(item)));
			return copy;
		}
		return value;
	}

}
