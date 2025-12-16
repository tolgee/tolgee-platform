# AGENTS.md

This file provides Tolgee-specific guidance for AI coding agents working on the Tolgee localization platform.

## Backend Development

### Database Migrations
After modifying JPA entities, always run:
```bash
./gradlew diffChangeLog
```
This generates Liquibase changelog entries. If you get "docker command not found", add `--no-daemon` flag.

### Backend Testing
Tests are split into multiple categories that run in parallel in CI:
```bash
./gradlew server-app:runContextRecreatingTests && \
./gradlew server-app:runStandardTests && \
./gradlew server-app:runWebsocketTests && \
./gradlew server-app:runWithoutEeTests && \
./gradlew ee-test:test && \
./gradlew data:test && \
./gradlew security:test
```

Don't use the bare test task (it doesn't work) – always run a specific test suite even when running a single test, e.g:
```bash
# Don't do this
./gradlew test --tests "io.tolgee.unit.formats.android.out.AndroidSdkFileExporterTest"

# Do this
./gradlew :data:test --tests "io.tolgee.unit.formats.android.out.AndroidSdkFileExporterTest"
```

**TestData Pattern**: Use TestData classes for test setup:
```kotlin
class YourControllerTest {
  @Autowired
  lateinit var testDataService: TestDataService

  lateinit var testData: YourTestData

  @BeforeEach
  fun setup() {
    testData = YourTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }
}
```

**JSON Response Testing**: Use `.andAssertThatJson` for API responses:
```kotlin
performProjectAuthGet("items").andAssertThatJson {
  node("_embedded.items") {
    node("[0].id").isEqualTo(1)
    node("[0].name").isEqualTo("Item name")
  }
  node("page.totalElements").isNumber.isEqualTo(BigDecimal(2))
}
```

### Code Formatting
Always run before commits:
```bash
./gradlew ktlintFormat
```

## Frontend Development

### Path Aliases
Tolgee uses custom TypeScript path aliases instead of relative imports:
- `tg.component/*` → `component/*`
- `tg.service/*` → `service/*`
- `tg.hooks/*` → `hooks/*`
- `tg.views/*` → `views/*`
- `tg.globalContext/*` → `globalContext/*`

Example: `import { useUser } from 'tg.hooks/useUser'`

### API Schema Regeneration
After backend API changes, regenerate TypeScript types. **Backend must be running first**:
```bash
# 1. Start backend (in separate terminal)
./gradlew server-app:bootRun --args='--spring.profiles.active=dev'

# 2. Regenerate schemas
cd webapp
npm run schema        # For main API
npm run billing-schema # For billing API (if applicable)
```

### API Communication
Use typed React Query hooks from `useQueryApi.ts` (not raw React Query):
```typescript
// Query example
const { data, isLoading } = useApiQuery({
  url: '/v2/projects/{projectId}/languages',
  method: 'get',
  path: { projectId: project.id },
});

// Mutation example
const mutation = useApiMutation({
  url: '/v2/projects/{projectId}/languages',
  method: 'post',
  invalidatePrefix: '/v2/projects',
});

const handleSubmit = (data) => {
  mutation.mutate({
    path: { projectId: project.id },
    content: data,
  });
};
```

### Business Event Tracking
Use Tolgee-specific hooks for analytics:
```typescript
import { useReportEvent } from 'tg.hooks/useReportEvent';

const reportEvent = useReportEvent();
reportEvent('event_name', { key: 'value' });

// For component mount events:
import { useReportOnce } from 'tg.hooks/useReportEvent';
useReportOnce('page_viewed', { pageName: 'settings' });
```

## Testing

### E2E Test Data Setup
Creating E2E test data requires **3 components**:

1. **TestData Class** (`backend/data/src/main/kotlin/io/tolgee/development/testDataBuilder/data/YourFeatureTestData.kt`):
```kotlin
class YourTestData : BaseTestData() {
  val specificEntity: Entity

  init {
    root.apply {
      specificEntity = addEntity {
        name = "Test Entity"
      }.self
    }
  }
}
```

2. **E2E Data Controller** (`backend/development/src/main/kotlin/io/tolgee/controllers/internal/e2eData/YourFeatureE2eDataController.kt`):
```kotlin
@RestController
@RequestMapping("/api/internal/e2e-data/your-feature")
class YourFeatureE2eDataController : AbstractE2eDataController() {
  // Implement data generation endpoints
}
```

3. **Frontend Test Data Object** (`e2e/cypress/common/apiCalls/testData/testData.ts`):
```typescript
export const yourFeatureTestData = generateTestDataObject('your-feature');
```

Usage in tests:
```typescript
beforeEach(() => {
  yourFeatureTestData.clean();
  yourFeatureTestData.generateStandard().then((r) => {
    const testData = r.body;
    // Use testData in your tests
  });
});
```

**Note**: Use `generateStandard()`, not `generate()` (outdated pattern).

### data-cy Attributes (CRITICAL)
**STRICTLY ENFORCED**: Always use `data-cy` attributes for selectors, never text content.

- All data-cy values are typed in `e2e/cypress/support/dataCyType.d.ts` (auto-generated, don't modify)
- Use typed helpers: `gcy('...')` or `cy.gcy('...')`
- Add data-cy to all components accessed from tests
- Make data-cy attributes specific and descriptive

Example:
```tsx
// Component
<Alert severity="error" data-cy="signup-error-seats-spending-limit">
  <T keyName="spending_limit_dialog_title" />
</Alert>

// Test (GOOD)
gcy('signup-error-seats-spending-limit').should('be.visible');

// Test (BAD - don't use text content)
cy.contains('exceeded').should('be.visible');
```

### Error Codes
Backend error codes use `Message.kt` enum, converted to **lowercase** when sent to frontend:
```typescript
cy.intercept('POST', '/v2/projects/*/keys*', {
  statusCode: 400,
  body: {
    code: 'plan_key_limit_exceeded',  // lowercase
    params: [1000, 1001],
  },
}).as('createKey');
```

## Git Workflow

### Branch Naming
Format: `firstname-lastname/feature-description`

Generate name from git config:
```bash
git config get user.name | awk '{print $1, $2}' | \
  iconv -f UTF-8 -t ASCII//TRANSLIT | \
  tr -cd '[:alpha:]' | tr '[:upper:]' '[:lower:]'
```

### Commit Message Prefixes
- `feat:` - Breaking changes or new features
- `fix:` - Non-breaking bug fixes
- `chore:` - Non-behavior changes (docs, tests, formatting)

Example: `feat: add CSV export feature`

## Critical Quirks

### Translation Keys
**NEVER** update translation files with new keys manually. Translation keys are automatically added to files after your changes are merged to the main branch. Freely use nonexistent keys in code - they'll be handled outside the codebase.
