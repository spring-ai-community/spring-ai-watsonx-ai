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

package org.springaicommunity.watsonx.auth;

import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedConstruction;

/**
 * JUnit extension that stubs every {@link WatsonxAiAuthentication} created during a test
 * to return a fixed access token, so tests against mocked watsonx.ai endpoints make no
 * IBM IAM call.
 *
 * @author Ana Katrina Inguengan
 */
public class StubWatsonxAiAuthentication implements BeforeEachCallback, AfterEachCallback {

	public static final String ACCESS_TOKEN = "test-access-token";

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
		.create(StubWatsonxAiAuthentication.class);

	@Override
	public void beforeEach(ExtensionContext context) {
		MockedConstruction<WatsonxAiAuthentication> authentication = mockConstruction(WatsonxAiAuthentication.class,
				(mock, mockContext) -> when(mock.getAccessToken()).thenReturn(ACCESS_TOKEN));
		context.getStore(NAMESPACE).put(MockedConstruction.class, authentication);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		MockedConstruction<?> authentication = context.getStore(NAMESPACE)
			.remove(MockedConstruction.class, MockedConstruction.class);
		if (authentication != null) {
			authentication.close();
		}
	}

}
