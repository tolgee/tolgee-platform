package io.tolgee.ee.component.slackIntegration.notification

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import com.slack.api.model.kotlin_extension.block.dsl.LayoutBlockDsl
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.ee.component.slackIntegration.data.SlackKeyInfoDto
import io.tolgee.ee.component.slackIntegration.data.SlackTranslationInfoDto
import io.tolgee.model.enums.TranslationState
import io.tolgee.util.I18n
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SlackNotificationBlocksProvider(
  private val i18n: I18n,
) {
  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
  }

  fun getUserLoginSuccessBlocks() =
    withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.success_login"))
      }
      context {
        plainText(i18n.translate("slack.common.context.success_login"))
      }
    }

  fun getAuthorBlocks(authorContext: String): List<LayoutBlock> {
    return withBlocks {
      context {
        markdownText(authorContext)
      }
    }
  }

  fun getKeyInfoBlock(
    context: SlackMessageContext,
    key: SlackKeyInfoDto,
    head: String,
  ): List<LayoutBlock> =
    withBlocks {
      section {
        authorHeadSection(context, head)
      }

      val columnFields = mutableListOf<Pair<String, String?>>()
      columnFields.add("Key" to key.name)
      key.tags?.let { tags ->
        val tagNames = tags.joinToString(", ")
        if (tagNames.isNotBlank()) {
          columnFields.add("Tags" to tagNames)
        }
      }
      columnFields.add("Namespace" to key.namespace)
      columnFields.add("Description" to key.description)

      field(columnFields)
    }

  fun LayoutBlockDsl.field(keyValue: List<Pair<String, String?>>) {
    section {
      val filtered = keyValue.filter { it.second != null && it.second!!.isNotEmpty() }

      if (filtered.isEmpty()) return@section
      fields {
        filtered.forEachIndexed { index, (key, value) ->
          val finalValue = value + if (index % 2 == 1 && index != filtered.size - 1) "\n\u200d" else ""
          markdownText("*$key* \n$finalValue")
        }
      }
    }
  }

  fun getImportBlocks(
    context: SlackMessageContext,
    count: Long,
  ) = withBlocks {
    section {
      authorHeadSection(context, i18n.translate("slack.common.message.imported") + " $count keys")
    }
  }

  fun getBlocksTooManyTranslations(
    context: SlackMessageContext,
    count: Long,
  ) = withBlocks {
    section {
      authorHeadSection(context, i18n.translate("slack.common.message.too-many-translations").format(count))
    }
  }

  fun SectionBlockBuilder.authorHeadSection(
    context: SlackMessageContext,
    head: String,
  ) {
    val authorMention = context.authorMention
    markdownText(" *$authorMention* $head")
  }

  fun getRedirectButtonAttachment(url: String): Attachment =
    Attachment
      .builder()
      .blocks(
        withBlocks {
          actions {
            logger.trace("URL: $url")
            redirectOnPlatformButton(url)
          }
        },
      ).color("#00000000")
      .build()

  private fun ActionsBlockBuilder.redirectOnPlatformButton(tolgeeUrl: String) {
    button {
      text(i18n.translate("slack.common.text.button.tolgee_redirect"), emoji = true)
      value("redirect")
      url(tolgeeUrl)
      actionId("button_redirect_to_tolgee")
      style("danger")
    }
  }

  fun getBlocksEmptyTranslation(
    context: SlackMessageContext,
    translation: SlackTranslationInfoDto,
  ) = withBlocks {
    if (context.shouldSkipModification(translation.languageTag)) {
      return@withBlocks
    }
    val languageName = translation.languageName
    val flagEmoji = translation.languageFlagEmoji
    val ifBase =
      if (context.baseLanguage.id == translation.languageId) {
        "(base)"
      } else {
        ""
      }

    section {
      markdownText("$flagEmoji *$languageName* $ifBase")
    }

    context {
      markdownText("No translation")
    }
  }

  fun getBlocksWithTranslation(
    context: SlackMessageContext,
    translation: SlackTranslationInfoDto,
    author: String?,
  ) = withBlocks {
    if (context.shouldSkipModification(translation.languageTag)) {
      return@withBlocks
    }
    section {
      languageInfoSection(context, translation)
    }

    section {
      val currentTranslate = translation.text!!
      markdownText(currentTranslate)
    }
    val contextText = author ?: return@withBlocks
    if (contextText.isEmpty()) {
      return@withBlocks
    }
    context {
      markdownText(contextText)
    }
  }

  private fun SectionBlockBuilder.languageInfoSection(
    context: SlackMessageContext,
    translation: SlackTranslationInfoDto,
  ) {
    val languageName = translation.languageName
    val flagEmoji = translation.languageFlagEmoji
    val ifBase =
      if (context.baseLanguage.id == translation.languageId) {
        "(base)"
      } else {
        ""
      }

    markdownText("$flagEmoji *$languageName* $ifBase")
  }

  fun createAttachmentForLanguage(
    context: SlackMessageContext,
    translation: SlackTranslationInfoDto,
    author: String?,
  ): Attachment? {
    val baseLanguage = context.slackConfig.project.baseLanguage ?: return null

    if (context.shouldSkipModification(translation.languageTag)) {
      return null
    }

    val color = determineColorByState(translation.state)
    val blocksBody =
      if (translation.text != null) {
        getBlocksWithTranslation(context, translation, author)
      } else {
        getBlocksEmptyTranslation(context, translation)
      }
    return Attachment
      .builder()
      .color(color)
      .blocks(blocksBody)
      .fallback(translation.text ?: "")
      .build()
  }

  private fun determineColorByState(state: TranslationState?): String {
    return when (state) {
      TranslationState.TRANSLATED -> "#FFCE00"
      TranslationState.UNTRANSLATED -> "#BCC2CB"
      TranslationState.REVIEWED -> "#00B962"
      else -> "#BCC2CB"
    }
  }
}
