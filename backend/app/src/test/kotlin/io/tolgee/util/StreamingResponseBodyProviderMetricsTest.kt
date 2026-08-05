package io.tolgee.util

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.Metrics
import io.tolgee.testing.assert
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.Session
import org.hibernate.jdbc.Work
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.sql.Connection

class StreamingResponseBodyProviderMetricsTest {
  private val meterRegistry = SimpleMeterRegistry()
  private val metrics = Metrics(meterRegistry)
  private val provider = StreamingResponseBodyProvider(entityManager(), ObjectMapper(), metrics)

  @Test
  fun `times a completed stream under its own stream type`() {
    provider
      .createStreamingResponseBody(StreamType.EXPORT_ZIP) { it.write(1) }
      .writeTo(ByteArrayOutputStream())

    timerCount(StreamType.EXPORT_ZIP).assert.isEqualTo(1L)
    timerCount(StreamType.MT_SUGGEST).assert.isEqualTo(0L)
  }

  /**
   * A stream that dies holding its connection is exactly what this metric exists to expose, so the
   * sample has to be recorded from the finally rather than after a successful body.
   */
  @Test
  fun `times a stream that fails part-way through`() {
    val body =
      provider.createStreamingResponseBody(StreamType.IMPORT_APPLY) {
        throw IllegalStateException("upstream died")
      }

    assertThatThrownBy { body.writeTo(ByteArrayOutputStream()) }
      .isInstanceOf(IllegalStateException::class.java)

    timerCount(StreamType.IMPORT_APPLY).assert.isEqualTo(1L)
  }

  @Test
  fun `times an ndjson stream too`() {
    provider
      .streamNdJson(StreamType.MT_SUGGEST) { write -> write("hello") }
      .body!!
      .writeTo(ByteArrayOutputStream())

    timerCount(StreamType.MT_SUGGEST).assert.isEqualTo(1L)
  }

  private fun timerCount(streamType: StreamType): Long =
    meterRegistry
      .find("tolgee.async.streaming.duration")
      .tag("stream_type", streamType.tag)
      .timer()
      ?.count() ?: 0L

  private fun entityManager(): EntityManager {
    val connection = Mockito.mock(Connection::class.java)
    val session = Mockito.mock(Session::class.java)
    whenever(session.doWork(any())) doAnswer { invocation ->
      invocation.getArgument<Work>(0).execute(connection)
    }
    val entityManager = Mockito.mock(EntityManager::class.java)
    whenever(entityManager.unwrap(Session::class.java)) doAnswer { session }
    return entityManager
  }
}
