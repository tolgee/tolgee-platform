package io.tolgee.formats.ios.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.getPluralFormsForLocale
import io.tolgee.formats.ios.APPLE_FILE_ORIGINAL_CUSTOM_KEY
import io.tolgee.formats.ios.APPLE_PLURAL_PROPERTY_KEY
import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
import io.tolgee.formats.xliff.out.XliffFileWriter
import io.tolgee.model.ILanguage
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class AppleXliffExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  baseTranslationsProvider: () -> List<ExportTranslationView>,
  val baseLanguage: ILanguage,
) : FileExporter {
  override val fileExtension: String = ExportFormat.XLIFF.extension

  private val baseTranslations by lazy {
    baseTranslationsProvider().associateBy { it.key.namespace to it.key.name }
  }

  /**
   * Path -> Xliff Model
   */
  val models = mutableMapOf<String, XliffModel>()

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return models.asSequence().map { (fileName, resultItem) ->
      fileName to XliffFileWriter(xliffModel = resultItem, enableHtml = true).produceFiles()
    }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val resultItem = getResultXliffFile(translation)
      addTranslation(resultItem, translation)
    }
  }

  private fun addTranslation(
    resultItem: XliffFile,
    translation: ExportTranslationView,
  ) {
    val targetConverted = translation.text?.let { IcuToIOsMessageConvertor(message = it).convert() }
    val baseConverted =
      baseTranslations[translation.key.namespace to translation.key.name]
        ?.text?.let { IcuToIOsMessageConvertor(it).convert() }

    if (targetConverted?.isPlural() == true || baseConverted?.isPlural() == true) {
      handlePlural(resultItem, translation, baseConverted, targetConverted)
      return
    }
    return handleSingle(resultItem, translation, baseConverted?.singleResult ?: "", targetConverted?.singleResult)
  }

  private fun handleSingle(
    resultItem: XliffFile,
    translation: ExportTranslationView,
    source: String,
    target: String?,
  ) {
    resultItem.transUnits.add(
      XliffTransUnit().apply {
        this.id = translation.key.name
        this.source = source
        this.target = target
      },
    )
  }

  private fun handlePlural(
    resultItem: XliffFile,
    targetTranslation: ExportTranslationView,
    source: PossiblePluralConversionResult?,
    target: PossiblePluralConversionResult?,
  ) {
    val fileType = resultItem.getFileType ?: FileType.STRINGSDICT

    val property = targetTranslation.key.custom?.get(APPLE_PLURAL_PROPERTY_KEY) as? String ?: "property"

    if (fileType == FileType.STRINGSDICT) {
      resultItem.transUnits.add(
        XliffTransUnit().apply {
          this.id = "/${targetTranslation.key.name}:dict/NSStringLocalizedFormatKey:dict/:string"
          this.source = "%#@$property@"
          this.target = "%#@$property@"
        },
      )
    }

    val sourceForms = populateForms(baseLanguage.tag, source)
    val targetForms = populateForms(targetTranslation.languageTag, target)

    val allFormKeywords = sourceForms.keys + targetForms.keys

    allFormKeywords.forEach { keyword ->
      resultItem.transUnits.add(
        XliffTransUnit().apply {
          this.id = getPluralTransUnitId(targetTranslation.key.name, property, keyword, fileType)
          this.source = sourceForms[keyword] ?: sourceForms["other"] ?: ""
          this.target = targetForms[keyword] ?: targetForms["other"]
        },
      )
    }
  }

  private fun getPluralTransUnitId(
    keyName: String,
    property: String,
    keyword: String,
    fileType: FileType,
  ): String {
    return when (fileType) {
      FileType.XCSTRINGS -> return "$keyName|==|plural.$keyword"
      FileType.STRINGSDICT -> "/$keyName:dict/$property:dict/$keyword:dict/:string"
    }
  }

  private fun populateForms(
    languageTag: String,
    conversionResult: PossiblePluralConversionResult?,
  ): Map<String, String> {
    if (conversionResult == null) {
      return emptyMap()
    }
    val otherForm = conversionResult.formsResult?.get("other") ?: conversionResult.singleResult ?: ""
    val allForms = getPluralFormsForLocale(languageTag)
    return allForms.associateWith { (conversionResult.formsResult?.get(it) ?: otherForm) }
  }

  private fun getResultXliffFile(translation: ExportTranslationView): XliffFile {
    val absolutePath = translation.getFilePath(translation.key.namespace)
    val xliffModel =
      models.computeIfAbsent(absolutePath) {
        XliffModel()
      }

    val targetFileOriginal = translation.key.custom?.get(APPLE_FILE_ORIGINAL_CUSTOM_KEY) as? String ?: ""

    val file =
      xliffModel.files.find { it.original == targetFileOriginal } ?: let {
        val file =
          XliffFile().apply {
            this.original = targetFileOriginal
            this.sourceLanguage = translation.languageTag
            this.targetLanguage = translation.languageTag
          }
        xliffModel.files.add(file)
        file
      }
    return file
  }

  private val XliffFile.getFileType: FileType?
    get() {
      if (this.original?.endsWith(".stringsdict") == true) {
        return FileType.STRINGSDICT
      } else if (this.original?.endsWith(".xcstrings") == true) {
        return FileType.XCSTRINGS
      }
      return null
    }

  enum class FileType {
    XCSTRINGS,
    STRINGSDICT,
  }
}
