# OAuth 2.1 authorization server

Tolgee can now let apps (browser extension, CLI, MCP clients) sign a user in **through the browser**
and receive a short-lived token, instead of the user pasting a long-lived API key. This is what
"Sign in with Google/GitHub" does — Tolgee is now the thing you sign in *with*.

This page explains, in plain words, how it works and what the jargon means.

## 1. Overview

**The problem.** Before, every non-webapp client authenticated with a static secret (a Project API
Key or Personal Access Token) sent as `X-API-Key`. You generate it in the UI, copy it, paste it into
the client. That secret is long-lived and, for a public-project community translator, we can't safely
hand one out at all.

**The idea.** With OAuth, the app sends the user to Tolgee in the browser, the user logs in and
approves ("this app may suggest translations on Project X"), and the app gets back a **short-lived
access token**. The token is a signed JWT that the existing API already understands as a
`Authorization: Bearer <token>` header.

**Two halves:**
- **Authorization server** (new) — mints tokens: the `/oauth2/authorize`, `/oauth2/token`,
  `/oauth2/jwks` endpoints.
- **Resource server** (already existed) — the REST API that accepts the token. We only had to teach
  it to recognize these new tokens next to the old ones.

## 2. How To (the flow)

The headline case: a **community translator** authorizes the browser extension on a public project.

```mermaid
sequenceDiagram
    participant U as User (browser)
    participant C as App (extension/CLI)
    participant AS as Tolgee auth server
    participant API as Tolgee REST API

    C->>AS: 1. open /oauth2/authorize?...  (+ PKCE code_challenge)
    AS->>U: 2. log in + consent screen<br/>"Allow <app> to suggest & comment on Project X?"
    U->>AS: 3. Allow
    AS->>C: 4. redirect back with a one-time code
    C->>AS: 5. POST /oauth2/token (code + PKCE code_verifier)
    AS->>C: 6. access_token (short-lived JWT) + refresh_token
    C->>API: 7. GET /v2/... with Authorization: Bearer <access_token>
    API->>API: 8. verify signature via JWKS, then<br/>allow = token scopes ∩ user's live permissions
    API->>C: 9. data
```

Key point about step 8: the token can only ever **narrow** what the user is already allowed to do.
Effective access = `token's scopes ∩ the user's live permissions on that project`, re-checked on every
request. Lose access to the project and the token instantly stops working there — nothing is "baked in".

### The extra hop for browsers (session bootstrap)

Tolgee's web login is stateless (a JWT held in the browser's local storage, not a cookie). But step 1
above is a plain browser navigation, which **can't** send that JWT. So when the browser lands on
`/oauth2/authorize` unauthenticated, we bounce it to a small SPA page that turns the stored JWT into a
short-lived server session (a cookie), then continues to `/oauth2/authorize`. After that the standard
flow above runs.

## 3. Reference — the jargon

### PKCE ("pixy", Proof Key for Code Exchange)
The apps here are **public clients**: they have no secret they can keep hidden (anyone can unpack a
browser extension or CLI). PKCE replaces the missing secret with a one-time proof:
1. Before starting, the app makes a random string, the **`code_verifier`**.
2. It sends only its SHA-256 hash, the **`code_challenge`**, on `/oauth2/authorize` (step 1).
3. When exchanging the code for a token (step 5), it sends the original `code_verifier`.
4. The server hashes it and checks it matches the challenge.

So even if someone steals the one-time code in transit, it's useless without the `code_verifier` that
never left the app. PKCE is required for every Tolgee OAuth client.

### JWT (JSON Web Token)
The access token is a JWT: three base64 parts (`header.payload.signature`). The payload holds claims
like `sub` (the user id), `scope`, `aud` (audience), `exp` (expiry), and Tolgee's `tg.prj` (which
projects the token is bound to). It's **signed**, so the API can trust it without a database lookup —
that's why token validation on the API hot path costs nothing but a signature check.

### JWK and JWKS
- A **JWK** (JSON Web Key) is just a cryptographic key written as JSON.
- **JWKS** (JWK Set) is the list of the server's **public** keys, published at `/oauth2/jwks`.

Tolgee signs tokens with a **private** RSA key (kept secret, on the server). Anyone — including our own
API — verifies a token's signature using the matching **public** key fetched from `/oauth2/jwks`. The
private key never leaves the server; only public keys are published. (See `OAuth2KeyConfig`.)

### `kid` (Key ID)
A server may have more than one key at a time (e.g. during key rotation: a new key while the old one is
still honored until its tokens expire). Each key gets a unique **`kid`**. The token's header records
which `kid` signed it, so a verifier knows exactly which public key from the JWKS to use. In
`OAuth2KeyConfig` we generate the `kid` when the key is created and **persist it with the key**, so it
stays stable across restarts and replicas — otherwise tokens signed before a restart would suddenly
fail to verify.

### issuer
The base URL that identifies this authorization server; it's the `iss` claim in tokens and the root of
discovery (`{issuer}/.well-known/oauth-authorization-server`), and Spring builds every endpoint URL
relative to it. It must therefore be the URL where the OAuth endpoints (`/oauth2/token`, `/oauth2/jwks`,
…) are actually reachable — i.e. the **backend / API URL** (`back-end-url`).

We compute it as `backEndUrl ?: frontEndUrl` (`OAuth2AuthorizationServerConfig`). The `frontEndUrl`
fallback is **not** "use the web app as the issuer" — it only exists for deployments that serve the API
and the web app from **one origin** (the backend also serves the built SPA), where `back-end-url` is
often left unset and `front-end-url` *is* that single origin. If your backend runs on a separate URL you
**must set `back-end-url`**, and then the fallback never fires. (If your topology always separates the
two, treat `back-end-url` as required.)

### scope vs. project set
- **scope** = a capability verb (`translations.suggest`, `translation-comments.add`) — *what* the token
  may do. Standard OAuth `scope` claim.
- **project set** (`tg.prj` claim) = *where* it may do it: specific project ids, or the `*` sentinel
  meaning "don't narrow by project" (still bounded by the user's live permissions).

### Registered clients: how an app becomes "known"
Before Tolgee will issue tokens to an app, it must know that app's `client_id` and its allowed
`redirect_uris` (so a stolen code can't be sent to an attacker's URL). There are three ways to do that.

**a) Pre-registration (what our own apps use).** The browser extension and CLI are seeded as fixed
clients with a known `client_id` (`PreRegisteredClients.kt`). Simple and safe, but only works for apps
*we* control — we can't pre-register every third-party MCP tool in the world.

**b) CIMD — Client ID Metadata Document (supported, for third-party MCP clients).** Instead of a made-up
id, the client's `client_id` **is an HTTPS URL** that points at a small JSON document describing itself
(its name, `redirect_uris`, that it's a public+PKCE client). The first time such a `client_id` arrives,
Tolgee **fetches that URL, validates the document, and uses it** — no manual registration, nothing stored
long-term. See `CimdMetadataFetcher` / `CimdRegisteredClientRepository`.

Because Tolgee makes an outbound HTTP request to a URL an untrusted client chose, this is the most
security-sensitive surface, so the fetch is hardened: **https only**, optional host allow-list, the shared
SSRF guard (no internal / loopback / link-local targets), **no redirects**, and hard size + timeout caps.
The document's `client_id` must equal the URL, and every `redirect_uri` must be same-origin as it. The
generated internal id includes a hash of the redirect URIs, so if a client later changes its redirect
URIs the id changes and the user is asked to consent again.

- **Pros:** zero-touch onboarding of any spec-compliant client; nothing to store or expire; the client
  "owns" its own metadata at a URL it controls.
- **Cons:** requires an outbound fetch (SSRF risk to manage); the client must host a reachable HTTPS
  document; metadata can change under us (mitigated by re-consent on redirect change).

**c) DCR — Dynamic Client Registration (RFC 7591, NOT built in v1).** DCR is the older approach: the
client `POST`s its metadata to a public `/register` endpoint on the server, and the server stores it and
hands back a freshly-minted `client_id`. 

- **Pros:** the client is persisted server-side (stable across metadata changes); widely specified.
- **Cons:** it's an **open, unauthenticated write endpoint** — anyone can create client records, which is a
  spam/abuse and storage-growth surface that must be rate-limited and pruned; and it needs a second,
  parallel registration code path. The MCP clients we target already support CIMD, and Anthropic
  recommends CIMD as the default, so we deliberately skipped DCR in v1 to avoid the extra attack surface.
  It remains a possible fast-follow only if we hit a DCR-only client.

## Where it lives in the code

| Concern | Files |
|---|---|
| Signing keys / JWKS | `backend/data/.../security/oauth2/OAuth2KeyConfig.kt` |
| Auth-server filter chain, issuer | `backend/app/.../configuration/OAuth2AuthorizationServerConfig.kt` |
| Token claims (`sub`, `scope`, `tg.prj`, `aud`) | `TolgeeOAuth2TokenCustomizer.kt` |
| API accepts the token + narrows scopes | `AuthenticationFilter.kt`, `OAuth2AccessTokenResolver.kt`, `SecurityService.getCurrentPermittedScopes` |
| Browser session bootstrap + consent info | `backend/api/.../controllers/oauth2/OAuth2FlowController.kt` |
| Connected apps / revoke | `ConnectedAppsController.kt` |
| CIMD | `CimdMetadataFetcher.kt`, `CimdRegisteredClientRepository.kt` |
| Grant/consent/client storage | `db/changelog/oauth2/oauth2-server.xml` (Spring Authorization Server JDBC schema) |

## Round-1 limitations (tracked follow-ups)

These are known gaps, deferred to the client rounds that first exercise them:

- **Refresh is stock rotate-on-use.** `reuseRefreshTokens(false)` gives Spring's plain rotation with
  no grace window and no reuse-detection family-revocation. A client that refreshes proactively can
  hit `invalid_grant` on a near-simultaneous second refresh. When the first refreshing client lands
  (browser extension / CLI), replace `OAuth2RefreshTokenAuthenticationProvider` with one that accepts
  a just-superseded token within a short grace window and revokes the authorization family on replay
  of an already-rotated token. (SAS also does not issue refresh tokens to public/`NONE`-auth clients
  by default, so round-1 clients receive only short-lived access tokens.)
- **Nightly cleanup does not prune abandoned pre-consent rows.** SAS persists an `oauth2_authorization`
  row when consent is *required*, before any code/token is issued — its expiry columns are all NULL,
  so the COALESCE-based `deleteExpiredBefore` never removes it. No round-1 client is consent-required
  by default (the CLI client skips consent; the browser-extension client is only seeded when redirect
  URIs are configured; CIMD is off), so no such rows are created today. When a consent-required client
  is enabled, add a `created_at` column (DB default) to the SAS schema and extend the cleanup to also
  delete all-NULL-expiry rows older than a short grace window.
