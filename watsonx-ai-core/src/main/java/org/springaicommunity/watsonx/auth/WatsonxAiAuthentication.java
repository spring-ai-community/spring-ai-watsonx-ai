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
import com.ibm.cloud.sdk.core.security.IamToken;
import java.util.Objects;

/**
 * watsonx.ai Authentication API that utilizes IBM Cloud SDK. For more information, refer to <a
 * href=https://cloud.ibm.com/docs/api-handbook?topic=api-handbook-authentication>IBM Cloud
 * Authentication</a>.
 *
 * @author Tristan Mahinay
 * @since 1.0.0
 */
public final class WatsonxAiAuthentication {
  private final IamAuthenticator iamAuthenticator;
  private IamToken token;

  public WatsonxAiAuthentication(String apiKey) {
    this.iamAuthenticator = new IamAuthenticator.Builder().apikey(apiKey).build();
  }

  public String getAccessToken() {
    if (Objects.isNull(this.token) || this.token.needsRefresh()) {
      this.token = this.iamAuthenticator.requestToken();
    }

    return this.token.getAccessToken();
  }
}
