# Security Policy

## Supported versions

Security fixes go into the latest 2.0.x release.

| Version | Supported |
|---------|-----------|
| 2.0.x   | Yes       |
| 1.x     | No        |

## Reporting a vulnerability

Please don't open a public issue, pull request or discussion for a security problem.

Report it privately through GitHub instead:

1. Go to the [Security tab](https://github.com/spring-ai-community/spring-ai-watsonx-ai/security) of this repository.
2. Click **Report a vulnerability**.
3. Describe the problem, the affected version, and the steps to reproduce it.

The maintainers will reply in the advisory, work on a fix with you there, and publish the advisory once a fixed version is released.

## Vulnerabilities in dependencies

This project uses the dependency versions managed by Spring Boot and Spring AI. If a vulnerability is in one of those dependencies (for example Jackson or Netty) and not in this project's code, please report it to that project. A fix usually reaches this project through a Spring Boot or Spring AI upgrade.
