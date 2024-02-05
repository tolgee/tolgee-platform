package io.tolgee.formats.ios.`in`

import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.xliff.`in`.parser.XliffParserResult
import io.tolgee.formats.xliff.`in`.parser.XliffParserResultFile
import io.tolgee.formats.xliff.`in`.parser.XliffParserResultTransUnit
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor

class AppleXliffFileProcessor(override val context: FileProcessorContext, private val parsed: XliffParserResult) :
  ImportFileProcessor() {
  override fun process() {
    parsed.files.forEach { file ->
      /**
       * KeyName -> Map (Form -> Pair (Source, Target ))
       */
      val pluralsUnits = mutableMapOf<String, MutableMap<String, Pair<String?, String?>>>()

      file.transUnits.forEach transUnitsForeach@{ transUnit ->
        val fileOriginal = file.original
        val transUnitId =
          transUnit.id ?: let {
            context.fileEntity.addIssue(
              FileIssueType.ID_ATTRIBUTE_NOT_PROVIDED,
              mapOf(FileIssueParamType.FILE_NODE_ORIGINAL to (fileOriginal ?: "")),
            )
            return@transUnitsForeach
          }

        if (!fileOriginal.isNullOrBlank()) {
          context.addKeyCodeReference(transUnitId, fileOriginal, null)
        }

        transUnit.note?.let { context.addKeyComment(transUnitId, it) }

        val pluralFormMatch = PLURAL_FORM_REGEX.matchEntire(transUnitId)
        if (pluralFormMatch != null) {
          val keyName = pluralFormMatch.groups["keyname"]?.value ?: return@transUnitsForeach
          pluralsUnits.compute(keyName) { _, map ->
            val formMap = map ?: mutableMapOf()
            val form = pluralFormMatch.groups["form"]?.value ?: return@compute formMap
            formMap[form] = transUnit.source to transUnit.target
            formMap
          }
          return@transUnitsForeach
        }

        if (transUnitId.matches(PLURAL_DEF_REGEX)) {
          // let's ignore this one, since it has no meaning for us now
          return@transUnitsForeach
        }

        addTranslations(transUnit, transUnitId, file)
      }

      handlePlurals(pluralsUnits, file)
    }
  }

  private fun handlePlurals(
    pluralsUnits: MutableMap<String, MutableMap<String, Pair<String?, String?>>>,
    file: XliffParserResultFile,
  ) {
    pluralsUnits.forEach { (keyName, forms) ->
      val sourceForms = forms.mapValues { it.value.first }
      val targetForms = forms.mapValues { it.value.second }
      addPluralTranslation(keyName, sourceForms, file)
      addPluralTranslation(keyName, targetForms, file)
    }
  }

  private fun addPluralTranslation(
    keyName: String,
    forms: Map<String, String?>,
    file: XliffParserResultFile,
  ) {
    if (forms.containsKey("other")) {
      val formsNotNull =
        forms.mapNotNull {
          val value = it.value ?: return@mapNotNull null
          it.key to convertMessage(value, true)
        }.toMap()
      val converted = FormsToIcuPluralConvertor(formsNotNull).convert()
      context.addTranslation(keyName, file.sourceLanguage ?: "unknown source", converted)
    }
  }

  private fun addTranslations(
    transUnit: XliffParserResultTransUnit,
    transUnitId: String,
    file: XliffParserResultFile,
  ) {
    transUnit.source?.let { source ->
      context.addTranslation(transUnitId, file.sourceLanguage ?: "unknown source", convertMessage(source, false))
    }

    transUnit.target?.let { target ->
      context.addTranslation(transUnitId, file.targetLanguage ?: "unknown target", convertMessage(target, false))
    } ?: let {
      context.fileEntity.addIssue(
        FileIssueType.TARGET_NOT_PROVIDED,
        mapOf(FileIssueParamType.KEY_NAME to transUnitId),
      )
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
