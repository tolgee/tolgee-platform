package io.tolgee.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.security.PermissionService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.StreamingResponseBodyProvider
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  "/api/project/{projectId:[0-9]+}/export",
  "/api/project/export",
  "/api/repository/{projectId:[0-9]+}/export",
  "/api/repository/export",
)
@Tag(name = "Export")
class ExportController(
  private val translationService: TranslationService,
  private val permissionService: PermissionService,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
) : IController {
  @GetMapping(value = ["/jsonZip"], produces = ["application/zip"])
  @Operation(summary = "Export to ZIP of jsons", description = "Exports data as ZIP of jsons", deprecated = true)
  @RequiresProjectPermissions([ Scope.TRANSLATIONS_VIEW ])
  @AllowApiAccess
  @Deprecated("Use v2 export controller")
  fun doExportJsonZip(
    @PathVariable("projectId") projectId: Long?,
  ): ResponseEntity<StreamingResponseBody> {
    val allLanguages =
      permissionService.getPermittedViewLanguages(
        projectHolder.project.id,
        authenticationFacade.authenticatedUser.id,
      )

    return ResponseEntity
      .ok()
      .header(
        "Content-Disposition",
        String.format("attachment; filename=\"%s.zip\"", projectHolder.project.name),
      )
      .body(
        streamingResponseBodyProvider.createStreamingResponseBody { out: OutputStream ->
          val zipOutputStream = ZipOutputStream(out)
          val translations =
            translationService.getTranslations(
              allLanguages.map { it.tag }.toSet(),
              null,
              projectHolder.project.id,
              '.',
            )
          for ((key, value) in translations) {
            zipOutputStream.putNextEntry(ZipEntry(String.format("%s.json", key)))
            val data = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(value)
            val byteArrayInputStream = ByteArrayInputStream(data)
            IOUtils.copy(byteArrayInputStream, zipOutputStream)
            byteArrayInputStream.close()
            zipOutputStream.closeEntry()
          }
          zipOutputStream.close()
        },
      )
  }
}
