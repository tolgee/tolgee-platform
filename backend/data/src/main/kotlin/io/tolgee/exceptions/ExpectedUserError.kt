package io.tolgee.exceptions

/**
 * Marker interface for exceptions caused by expected user or external misconfiguration —
 * e.g. a user's webhook is unreachable, the user's Slack bot was removed from the channel.
 *
 * Such failures are normal operational conditions the user has to fix on their side, not
 * bugs in our code, so they must not be reported to Sentry. Code that decides whether to
 * send an event to Sentry should treat any throwable in the cause chain that implements
 * this interface as expected.
 */
interface ExpectedUserError
