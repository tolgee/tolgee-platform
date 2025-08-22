# Junie Guidelines for Tolgee Platform

## Introduction

Welcome to the Junie Guidelines for the Tolgee Platform!
This document is designed to help new contributors and junior developers get started with contributing to the Tolgee project.
It provides a set of best practices, coding standards, and workflows to follow when working on the Tolgee codebase.

## Getting Started

Before you begin contributing to Tolgee, make sure you have:

1. Read the [README.md](../README.md) to understand what Tolgee is and its core features
2. Followed the setup instructions in [DEVELOPMENT.md](../DEVELOPMENT.md) to set up your development environment
3. Familiarized yourself with the [CONTRIBUTING.md](../CONTRIBUTING.md) guidelines

## Project Structure

Tolgee is organized into several key components:

- **Backend**: Kotlin-based Spring Boot application
  - Located in the `/backend` directory
  - Uses Gradle for build management
  - Follows a modular architecture with components like:
    - `/api`: Contains API controllers and DTOs
    - `/app`: Main application module
    - `/data`: Data models and repositories
    - `/security`: Authentication and authorization
    - `/testing`: Testing utilities

- **Frontend (Webapp)**: React-based web application
  - Located in the `/webapp` directory
  - Uses npm for package management
  - Built with TypeScript, Material-UI, and React

- **E2E Tests**: End-to-end tests for the application
  - Located in the `/e2e` directory
  - Uses Cypress for testing

- **Enterprise Edition (EE)**: Enterprise features
  - Located in the `/ee` directory
  - Contains both backend and frontend components

- **Billing**: Closed-source billing components
  - Located in the `../billing` directory (outside the public repository)
  - Contains billing-related functionality
  - Not always available in local development environments

## Coding Standards

### Backend (Kotlin)

1. **Code Style**: Follow the Kotlin coding conventions as enforced by ktlint
   - Run `./gradlew ktlintFormat` before committing to ensure proper formatting

2. **Package Structure**:
   - Use the `io.tolgee` package namespace
   - Organize code by feature rather than by layer

3. **Testing**:
   - Write unit tests for all new functionality
   - Integration tests should be used for testing complex interactions
   - **Do not run** all tests locally - like `./gradlew test` as it takes too long;
     instead, push changes and let the CI pipeline run tests
     or run a single test file locally to test specific feature you have just modified
   - While writing tests, if you encounter a code 500
     or some function crashing without a good reason,
     don't be afraid to fix the underlying issue;
     don't just modify the test to make it pass if the behavior is clearly wrong
   - Avoid repeating yourself—if every test in the file needs a special piece of code to test something,
     maybe it is time to implement a helper function for it;
     Try to keep these helper functions outside the tests themselves so they can be reused between different test files
   - Use TestData classes for setting up test data:
     ```kotlin
     // Example of using a TestData class in a test
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
         userAccount = null
       }
     }
     ```
   - Extend existing TestData classes for specific test scenarios:
     ```kotlin
     // Example of creating a custom TestData class
     class YourTestData : BaseTestData() {
       val specificEntity: Entity

       init {
         root.apply {
           specificEntity = addEntity {
             name = "Test Entity"
             // Set other properties
           }.self
         }
       }
     }
     ```

   - **Creating Test Data for E2E Tests**:
     - Test data for E2E tests consists of three main components:
       1. **TestData Class**: Defines the data structure and relationships
       2. **E2E Data Controller**: Exposes endpoints to generate and clean test data
       3. **Frontend Test Data Object**: References the backend endpoints in Cypress tests

     - **Step 1: Create a TestData Class**
       - Location: `/backend/data/src/main/kotlin/io/tolgee/development/testDataBuilder/data/`
       - Naming Convention: `YourFeatureTestData.kt`
       - Example files for reference:
         - `BaseTestData.kt` - Basic structure with a project and user
         - `EmptyProjectTestData.kt` - Simple test data with an empty project
         - `TranslationsTestData.kt` - Complex test data with translations
         - `SelfHostedLimitsTestData.kt` - Test data for self-hosted limits

     - **Step 2: Create an E2E Data Controller**
       - Location: `/backend/development/src/main/kotlin/io/tolgee/controllers/internal/e2eData/`
       - Naming Convention: `YourFeatureE2eDataController.kt`
       - Example files for reference:
         - `AbstractE2eDataController.kt` - Base controller with common functionality
         - `EmptyProjectE2eDataController.kt` - Simple controller
         - `TranslationsE2eDataController.kt` - Controller with additional endpoints
         - `SelfHostedLimitsE2eDataController.kt` - Controller for self-hosted limits
         - 
     - **Step 3: Update the Frontend Test Data Object**
       - Location: `/e2e/cypress/common/apiCalls/testData/testData.ts`
       - Add a new test data object that references your backend endpoints:
         ```typescript
         export const yourFeatureTestData = generateTestDataObject('your-feature');
         ```
       - The string parameter should match the path in your E2E Data Controller's RequestMapping

     - **Step 4: Use the Test Data in Cypress Tests**
       - Location: `/e2e/cypress/e2e/your-feature.cy.ts`
       - Example usage:
         ```typescript
         import { yourFeatureTestData } from '../common/apiCalls/testData/testData';

         describe('Your Feature', () => {
           beforeEach(() => {
             yourFeatureTestData.clean();
             yourFeatureTestData.generateStandard().then((r) => {
               const testData = r.body;
               const projectId = testData.projects[0].id;
               // Use the test data in your tests
             });
           });

           it('tests something', () => {
             // Test implementation
           });
         });
         ```
       - Avoid using `yourFeatureTestData.generate()` function for new tests;
         it is used in outdated tests, but new tests should be using the standard one - `yourFeatureTestData.generateStandard()`
   - Use `.andAssertThatJson` for testing JSON responses:
     ```kotlin
     // Example of testing JSON responses
     @Test
     fun `returns list of items`() {
       performProjectAuthGet("items").andAssertThatJson {
         node("_embedded.items") {
           node("[0].id").isEqualTo(1)
           node("[0].name").isEqualTo("Item name")
         }
         node("page.totalElements").isNumber.isEqualTo(BigDecimal(2))
       }
     }
     ```
   - For more examples of JSON testing, see:
     - `TaskControllerTest.kt` - Testing complex JSON responses
     - `V2LanguagesController.kt` - Testing API endpoints
     - `AbstractControllerTest.kt` - Base class for controller tests

4. **Documentation**:
   - Document all public APIs with KDoc comments
   - Include examples where appropriate
   - Use SpringDoc OpenAPI for REST API documentation:
     ```kotlin
     // Example of documenting a REST controller
     @RestController
     @RequestMapping("/v2/your-endpoint")
     @Tag(name = "Your Feature", description = "Description of your feature")
     @OpenApiOrderExtension(1) // Controls order in documentation
     class YourController {

       @GetMapping("")
       @Operation(summary = "Get all items")
       @UseDefaultPermissions
       @AllowApiAccess
       @OpenApiOrderExtension(1)
       fun getAll(
         @ParameterObject pageable: Pageable,
         @ParameterObject filters: YourFilters
       ): PagedModel<YourModel> {
         // Implementation
       }

       @PostMapping("")
       @Operation(summary = "Create an item")
       @RequiresProjectPermissions([Scope.YOUR_PERMISSION])
       @AllowApiAccess
       @OpenApiOrderExtension(2)
       fun create(
         @RequestBody @Valid dto: YourRequest
       ): YourModel {
         // Implementation
       }
     }
     ```
   - Always document:
     - Controller classes with @Tag
     - Methods with @Operation
     - Request parameters with @ParameterObject when appropriate
     - Required permissions with @RequiresProjectPermissions or @UseDefaultPermissions

### Frontend (TypeScript/React)

1. **Code Style**: Follow the project's ESLint and Prettier configurations
   - Run `npm run prettier` and `npm run eslint` before committing

2. **Component Structure**:
   - Use functional components with hooks
   - Keep components small and focused on a single responsibility
   - Use TypeScript interfaces to define props

3. **State Management**:
   - Use React Query for API data fetching and caching
   - Context is not used extensively in the project
   - Avoid prop drilling when possible

4. **API Communication**:
   - Use the typed React Query hooks from `useQueryApi.ts`:
     ```typescript
     // Example of using useApiQuery
     const { data, isLoading } = useApiQuery({
       url: '/v2/projects/{projectId}/languages',
       method: 'get',
       path: { projectId: project.id },
     });

     // Example of using useApiMutation
     const mutation = useApiMutation({
       url: '/v2/projects/{projectId}/languages',
       method: 'post',
       invalidatePrefix: '/v2/projects',
     });

     // Then use it in a component
     const handleSubmit = (data) => {
       mutation.mutate({
         path: { projectId: project.id },
         content: data,
       });
     };
     ```
   - These hooks provide type safety based on the API schema

5. **E2E Testing**:
   - Currently, not many unit tests are written for frontend
   - Component testing with Testing Library is not yet implemented
   - For E2E tests with Cypress:
     - Always use data-cy attributes for selecting elements instead of text content
     - Add missing data-cy to all components that you need to access from tests
     - Make data-cy attributes as specific as possible to uniquely identify elements
     - Avoid using `first()` to select first item from list, use `filter()` with known content instead
     - Avoid writing comments that explain following line of code without adding any additional context (they only make the code longer)
     - For specific UI components, use descriptive names that indicate the component's purpose:
       ```tsx
       <Alert severity="error" data-cy="signup-error-seats-spending-limit">
         <Typography variant="h5" sx={{ mb: 1 }}>
           <T keyName="spending_limit_dialog_title" />
         </Typography>
       </Alert>
       ```
     - Example of using data-cy selector in a test:
       ```typescript
       // Bad practice - using text content which can change with localization
       cy.contains('exceeded').should('be.visible');

       // Good practice - using specific data-cy selector
       gcy('error-message-seats_spending_limit_exceeded').should('be.visible');
       ```
     - This file contains all the dataCy possible values. Don't use other.
       The file is autogenerated from the source. Don't modify it.
       e2e/cypress/support/dataCyType.d.ts
     - there are cypress ways how to get typed data cy `gcy(...)` or `cy.gcy(...)`

6. **Error Handling**:
   - Use error codes from `io.tolgee.constants.Message` enum in lowercase format
   - All backend error codes are defined in this enum and converted to lowercase when sent to the frontend
   - Example of using error codes in tests:
     ```typescript
     // Example of mocking an error response in Cypress tests
     cy.intercept('POST', '/v2/projects/*/keys*', {
       statusCode: 400,
       body: {
         code: 'plan_key_limit_exceeded',
         params: [1000, 1001],
       },
     }).as('createKey');
     ```

## Workflow Guidelines

### Git Workflow

1. **Branching**:
   - Create a new branch for each feature or bug fix
   - Use a descriptive name that reflects the purpose of the branch
   - Format: `developername/feature-name` (e.g., `john/add-export-feature`)
   - Use this command to get `fullname` for branch name:
     `git config get user.name | awk '{print $1, $2}' | iconv -f UTF-8 -t ASCII//TRANSLIT | tr -cd '[:alpha:]' | tr '[:upper:]' '[:lower:]'`

2. **Commits**:
   - Use the conventional commits format
   - Format: `type: message` (e.g., `feat: add CSV export feature`)
   - Common types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
   - Keep commits focused and atomic

## Common Pitfalls and How to Avoid Them

1. **Database Migrations**:
   - Always use Liquibase for database changes
   - Generate changelog entries with `./gradlew diffChangeLog`
   - Test migrations thoroughly before submitting

2. **API Changes**:
   - Document all API changes
   - Consider backward compatibility

3. **Performance Considerations**:
   - Be mindful of database query performance
   - Use pagination for large data sets
   - Consider caching for frequently accessed data

4. **Translation keys**:
   - Never update translation files with new keys
   - Freely use nonexistent translation keys in the codebase,
     their translation will be handled outside the codebase,
     and the keys will be added to the translation files automatically after the changes are merged to the main branch
