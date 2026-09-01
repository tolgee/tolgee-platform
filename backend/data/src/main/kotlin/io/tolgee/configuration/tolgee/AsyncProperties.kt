package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.async")
@DocProperty(
  displayName = "Asynchronous execution",
  description =
    "Sizing of the thread pools Tolgee uses for work that runs outside the HTTP request thread.\n\n" +
      "By default both are derived from the size of the database connection pool, so a larger " +
      "instance automatically gets more concurrency without any extra configuration.\n\n" +
      ":::warning\n" +
      "Every in-flight streaming response holds one database connection for its entire duration. " +
      "That is why `streaming.max-threads` is derived from, and must stay well below, your database " +
      "connection pool size — together with `tolgee.batch.concurrency` and the connections ordinary " +
      "requests need.\n" +
      ":::",
)
class AsyncProperties {
  var streaming: StreamingAsyncProperties = StreamingAsyncProperties()
  var background: BackgroundAsyncProperties = BackgroundAsyncProperties()
}

@ConfigurationProperties(prefix = "tolgee.async.streaming")
@DocProperty(
  displayName = "Streaming responses",
  description =
    "Thread pool serving streaming HTTP responses: project and glossary export, import progress " +
      "streaming and machine-translation suggestion streaming.",
)
class StreamingAsyncProperties {
  @DocProperty(
    description =
      "How many streaming responses this instance can produce at the same time.\n\n" +
        "Each one occupies one thread **and one database connection** until the response is " +
        "finished, so this must stay well below your database connection pool size.\n\n" +
        "There is no way to switch streaming off: `0` and any negative value mean *derive it*, " +
        "not *disable it*.",
    defaultValue = "-1",
    defaultExplanation = "A third of the database connection pool size, never less than 2",
  )
  var maxThreads: Int = -1

  @DocProperty(
    description =
      "How many streaming requests may wait for a free thread before Tolgee replies " +
        "`503 Service Unavailable`.\n\n" +
        "A queued request already counts against `spring.mvc.async.request-timeout`, so the queue " +
        "cannot make a request wait longer than that — it absorbs bursts while threads turn over, " +
        "and requests that still cannot be served in time are answered rather than left hanging.",
    defaultValue = "-1",
    defaultExplanation = "50, or max-threads if that is larger",
  )
  var queueCapacity: Int = -1

  @DocProperty(
    description = "How long an idle streaming thread is kept alive before it is released, in seconds.",
    defaultExplanation = "1 minute",
  )
  var keepAliveSeconds: Int = 60
}

@ConfigurationProperties(prefix = "tolgee.async.background")
@DocProperty(
  displayName = "Background tasks",
  description =
    "Thread pool serving background (`@Async`) work: e-mail sending, analytics reporting, " +
      "translation statistics recomputation and project hard-deletes. Websocket activity broadcasts " +
      "and automation triggers run on their own single-threaded pools, so their ordering and " +
      "debouncing are preserved, and are not affected by this setting.\n\n" +
      "Its queue is unbounded — background work is queued, never dropped and never run on the " +
      "thread that submitted it.",
)
class BackgroundAsyncProperties {
  @DocProperty(
    description =
      "How many background tasks Tolgee runs in parallel on this instance.\n\n" +
        "There is no way to switch background processing off: `0` and any negative value mean " +
        "*derive it*, not *disable it*.",
    defaultValue = "-1",
    defaultExplanation = "A sixth of the database connection pool size, never less than 2",
  )
  var maxThreads: Int = -1

  @DocProperty(
    description = "How long an idle background thread is kept alive before it is released, in seconds.",
    defaultExplanation = "1 minute",
  )
  var keepAliveSeconds: Int = 60
}
