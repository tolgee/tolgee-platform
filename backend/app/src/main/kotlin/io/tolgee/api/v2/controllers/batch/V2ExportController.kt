package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Permission
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.export.ExportService
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springdoc.api.annotations.ParameterObject
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}"])
@Tag(name = "Export controller")
@Suppress("SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection")
class V2ExportController(
  private val exportService: ExportService,
  private val projectHolder: ProjectHolder
) {
  @GetMapping(value = ["/export"])
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.VIEW)
  @Operation(summary = "Exports data")
  fun export(
    @PathVariable projectId: Long,
    @ParameterObject params: ExportParams
  ): ResponseEntity<StreamingResponseBody> {
    val exported = exportService.export(projectId, params)
    checkExportNotEmpty(exported)
    return getExportResponse(params, exported)
  }

  @PostMapping(value = ["/export"])
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.VIEW)
  @Operation(
    summary = """Exports data (post). Useful when providing params exceeding allowed query size.
  """
  )
  fun exportPost(
    @PathVariable projectId: Long,
    @RequestBody params: ExportParams
  ): ResponseEntity<StreamingResponseBody> {
    return export(projectId, params)
  }

  private fun getZipHeaders(projectName: String): HttpHeaders {
    return getHeaders("$projectName.zip", "application/zip")
  }

  private fun getHeaders(fileName: String, mediaType: String): HttpHeaders {
    val httpHeaders = HttpHeaders()
    httpHeaders.contentType = MediaType.valueOf(mediaType)
    httpHeaders.contentDisposition = ContentDisposition.parse(
      """attachment; filename="$fileName"""",
    )
    return httpHeaders
  }

  private fun getExportResponse(
    params: ExportParams,
    exported: Map<String, InputStream>
  ): ResponseEntity<StreamingResponseBody> {
    if (params.zip) {
      return getZipResponseEntity(exported)
    } else if (exported.entries.size == 1) {
      return exportSingleFile(exported, params)
    }
    throw BadRequestException(message = Message.MULTIPLE_FILES_MUST_BE_ZIPPED)
  }

  private fun checkExportNotEmpty(exported: Map<String, InputStream>) {
    if (exported.entries.isEmpty()) {
      throw BadRequestException(message = Message.NO_EXPORTED_RESULT)
    }
  }

  private fun exportSingleFile(
    exported: Map<String, InputStream>,
    params: ExportParams
  ): ResponseEntity<StreamingResponseBody> {
    val (fileName, stream) = exported.entries.first()
    val fileNameWithoutSlash = fileName.replace("^/(.*)".toRegex(), "$1")
    val headers = getHeaders(fileNameWithoutSlash, params.format.mediaType)

    return ResponseEntity.ok().headers(headers).body(
      StreamingResponseBody { out: OutputStream ->
        IOUtils.copy(stream, out)
        stream.close()
        out.close()
      }
    )
  }

  private fun getZipResponseEntity(exported: Map<String, InputStream>): ResponseEntity<StreamingResponseBody> {
    val httpHeaders = getZipHeaders(projectHolder.project.name)

    return ResponseEntity.ok().headers(httpHeaders).body(
      StreamingResponseBody { out: OutputStream ->
        streamZipResponse(out, exported)
      }
    )
  }

  private fun streamZipResponse(
    out: OutputStream,
    exported: Map<String, InputStream>
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
