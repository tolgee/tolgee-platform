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
 * This manager implements the HTTP conditional request mechanism (If-Modified-Since/Last-Modified headers)
 * to enable efficient caching of project-related data. It helps reduce unnecessary data transfer and
 * processing by allowing clients to cache responses and only receive new data when the project has
 * actually been modified.
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
   * 1. Retrieving the last modification timestamp of the current project
   * 2. Checking if the client's If-Modified-Since header indicates the data is still current
   * 3. If data hasn't changed, returning null (which translates to HTTP 304 Not Modified)
   * 4. If data has changed, executing the provided function and wrapping the result in a ResponseEntity
   *    with appropriate cache control headers
   *
   * The response includes:
   * - Last-Modified header set to the project's modification timestamp
   * - Cache-Control header set to max-age=0 to ensure validation on each request
   *
   * This mechanism helps optimize performance by preventing export data computation and loading from database when
   * not modified.
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
    val lastModified: Long = projectTranslationLastModifiedManager.getLastModified(projectHolder.project.id)

    if (request.checkNotModified(lastModified)) {
      return null
    }

    val headersBuilder =
      ResponseEntity
        .ok()
        .lastModified(lastModified)
        .cacheControl(DEFAULT_CACHE_CONTROL_HEADER)

    val response = fn(headersBuilder)

    return headersBuilder
      .body(
        response,
      )
  }

  companion object {
    val DEFAULT_CACHE_CONTROL_HEADER = CacheControl.maxAge(0, TimeUnit.SECONDS)
  }
}
