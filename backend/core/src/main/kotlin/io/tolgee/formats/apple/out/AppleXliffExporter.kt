package io.tolgee.formats.apple.out

import io.tolgee.formats.ExportFormat
import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.apple.APPLE_CORRESPONDING_STRINGS_FILE_ORIGINAL
import io.tolgee.formats.apple.APPLE_FILE_ORIGINAL_CUSTOM_KEY
import io.tolgee.formats.apple.APPLE_PLURAL_PROPERTY_CUSTOM_KEY
import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
import io.tolgee.formats.xliff.out.XliffFileWriter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class AppleXliffExporter(
  private val translations: List<ExportTranslationView>,
  baseTranslationsProvider: () -> List<ExportTranslationView>,
  private val baseLanguageTag: String,
  private val isProjectIcuPlaceholdersEnabled: Boolean = true,
  private val filePathProvider: ExportFilePathProvider,
) : FileExporter {
  private val fileExtension: String = ExportFormat.XLIFF.extension

  private val baseTranslations by lazy {
    baseTranslationsProvider().associateBy { it.key.namespace to it.key.name }
  }

  /**
   * Map (keyName -> Map (form -> value) )
   */
  private val convertedSources = mutableMapOf<String, PossiblePluralConversionResult?>()

  /**
   * Map (keyName -> Map (form -> value) )
   */
  private lateinit var allLanguages: Set<String>

  /**
   * Path -> Xliff Model
   */
  val models = mutableMapOf<String, XliffModel>()

  private val allStringsDict = mutableMapOf<String, KeyInStringsDict>()

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return models
      .asSequence()
      .map { (fileName, resultItem) ->
        fileName to XliffFileWriter(xliffModel = resultItem, enableXmlContent = false).produceFiles()
      }.toMap()
  }

  private fun prepare() {
    allLanguages = translations.map { it.languageTag }.toSet()
    addTargetTranslations()
    addSourceTranslations()
    addAllFromStringsdictToStrings()
  }

  private fun addAllFromStringsdictToStrings() {
    models.values.forEach { model ->
      model.files.filter { it.getFileType == FileType.STRINGSDICT }.forEach filesForeach@{ stringsdictFile ->
        val targetLanguage = stringsdictFile.targetLanguage ?: return@filesForeach

        allStringsDict.forEach allStringsDictForeach@{ keyInStringsdict ->
          getFileByTargetFileOriginal(
            xliffModel = model,
            targetFileOriginal = keyInStringsdict.value.correspondingFileOriginal,
            languageTag = targetLanguage,
          ).createTransUnitIfMissing(keyInStringsdict.key).apply {
            this.source = keyInStringsdict.key
            this.note = keyInStringsdict.value.note
          }
        }
      }
    }
  }

  private fun addSourceTranslations() {
    allLanguages.forEach { targetLanguageTag ->
      baseTranslations.forEach {
        addSourceTranslation(it.value, targetLanguageTag)
      }
    }
  }

  private fun addTargetTranslations() {
    translations.forEach { translation ->
      addTargetTranslation(translation)
    }
  }

  private fun addTargetTranslation(translation: ExportTranslationView) {
    val converted =
      translation.text?.let {
        IcuToAppleMessageConvertor(
          message = it,
          translation.key.isPlural,
          isProjectIcuPlaceholdersEnabled,
        ).convert()
      }

    if (converted?.isPlural() == true) {
      handlePlural(translation, converted)
      return
    }
    return handleSingle(translation, converted?.singleResult, false)
  }

  private fun addSourceTranslation(
    translation: ExportTranslationView,
    targetLanguageTag: String,
  ) {
    val converted =
      convertedSources.computeIfAbsent(translation.key.name) {
        translation.text?.let {
          IcuToAppleMessageConvertor(
            message = it,
            forceIsPlural = translation.key.isPlural,
            isProjectIcuPlaceholdersEnabled,
          ).convert()
        }
      }

    if (converted?.isPlural() == true) {
      handlePlural(translation, converted, targetLanguageTag, true)
      return
    }
    return handleSingle(translation, converted?.singleResult, true, targetLanguageTag = targetLanguageTag)
  }

  private fun handleSingle(
    translation: ExportTranslationView,
    value: String?,
    isSource: Boolean,
    targetLanguageTag: String = translation.languageTag,
  ) {
    getResultXliffFile(targetLanguageTag, translation.key, isPlural = false)
      .createTransUnitIfMissing(translation.key.name)
      .apply {
        note = translation.key.description
        setValue(isSource, value)
      }
  }

  private fun handlePlural(
    translation: ExportTranslationView,
    converted: PossiblePluralConversionResult?,
    targetLanguageTag: String = translation.languageTag,
    isSource: Boolean = false,
  ) {
    val resultFile = getResultXliffFile(targetLanguageTag, key = translation.key, isPlural = true)

    val property = translation.key.custom?.get(APPLE_PLURAL_PROPERTY_CUSTOM_KEY) as? String ?: "property"

    val fileType = resultFile.getFileType ?: FileType.STRINGSDICT
    if (fileType == FileType.STRINGSDICT) {
      addToAllStringsdictKeys(translation)
      resultFile
        .createTransUnitIfMissing(
          "/${translation.key.name}:dict/NSStringLocalizedFormatKey:dict/:string",
        ).apply {
          setValue(isSource, "%#@$property@")
        }
    }

    val pluralFormVariants = populateForms(targetLanguageTag, converted)

    pluralFormVariants.keys.forEach { keyword ->
      resultFile
        .createTransUnitIfMissing(
          id = getPluralTransUnitId(translation.key.name, property, keyword, fileType),
        ).apply {
          val result = pluralFormVariants[keyword] ?: pluralFormVariants["other"]
          setValue(isSource, result)
        }
    }
  }

  private fun addToAllStringsdictKeys(translation: ExportTranslationView) {
    // when importing the xliff apple requires us to store it exactly to the same file original attribute
    // the issue is that the files have to be stored in different paths,
    // and so we need to remember the value when importing
    val correspondingStringsFileOriginal =
      translation.key.custom?.get(APPLE_CORRESPONDING_STRINGS_FILE_ORIGINAL) as String?
    if (correspondingStringsFileOriginal != null) {
      allStringsDict[translation.key.name] =
        KeyInStringsDict(
          key = translation.key.name,
          note = translation.key.description,
          correspondingFileOriginal = correspondingStringsFileOriginal,
        )
    }
  }

  private fun XliffTransUnit.setValue(
    isSource: Boolean,
    result: String?,
  ) {
    if (isSource) {
      source = result ?: ""
      return
    }
    this.target = result
  }

  private fun XliffFile.createTransUnitIfMissing(id: String): XliffTransUnit {
    return this.transUnits.find { it.id == id } ?: XliffTransUnit().apply {
      this.id = id
      this@createTransUnitIfMissing.transUnits.add(this)
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
    if (conversionResult?.formsResult == null) {
      return emptyMap()
    }
    return io.tolgee.formats.populateForms(languageTag, conversionResult.formsResult)
  }

  private fun getResultXliffFile(
    languageTag: String,
    key: ExportKeyView,
    isPlural: Boolean,
  ): XliffFile {
    val absolutePath = getFilePath(languageTag, key.namespace)
    val xliffModel =
      models.computeIfAbsent(absolutePath) {
        XliffModel()
      }

    val targetFileOriginal =
      key.custom?.get(APPLE_FILE_ORIGINAL_CUSTOM_KEY) as? String ?: let {
        val filename = key.namespace ?: "Localizable"
        val extension = if (isPlural) "stringsdict" else "strings"
        "$filename.$extension"
      }

    return getFileByTargetFileOriginal(xliffModel, targetFileOriginal, languageTag)
  }

  private fun getFileByTargetFileOriginal(
    xliffModel: XliffModel,
    targetFileOriginal: String,
    languageTag: String,
  ) = xliffModel.files.find { it.original == targetFileOriginal } ?: let {
    val file =
      XliffFile().apply {
        this.original = targetFileOriginal
        this.sourceLanguage = baseLanguageTag
        this.targetLanguage = languageTag
      }
    xliffModel.files.add(file)
    file
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

  private fun getFilePath(
    languageTag: String,
    namespace: String?,
  ): String {
    return filePathProvider.getFilePath(namespace, languageTag)
  }

  data class KeyInStringsDict(
    val key: String,
    val note: String?,
    val correspondingFileOriginal: String,
  )
}
