package io.tolgee.component

import io.tolgee.security.ProjectHolder
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest
import java.util.concurrent.TimeUnit

/**
 * Component responsible for managing HTTP conditional requests based on project data modifications.
 *
 * This manager implements HTTP conditional request mechanisms (If-Modified-Since/Last-Modified headers
 * and If-None-Match/ETag headers) to enable efficient caching of project-related data. It helps reduce
 * unnecessary data transfer and processing by allowing clients to cache responses and only receive new
 * data when the project has actually been modified.
 */
@Component
class ProjectLastModifiedManager(
  private val projectTranslationLastModifiedManager: ProjectTranslationLastModifiedManager,
  private val projectHolder: ProjectHolder,
) {
  /**
   * Executes a function only when the project data has been modified since the client's last request.
   *
   * This method implements HTTP conditional request handling by:
   * 1. Retrieving the last modification timestamp and eTag of the current project
   * 2. Checking if the client's If-Modified-Since or If-None-Match headers indicate the data is still current
   * 3. If data hasn't changed, returning HTTP 304 Not Modified response with appropriate headers
   * 4. If data has changed, executing the provided function and wrapping the result in a ResponseEntity
   *    with appropriate cache control headers
   *
   * The response includes:
   * - Last-Modified header set to the project's modification timestamp
   * - ETag header set to the project's unique identifier
   * - Cache-Control header set to max-age=0 to ensure validation on each request
   *
   */
  fun <T> onlyWhenProjectDataChanged(
    request: WebRequest,
    fn: (
      /**
       * Enables setting of additional headers on the response.
       */
      headersBuilder: ResponseEntity.HeadersBuilder<*>,
    ) -> T?,
  ): ResponseEntity<T>? {
    val (lastModified, eTag) = projectTranslationLastModifiedManager.getLastModifiedInfo(projectHolder.project.id)

    // Custom conditional request logic that works for all HTTP methods (not just GET/HEAD)
    if (isNotModified(request, eTag, lastModified)) {
      // Return 304 Not Modified response with proper headers for all HTTP methods
      return ResponseEntity
        .status(304)
        .lastModified(lastModified)
        .eTag(eTag)
        .cacheControl(DEFAULT_CACHE_CONTROL_HEADER)
        .build()
    }

    val headersBuilder =
      ResponseEntity
        .ok()
        .lastModified(lastModified)
        .eTag(eTag)
        .cacheControl(DEFAULT_CACHE_CONTROL_HEADER)

    val response = fn(headersBuilder)

    return headersBuilder
      .body(
        response,
      )
  }

  /**
   * Custom implementation of conditional request checking that works for all HTTP methods.
   *
   * Unlike Spring's checkNotModified which only returns 304 for GET/HEAD methods,
   * this implementation returns true (indicating not modified) for any HTTP method
   * when the conditional headers indicate the client already has the current version.
   *
   * @param request The web request containing conditional headers
   * @param eTag The current ETag value for the resource
   * @param lastModified The last modification timestamp in milliseconds
   * @return true if the resource has not been modified, false otherwise
   */
  private fun isNotModified(
    request: WebRequest,
    eTag: String,
    lastModified: Long,
  ): Boolean {
    // Check If-None-Match header (ETag-based conditional)
    val ifNoneMatch = request.getHeader("If-None-Match")
    if (ifNoneMatch != null) {
      // If the ETag matches, the resource hasn't been modified
      return ifNoneMatch == eTag || ifNoneMatch == "\"$eTag\""
    }

    // Check If-Modified-Since header (timestamp-based conditional)
    val ifModifiedSince = request.getHeader("If-Modified-Since")
    if (ifModifiedSince != null) {
      try {
        val ifModifiedSinceDate =
          java.time.Instant
            .from(
              java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                .parse(ifModifiedSince),
            ).toEpochMilli()
        // If the resource wasn't modified after the client's timestamp, it hasn't been modified
        // There is the 1s precision, so we need to trim the milliseconds
        return lastModified / 1000 <= ifModifiedSinceDate / 1000
      } catch (e: Exception) {
        // If we can't parse the date, assume it has been modified
        return false
      }
    }

    // No conditional headers present, assume modified
    return false
  }

  companion object {
    val DEFAULT_CACHE_CONTROL_HEADER = CacheControl.maxAge(0, TimeUnit.SECONDS)
  }
}
