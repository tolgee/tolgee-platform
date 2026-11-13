package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/keys/{keyId:[0-9]+}/auto-translate",
    "/v2/projects/keys/{keyId:[0-9]+}/auto-translate",
  ],
)
@Tag(name = "Auto Translation")
@Suppress("MVCPathVariableInspection")
class AutoTranslationController(
  private val autoTranslationFacade: AutoTranslationFacade,
) {
  @PutMapping("")
  @Operation(
    summary = "Auto translates keys",
    description = """Uses enabled auto-translation methods.
You need to set at least one of useMachineTranslation or useTranslationMemory to true.

When languages are specified, their existing translations are replaced with the result obtained from the specified source.
Without languages, only untranslated languages the caller may translate are filled.
    """,
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  fun autoTranslate(
    @PathVariable keyId: Long,
    @Parameter(
      description = """Tags of languages to auto-translate.
When no languages provided, it translates only untranslated languages.""",
    )
    @RequestParam
    languages: Set<String>?,
    @RequestParam useMachineTranslation: Boolean?,
    @RequestParam useTranslationMemory: Boolean?,
  ) {
    autoTranslationFacade.autoTranslate(keyId, languages, useMachineTranslation, useTranslationMemory)
  }
}
