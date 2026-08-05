package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.util.StreamType
import io.tolgee.util.StreamingResponseBodyProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

/**
 * Exists so the streaming backpressure path can be exercised over real HTTP. Every production
 * streaming endpoint needs an authenticated project, which makes it impractical to assert what a
 * client actually receives when the streaming pool is saturated.
 */
@InternalController(["internal/streaming"])
class StreamingBackpressureTestController(
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
) {
  @GetMapping(value = ["/stream"])
  @Operation(description = "Streams a fixed body through the same provider the real endpoints use")
  fun stream(): ResponseEntity<StreamingResponseBody> =
    ResponseEntity.ok(
      streamingResponseBodyProvider.createStreamingResponseBody(StreamType.INTERNAL_TEST) { outputStream ->
        outputStream.write("streamed".toByteArray())
      },
    )
}
