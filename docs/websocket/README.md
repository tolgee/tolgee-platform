# Websockets

How a STOMP connection to `/websocket` is authenticated and what a client may subscribe to. This is
transport-level and applies to every credential type — JWT, PAT, project API key and OAuth access token alike.
OAuth-specific consequences live in [docs/oauth/README.md](../oauth/README.md).

## Authenticating a connection

A credential is read from the STOMP **CONNECT** frame only — `Authorization: Bearer <jwt|tgoat_…>`, `X-API-Key`
for a PAT or a project API key, or the legacy `jwtToken` header. A credential presented on the HTTP handshake is
deliberately ignored and yields an unauthenticated socket.

## What a connection may subscribe to

Subscribing to a project topic requires `keys.view` on that project, and a scoped credential must also cover it.
Two things about that differ from an HTTP call and are worth knowing:

- Every project topic requires exactly `keys.view`, whatever the topic then carries — that is every event type in
  `WebsocketEventType` under `/projects/{id}/`. A grant consented to as `keys.view` alone therefore receives
  `translation-data-modified` and `qa-issues-updated` payloads whose HTTP readers require `translations.view`, and
  `batch-job-progress`. This is the project API key's rule, applied unchanged.
- The credential is resolved **once, at CONNECT**, and never re-checked; see the parity notes in the OAuth doc.

A **user topic is refused to any scoped credential** — a project API key and an OAuth grant alike. `/users/{id}/…`
carries the account's own notifications, which no scope describes; a credential the user handed to an app does not
speak for the user there. An unscoped credential reaches only its own user's topic.

## Why the session attribute is the authority

Only what the STOMP resolver accepted decides a subscription: the verdict is stored on the session at CONNECT and
the subscribe decision reads nothing else. That matters because the handshake is an ordinary HTTP request that the
servlet filter chain authenticates too, and Spring will otherwise offer that principal to later frames. The frame
principal is never consulted for authorization, and it cannot be relied on to be absent — SockJS's HTTP fallbacks
(xhr, xhr-streaming, eventsource) take their principal straight off the handshake request.

## Other websocket endpoints

This covers the STOMP endpoint at `/websocket` only. The EE QA-preview endpoint `/ws/qa-preview`
(`QaCheckPreviewWebSocketHandler`) authenticates separately, from a token in its own init message, and accepts a
JWT alone — an OAuth token, a project API key and a PAT all fail there.

## Refusals: silent deny vs closing the socket

A destination is only recognised when its final segment is a `WebsocketEventType` name, so
`/projects/{id}/<anything-else>` — a wildcard included — is denied rather than treated as a project topic.

Client `SEND` **and `MESSAGE`** frames are refused outright. Every server-side event is published through
`SimpMessagingTemplate`, which reaches the broker channel directly, so no legitimate client sends either;
relayed to the simple broker, both would be fanned out to every subscriber of the destination. `MESSAGE` has to
be named alongside `SEND` because they are the only two commands carrying `SimpMessageType.MESSAGE`, which is
what the broker dispatches on, and nothing on the inbound path rejects a client for using a command that is
nominally server-to-client — the STOMP decoder does not check direction. The frame is dropped silently — there
is no `ERROR` frame back — so when client-to-server messaging is eventually wanted, set
`config.setApplicationDestinationPrefixes("/app")` and narrow the deny to destinations outside that prefix,
for both commands, rather than removing the guard.

A subscription to a project topic that is refused — wrong project, missing scope, or a project that no longer
exists — is dropped silently; the connection stays open and its other subscriptions keep working. The socket is
closed by a SUBSCRIBE to a recognised project or user topic from a session that never authenticated — not by any
other frame from one, as the dropped `SEND` and `MESSAGE` above show — and by anything the authorization
decision throws other than the not-found case it handles. So on the websocket, unlike the HTTP 403-vs-404
behaviour described in the parity notes of [docs/oauth/README.md](../oauth/README.md), "forbidden" and "no such
project" are indistinguishable.

An unresolvable credential does not merely get its subscriptions refused. On the first project or user SUBSCRIBE
the server sends a STOMP `ERROR` frame whose `message` header is `Unauthenticated` and then closes the
connection. **That header string is a contract across three files** — it is thrown in `WebSocketConfig.preSend`,
compared in `WebsocketClient.ts`, and asserted in `WebsocketTestHelper` — so rewording the exception message turns
the webapp's deactivate path back into an endless redial, with nothing failing to compile.

The frame says nothing about *why*: an expired token, a revoked grant and a malformed string are
indistinguishable. That header is also the only thing separating it from an `ERROR` raised by a server-side
failure, which must stay reconnectable, so the two cannot share a reaction. The webapp deactivates the client on
the `Unauthenticated` one and redials on anything else. A deactivated client stays dead until `jwtToken` or
`allowPrivate` changes and `useWsClientService` builds a new one, so an expired credential costs that tab its live
updates for the rest of the token's life, with no user-visible signal and no refresh attempt.

Any client that does redial instead drives an unmetered validation loop: neither STOMP CONNECT nor SUBSCRIBE runs
under the IP auth rate limit that `AuthenticationFilter.doAuthenticate` applies. Rate-limiting CONNECT alone would
not stop the redialing, only make each attempt cheaper — and it would leave SUBSCRIBE, whose authorization check
hits the database on the transport thread, unmetered.

## Gaps this transport shares with the HTTP path

Neither predates nor is specific to any one credential — they apply to JWT, PAT, project API key and OAuth token
on the websocket equally:

- **No SSO liveness check.** `checkIfSsoUserStillValid` runs on every HTTP credential and on none of the websocket
  ones, so an SSO account deprovisioned at the IdP keeps a working socket.
- **A scoped-credential socket is registered under the account's own username.** The resolved authentication is
  mirrored into Spring's principal slot, and `TolgeeAuthentication.getName()` is the username, so
  `SimpUserRegistry` files a project-API-key or OAuth session indistinguishably from the user's browser session.
  Nothing reads it today — there are no `/user/**` destinations — but whoever adds them must gate on
  `ScopedCredential` there, or a `convertAndSendToUser` would reach the very session `isUserSubscribeAllowed`
  refuses the explicit `/users/{id}/` topic to.

