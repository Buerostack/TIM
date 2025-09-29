# TIM Database Overview

This document explains how TIM uses PostgreSQL for **custom JWT** and **TARA** flows. TIM is designed with **strict seclusion** between the two concerns:

- Schema `custom` — all data related to custom JWT minting/validation.
- Schema `tara` — all data related to TARA-based JWT minting/validation (plus OAuth2 state during login redirect flow).

No tables are shared across these schemas. This separation is intentional for security and auditability.

## Connection

- Default JDBC URL (inside Docker): `jdbc:postgresql://postgres:5432/tim`
- Default user/password: `tim` / `123`
- Default DB name: `tim`

TIM is stateless; the DB stores only **metadata** (not private keys). JWT signing private key lives in `jwtkeystore.jks` inside the container. The DB is used for:

- Token metadata (audit: issuance time, name, subject, expiry).
- Allow/Deny lists.
- TARA OAuth2 ephemeral state (`tara.oauth_state`) during redirect.
