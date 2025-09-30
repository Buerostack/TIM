# TIM Overview

## What is TIM?

TIM (TARA Integration Module) is a secure identity gateway that issues and manages JWT tokens.  
It supports two modes:

1. **TARA-based authentication** — Users authenticate via Estonia’s national login system (redirect flow). TIM then issues a signed JWT to represent the session.  
2. **Custom JWTs** — Services or developers can generate tokens programmatically via TIM APIs without going through TARA.

## Why is TIM important?

- Ensures all tokens are standardized (JWT format).
- Tokens are digitally signed and verifiable anywhere.
- Provides separation of concerns: authentication is centralized in TIM, while applications focus on business logic.
- Strengthens compliance: no app is allowed to bypass identity rules.

## Typical flows

- Citizen logs in with TARA → TIM mints a secure JWT → applications consume that JWT.  
- Developer testing an API requests a custom JWT → services validate it via TIM’s public key.  
