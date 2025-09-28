package io.github.springaicommunity.watsonx.auth;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.security.IamToken;
import java.util.Objects;

/**
 * watsonx.ai Authentication API that utilizes IBM Cloud SDK.
 *
 * @author Tristan Mahinay
 * @since 1.1.0-SNAPSHOT
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
