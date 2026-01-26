# Short-Term Observations

Quick dumps of potentially useful observations. To be processed during `/consolidate-memory`.

---

## [2026-01-12 18:05] Codebase: Gradle project structure

Tolgee uses flat Gradle module names, not nested. Use `:core:compileKotlin` not `:backend:core:compileKotlin`. The backend directory structure doesn't map to Gradle project paths.

## [2026-01-12 18:45] Architecture: Interface naming convention in core module

In the `core` module's new architecture, interface names use "I" as a **pronoun** (meaning "me"), not as an abbreviation for "Interface". The pattern is "I <verb> <noun>":
- `IQueryProjects` = "I query projects"
- `IFetchTranslations` = "I fetch translations"
- `IFetchProjects` = "I fetch projects"

This reads as a first-person statement of what the component does. Avoid names like `IProjectQueries` where "I" could be mistaken for "Interface".

## [2026-01-12 20:30] Kotlin: Outer classes cannot access private constructors of nested classes

Unlike Java, in Kotlin outer classes **cannot** access private members (including constructors) of their nested classes. This is true even for `inner class`.

```kotlin
class Outer {
  class Nested private constructor()  // Java: Outer can call this. Kotlin: NO.
  inner class Inner private constructor()  // Still NO access in Kotlin.

  fun create() = Nested()  // Compile error in Kotlin!
}
```

**Note:** This limitation led to using a companion object factory pattern initially, but was later replaced with ArchUnit bytecode-level enforcement (see observation below).

## [2026-01-25 14:30] ArchUnit: Bytecode-level call-graph analysis for constructor enforcement

ArchUnit can trace actual constructor calls at bytecode level using `constructorCallsToSelf`, `originOwner`, and `origin.name`. This is more powerful than source-level analysis (like Konsist) because it verifies actual call sites at method-level granularity.

```kotlin
private fun onlyBeInstantiatedByAuthorizeMethodOnEnclosingClass() = object : ArchCondition<JavaClass>(
  "only be instantiated by the authorize() method on their enclosing class"
) {
  override fun check(proofClass: JavaClass, events: ConditionEvents) {
    val enclosingClass = proofClass.enclosingClass.orElse(null)

    proofClass.constructorCallsToSelf.forEach { call ->
      val callerClass = call.originOwner   // The class containing the call
      val callerMethod = call.origin.name  // The method name containing the call

      val isValidCall = callerClass == enclosingClass && callerMethod == "authorize"
      if (!isValidCall) {
        events.add(SimpleConditionEvent.violated(proofClass, "..."))
      }
    }
  }
}
```

**Design insight:** Instead of separate tests for "method exists" and "constructor called from right class", combine into a single stronger constraint: "constructor called from specific method on specific class". This is tighter - if there's no `authorize()` method calling the Proof constructor, the test fails.

**Key APIs:**
- `JavaClass.constructorCallsToSelf` - all calls to the class's constructors
- `call.originOwner` - the class containing the code that made the call
- `call.origin.name` - the method name containing the call
- `JavaClass.enclosingClass` - the outer class for nested classes

## [2026-01-25 15:00] ArchUnit: importPackages only scans test's classpath in multi-module Gradle

**Gotcha:** `ClassFileImporter().importPackages("io.tolgee")` only imports classes from the test module's classpath, NOT all classes in the project with that package name.

In a multi-module Gradle project:
- Test in `:core` module won't see classes from `:api` module (unless `:api` is a dependency of `:core`)
- A violation in `:api` calling `FetchTranslationComments.Proof()` won't be detected

**Solutions:**
1. Move architecture test to a module that depends on all other modules (e.g., `:server-app`)
2. Create a dedicated architecture test module with dependencies on all modules
3. Configure `ClassFileImporter` with explicit paths to compiled class directories

**Verification:** Always test that architecture tests actually catch violations by temporarily adding a violating call and confirming the test fails.

## [2026-01-25 15:30] Kotlin: internal visibility NOT always enforced via DefaultConstructorMarker

**Gotcha:** Kotlin's `internal` visibility for **parameterless constructors** does NOT add a `DefaultConstructorMarker` parameter at bytecode level. The constructor remains `public` in bytecode with no synthetic parameters.

```kotlin
class Proof internal constructor() : AuthorizationProof
// Bytecode: public Proof() - NO DefaultConstructorMarker!
```

Kotlin enforces `internal` purely through the `@kotlin.Metadata` annotation's encoded visibility info, which the Kotlin compiler reads at compile time. Java code and bytecode tools (like ArchUnit) see a public constructor.

**Implication:** You cannot use ArchUnit to verify Kotlin `internal` visibility by checking for `DefaultConstructorMarker`. Rely on Kotlin compiler enforcement instead.

## [2026-01-25 15:35] Pattern: Two-layer protection for constructor access control

When you need to restrict constructor access in a multi-module Kotlin project:

1. **Cross-module (compile-time):** Use `internal constructor()` - Kotlin compiler prevents other modules from calling it
2. **Intra-module (test-time):** Use ArchUnit call-site verification - ensures only specific methods can instantiate

```kotlin
// In :core module
class FetchTranslationComments {
  class Proof internal constructor() : AuthorizationProof  // Layer 1: cross-module
  fun authorize(): Proof = Proof()  // Only valid call site
}

// ArchUnit test verifies Proof() only called from authorize() - Layer 2: intra-module
```

This combines compile-time safety (Kotlin) with test-time verification (ArchUnit) for comprehensive protection.

## [2026-01-25 17:00] Kotlin: Value class parameters cause method name mangling at bytecode level

**Gotcha:** When a Kotlin function has value class parameters, the method name is mangled at bytecode level with a hash suffix.

```kotlin
// Source code
fun authorize(userId: UserId, projectId: ProjectId): Proof?

// Bytecode method name
authorize-wQ38nww()  // NOT "authorize"!
```

This broke the ArchUnit test that was checking `callerMethod == "authorize"` because the actual bytecode method name includes the hash suffix.

**Fix:** Use `startsWith("authorize")` instead of exact equality when checking method names in ArchUnit tests:

```kotlin
val isValidCall = callerClass == enclosingClass && callerMethod.startsWith("authorize")
```

**Why this happens:** Kotlin mangles method names with value class parameters to preserve type safety when called from Java (since Java doesn't understand value classes). The hash is derived from the parameter types.

## [2026-01-25 17:30] Architecture: Rate limiting has three components, all consume-and-throw

Rate limiting in Tolgee uses three components, all of which do atomic consume-and-throw (not separate increment/evaluate):

1. **`GlobalIpRateLimitFilter`** - Servlet filter for unauthenticated requests, rate limits by IP
2. **`GlobalUserRateLimitFilter`** - Servlet filter for authenticated requests, rate limits by user ID
3. **`RateLimitInterceptor`** - Spring MVC interceptor for per-endpoint `@RateLimited` annotation

All three call `RateLimitService.consumeBucket()` → `doConsumeBucket()` which throws `RateLimitedException` if `bucket.tokens == 0`.

**Key insight:** There is no "GlobalRateLimitFilter" - it's two separate filters. The blocking happens at consume time, not in a separate evaluation step.

## [2026-01-25 18:08] Testing: ProjectAuthControllerTest requires annotation-aware setup

When extending `ProjectAuthControllerTest`, the `projectSupplier` setter triggers `projectAuthRequestPerformer` getter which throws if the test method doesn't have `@ProjectJWTAuthTestMethod` or `@ApiKeyAccessTestMethod`. Tests that don't use project auth (e.g., testing unauthenticated access or custom headers) need conditional setup:

```kotlin
@BeforeEach
fun setup(testInfo: TestInfo) {
  // ... setup test data ...

  val method = testInfo.testMethod.orElse(null)
  if (method?.getAnnotation(ProjectJWTAuthTestMethod::class.java) != null) {
    this.projectSupplier = { testData.project }
    userAccount = testData.user
  }
}
```

## [2026-01-25 18:08] Security: Unauthenticated project access returns 403, not 401

Project-level endpoints in Tolgee return **403 Forbidden** (not 401 Unauthorized) when no authentication is provided. This is consistent across all project resources. The 401 status is reserved for invalid/malformed credentials. See `ProjectApiKeyAuthenticationTest.accessWithApiKey_failure()` for reference.

## [2026-01-25 18:08] Security: PAK mismatch check requires user permission on target project

When testing "PAK for different project" (403 `pak_created_for_different_project`), the PAK owner must have permission on the target project. Otherwise, the permission check fails first with 404 `project_not_found`. To test PAK mismatch:
1. Create user with PAK for Project A
2. Give that user VIEW permission on Project B
3. Use PAK to access Project B → triggers 403 PAK mismatch

## [2026-01-25 18:08] Bug: Sort property validation returns 500 instead of 400

The exception handler in `ExceptionHandlers.kt:230-239` for `InvalidDataAccessApiUsageException` checks for message containing "could not resolve property", but Hibernate 6.x throws `PathElementException` with message "Could not resolve attribute". This causes unknown sort properties to return 500 `unexpected_error_occurred` instead of 400 `unknown_sort_property`. The handler needs updating to match the new Hibernate message format.

## [2026-01-25 18:35] Configuration: JPA repository scanning for io.tolgee.core

When adding new `@Repository` JPA interfaces in `io.tolgee.core`, they must be explicitly included in the `@EnableJpaRepositories` scan in `Application.kt`. The default config only scans `io.tolgee.repository`:

```kotlin
// Before - only scans io.tolgee.repository
@EnableJpaRepositories("io.tolgee.repository")

// After - also scans io.tolgee.core for new domain repositories
@EnableJpaRepositories("io.tolgee.repository", "io.tolgee.core")
```

Without this, Spring fails with `NoSuchBeanDefinitionException` for repository interfaces like `IQueryProjects`, `INameTranslationQueries`, etc. The error manifests as 500 responses in tests with chained "Unsatisfied dependency" exceptions in the bean creation stack.

