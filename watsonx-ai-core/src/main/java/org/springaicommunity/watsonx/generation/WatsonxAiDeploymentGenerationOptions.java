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

package org.springaicommunity.watsonx.generation;

import java.util.List;
import java.util.Objects;

/**
 * Options for a watsonx.ai text generation deployment: which deployment to call, and the
 * generation parameters. A parameter that isn't set is not sent, so the deployed prompt
 * template's own value applies.
 *
 * @author Ana Katrina Inguengan
 * @since 2.1.0
 */
public class WatsonxAiDeploymentGenerationOptions {

	/** The ID or serving name of the deployment. */
	private String deploymentId;

	/** {@code greedy} or {@code sample}. */
	private String decodingMethod;

	/** The maximum number of tokens to generate. */
	private Integer maxNewTokens;

	/** The minimum number of tokens to generate. */
	private Integer minNewTokens;

	/** The sampling temperature. */
	private Double temperature;

	/** The cumulative probability of the tokens to sample from. */
	private Double topP;

	/** The number of most likely tokens to sample from. */
	private Integer topK;

	/** The penalty for repeated tokens. */
	private Double repetitionPenalty;

	/** The seed for sampling. */
	private Integer randomSeed;

	/** Sequences that stop the generation. */
	private List<String> stopSequences;

	/** The time limit for the generation, in milliseconds. */
	private Long timeLimit;

	public WatsonxAiDeploymentGenerationOptions() {
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A copy of these options, with every value the other options set.
	 * @param other the options that take precedence, may be {@code null}
	 * @return the merged options
	 */
	public WatsonxAiDeploymentGenerationOptions merge(final WatsonxAiDeploymentGenerationOptions other) {
		WatsonxAiDeploymentGenerationOptions merged = copy();
		if (other == null) {
			return merged;
		}
		merged.deploymentId = (other.deploymentId != null) ? other.deploymentId : merged.deploymentId;
		merged.decodingMethod = (other.decodingMethod != null) ? other.decodingMethod : merged.decodingMethod;
		merged.maxNewTokens = (other.maxNewTokens != null) ? other.maxNewTokens : merged.maxNewTokens;
		merged.minNewTokens = (other.minNewTokens != null) ? other.minNewTokens : merged.minNewTokens;
		merged.temperature = (other.temperature != null) ? other.temperature : merged.temperature;
		merged.topP = (other.topP != null) ? other.topP : merged.topP;
		merged.topK = (other.topK != null) ? other.topK : merged.topK;
		merged.repetitionPenalty = (other.repetitionPenalty != null) ? other.repetitionPenalty
				: merged.repetitionPenalty;
		merged.randomSeed = (other.randomSeed != null) ? other.randomSeed : merged.randomSeed;
		merged.stopSequences = (other.stopSequences != null) ? other.stopSequences : merged.stopSequences;
		merged.timeLimit = (other.timeLimit != null) ? other.timeLimit : merged.timeLimit;
		return merged;
	}

	/**
	 * A copy of these options.
	 * @return the copy
	 */
	public WatsonxAiDeploymentGenerationOptions copy() {
		WatsonxAiDeploymentGenerationOptions copy = new WatsonxAiDeploymentGenerationOptions();
		copy.deploymentId = this.deploymentId;
		copy.decodingMethod = this.decodingMethod;
		copy.maxNewTokens = this.maxNewTokens;
		copy.minNewTokens = this.minNewTokens;
		copy.temperature = this.temperature;
		copy.topP = this.topP;
		copy.topK = this.topK;
		copy.repetitionPenalty = this.repetitionPenalty;
		copy.randomSeed = this.randomSeed;
		copy.stopSequences = (this.stopSequences != null) ? List.copyOf(this.stopSequences) : null;
		copy.timeLimit = this.timeLimit;
		return copy;
	}

	public String getDeploymentId() {
		return this.deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getDecodingMethod() {
		return this.decodingMethod;
	}

	public void setDecodingMethod(String decodingMethod) {
		this.decodingMethod = decodingMethod;
	}

	public Integer getMaxNewTokens() {
		return this.maxNewTokens;
	}

	public void setMaxNewTokens(Integer maxNewTokens) {
		this.maxNewTokens = maxNewTokens;
	}

	public Integer getMinNewTokens() {
		return this.minNewTokens;
	}

	public void setMinNewTokens(Integer minNewTokens) {
		this.minNewTokens = minNewTokens;
	}

	public Double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Double getTopP() {
		return this.topP;
	}

	public void setTopP(Double topP) {
		this.topP = topP;
	}

	public Integer getTopK() {
		return this.topK;
	}

	public void setTopK(Integer topK) {
		this.topK = topK;
	}

	public Double getRepetitionPenalty() {
		return this.repetitionPenalty;
	}

	public void setRepetitionPenalty(Double repetitionPenalty) {
		this.repetitionPenalty = repetitionPenalty;
	}

	public Integer getRandomSeed() {
		return this.randomSeed;
	}

	public void setRandomSeed(Integer randomSeed) {
		this.randomSeed = randomSeed;
	}

	public List<String> getStopSequences() {
		return this.stopSequences;
	}

	public void setStopSequences(List<String> stopSequences) {
		this.stopSequences = stopSequences;
	}

	public Long getTimeLimit() {
		return this.timeLimit;
	}

	public void setTimeLimit(Long timeLimit) {
		this.timeLimit = timeLimit;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WatsonxAiDeploymentGenerationOptions other)) {
			return false;
		}
		return Objects.equals(this.deploymentId, other.deploymentId)
				&& Objects.equals(this.decodingMethod, other.decodingMethod)
				&& Objects.equals(this.maxNewTokens, other.maxNewTokens)
				&& Objects.equals(this.minNewTokens, other.minNewTokens)
				&& Objects.equals(this.temperature, other.temperature) && Objects.equals(this.topP, other.topP)
				&& Objects.equals(this.topK, other.topK)
				&& Objects.equals(this.repetitionPenalty, other.repetitionPenalty)
				&& Objects.equals(this.randomSeed, other.randomSeed)
				&& Objects.equals(this.stopSequences, other.stopSequences)
				&& Objects.equals(this.timeLimit, other.timeLimit);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.deploymentId, this.decodingMethod, this.maxNewTokens, this.minNewTokens,
				this.temperature, this.topP, this.topK, this.repetitionPenalty, this.randomSeed, this.stopSequences,
				this.timeLimit);
	}

	public static final class Builder {

		private final WatsonxAiDeploymentGenerationOptions options = new WatsonxAiDeploymentGenerationOptions();

		private Builder() {
		}

		public Builder deploymentId(String deploymentId) {
			this.options.deploymentId = deploymentId;
			return this;
		}

		public Builder decodingMethod(String decodingMethod) {
			this.options.decodingMethod = decodingMethod;
			return this;
		}

		public Builder maxNewTokens(Integer maxNewTokens) {
			this.options.maxNewTokens = maxNewTokens;
			return this;
		}

		public Builder minNewTokens(Integer minNewTokens) {
			this.options.minNewTokens = minNewTokens;
			return this;
		}

		public Builder temperature(Double temperature) {
			this.options.temperature = temperature;
			return this;
		}

		public Builder topP(Double topP) {
			this.options.topP = topP;
			return this;
		}

		public Builder topK(Integer topK) {
			this.options.topK = topK;
			return this;
		}

		public Builder repetitionPenalty(Double repetitionPenalty) {
			this.options.repetitionPenalty = repetitionPenalty;
			return this;
		}

		public Builder randomSeed(Integer randomSeed) {
			this.options.randomSeed = randomSeed;
			return this;
		}

		public Builder stopSequences(List<String> stopSequences) {
			this.options.stopSequences = stopSequences;
			return this;
		}

		public Builder timeLimit(Long timeLimit) {
			this.options.timeLimit = timeLimit;
			return this;
		}

		public WatsonxAiDeploymentGenerationOptions build() {
			return this.options.copy();
		}

	}

}
