package io.tolgee.hateoas.autoTranslationConfig

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "configs", itemRelation = "config")
data class AutoTranslationConfigModel(
  val languageId: Long? = null,
  @Schema(
    description =
      "If true, new keys will be automatically translated via batch operation using translation memory" +
        " when 100% match is found",
  )
  var usingTranslationMemory: Boolean = false,
  @Schema(
    description =
      "If true, new keys will be automatically translated via batch operation" +
        "using primary machine translation service." +
        "" +
        "When \"usingTranslationMemory\" is enabled, it tries to translate it with translation memory first.",
  )
  var usingMachineTranslation: Boolean = false,
  @Schema(
    description =
      "If true, import will trigger batch operation to translate the new new keys." +
        "\n" +
        "It includes also the data imported via CLI, Figma, or other integrations using batch key import.",
  )
  var enableForImport: Boolean = false,
) : RepresentationModel<AutoTranslationConfigModel>()
