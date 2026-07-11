package io.tolgee.formats.apple.`in`.xliff

import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.apple.APPLE_CORRESPONDING_STRINGS_FILE_ORIGINAL
import io.tolgee.formats.apple.APPLE_FILE_ORIGINAL_CUSTOM_KEY
import io.tolgee.formats.apple.APPLE_PLURAL_PROPERTY_CUSTOM_KEY
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext

class AppleXliffFileProcessor(
  override val context: FileProcessorContext,
  private val parsed: XliffModel,
) : ImportFileProcessor() {
  /**
   * file -> Map (KeyName -> Map (Form -> Pair (Source, Target )))
   */
  private val allPlurals = mutableMapOf<XliffFile, MutableMap<String, MutableMap<String, Pair<String?, String?>>>>()

  override fun process() {
    // for apple xliff, we currently don't support namespaces
    handleNamespace()
    parsed.files.forEach { file ->
      file.transUnits.forEach transUnitsForeach@{ transUnit ->
        // if there is a key defined in base .stringsdict but is missing in the target .stringsdict
        // it adds the key also in the .strings file section in xliff, and sets the "translate" value to "no"
        // in the same time, the key is present in the .stringsdict section. This led to the target translation was
        // imported 2 times, and ignored by the import process
        if (transUnit.translate == "no") {
          return@transUnitsForeach
        }

        val fileOriginal = file.original
        val transUnitId =
          transUnit.id ?: let {
            context.fileEntity.addIssue(
              FileIssueType.ID_ATTRIBUTE_NOT_PROVIDED,
              mapOf(FileIssueParamType.FILE_NODE_ORIGINAL to (fileOriginal ?: "")),
            )
            return@transUnitsForeach
          }

        addNote(transUnit, transUnitId)

        val (pluralFormRegex, pluralDefRegex) = getPluralRegexes(file)

        val pluralFormMatch = pluralFormRegex.matchEntire(transUnitId)
        if (pluralFormMatch != null) {
          handlePlural(pluralFormMatch, file, transUnit)
          return@transUnitsForeach
        }

        if (pluralDefRegex != null && transUnitId.matches(pluralDefRegex)) {
          // let's ignore this one, since it has no meaning for us now
          return@transUnitsForeach
        }

        handleSingle(fileOriginal, transUnitId, transUnit, file)
      }
    }

    handlePlurals()
  }

  private fun handleNamespace() {
    context.namespace = ""
  }

  private fun getPluralRegexes(file: XliffFile): Pair<Regex, Regex?> {
    if (file.original?.contains(".stringsdict") == true) {
      return STRINGSDICT_PLURAL_FORM_REGEX to STRINGSDICT_PLURAL_DEF_REGEX
    }

    return XCSTRINGS_PLURAL_FORM_REGEX to null
  }

  private fun addNote(
    transUnit: XliffTransUnit,
    transUnitId: String,
  ) {
    context.addKeyDescription(transUnitId, transUnit.note)
  }

  private fun handleSingle(
    fileOriginal: String?,
    transUnitId: String,
    transUnit: XliffTransUnit,
    file: XliffFile,
  ) {
    if (!fileOriginal.isNullOrBlank()) {
      context.setCustom(transUnitId, APPLE_FILE_ORIGINAL_CUSTOM_KEY, fileOriginal)
    }

    addTranslations(transUnit, transUnitId, file)
  }

  private fun handlePlural(
    pluralFormMatch: MatchResult,
    file: XliffFile,
    transUnit: XliffTransUnit,
  ) {
    val keyName = pluralFormMatch.groups["keyname"]?.value ?: return

    assignPropertyName(keyName, pluralFormMatch)

    val pluralFile =
      allPlurals.computeIfAbsent(file) { mutableMapOf() }

    pluralFile.compute(keyName) { _, map ->
      val formMap = map ?: mutableMapOf()
      val form = pluralFormMatch.groups["form"]?.value ?: return@compute formMap
      formMap[form] = transUnit.source to transUnit.target
      formMap
    }
  }

  private fun assignPropertyName(
    keyName: String,
    pluralFormMatch: MatchResult,
  ) {
    try {
      val propertyName = pluralFormMatch.groups["property"]?.value ?: return
      context.setCustom(keyName, APPLE_PLURAL_PROPERTY_CUSTOM_KEY, propertyName)
    } catch (e: IllegalArgumentException) {
      // the property group is optional, so it might throw
    }
  }

  /**
   * The plurals have to be handled last. We need to replace te non-plural translations, because
   * the plural keys appear in the .strings file section in xliff if missing in localizad .stringsdict file
   * if the non-plural keys were not removed from the .strings section it would show false positive file issues
   * (key defined multiple times in the same file)
   */
  private fun handlePlurals() {
    allPlurals.forEach { (file, pluralsUnits) ->
      pluralsUnits.forEach { (keyName, forms) ->
        context.keys[keyName]?.keyMeta?.custom?.get(APPLE_FILE_ORIGINAL_CUSTOM_KEY)?.let {
          // when importing the xliff apple requires us to store it exactly to the same file original attribute
          // the issue is that the files have to be stored in different paths,
          // and so we need to remember the value when importing
          context.setCustom(keyName, APPLE_CORRESPONDING_STRINGS_FILE_ORIGINAL, it)
        }
        context.setCustom(keyName, APPLE_FILE_ORIGINAL_CUSTOM_KEY, file.original ?: "")
        val sourceForms = forms.mapValues { it.value.first }
        val targetForms = forms.mapValues { it.value.second }
        addPluralTranslation(keyName, sourceForms, file.sourceLanguage ?: "unknown source")
        addPluralTranslation(keyName, targetForms, file.targetLanguage ?: "unknown target")
      }
    }
  }

  private fun addPluralTranslation(
    keyName: String,
    forms: Map<String, String?>,
    language: String,
  ) {
    if (forms.containsKey("other") && forms["other"] != null) {
      val converted =
        messageConvertor.convert(
          rawData = forms,
          languageTag = language,
          convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
          isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
        )
      context.addTranslation(
        keyName,
        language,
        converted.message,
        replaceNonPlurals = true,
        convertedBy = importFormat,
        rawData = forms,
        pluralArgName = converted.pluralArgName,
      )
    }
  }

  private fun addTranslations(
    transUnit: XliffTransUnit,
    transUnitId: String,
    file: XliffFile,
  ) {
    transUnit.source?.let { source ->
      val convertedSource = convertMessage(source)
      context.addTranslation(
        transUnitId,
        file.sourceLanguage ?: "unknown source",
        convertedSource.message,
        pluralArgName = convertedSource.pluralArgName,
        rawData = source,
        convertedBy = importFormat,
      )
    }

    transUnit.target?.let { target ->
      val convertedTarget = convertMessage(target)
      context.addTranslation(
        transUnitId,
        file.targetLanguage ?: "unknown target",
        convertedTarget.message,
        pluralArgName = convertedTarget.pluralArgName,
        rawData = target,
        convertedBy = importFormat,
      )
    }
  }

  private fun convertMessage(message: String): MessageConvertorResult {
    return messageConvertor.convert(
      message,
      "who-knows",
      context.importSettings.convertPlaceholdersToIcu,
      context.projectIcuPlaceholdersEnabled,
    )
  }

  companion object {
    private val importFormat = ImportFormat.APPLE_XLIFF

    private val messageConvertor = importFormat.messageConvertor

    private val XCSTRINGS_PLURAL_FORM_REGEX =
      "^/?(?<keyname>[^:]+)\\|==\\|plural\\.(?<form>[a-z0-9=]+)\$".toRegex()
    private val STRINGSDICT_PLURAL_DEF_REGEX =
      "^/?(?<keyname>[^:]+):dict/(?<property>[^:]+):dict/:string$".toRegex()
    private val STRINGSDICT_PLURAL_FORM_REGEX =
      "^/?(?<keyname>[^:]+):dict/(?<property>[^:]+):dict/(?<form>[a-z0-9=]+):dict/:string\$".toRegex()
  }
}
