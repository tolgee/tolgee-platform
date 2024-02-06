package io.tolgee.formats.ios.`in`

import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.ios.APPLE_FILE_ORIGINAL_CUSTOM_KEY
import io.tolgee.formats.ios.APPLE_FILE_ORIGINAL_PROPERTY_KEY
import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor

class AppleXliffFileProcessor(override val context: FileProcessorContext, private val parsed: XliffModel) :
  ImportFileProcessor() {
  override fun process() {
    parsed.files.forEach { file ->
      /**
       * KeyName -> Map (Form -> Pair (Source, Target ))
       */
      val filePluralUnits = mutableMapOf<String, MutableMap<String, Pair<String?, String?>>>()

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

        transUnit.note?.let { context.addKeyComment(transUnitId, it) }

        val pluralFormMatch = PLURAL_FORM_REGEX.matchEntire(transUnitId)
        if (pluralFormMatch != null) {
          handlePlural(pluralFormMatch, fileOriginal, filePluralUnits, transUnit)
          return@transUnitsForeach
        }

        if (transUnitId.matches(PLURAL_DEF_REGEX)) {
          // let's ignore this one, since it has no meaning for us now
          return@transUnitsForeach
        }

        handleSingle(fileOriginal, transUnitId, transUnit, file)
      }

      handlePlurals(filePluralUnits, file)
    }
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
    fileOriginal: String?,
    pluralsUnits: MutableMap<String, MutableMap<String, Pair<String?, String?>>>,
    transUnit: XliffTransUnit,
  ) {
    val keyName = pluralFormMatch.groups["keyname"]?.value ?: return
    val propertyName = pluralFormMatch.groups["property"]?.value ?: return

    if (!fileOriginal.isNullOrBlank()) {
      context.setCustom(keyName, APPLE_FILE_ORIGINAL_CUSTOM_KEY, fileOriginal)
      context.setCustom(keyName, APPLE_FILE_ORIGINAL_PROPERTY_KEY, propertyName)
    }

    pluralsUnits.compute(keyName) { _, map ->
      val formMap = map ?: mutableMapOf()
      val form = pluralFormMatch.groups["form"]?.value ?: return@compute formMap
      formMap[form] = transUnit.source to transUnit.target
      formMap
    }
  }

  private fun handlePlurals(
    pluralsUnits: MutableMap<String, MutableMap<String, Pair<String?, String?>>>,
    file: XliffFile,
  ) {
    pluralsUnits.forEach { (keyName, forms) ->
      val sourceForms = forms.mapValues { it.value.first }
      val targetForms = forms.mapValues { it.value.second }
      addPluralTranslation(keyName, sourceForms, file.sourceLanguage ?: "unknown source")
      addPluralTranslation(keyName, targetForms, file.targetLanguage ?: "unknown target")
    }
  }

  private fun addPluralTranslation(
    keyName: String,
    forms: Map<String, String?>,
    language: String,
  ) {
    if (forms.containsKey("other") && forms["other"] != null) {
      val formsNotNull =
        forms.mapNotNull {
          val value = it.value ?: return@mapNotNull null
          it.key to convertMessage(value, true)
        }.toMap()
      val converted = FormsToIcuPluralConvertor(formsNotNull).convert()
      context.addTranslation(keyName, language, converted)
    }
  }

  private fun addTranslations(
    transUnit: XliffTransUnit,
    transUnitId: String,
    file: XliffFile,
  ) {
    transUnit.source?.let { source ->
      context.addTranslation(transUnitId, file.sourceLanguage ?: "unknown source", convertMessage(source, false))
    }

    transUnit.target?.let { target ->
      context.addTranslation(transUnitId, file.targetLanguage ?: "unknown target", convertMessage(target, false))
    }
  }

  private fun convertMessage(
    message: String,
    isPlural: Boolean,
  ): String {
    return io.tolgee.formats.convertMessage(message, isPlural) {
      IOsToIcuParamConvertor()
    }
  }

  companion object {
    private val PLURAL_DEF_REGEX =
      "^/?(?<keyname>[^:]+):dict/(?<property>[^:]+):dict/:string$".toRegex()
    private val PLURAL_FORM_REGEX =
      "^/?(?<keyname>[^:]+):dict/(?<property>[^:]+):dict/(?<form>[a-z0-9=]+):dict/:string\$".toRegex()
  }
}
