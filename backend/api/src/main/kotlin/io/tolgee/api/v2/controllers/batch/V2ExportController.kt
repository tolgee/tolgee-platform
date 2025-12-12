package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.ProjectLastModifiedManager
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
) {
  @GetMapping(value = [""])
  @Operation(
    summary = "Export data",
    description = """
      Exports project data in various formats (JSON, properties, YAML, etc.).
      
      ## HTTP Conditional Requests Support
      
      This endpoint supports HTTP conditional requests using the If-Modified-Since header:
      
      - **If-Modified-Since header provided**: The server checks if the project data has been modified since the specified date
      - **Data not modified**: Returns HTTP 304 Not Modified with empty body
      - **Data modified or no header**: Returns HTTP 200 OK with the exported data and Last-Modified header
      
      The Last-Modified header in the response contains the timestamp of the last project modification, 
      which can be used for subsequent conditional requests to avoid unnecessary data transfer when the 
      project hasn't changed.
      
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
    return projectLastModifiedManager.onlyWhenProjectDataChanged(request) { headersBuilder ->
      params.languages =
        languageService
          .getLanguagesForExport(params.languages, projectHolder.project.id, authenticationFacade.authenticatedUser.id)
          .toList()
          .map { language -> language.tag }
          .toSet()
      val exported = exportService.export(projectHolder.project.id, params)
      checkExportNotEmpty(exported)
      val responseEntity = getExportResponse(params, exported)
      headersBuilder.headers(responseEntity.headers)
      responseEntity.body
    }
  }

  @PostMapping(value = [""])
  @Operation(
    summary = "Export data (post)",
    description = """
      Exports project data in various formats (JSON, properties, YAML, etc.). 
      Useful when exceeding allowed URL size with GET requests.
      
      ## HTTP Conditional Requests Support
      
      This endpoint supports HTTP conditional requests using the If-Modified-Since header:
      
      - **If-Modified-Since header provided**: The server checks if the project data has been modified since the specified date
      - **Data not modified**: Returns HTTP 412 Precondition Failed with empty body (as per HTTP specification for POST requests)
      - **Data modified or no header**: Returns HTTP 200 OK with the exported data and Last-Modified header
      
      Note: Unlike GET requests which return 304 Not Modified, POST requests return 412 Precondition Failed 
      when the If-Modified-Since condition is not met, as POST is considered a modifying method according 
      to HTTP specifications.
      
      The Last-Modified header in the response contains the timestamp of the last project modification, 
      which can be used for subsequent conditional requests to avoid unnecessary data transfer when the 
      project hasn't changed.
      
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
  ): ResponseEntity<StreamingResponseBody> {
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
  ): ResponseEntity<StreamingResponseBody> {
    val (fileName, stream) = exported.entries.first()
    val fileNameWithoutSlash = fileName.replace("^/(.*)".toRegex(), "$1")
    val headers = getHeaders(fileNameWithoutSlash, params.format.mediaType)

    return ResponseEntity.ok().headers(headers).body(
      streamingResponseBodyProvider.createStreamingResponseBody { out: OutputStream ->
        IOUtils.copy(stream, out)
        stream.close()
        out.close()
      },
    )
  }

  private fun getZipResponseEntity(exported: Map<String, InputStream>): ResponseEntity<StreamingResponseBody> {
    val httpHeaders = getZipHeaders(projectHolder.project.name)

    return ResponseEntity.ok().headers(httpHeaders).body(
      streamingResponseBodyProvider.createStreamingResponseBody { out: OutputStream ->
        streamZipResponse(out, exported)
      },
    )
  }

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
