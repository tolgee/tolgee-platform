package io.tolgee.ee.service.prompt

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.constants.Message
import io.tolgee.ee.service.prompt.PromptServiceEeImpl.Companion.Variable
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.key.Key
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MetadataKey
import io.tolgee.service.machineTranslation.MetadataProvider
import io.tolgee.service.machineTranslation.MtTranslatorContext
import io.tolgee.service.machineTranslation.PluralTranslationUtil
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
class PromptVariablesService(
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val translationService: TranslationService,
  private val applicationContext: ApplicationContext,
  private val promptFragmentsService: PromptFragmentsService,
) {
  @Transactional
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

    val languages = languageService.getProjectLanguages(projectId)

    val tLanguage =
      targetLanguageId?.let {
        languageService.find(targetLanguageId, projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
      }
    val sLanguage = languageService.get(project.baseLanguage!!.id, projectId)

    val sTranslation = key?.let { translationService.find(it, sLanguage).getOrNull() }
    val tTranslation = key?.let { tLanguage?.let { translationService.find(key, tLanguage).getOrNull() } }

    val variables = mutableListOf<Variable>()

    val source = Variable("source")

    source.props.add(Variable("language", sLanguage.name))
    source.props.add(Variable("translation", sTranslation?.text ?: ""))
    source.props.add(Variable("languageNote", sLanguage.aiTranslatorPromptDescription ?: ""))
    variables.add(source)

    val target = Variable("target")

    target.props.add(Variable("language", tLanguage?.name))
    target.props.add(Variable("translation", tTranslation?.text ?: ""))

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

    target.props.add(
      Variable(
        "pluralFormExamples",
        value = pluralSourceExamples?.map { "${it.key} (e.g. ${it.value})" }?.joinToString("\n"),
      ),
    )

    target.props.add(
      Variable(
        "exactForms",
        value = pluralSourceExamples?.map { it.key }?.joinToString(" "),
      ),
    )

    target.props.add(
      Variable(
        "exampleIcuPlural",
        value = pluralSourceExamples?.let { "{count, plural, ${it.map { "${it.key} {...}" }.joinToString(" ")}}" },
      ),
    )
    target.props.add(
      Variable(
        "languageNote",
        tLanguage?.aiTranslatorPromptDescription ?: "",
      ),
    )

    variables.add(target)

    val otherVar = Variable("other")

    languages.filter {
      it.id != sLanguage.id && it.id != tLanguage?.id
    }.forEach { language ->
      val langVar = Variable(language.tag)
      langVar.props.add(Variable("language", language.name))
      langVar.props.add(Variable("languageNote", language.aiTranslatorPromptDescription))
      langVar.props.add(
        Variable("translation", lazyValue = {
          key?.let { translationService.find(it, language).getOrNull()?.text }
        }),
      )
      otherVar.props.add(langVar)
    }

    variables.add(otherVar)

    val projectVar = Variable("project")

    projectVar.props.add(Variable("name", project.name))
    projectVar.props.add(Variable("description", project.aiTranslatorPromptDescription ?: ""))

    variables.add(projectVar)

    val keyVar = Variable("key")
    keyVar.props.add(Variable("name", key?.name))
    keyVar.props.add(Variable("description", key?.keyMeta?.description ?: ""))
    variables.add(keyVar)

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
    variables.add(relatedKeys)

    val translationMemory = Variable("translationMemory")
    translationMemory.props.add(
      Variable(
        "json",
        description = "",
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
    variables.add(translationMemory)

    val screenshots = key?.keyScreenshotReferences?.map { "${it.screenshot.id}" }
    val screenshotsVar = Variable("screenshots")

    screenshotsVar.props.add(
      Variable(
        "first",
        screenshots?.let { encodeScreenshots(it.take(1), "small") },
      ),
    )

    screenshotsVar.props.add(
      Variable(
        "firstFull",
        screenshots?.let { encodeScreenshots(it.take(1), "full") },
      ),
    )

    screenshotsVar.props.add(
      Variable(
        "all",
        screenshots?.let { encodeScreenshots(it, "small") },
      ),
    )

    screenshotsVar.props.add(
      Variable(
        "allFull",
        screenshots?.let { encodeScreenshots(it, "full") },
      ),
    )

    variables.add(screenshotsVar)

    val fragments = Variable("fragment", props = promptFragmentsService.getAllFragments())
    variables.add(fragments)

    return variables
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
    return list.map { id -> encodeScreenshot(id.toLong(), type) }.joinToString("\n")
  }
}
