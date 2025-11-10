package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.service.glossary.GlossaryExportService
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.util.StreamingResponseBodyProvider
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/glossaries/{glossaryId:[0-9]+}")
@Tag(name = "Glossary Export")
class GlossaryExportController(
  private val glossaryService: GlossaryService,
  private val glossaryExportService: GlossaryExportService,
  private val organizationHolder: OrganizationHolder,
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
) {
  @GetMapping("/export")
  @Operation(summary = "Export glossary terms as CSV")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @RequiresFeatures(Feature.GLOSSARY)
  fun export(
    @PathVariable organizationId: Long,
    @PathVariable glossaryId: Long,
  ): ResponseEntity<StreamingResponseBody> {
    val organization = organizationHolder.organization
    val glossary = glossaryService.get(organization.id, glossaryId)

    val headers =
      HttpHeaders().apply {
        contentType = MediaType.parseMediaType("text/csv;charset=UTF-8")
        accessControlExposeHeaders = listOf("Content-Disposition")
        contentDisposition =
          ContentDisposition
            .attachment()
            .filename("glossary-${glossary.name}.csv")
            .build()
      }

    val stream = glossaryExportService.exportCsv(glossary)

    return ResponseEntity.ok().headers(headers).body(
      streamingResponseBodyProvider.createStreamingResponseBody { out: OutputStream ->
        stream.use { IOUtils.copy(stream, out) }
        out.close()
      },
    )
  }
}
