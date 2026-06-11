APPS_TODO

- [ ] Handle the situation of missing scope -> ask for consent for additional scopes
- [ ] Cache the app scopes, so every request doesn't need to trigger db query
- [ ] Proper oAuth for apps?

## Security follow-ups (logged during pre-preview review)

These are known, accepted-for-now gaps surfaced while hardening the apps backend before
the preview deployment. Tracked here rather than fixed because they are lower-risk or need
a larger design decision.

- [ ] **`webhookSecret` stored in plaintext** (`AppInstallService.register`). The clientSecret
  is hashed, but the webhook secret is kept in plaintext because HMAC signing of outgoing
  webhooks needs the raw value. Should be encrypted at rest (e.g. column encryption) rather
  than stored as cleartext.
- [ ] **Shared JWT signing key** — `AppTokenService` reuses the platform's `jwt_signing_key`
  to sign app context tokens. The `aud=tg.app` claim separates them from user JWTs, but a
  dedicated key (and ideally asymmetric / JWKS) is the right end state so app tokens can't be
  confused with user tokens if the audience check ever regresses.
- [ ] **Webhook delivery URL is not SSRF-validated.** The manifest *fetch* is now guarded by
  `UrlSecurity` (`AppManifestFetcher.fetch`), but the resolved webhook URL (where Tolgee POSTs
  activity) is stored and delivered to without the same private-network check. Validate it at
  send time in the apps webhook path (kept out of the manifest fetcher because the test
  manifests use a non-resolving `app.example.com` baseUrl).
- [ ] **Adding scopes after install requires owner action.** Plugin-initiated manifest updates
  (`/v2/apps/self/manifest-url`) are now narrow-only: they can drop scopes but never widen
  them. To grant a newly-declared scope, an org owner must refresh from the UI (owner path
  still widens). A proper incremental-consent flow would make this self-service safely.

## Local development note

`AppManifestFetcher` now rejects manifest URLs that resolve to private/loopback/link-local
addresses (SSRF protection). Local app development registers a `localhost` manifest URL, which
the guard would block. `application-dev.yaml` is gitignored (per-developer), so add this to
your local one when developing apps against a local Tolgee:

```yaml
tolgee:
  internal:
    disable-url-ssrf-protection: true
```
