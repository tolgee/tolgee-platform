/**
 * Copyright (C) 2024 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.email

import io.tolgee.configuration.tolgee.SmtpProperties
import io.tolgee.dtos.misc.EmailAttachment
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext
import java.util.*

@Service
class EmailService(
  private val applicationContext: ApplicationContext,
  private val smtpProperties: SmtpProperties,
  private val mailSender: JavaMailSender,
  private val emailGlobalVariablesProvider: EmailGlobalVariablesProvider,
  @Qualifier("emailTemplateEngine") private val templateEngine: TemplateEngine,
) {
  private val smtpFrom
    get() =
      smtpProperties.from
        ?: throw IllegalStateException(
          "SMTP sender is not configured. " +
            "See https://docs.tolgee.io/platform/self_hosting/configuration#smtp",
        )

  @Async
  fun sendEmailTemplate(
    recipient: String,
    template: String,
    locale: Locale,
    properties: Map<String, Any> = mapOf(),
    attachments: List<EmailAttachment> = listOf(),
  ) {
    val globalVariables = emailGlobalVariablesProvider()
    val context = Context(locale, properties)
    context.setVariables(globalVariables)

    // Required because we're outside of Spring MVC here
    // Otherwise, bean resolution does not work for some reason
    val tec = ThymeleafEvaluationContext(applicationContext, null)
    context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME, tec)

    // Do two passes, so Thymeleaf expressions rendered by messages can get processed
    val firstPass = templateEngine.process(template, context)
    val html = templateEngine.process(firstPass, context)

    val subject = extractEmailTitle(html)
    sendEmail(recipient, subject, html, attachments)
  }

  @Async
  fun sendEmail(
    recipient: String,
    subject: String,
    contents: String,
    attachments: List<EmailAttachment> = listOf(),
  ) {
    val message = mailSender.createMimeMessage()
    val helper = MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF8")

    helper.setFrom(smtpFrom)
    helper.setTo(recipient)
    helper.setSubject(subject)
    helper.setText(contents, true)
    attachments.forEach { helper.addAttachment(it.name, it.inputStreamSource) }

    mailSender.send(message)
  }

  private fun extractEmailTitle(html: String): String {
    return REGEX_TITLE.find(html)!!.groupValues[1]
  }

  companion object {
    private val REGEX_TITLE = Regex("<title>(.+?)</title>")
  }
}
