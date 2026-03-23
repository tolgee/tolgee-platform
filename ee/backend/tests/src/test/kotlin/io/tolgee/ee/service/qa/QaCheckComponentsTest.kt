package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.stereotype.Component
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

class QaCheckComponentsTest {
  @Test
  fun `each QaCheckType has exactly one QaCheck component`() {
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AssignableTypeFilter(QaCheck::class.java))
    val candidates = scanner.findCandidateComponents("io.tolgee.ee.service.qa")

    val componentClasses = candidates.map { Class.forName(it.beanClassName) }

    // Every QaCheck implementation must be a Spring-managed component
    componentClasses.forEach { clazz ->
      assertThat(AnnotatedElementUtils.hasAnnotation(clazz, Component::class.java))
        .`as`("${clazz.simpleName} must be annotated with a Spring stereotype (@Component, @Service, etc.)")
        .isTrue()
    }

    val checkTypes = componentClasses.map { instantiateCheck(it).type }

    val missing = QaCheckType.entries.toSet() - checkTypes.toSet()
    val extra = checkTypes.toSet() - QaCheckType.entries.toSet()

    assertThat(missing)
      .`as`("QaCheckTypes missing a QaCheck component")
      .isEmpty()

    assertThat(extra)
      .`as`("QaCheck components with unknown QaCheckType")
      .isEmpty()

    val duplicates = checkTypes.groupingBy { it }.eachCount().filter { it.value > 1 }
    assertThat(duplicates)
      .`as`("QaCheckTypes with multiple QaCheck components")
      .isEmpty()
  }

  private fun instantiateCheck(clazz: Class<*>): QaCheck {
    val primaryConstructor = clazz.kotlin.primaryConstructor
    if (primaryConstructor == null || primaryConstructor.parameters.isEmpty()) {
      return clazz.getDeclaredConstructor().newInstance() as QaCheck
    }
    val javaConstructor =
      primaryConstructor.javaConstructor
        ?: error("Cannot resolve Java constructor for ${clazz.simpleName}")
    val args = javaConstructor.parameterTypes.map { Mockito.mock(it) }.toTypedArray()
    return javaConstructor.newInstance(*args) as QaCheck
  }
}
