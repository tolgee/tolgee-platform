package io.tolgee.testing.mocking

import org.mockito.Mockito
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener
import java.util.*

class MockWrappedBeanResetTestExecutionListener : AbstractTestExecutionListener() {
  private val beanNamesCache = Collections.synchronizedMap(IdentityHashMap<ApplicationContext, List<String>>())

  override fun beforeTestMethod(testContext: TestContext) {
    Mockito.reset(
      *getBeansForCurrentContext(testContext.applicationContext)
    )
  }

  override fun afterTestMethod(testContext: TestContext) {
    Mockito.reset(
      *getBeansForCurrentContext(testContext.applicationContext)
    )
  }

  private fun getBeansForCurrentContext(applicationContext: ApplicationContext): Array<Any> {
    val beanNames = getBeanNamesForCurrentContext(applicationContext)
    return beanNames.map { name ->
      applicationContext.getBean(name)
    }.toTypedArray()
  }

  private fun getBeanNamesForCurrentContext(applicationContext: ApplicationContext): List<String> {
    return beanNamesCache.computeIfAbsent(
      applicationContext
    ) { c: ApplicationContext? ->
      findMockWrappedBeanNames(
        applicationContext
      )
    }
  }

  private fun findMockWrappedBeanNames(
    applicationContext: ApplicationContext
  ): List<String> {
    return listOf(*applicationContext.getBeanNamesForAnnotation(MockWrappedBean::class.java))
  }
}
