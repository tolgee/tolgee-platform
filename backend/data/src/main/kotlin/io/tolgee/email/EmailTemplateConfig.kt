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

import com.transferwise.icu.ICUReloadableResourceBundleMessageSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.messageresolver.IMessageResolver
import org.thymeleaf.spring6.messageresolver.SpringMessageResolver
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
import org.thymeleaf.templateresolver.StringTemplateResolver
import java.util.*

@Configuration
class EmailTemplateConfig {
  @Bean("emailTemplateResolver")
  fun templateResolver(): ClassLoaderTemplateResolver {
    val templateResolver = ClassLoaderTemplateResolver()
    templateResolver.characterEncoding = "UTF-8"
    templateResolver.prefix = "/email-templates/"
    templateResolver.suffix = ".html"
    return templateResolver
  }

  @Bean("emailIcuMessageSource")
  fun messageSource(): MessageSource {
    val messageSource = ICUReloadableResourceBundleMessageSource()
    messageSource.setBasenames("classpath:email-i18n/messages", "email-i18n-test/messages")
    messageSource.setDefaultEncoding("UTF-8")
    messageSource.setDefaultLocale(Locale.ENGLISH)
    return messageSource
  }

  @Bean("emailMessageResolver")
  fun messageResolver(
    @Qualifier("emailIcuMessageSource") messageSource: MessageSource,
    @Qualifier("emailTemplateEngine") templateEngine: EmailTemplateEngine
  ): IMessageResolver {
    val messageResolver = SpringMessageResolver()
    messageResolver.messageSource = messageSource

    val resolver = EmailMessageResolver(messageResolver, templateEngine)
    templateEngine.emailMessageResolver = resolver
    return resolver
  }

  @Bean("emailTemplateEngine")
  fun templateEngine(
    @Qualifier("emailTemplateResolver") templateResolver: ITemplateResolver,
  ): EmailTemplateEngine {
    val stringTemplateResolver = StringTemplateResolver()
    stringTemplateResolver.resolvablePatternSpec.addPattern("<!--@frag-->*")

    val templateEngine = EmailTemplateEngine()
    templateEngine.enableSpringELCompiler = true
    templateEngine.templateResolvers = setOf(stringTemplateResolver, templateResolver)
    return templateEngine
  }
}
