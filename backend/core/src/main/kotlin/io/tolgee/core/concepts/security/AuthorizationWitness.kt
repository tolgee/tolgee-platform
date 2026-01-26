package io.tolgee.core.concepts.security

/**
 * A marker interface representing proof of authorization to execute a [io.tolgee.core.concepts.architecture.Scenario].
 *
 * ## The Scenario/Witness Pattern
 *
 * A Scenario cannot be executed without a Witness object. The key invariant is:
 * **Only the Scenario itself can determine how its Witness is constructed.**
 *
 * This ensures authorization logic is fully encapsulated within the Scenario class. Each Scenario defines:
 * 1. A nested `Witness` class with an `internal constructor` implementing [AuthorizationWitness]
 * 2. An `authorize()` method that returns a `Witness` instance
 *
 * Example:
 * ```kotlin
 * @ScenarioService
 * class MyScenario : Scenario<Input, Output, Witness> {
 *     class Witness internal constructor() : AuthorizationWitness
 *
 *     fun authorize(/* authorization inputs */): Witness {
 *         // Validate authorization, then return proof
 *         return Witness()
 *     }
 *
 *     override fun Witness.execute(input: Input): Output { ... }
 * }
 * ```
 *
 * ## Enforcement
 *
 * Two layers of protection ensure only the Scenario's `authorize()` method can create Witness instances:
 *
 * 1. **Cross-module protection (compile-time):** The `internal` constructor prevents code in other
 *    Gradle modules from instantiating Witness. Kotlin enforces this at compile time.
 *
 * 2. **Intra-module protection (test-time):** Architecture tests using ArchUnit verify at the
 *    bytecode level that Witness constructors are only called from the `authorize()` method on
 *    their enclosing Scenario class. This prevents other code within the same module from
 *    bypassing authorization.
 *
 * @see io.tolgee.architecture.ScenarioArchitectureTest
 */
interface AuthorizationWitness
