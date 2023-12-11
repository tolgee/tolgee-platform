package io.tolgee.testing.mocking

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.RuntimeBeanReference
import org.springframework.beans.factory.support.BeanDefinitionRegistry

/**
 * See [MockWrappedBean] for explanation.
 */
class MockWrappedBeanResetBeanProcessor : BeanFactoryPostProcessor {
  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    require(beanFactory is BeanDefinitionRegistry) {
      String.format(
        "Expected beanFactory %s to be instance of %s", beanFactory.javaClass,
        BeanDefinitionRegistry::class.java
      )
    }
    for (mockWrapperBeanName in beanFactory.getBeanNamesForAnnotation(MockWrappedBean::class.java)) {
      val mockWrapperBeanDefinition = beanFactory.getBeanDefinition(mockWrapperBeanName)
      check(mockWrapperBeanDefinition is AnnotatedBeanDefinition) {
        "Definition for bean '%s' was expected to be of type %s, but %s found".formatted(
          mockWrapperBeanName,
          AnnotatedBeanDefinition::class.java, mockWrapperBeanDefinition.javaClass
        )
      }
      val realImplCandidateName =
        getRealImplCandidateNameNaively(beanFactory, mockWrapperBeanName, mockWrapperBeanDefinition)
      val realImplBeanDefinition = beanFactory.getBeanDefinition(realImplCandidateName)

      val realImplCandidateNewName = realImplCandidateName + "_real"

      realImplBeanDefinition.isAutowireCandidate = false

      beanFactory.removeBeanDefinition(realImplCandidateName)
      beanFactory.removeBeanDefinition(mockWrapperBeanName)

      mockWrapperBeanDefinition.isPrimary = realImplBeanDefinition.isPrimary
      beanFactory.registerBeanDefinition(realImplCandidateName, mockWrapperBeanDefinition)
      beanFactory.registerBeanDefinition(realImplCandidateNewName, realImplBeanDefinition)

      // but since we've disabled autowiring, we now have to help Spring find the correct delegate
      mockWrapperBeanDefinition.getConstructorArgumentValues()
        .addGenericArgumentValue(RuntimeBeanReference(realImplCandidateNewName))

      // prevents cyclic dependencies
      mockWrapperBeanDefinition.setDependsOn(realImplCandidateNewName)
    }
  }

  companion object {
    /**
     * This method is most of the reason why this is not a lib yet. Suggestions for improvements are welcome.
     */
    private fun getRealImplCandidateNameNaively(
      beanFactory: ConfigurableListableBeanFactory,
      mockWrapperBeanName: String,
      mockWrapperBeanDefinition: BeanDefinition
    ): String {
      val realImplCandidateNames: MutableSet<String> =
        HashSet(listOf(*beanFactory.getBeanNamesForType(mockWrapperBeanDefinition.resolvableType)))
      realImplCandidateNames.remove(mockWrapperBeanName)

      findExactQualifier(beanFactory, mockWrapperBeanName, realImplCandidateNames)?.let {
        return it
      }

      if (realImplCandidateNames.size > 1) {
        realImplCandidateNames.removeIf {
          !beanFactory.getBeanDefinition(it).isPrimary
        }
      }

      check(realImplCandidateNames.size == 1) {
        "We've tried to naively autowire real impl for bean factory '$mockWrapperBeanName', " +
          "which turned up $realImplCandidateNames, but we need exactly one candidate."
      }

      return realImplCandidateNames.iterator().next()
    }

    private fun findExactQualifier(
      beanFactory: ConfigurableListableBeanFactory,
      mockWrapperBeanName: String,
      realImplCandidateNames: Set<String>
    ): String? {
      val exactQualifier =
        beanFactory.findAnnotationOnBean(mockWrapperBeanName, MockWrappedBean::class.java)?.`for`

      if (exactQualifier.isNullOrBlank()) {
        return null
      }

      if (realImplCandidateNames.contains(exactQualifier)) {
        return exactQualifier
      }

      throw IllegalStateException("Bean with qualifier $exactQualifier not found")
    }
  }
}
