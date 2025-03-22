# Tolgee Testing Guide

This guide explains how to write efficient tests for Tolgee using our optimized test infrastructure.

## Test Types

### Repository Tests

Use `RepositoryTest` as the base class for tests that only need to interact with repositories:

```kotlin
class MyRepositoryTest : RepositoryTest() {
    @Autowired
    private lateinit var myRepository: MyRepository
    
    @Test
    fun testRepositoryOperation() {
        // Test code...
    }
}
```

These tests load only the repository layer, resulting in much faster context loading.

### Lightweight Integration Tests

Use `LightweightIntegrationTest` for tests that need services but not the full application context:

```kotlin
class MyServiceTest : LightweightIntegrationTest() {
    @Autowired
    private lateinit var myService: MyService
    
    @Test
    fun testServiceOperation() {
        // Test code...
    }
}
```

These tests load a minimal context, significantly reducing startup time.

### Full Integration Tests

For tests that need the full application context, use `AbstractSpringTest`:

```kotlin
class MyFullIntegrationTest : AbstractSpringTest() {
    // Test code...
}
```

## Best Practices

1. **Use Isolated Test Data**: Create unique test data for each test to avoid conflicts.
   
   ```kotlin
   val uniqueName = "Test-${UUID.randomUUID()}"
   ```

2. **Avoid Database Truncation**: The new test infrastructure is designed to avoid the need for database truncation between tests.

3. **Use Transactions**: Tests are automatically wrapped in transactions that are rolled back after each test.

4. **Parallel Execution**: Tests are configured to run in parallel. Ensure your tests don't have side effects that would cause conflicts.

5. **Choose the Right Base Class**: Use the most lightweight base class that meets your needs to minimize context loading time.

## Running Tests

To run all tests:
```bash
./gradlew test
```

To run a specific test:
```bash
./gradlew test --tests "io.tolgee.example.MyTest"
```

To run tests with debug output:
```bash
./gradlew test -Dorg.gradle.logging.level=info
``` 