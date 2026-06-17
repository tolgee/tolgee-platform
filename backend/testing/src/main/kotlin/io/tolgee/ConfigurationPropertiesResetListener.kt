package io.tolgee

import io.tolgee.testing.ConfigurationPropertiesSnapshot
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.util.ClassUtils

class ConfigurationPropertiesResetListener : TestExecutionListener {
  override fun beforeTestMethod(testContext: TestContext) {
    val snapshots = rootBeans(testContext).mapValues { (name, bean) -> snapshotBean(name, bean) }
    testContext.setAttribute(SNAPSHOTS_ATTRIBUTE, snapshots)
  }

  internal fun snapshotBean(
    name: String,
    bean: Any,
  ): String =
    try {
      ConfigurationPropertiesSnapshot.snapshot(bean)
    } catch (e: Exception) {
      throw IllegalStateException(
        "${javaClass.simpleName} could not snapshot @ConfigurationProperties bean '$name' " +
          "(${bean.javaClass.name}). A field on it is not Jackson-serializable — annotate it " +
          "@JsonIgnore or make it serializable.",
        e,
      )
    }

  override fun afterTestMethod(testContext: TestContext) {
    @Suppress("UNCHECKED_CAST")
    val snapshots = testContext.getAttribute(SNAPSHOTS_ATTRIBUTE) as? Map<String, String> ?: return
    val beans = rootBeans(testContext)
    snapshots.forEach { (name, snapshot) ->
      beans[name]?.let { restoreBean(name, it, snapshot) }
    }
  }

  internal fun restoreBean(
    name: String,
    bean: Any,
    snapshot: String,
  ) {
    try {
      ConfigurationPropertiesSnapshot.restore(bean, snapshot)
    } catch (e: Exception) {
      throw IllegalStateException(
        "${javaClass.simpleName} could not restore @ConfigurationProperties bean '$name' " +
          "(${bean.javaClass.name}). Its snapshot did not deserialize back into the live instance.",
        e,
      )
    }
  }

  private fun rootBeans(testContext: TestContext): Map<String, Any> =
    testContext.applicationContext
      .getBeansWithAnnotation(ConfigurationProperties::class.java)
      .filterValues { isResettableRoot(it) }

  /**
   * Our `@ConfigurationProperties` root holder beans. Restricted to `io.tolgee` classes so framework
   * roots (e.g. Spring Boot's `SslProperties`, which holds types our mapper can't serialize) are left
   * alone, and to beans whose own class carries the annotation so infrastructure beans that get it from
   * a `@Bean` factory method (e.g. the `spring.datasource` DataSource) are excluded.
   */
  internal fun isResettableRoot(bean: Any): Boolean {
    val userClass = ClassUtils.getUserClass(bean)
    return userClass.name.startsWith("io.tolgee.") &&
      userClass.isAnnotationPresent(ConfigurationProperties::class.java)
  }

  companion object {
    private const val SNAPSHOTS_ATTRIBUTE = "configurationPropertiesSnapshots"
  }
}
