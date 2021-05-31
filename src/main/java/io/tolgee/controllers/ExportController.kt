package io.tolgee.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.ApiScope
import io.tolgee.model.Permission
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.repository_auth.AccessWithRepositoryPermission
import io.tolgee.security.repository_auth.RepositoryHolder
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
@RequestMapping("/api/repository/{repositoryId}/export", "/api/repository/export")
@Tag(name = "Export")
class ExportController @Autowired constructor(private val translationService: TranslationService,
                                              private val securityService: SecurityService,
                                              private val languageService: LanguageService,
                                              private val repositoryHolder: RepositoryHolder
) : IController {
    @GetMapping(value = ["/jsonZip"], produces = ["application/zip"])
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
    @AccessWithRepositoryPermission(Permission.ProjectPermissionType.VIEW)
    @Operation(summary = "Exports data as ZIP of jsons")
    fun doExportJsonZip(@PathVariable("repositoryId") repositoryId: Long?): ResponseEntity<StreamingResponseBody> {
        securityService.checkRepositoryPermission(repositoryHolder.project.id, Permission.ProjectPermissionType.VIEW)
        val languages = languageService.findAll(repositoryHolder.project.id)
        return ResponseEntity
                .ok()
                .header("Content-Disposition",
                        String.format("attachment; filename=\"%s.zip\"", repositoryHolder.project.name))
                .body(StreamingResponseBody { out: OutputStream ->
                    val zipOutputStream = ZipOutputStream(out)
                    val translations = translationService.getTranslations(languages.abbreviations,
                            repositoryHolder.project.id)
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
