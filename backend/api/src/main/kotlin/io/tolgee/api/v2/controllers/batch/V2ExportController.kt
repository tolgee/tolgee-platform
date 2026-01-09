package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.ProjectLastModifiedManager
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.service.export.ExportService
import io.tolgee.service.language.LanguageService
import io.tolgee.util.StreamingResponseBodyProvider
import io.tolgee.util.nullIfEmpty
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/export", "/v2/projects/export"])
@Tag(name = "Export")
@Suppress("MVCPathVariableInspection")
@OpenApiOrderExtension(4)
class V2ExportController(
  private val exportService: ExportService,
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
  private val authenticationFacade: AuthenticationFacade,
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
  private val projectLastModifiedManager: ProjectLastModifiedManager,
  private val rateLimitService: RateLimitService,
  private val tolgeeProperties: TolgeeProperties,
) {
  @GetMapping(value = [""])
  @Operation(
    summary = "Export data",
    description = """
      Exports project data in various formats (JSON, properties, YAML, etc.).
      
      ## HTTP Conditional Requests Support
      
      This endpoint supports HTTP conditional requests using both If-Modified-Since and If-None-Match headers:
      
      - **If-Modified-Since header provided**: The server checks if the project data has been modified since the specified date
      - **If-None-Match header provided**: The server checks if the project data has changed by comparing the eTag value
      - **Data not modified**: Returns HTTP 304 Not Modified with empty body
      - **Data modified or no header**: Returns HTTP 200 OK with the exported data, Last-Modified header, and ETag header
      
      The Last-Modified header in the response contains the timestamp of the last project modification,
      and the ETag header contains a unique identifier for the current project state. Both can be used 
      for subsequent conditional requests to avoid unnecessary data transfer when the project hasn't changed.
      
      Cache-Control header is set to max-age=0 to ensure validation on each request.
    """,
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @ExportApiResponse
  fun exportData(
    @ParameterObject params: ExportParams,
    request: WebRequest,
  ): ResponseEntity<StreamingResponseBody>? {
    rateLimitService.checkPerUserRateLimit(
      "export",
      limit = tolgeeProperties.rateLimit.exportRequestLimit,
      refillDuration = Duration.ofMillis(tolgeeProperties.rateLimit.exportRequestWindow),
    )
    return projectLastModifiedManager.onlyWhenProjectDataChanged(request) { headersBuilder ->
      params.languages =
        languageService
          .getLanguagesForExport(params.languages, projectHolder.project.id, authenticationFacade.authenticatedUser.id)
          .toList()
          .map { language -> language.tag }
          .toSet()
      val exported = exportService.export(projectHolder.project.id, params)
      checkExportNotEmpty(exported)
      val preparedResponse = getExportResponse(params, exported)
      headersBuilder.headers(preparedResponse.headers)
      preparedResponse.body
    }
  }

  @PostMapping(value = [""])
  @Operation(
    summary = "Export data (post)",
    description = """
      Exports project data in various formats (JSON, properties, YAML, etc.). 
      Useful when exceeding allowed URL size with GET requests.
      
      ## HTTP Conditional Requests Support
      
      This endpoint supports HTTP conditional requests using both If-Modified-Since and If-None-Match headers:
      
      - **If-Modified-Since header provided**: The server checks if the project data has been modified since the specified date
      - **If-None-Match header provided**: The server checks if the project data has changed by comparing the eTag value
      - **Data not modified**: Returns HTTP 304 Not Modified with empty body
      - **Data modified or no header**: Returns HTTP 200 OK with the exported data, Last-Modified header, and ETag header
      
      Note: This endpoint uses a custom implementation that returns 304 Not Modified for all HTTP methods
      (including POST) when conditional headers indicate the data hasn't changed. This differs from Spring's
      default behavior which returns 412 for POST requests, but is appropriate here since POST is used only
      to accommodate large request parameters, not to modify data.
      
      The Last-Modified header in the response contains the timestamp of the last project modification,
      and the ETag header contains a unique identifier for the current project state. Both can be used 
      for subsequent conditional requests to avoid unnecessary data transfer when the project hasn't changed.
      
      Cache-Control header is set to max-age=0 to ensure validation on each request.
    """,
  )
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @ExportApiResponse
  fun exportPost(
    @RequestBody params: ExportParams,
    request: WebRequest,
  ): ResponseEntity<StreamingResponseBody>? {
    return exportData(params, request)
  }

  private fun getZipHeaders(projectName: String): HttpHeaders {
    return getHeaders("$projectName.zip", "application/zip")
  }

  private fun getHeaders(
    fileName: String,
    mediaType: String,
  ): HttpHeaders {
    val httpHeaders = HttpHeaders()
    mediaType.nullIfEmpty?.let {
      httpHeaders.contentType = MediaType.valueOf(it)
    }
    httpHeaders.accessControlExposeHeaders = listOf("Content-Disposition")
    httpHeaders.contentDisposition =
      ContentDisposition.parse(
        """attachment; filename="$fileName"""",
      )
    return httpHeaders
  }

  private fun getExportResponse(
    params: ExportParams,
    exported: Map<String, InputStream>,
  ): PreparedResponse {
    if (exported.entries.size == 1 && !params.zip) {
      return exportSingleFile(exported, params)
    }
    return getZipResponseEntity(exported)
  }

  private fun checkExportNotEmpty(exported: Map<String, InputStream>) {
    if (exported.entries.isEmpty()) {
      throw BadRequestException(message = Message.NO_EXPORTED_RESULT)
    }
  }

  private fun exportSingleFile(
    exported: Map<String, InputStream>,
    params: ExportParams,
  ): PreparedResponse {
    val (fileName, stream) = exported.entries.first()
    val fileNameWithoutSlash = fileName.replace("^/(.*)".toRegex(), "$1")
    val headers = getHeaders(fileNameWithoutSlash, params.format.mediaType)

    return PreparedResponse(
      headers = headers,
      body =
        streamingResponseBodyProvider.createStreamingResponseBody { out: OutputStream ->
          IOUtils.copy(stream, out)
          stream.close()
          out.close()
        },
    )
  }

  private fun getZipResponseEntity(exported: Map<String, InputStream>): PreparedResponse {
    val httpHeaders = getZipHeaders(projectHolder.project.name)

    return PreparedResponse(
      headers = httpHeaders,
      body =
        streamingResponseBodyProvider.createStreamingResponseBody { out: OutputStream ->
          streamZipResponse(out, exported)
        },
    )
  }

  data class PreparedResponse(
    val headers: HttpHeaders,
    val body: StreamingResponseBody?,
  )

  private fun streamZipResponse(
    out: OutputStream,
    exported: Map<String, InputStream>,
  ) {
    val zipOutputStream = ZipOutputStream(out)

    exported.forEach { (fileAbsolutePath, stream) ->
      zipOutputStream.putNextEntry(ZipEntry(fileAbsolutePath))
      IOUtils.copy(stream, zipOutputStream)
      stream.close()
      zipOutputStream.closeEntry()
    }

    zipOutputStream.close()
  }
}
