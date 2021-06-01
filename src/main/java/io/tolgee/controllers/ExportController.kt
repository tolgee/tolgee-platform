package io.tolgee.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.ApiScope
import io.tolgee.model.Permission
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationService
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/project/{projectId}/export", "/api/project/export")
@Tag(name = "Export")
class ExportController @Autowired constructor(private val translationService: TranslationService,
                                              private val securityService: SecurityService,
                                              private val languageService: LanguageService,
                                              private val projectHolder: ProjectHolder
) : IController {
    @GetMapping(value = ["/jsonZip"], produces = ["application/zip"])
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
    @AccessWithProjectPermission(Permission.ProjectPermissionType.VIEW)
    @Operation(summary = "Exports data as ZIP of jsons")
    fun doExportJsonZip(@PathVariable("projectId") projectId: Long?): ResponseEntity<StreamingResponseBody> {
        securityService.checkRepositoryPermission(projectHolder.project.id, Permission.ProjectPermissionType.VIEW)
        val languages = languageService.findAll(projectHolder.project.id)
        return ResponseEntity
                .ok()
                .header("Content-Disposition",
                        String.format("attachment; filename=\"%s.zip\"", projectHolder.project.name))
                .body(StreamingResponseBody { out: OutputStream ->
                    val zipOutputStream = ZipOutputStream(out)
                    val translations = translationService.getTranslations(languages.abbreviations,
                            projectHolder.project.id)
                    for ((key, value) in translations) {
                        zipOutputStream.putNextEntry(ZipEntry(String.format("%s.json", key)))
                        val data = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(value)
                        val byteArrayInputStream = ByteArrayInputStream(data)
                        IOUtils.copy(byteArrayInputStream, zipOutputStream)
                        byteArrayInputStream.close()
                        zipOutputStream.closeEntry()
                    }
                    zipOutputStream.close()
                })
    }
}
