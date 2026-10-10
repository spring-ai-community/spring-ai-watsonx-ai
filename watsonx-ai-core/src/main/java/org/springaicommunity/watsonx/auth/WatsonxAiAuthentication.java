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

import com.ibm.cloud.sdk.core.security.IamAuthenticator;

/**
 * watsonx.ai Authentication API that utilizes IBM Cloud SDK. For more information, refer
 * to
 * <a href="https://cloud.ibm.com/docs/api-handbook?topic=api-handbook-authentication">IBM
 * Cloud Authentication</a>.
 * <p>
 * Safe for concurrent use: token caching and refresh are delegated to the
 * {@link IamAuthenticator}, which refreshes a token that is close to expiry in the
 * background. Calls are serialized, because the authenticator stores a newly requested
 * token only after releasing its own lock, so concurrent callers could otherwise each
 * request a token.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public final class WatsonxAiAuthentication {

	private final IamAuthenticator iamAuthenticator;

	public WatsonxAiAuthentication(String apiKey) {
		this(new IamAuthenticator.Builder().apikey(apiKey).build());
	}

	WatsonxAiAuthentication(IamAuthenticator iamAuthenticator) {
		this.iamAuthenticator = iamAuthenticator;
	}

	public synchronized String getAccessToken() {
		return this.iamAuthenticator.getToken();
	}

}
