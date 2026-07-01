package io.tolgee.api.v2.controllers.administration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.openApiDocs.OpenApiSelfHostedExtension
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.service.projectExportImport.ProjectExportImportExporter
import io.tolgee.service.projectExportImport.ProjectExportImportImporter
import io.tolgee.util.StreamingResponseBodyProvider
import io.tolgee.util.VersionProvider
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.file.Files

@Suppress("SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/administration/projects/{projectId:\\d+}"])
@Tag(
  name = "Server Administration",
  description = "**Only for self-hosted instances** \n\nExport and import a whole project as a zip.",
)
@OpenApiSelfHostedExtension
class ProjectExportImportController(
  private val projectExportImportExporter: ProjectExportImportExporter,
  private val projectExportImportImporter: ProjectExportImportImporter,
  private val versionProvider: VersionProvider,
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
  private val authenticationFacade: AuthenticationFacade,
) : IController {
  @GetMapping(value = ["/export"])
  @Operation(
    summary = "Export a project",
    description =
      "Exports the whole project (content, branches, tasks, screenshots, settings) as a " +
        "self-contained zip that can be imported onto a project on another instance running the same " +
        "Tolgee version.",
  )
  @RequiresSuperAuthentication
  fun exportProject(
    @PathVariable projectId: Long,
  ): ResponseEntity<StreamingResponseBody> {
    val export = projectExportImportExporter.exportToTempFile(projectId, versionProvider.version)
    val tempFile = export.path
    val body =
      streamingResponseBodyProvider.createStreamingResponseBody { outputStream ->
        try {
          Files.copy(tempFile, outputStream)
        } finally {
          Files.deleteIfExists(tempFile)
        }
      }
    return ResponseEntity.ok().headers(zipHeaders(export.projectName)).body(body)
  }

  @PostMapping(value = ["/import"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(
    summary = "Import a project (destructive)",
    description =
      "Wipes ALL in-scope content of this project and replaces it with the uploaded export zip " +
        "(mirror / wipe-and-replace). The zip must come from an instance running the same Tolgee " +
        "version. Users are matched by username/email; content authored by a user not present on this " +
        "instance is attributed to the importing admin. Setting `ignoreVersion` bypasses the version " +
        "check — unsupported: a cross-version import may complete yet silently corrupt the project's data.",
  )
  @RequiresSuperAuthentication
  fun importProject(
    @PathVariable projectId: Long,
    @RequestParam("file") file: MultipartFile,
    @Parameter(
      description =
        "Bypass the schema-version check. Unsupported; intended only for cross-version admin recovery — " +
          "the import may complete yet silently corrupt data.",
    )
    @RequestParam("ignoreVersion", defaultValue = "false")
    ignoreVersion: Boolean = false,
  ) {
    file.inputStream.use { stream ->
      projectExportImportImporter.import(
        input = stream,
        targetProjectId = projectId,
        importingAdminId = authenticationFacade.authenticatedUser.id,
        runningVersion = versionProvider.version,
        ignoreVersion = ignoreVersion,
      )
    }
  }

  private fun zipHeaders(projectName: String): HttpHeaders {
    val headers = HttpHeaders()
    headers.contentType = MediaType.parseMediaType("application/zip")
    headers.accessControlExposeHeaders = listOf(HttpHeaders.CONTENT_DISPOSITION)
    headers.contentDisposition =
      ContentDisposition.attachment().filename("${safeFileName(projectName)}.zip").build()
    return headers
  }

  private fun safeFileName(projectName: String): String {
    val sanitized = projectName.replace(Regex("[^A-Za-z0-9-_. ]"), "_").trim()
    return sanitized.ifEmpty { "project" }
  }
}
