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

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.thymeleaf.TemplateEngine
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
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

  @Bean("emailMessageSource")
  fun messageSource(): MessageSource {
    val messageSource = ResourceBundleMessageSource()
    messageSource.setBasename("email-i18n.messages")
    messageSource.setDefaultEncoding("UTF-8")
    messageSource.setDefaultLocale(Locale.ENGLISH)
    println(messageSource.getMessage("powered-by", null, Locale.ENGLISH))
    println(messageSource.getMessage("powered-by", null, Locale.FRENCH))
    return messageSource
  }

  @Bean("emailTemplateEngine")
  fun templateEngine(
    @Qualifier("emailTemplateResolver") templateResolver: ITemplateResolver,
    @Qualifier("emailMessageSource") messageSource: MessageSource,
  ): TemplateEngine {
    val templateEngine = SpringTemplateEngine()
    templateEngine.templateResolvers = setOf(templateResolver)
    templateEngine.setTemplateEngineMessageSource(messageSource)
    return templateEngine
  }
}
