package io.tolgee.integrations.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.AppConfig
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.boot.web.servlet.ServletComponentScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ServletComponentScan
class SlackConfiguration : Logging {
  @Bean
  fun initSlackApp(): App {
    val app = App(
      AppConfig().apply {
        signingSecret = "81dfd547b0461b463466fff869ccace5"
//      clientSecret = "752ec9f6fb4e09b9a30d4c2577e37117"
//      clientId = "1995471414436.3909955104630"
      }
    )
    app.command("/hello") { req, ctx ->
      val client = app.slack.methods()
      val result = client.chatPostMessage { r: ChatPostMessageRequestBuilder ->
        r.token("xoxb-1995471414436-3916512929667-pfHhYm8cIgguEZhDvTKBzi71")
        r.channel(ctx.channelId)
        r.text("hellooo")
      }
      logger.info("result {}", result)
      ctx.ack("OK")
    }
    return app
  }
}
