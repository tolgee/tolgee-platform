package io.tolgee.hateoas.machineTranslation

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "machineTranslationProviders", itemRelation = "machineTranslationProvider")
@Suppress("unused")
class MachineTranslationProviderModel(
  @Schema(
    description =
      "BCP 47 tags of languages supported by the translation service. " +
        "When null, all possible languages are supported. \n\n" +
        "Please note that Tolgee tries to fall back to a higher subtag if the subtag is not supported.\n\n" +
        "E.g., if `pt-BR` is not supported. Tolgee fallbacks to `pt`.",
  )
  val supportedLanguages: List<String>?,
)
