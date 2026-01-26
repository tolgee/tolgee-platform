package io.tolgee.architecture

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import io.tolgee.core.concepts.architecture.Scenario
import io.tolgee.core.concepts.security.AuthorizationWitness
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Architecture tests for the Scenario/Proof pattern using ArchUnit.
 *
 * These tests verify at the bytecode level that:
 * 1. Every Scenario has a nested Proof class
 * 2. Every Proof class implements AuthorizationProof
 * 3. Proof constructors are only called from the authorize() method (intra-module protection)
 *
 * Note: Cross-module protection is handled by Kotlin's `internal` visibility modifier on the
 * constructor, which is enforced at compile time. The [internal] visibility is encoded in Kotlin
 * metadata (not bytecode visibility), so we rely on the Kotlin compiler to enforce it.
 */
internal class ScenarioArchitectureTest {
  private val importedClasses by lazy {
    ClassFileImporter()
      .withImportOption(ImportOption.DoNotIncludeTests())
      .importPackages("io.tolgee")
  }

  private val scenarioClasses by lazy {
    importedClasses
      .filter { it.isAssignableTo(Scenario::class.java) && !it.isInterface }
  }

  private val proofClasses by lazy {
    importedClasses
      .filter { it.isAssignableTo(AuthorizationWitness::class.java) && !it.isInterface }
  }

  @Test
  fun `all Scenario implementations should have a nested Proof class`() {
    assertThat(scenarioClasses).isNotEmpty

    scenarioClasses.forEach { scenarioClass ->
      val proofClass = scenarioClass.allSubclasses.find { it.simpleName == "Proof" }
        ?: importedClasses.find { it.name == "${scenarioClass.name}\$Proof" }

      assertThat(proofClass)
        .withFailMessage { "Scenario '${scenarioClass.simpleName}' should have a nested 'Proof' class" }
        .isNotNull
    }
  }

  @Test
  fun `all Proof classes should implement AuthorizationProof`() {
    scenarioClasses.forEach { scenarioClass ->
      val proofClass = importedClasses.find { it.name == "${scenarioClass.name}\$Proof" }

      if (proofClass != null) {
        assertThat(proofClass.isAssignableTo(AuthorizationWitness::class.java))
          .withFailMessage {
            "Proof class in '${scenarioClass.simpleName}' should implement AuthorizationProof"
          }
          .isTrue
      }
    }
  }

  @Test
  fun `Proof constructors should only be called from authorize method on their owning Scenario`() {
    assertThat(proofClasses).isNotEmpty

    classes()
      .that().implement(AuthorizationWitness::class.java)
      .should(onlyBeInstantiatedByAuthorizeMethodOnEnclosingClass())
      .check(importedClasses)
  }

  private fun onlyBeInstantiatedByAuthorizeMethodOnEnclosingClass() =
    object : ArchCondition<JavaClass>(
      "only be instantiated by the authorize() method on their enclosing class",
    ) {
      override fun check(
        proofClass: JavaClass,
        events: ConditionEvents,
      ) {
        val enclosingClass = proofClass.enclosingClass.orElse(null)

        proofClass.constructorCallsToSelf.forEach { call ->
          val callerClass = call.originOwner
          val callerMethod = call.origin.name

          // Kotlin mangles method names when value class parameters are used (e.g., "authorize-wQ38nww")
          val isValidCall = callerClass == enclosingClass && callerMethod.startsWith("authorize")

          if (!isValidCall) {
            events.add(
              SimpleConditionEvent.violated(
                proofClass,
                "${proofClass.name} constructor called from ${callerClass.name}.$callerMethod(), " +
                  "but should only be called from ${enclosingClass?.name}.authorize()",
              ),
            )
          }
        }

        // If no calls, that's fine - it means no violations
        if (proofClass.constructorCallsToSelf.isEmpty() && enclosingClass != null) {
          events.add(SimpleConditionEvent.satisfied(proofClass, "No constructor calls found"))
        }
      }
    }
}
