package io.tolgee.ee.component.slackIntegration.notification

import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.ee.component.slackIntegration.SlackUserLoginUrlProvider
import io.tolgee.ee.service.slackIntegration.SlackConfigReadService
import io.tolgee.service.language.LanguageService
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class SlackNotificationBlocksProvider(
  private val i18n: I18n,
  private val slackConfigReadService: SlackConfigReadService,
  private val languageService: LanguageService,
  private val slackUserLoginUrlProvider: SlackUserLoginUrlProvider,
) {
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
}
