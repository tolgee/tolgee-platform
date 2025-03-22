package io.tolgee.configuration

import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

/**
 * Bean post processor that helps optimize test context loading
 * by selectively enabling or disabling beans during testing.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class TestBeanPostProcessor : BeanPostProcessor {
    
    private val excludedBeanTypes = setOf(
        // List bean types that should be excluded from test context
        // For example, email senders, external API clients, etc.
        "io.tolgee.component.emailSender.EmailSender",
        "io.tolgee.component.contentDelivery.ContentDeliveryUploader",
        "io.tolgee.component.fileStorage.FileStorage"
    )
    
    @Throws(BeansException::class)
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {
        // Skip initialization of beans that are not needed for tests
        val beanType = bean.javaClass.name
        if (excludedBeanTypes.any { beanType.startsWith(it) }) {
            return null
        }
        return bean
    }
    
    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        return bean
    }
} 