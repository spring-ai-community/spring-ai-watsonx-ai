package org.springaicommunity.watsonx.autoconfigure.rerank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springaicommunity.watsonx.rerank.WatsonxAiDocumentReranker;
import org.springaicommunity.watsonx.rerank.WatsonxAiRerankModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class WatsonxAiRerankAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WatsonxAiDocumentRerankerConfiguration.class))
		.withBean(WatsonxAiRerankModel.class, () -> mock(WatsonxAiRerankModel.class));

	@Test
	void documentRerankerCreatedWhenRagIsPresent() {
		this.contextRunner.run(context -> assertThat(context).hasSingleBean(WatsonxAiDocumentReranker.class));
	}

	@Test
	void contextStartsWithoutRagAndDocumentRerankerIsNotCreated() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.ai.rag.postretrieval.document"))
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context).doesNotHaveBean("watsonxAiDocumentReranker");
			});
	}

}
