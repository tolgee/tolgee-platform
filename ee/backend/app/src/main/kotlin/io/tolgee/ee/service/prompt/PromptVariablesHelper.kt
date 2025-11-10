package io.tolgee.ee.service.prompt

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.machineTranslation.metadata.TranslationGlossaryItem
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.component.PromptLazyMap.Companion.Variable
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MetadataKey
import io.tolgee.service.machineTranslation.MetadataProvider
import io.tolgee.service.machineTranslation.MtTranslatorContext
import io.tolgee.service.machineTranslation.PluralTranslationUtil
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PromptVariablesHelper(
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val translationService: TranslationService,
  private val applicationContext: ApplicationContext,
  private val promptFragmentsHelper: PromptFragmentsHelper,
  private val screenshotService: ScreenshotService,
  private val glossaryTermService: GlossaryTermService,
) {
  /**
   * Determines if the given language tag corresponds to Chinese, Japanese, or Korean.
   *
   * @param tag the language tag to be checked, which may be null.
   * @return a `Variable` object indicating whether the tag represents Chinese, Japanese, or Korean.
   */
  fun cjkVariable(tag: String?): Variable {
    val isCJK = tag?.let { it.startsWith("zh") || it.startsWith("ja") || it.startsWith("ko") } ?: false
    return Variable("isCJK", isCJK, description = "Is Chinese, Japanese or Korean")
  }

  private fun getScreenshotVar(key: Key?): Variable {
    val keyScreenshotReferences = key?.let { screenshotService.getAllKeyScreenshotReferences(key) }
    val screenshots = keyScreenshotReferences?.map { "${it.screenshot.id}" }
    val screenshotsVar = Variable("screenshots")

    screenshotsVar.props.add(
      Variable(
        "first",
        screenshots?.let { encodeScreenshots(it.take(1), ScreenshotSize.SMALL.value) },
      ),
    )

    screenshotsVar.props.add(
      Variable(
        "firstFull",
        screenshots?.let { encodeScreenshots(it.take(1), ScreenshotSize.FULL.value) },
      ),
    )

    screenshotsVar.props.add(
      Variable(
        "all",
        screenshots?.let { encodeScreenshots(it, ScreenshotSize.SMALL.value) },
      ),
    )

    screenshotsVar.props.add(
      Variable(
        "allFull",
        screenshots?.let { encodeScreenshots(it, ScreenshotSize.FULL.value) },
      ),
    )

    return screenshotsVar
  }

  private fun getTranslationMemoryVar(
    projectId: Long,
    tLanguage: LanguageDto?,
    sTranslation: Translation?,
    key: Key?,
  ): Variable {
    val translationMemory = Variable("translationMemory")
    translationMemory.props.add(
      Variable(
        "json",
        description = "Translation memory items",
        lazyValue = {
          val context = MtTranslatorContext(projectId, applicationContext, false)
          val metadataProvider = MetadataProvider(context)
          val closeItems =
            tLanguage?.let {
              key?.let {
                metadataProvider.getExamples(
                  tLanguage,
                  isPlural = key.isPlural,
                  text = sTranslation?.text ?: "",
                  keyId = key.id,
                )
              }
            }
          if (!closeItems.isNullOrEmpty()) {
            closeItems.joinToString("\n") {
              val mapper = ObjectMapper()
              mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
              mapper.writeValueAsString(it)
            }
          } else {
            null
          }
        },
      ),
    )
    return translationMemory
  }

  private fun getGlossaryVar(
    project: ProjectDto,
    sourceLanguageTag: String,
    targetLanguageTag: String?,
    sourceText: String?,
  ): Variable {
    val glossary = Variable("glossary")

    val glossaryTerms by lazy {
      if (targetLanguageTag != null && sourceText != null) {
        glossaryTermService.getGlossaryTerms(
          project,
          sourceLanguageTag,
          targetLanguageTag,
          sourceText,
        )
      } else {
        emptySet()
      }
    }

    glossary.props.add(
      Variable("json", description = "Glossary items", lazyValue = {
        if (glossaryTerms.isNotEmpty()) {
          glossaryTerms.joinToString("\n") {
            val mapper = ObjectMapper()
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            mapper.writeValueAsString(removeUnnecessaryFields(it))
          }
        } else {
          null
        }
      }),
    )

    glossary.props.add(
      Variable("hasCaseSensitive", description = "Glossary items contain some caseSensitive item", lazyValue = {
        glossaryTerms.any { it.isCaseSensitive ?: false }
      }),
    )

    glossary.props.add(
      Variable("hasAbbreviation", description = "Glossary items contain some abbreviation item", lazyValue = {
        glossaryTerms.any { it.isAbbreviation ?: false }
      }),
    )

    glossary.props.add(
      Variable("hasForbiddenTerm", description = "Glossary items contain some forbidden item", lazyValue = {
        glossaryTerms.any { it.isForbiddenTerm ?: false }
      }),
    )

    glossary.props.add(
      Variable("hasNonTranslatable", description = "Glossary items contain some non-translatable item", lazyValue = {
        glossaryTerms.any { it.isNonTranslatable ?: false }
      }),
    )

    return glossary
  }

  private fun getStandardLanguageVars(
    language: LanguageDto?,
    translation: Translation?,
  ): MutableList<Variable> {
    val result = mutableListOf<Variable>()

    result.add(Variable("languageName", language?.name))
    result.add(Variable("languageTag", language?.tag))
    result.add(Variable("translation", escapeAsJson(translation?.text) ?: ""))
    result.add(Variable("languageNote", language?.aiTranslatorPromptDescription ?: ""))
    result.add(cjkVariable(language?.tag))
    return result
  }

  private fun getPluralVariables(
    projectId: Long,
    tLanguage: LanguageDto?,
    sTranslation: Translation?,
    key: Key?,
  ): MutableList<Variable> {
    val result = mutableListOf<Variable>()

    val context = MtTranslatorContext(projectId, applicationContext, false)
    val pluralFormsWithReplacedParam =
      if (key != null && key.isPlural && sTranslation != null && tLanguage?.tag != null) {
        context.getPluralFormsReplacingReplaceParam(
          sTranslation.text ?: "",
        )
      } else {
        null
      }

    val pluralSourceExamples =
      pluralFormsWithReplacedParam?.let {
        PluralTranslationUtil.getSourceExamples(
          context.baseLanguage.tag,
          tLanguage!!.tag,
          it,
        )
      }

    result.add(
      Variable(
        "pluralFormExamples",
        value = pluralSourceExamples?.map { "${it.key} (e.g. ${it.value})" }?.joinToString("\n"),
      ),
    )

    result.add(
      Variable(
        "exactForms",
        value = pluralSourceExamples?.map { it.key }?.joinToString(" "),
      ),
    )

    result.add(
      Variable(
        "exampleIcuPlural",
        value = pluralSourceExamples?.let { "{count, plural, ${it.map { "${it.key} {...}" }.joinToString(" ")}}" },
      ),
    )
    return result
  }

  private fun getOtherLanguageVars(
    otherLanguages: List<LanguageDto>,
    translations: List<Translation>?,
  ): MutableList<Variable> {
    val result = mutableListOf<Variable>()
    otherLanguages.forEach { language ->
      val langVar = Variable(language.tag)
      langVar.props.addAll(
        getStandardLanguageVars(language, translations?.find { it.language.tag == language.tag }),
      )
      result.add(langVar)
    }
    return result
  }

  private fun getRelatedKeysVar(
    projectId: Long,
    tLanguage: LanguageDto?,
    key: Key?,
    sLanguage: LanguageDto,
    sTranslation: Translation?,
  ): Variable {
    val relatedKeys = Variable("relatedKeys")
    relatedKeys.props.add(
      Variable(
        "json",
        description = "Related keys in json format (based on context extraction)",
        lazyValue = {
          val context = MtTranslatorContext(projectId, applicationContext, false)
          val metadataProvider = MetadataProvider(context)
          val closeItems =
            tLanguage?.let {
              key?.let {
                metadataProvider.getCloseItems(
                  sLanguage,
                  tLanguage,
                  MetadataKey(key.id, sTranslation?.text ?: "", tLanguage.id),
                )
              }
            }
          if (!closeItems.isNullOrEmpty()) {
            closeItems.joinToString("\n") {
              val mapper = ObjectMapper()
              mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
              mapper.writeValueAsString(it)
            }
          } else {
            null
          }
        },
      ),
    )
    return relatedKeys
  }

  @Transactional(readOnly = true)
  fun getVariables(
    projectId: Long,
    keyId: Long?,
    targetLanguageId: Long?,
  ): MutableList<Variable> {
    var key: Key? = null
    if (keyId !== null) {
      key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
      keyService.checkInProject(key, projectId)
    }

    val project = projectService.get(projectId)
    val projectDto = ProjectDto.fromEntity(project)

    val languages = languageService.getProjectLanguages(projectId)

    val tLanguage =
      targetLanguageId?.let { tLangId ->
        languages.find { it.id == tLangId } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
      }
    val sLanguage = languages.find { it.id == project.baseLanguage!!.id }!!

    val translations = key?.let { translationService.get(mapOf(key to languages.toList())) }

    val sTranslation = translations?.find { it.language.id == sLanguage.id }
    val tTranslation = translations?.find { it.language.id == tLanguage?.id }

    val variables = mutableListOf<Variable>()

    val source = Variable("source")

    source.props.addAll(getStandardLanguageVars(sLanguage, sTranslation))
    variables.add(source)

    val target = Variable("target")
    target.props.addAll(getStandardLanguageVars(tLanguage, tTranslation))
    target.props.addAll(getPluralVariables(projectId, tLanguage, sTranslation, key))
    variables.add(target)

    val otherVar = Variable("other")
    val otherLanguages =
      languages.filter {
        it.id != sLanguage.id && it.id != tLanguage?.id
      }
    otherVar.props.addAll(getOtherLanguageVars(otherLanguages, translations))
    variables.add(otherVar)

    val projectVar = Variable("project")
    projectVar.props.add(Variable("name", project.name))
    projectVar.props.add(Variable("description", project.aiTranslatorPromptDescription ?: ""))
    variables.add(projectVar)

    val keyVar = Variable("key")
    keyVar.props.add(Variable("name", key?.name))
    keyVar.props.add(Variable("description", key?.keyMeta?.description ?: ""))
    variables.add(keyVar)

    variables.add(getRelatedKeysVar(projectId, tLanguage, key, sLanguage, sTranslation))

    variables.add(getTranslationMemoryVar(projectId, tLanguage, sTranslation, key))

    variables.add(
      getGlossaryVar(
        projectDto,
        sLanguage.tag,
        tLanguage?.tag,
        sTranslation?.text,
      ),
    )

    variables.add(getScreenshotVar(key))

    val fragments = Variable("fragment", props = promptFragmentsHelper.getAllFragments())
    variables.add(fragments)

    return variables
  }

  fun escapeAsJson(text: String?): String? {
    return text?.let {
      val objectMapper: ObjectMapper = jacksonObjectMapper()
      objectMapper.writeValueAsString(text).removeSurrounding("\"")
    }
  }

  fun encodeScreenshot(
    id: Long,
    type: String,
  ): String {
    return "[[screenshot_${type}_$id]]"
  }

  fun encodeScreenshots(
    list: List<String>,
    type: String,
  ): String {
    return list.joinToString("\n") { id -> encodeScreenshot(id.toLong(), type) }
  }

  fun removeUnnecessaryFields(item: TranslationGlossaryItem): TranslationGlossaryItem {
    return TranslationGlossaryItem(
      source = item.source,
      isCaseSensitive = item.isCaseSensitive ?: false,
      // set empty items null for JSON stringification
      target = item.target?.takeIf { it.isNotBlank() },
      description = item.description?.takeIf { it.isNotBlank() },
      isNonTranslatable = item.isNonTranslatable?.takeIf { it },
      isAbbreviation = item.isAbbreviation?.takeIf { it },
      isForbiddenTerm = item.isForbiddenTerm?.takeIf { it },
    )
  }

  companion object {
    enum class ScreenshotSize(
      val value: String,
    ) {
      SMALL("small"),
      FULL("full"),
    }
  }
}
