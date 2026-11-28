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

package org.springaicommunity.watsonx.evaluation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response of the watsonx.ai detection on context documents and on generated text.
 *
 * @param detections what the detectors found. A detector with a threshold leaves out a
 * detection whose score is below it
 * @author Federico Mariani
 * @since 2.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatsonxAiDetectionResponse(@JsonProperty("detections") List<Detection> detections) {

	/**
	 * A detection.
	 *
	 * @param detectionType the detector, such as {@code answer_relevance}
	 * @param detection the label, such as {@code relevant} or {@code factual}
	 * @param detectorId the model that detected it, such as
	 * {@code granite_guardian_3_2_5b}
	 * @param score the probability, from 0 to 1, that the label is right
	 * @param evidence the evidence, such as the context sentences that support it, when
	 * the detector gives some
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Detection(@JsonProperty("detection_type") String detectionType,
			@JsonProperty("detection") String detection, @JsonProperty("detector_id") String detectorId,
			@JsonProperty("score") Double score, @JsonProperty("evidence") List<Evidence> evidence) {
	}

	/**
	 * An evidence of a detection.
	 *
	 * @param name the name of the evidence
	 * @param value the value of the evidence
	 * @param score the score of the evidence
	 * @param evidence the evidence it has in turn
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Evidence(@JsonProperty("name") String name, @JsonProperty("value") String value,
			@JsonProperty("score") Double score, @JsonProperty("evidence") List<Evidence> evidence) {
	}

}
