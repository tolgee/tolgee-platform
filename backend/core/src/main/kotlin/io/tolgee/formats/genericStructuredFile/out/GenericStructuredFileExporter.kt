package io.tolgee.formats.genericStructuredFile.out

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.generic.IcuToGenericFormatMessageConvertor
import io.tolgee.formats.nestedStructureModel.StructureModelBuilder
import io.tolgee.formats.path.ObjectPathItem
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class GenericStructuredFileExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  private val projectIcuPlaceholdersSupport: Boolean,
  private val objectMapper: ObjectMapper,
  private val rootKeyIsLanguageTag: Boolean = false,
  private val supportArrays: Boolean,
  private val messageFormat: ExportMessageFormat,
  private val customPrettyPrinter: CustomPrettyPrinter,
  private val filePathProvider: ExportFilePathProvider,
) : FileExporter {
  val result: LinkedHashMap<String, StructureModelBuilder> = LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return result
      .asSequence()
      .map { (fileName, modelBuilder) ->
        fileName to
          objectMapper
            .writer(customPrettyPrinter)
            .writeValueAsBytes(modelBuilder.result)
            .inputStream()
      }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      addTranslationToBuilder(translation)
    }
  }

  private fun addTranslationToBuilder(translation: ExportTranslationView) {
    if (translation.key.isPlural) {
      addPluralTranslation(translation)
      return
    }
    addSingularTranslation(translation)
  }

  private fun addSingularTranslation(translation: ExportTranslationView) {
    val builder = getFileContentResultBuilder(translation)
    builder.addValue(
      translation.languageTag,
      translation.key.name,
      convertMessage(translation.text, translation.key.isPlural),
    )
  }

  private val pluralsViaSuffixes
    get() = messageFormat == ExportMessageFormat.I18NEXT

  private val pluralsViaNestingForApple
    get() = exportParams.format == ExportFormat.APPLE_SDK

  private val pluralsViaNesting
    get() = !pluralsViaSuffixes && !pluralsViaNestingForApple && messageFormat != ExportMessageFormat.ICU

  private val placeholderConvertorFactory
    get() = messageFormat.paramConvertorFactory

  private fun addPluralTranslation(translation: ExportTranslationView) {
    if (pluralsViaNesting) {
      return addNestedPlural(translation)
    }
    if (pluralsViaNestingForApple) {
      return addNestedPlural(translation, appleStructure = true)
    }
    if (pluralsViaSuffixes) {
      return addSuffixedPlural(translation)
    }
    return addSingularTranslation(translation)
  }

  private fun addNestedPlural(
    translation: ExportTranslationView,
    appleStructure: Boolean = false,
  ) {
    val pluralForms =
      convertMessageForNestedPlural(translation.text) ?: let {
        // this should never happen, but if it does, it's better to add a null key then crash or ignore it
        addNullValue(translation)
        return
      }

    val nestedInside =
      if (appleStructure) {
        listOf(ObjectPathItem("variations", "variations"), ObjectPathItem("plural", "plural"))
      } else {
        emptyList()
      }

    val builder = getFileContentResultBuilder(translation)
    builder.addValue(
      translation.languageTag,
      translation.key.name,
      pluralForms,
      nestInside = nestedInside,
    )
  }

  private fun addSuffixedPlural(translation: ExportTranslationView) {
    val pluralForms =
      convertMessageForNestedPlural(translation.text) ?: let {
        // this should never happen, but if it does, it's better to add a null key then crash or ignore it
        addNullValue(translation)
        return
      }

    val builder = getFileContentResultBuilder(translation)
    pluralForms.forEach { (keyword, form) ->
      builder.addValue(
        translation.languageTag,
        "${translation.key.name}_$keyword",
        form,
      )
    }
  }

  private fun addNullValue(translation: ExportTranslationView) {
    val builder = getFileContentResultBuilder(translation)
    builder.addValue(
      translation.languageTag,
      translation.key.name,
      null,
    )
  }

  private fun convertMessage(
    text: String?,
    isPlural: Boolean,
  ): String? {
    return getMessageConvertor(text, isPlural).convert()
  }

  private fun getMessageConvertor(
    text: String?,
    isPlural: Boolean,
  ) = IcuToGenericFormatMessageConvertor(
    text,
    isPlural,
    isProjectIcuPlaceholdersEnabled = projectIcuPlaceholdersSupport,
    paramConvertorFactory = placeholderConvertorFactory,
  )

  private fun convertMessageForNestedPlural(text: String?): Map<String, String>? {
    return getMessageConvertor(text, true).getForcedPluralForms()
  }

  private fun getFileContentResultBuilder(translation: ExportTranslationView): StructureModelBuilder {
    val absolutePath = pathProvider.getFilePath(translation)
    return result.computeIfAbsent(absolutePath) {
      StructureModelBuilder(
        structureDelimiter = exportParams.structureDelimiter,
        supportArrays = supportArrays,
        rootKeyIsLanguageTag = rootKeyIsLanguageTag,
      )
    }
  }

  private val pathProvider = filePathProvider
}
