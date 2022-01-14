package io.tolgee.api.v2.controllers.batch

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.export.ExportParams
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
    val httpHeaders = getHeaders(projectHolder.project.name)

    return ResponseEntity.ok().headers(httpHeaders).body(
      StreamingResponseBody { out: OutputStream ->
        val zipOutputStream = ZipOutputStream(out)

        exportService.export(projectId, params).forEach { (fileAbsolutePath, stream) ->
          zipOutputStream.putNextEntry(ZipEntry(fileAbsolutePath))
          IOUtils.copy(stream, zipOutputStream)
          stream.close()
          zipOutputStream.closeEntry()
        }

        zipOutputStream.close()
      }
    )
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

  private fun getHeaders(projectName: String): HttpHeaders {
    val httpHeaders = HttpHeaders()
    httpHeaders.contentType = MediaType.valueOf("application/zip")
    httpHeaders.contentDisposition = ContentDisposition.parse(
      """attachment; filename="$projectName.zip"""",
    )
    return httpHeaders
  }
}
