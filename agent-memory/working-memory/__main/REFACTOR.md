# Translation Comments: A Vertical Slice of the Tolgee Architecture

This document provides a technical-manager level overview of the **Translation Comments** feature as a representative vertical slice of the Tolgee backend architecture. Use this as a reference for architectural discussions and refactoring planning.

---

## Executive Summary

**What it does**: Translation Comments allow users to leave comments on individual translations within a project. Comments support a resolution workflow (needs resolution → resolved) and integrate with the activity logging system for audit trails.

**Why this feature**: It's a complete vertical slice that demonstrates:
- The standard layered architecture (Controller → Service → Repository)
- Permission model with both ownership-based and scope-based access
- State machine pattern for comment resolution
- Activity logging integration
- HATEOAS response assembly

**Scope**: ~520 lines across 7 core files, making it digestible in a single session.

---

## Component Inventory

### Core Components

| Layer | File | Responsibility |
|-------|------|----------------|
| Controller | `backend/api/.../TranslationCommentController.kt` | REST endpoints, permission checks, request orchestration |
| Service | `backend/data/.../TranslationCommentService.kt` | Business logic, transaction management |
| Repository | `backend/data/.../TranslationCommentRepository.kt` | Data access, JPQL queries |
| Entity | `backend/data/.../TranslationComment.kt` | Domain model, JPA mappings, activity annotations |
| Request DTO | `backend/data/.../TranslationCommentDto.kt` | Input validation |
| Response Model | `backend/api/.../TranslationCommentModel.kt` | API response shape |
| Assembler | `backend/api/.../TranslationCommentModelAssembler.kt` | Entity → Response conversion |
| Enum | `backend/data/.../TranslationCommentState.kt` | State values |

### Full File Paths

```
backend/api/src/main/kotlin/io/tolgee/api/v2/controllers/translation/TranslationCommentController.kt
backend/data/src/main/kotlin/io/tolgee/service/translation/TranslationCommentService.kt
backend/data/src/main/kotlin/io/tolgee/repository/translation/TranslationCommentRepository.kt
backend/data/src/main/kotlin/io/tolgee/model/translation/TranslationComment.kt
backend/data/src/main/kotlin/io/tolgee/dtos/request/translation/comment/TranslationCommentDto.kt
backend/api/src/main/kotlin/io/tolgee/hateoas/translations/comments/TranslationCommentModelAssembler.kt
backend/data/src/main/kotlin/io/tolgee/model/enums/TranslationCommentState.kt
```

### Supporting Components (injected dependencies)

| Component | Purpose |
|-----------|---------|
| `AuthenticationFacade` | Get current authenticated user |
| `SecurityService` | Check project permissions and scopes |
| `TranslationService` | Access parent Translation entity |
| `ProjectHolder` | Get current project context |

---

## Data Model

### Entity Relationships

```
┌─────────────────────┐       ┌─────────────────────┐
│      Project        │       │     UserAccount     │
└──────────┬──────────┘       └──────────┬──────────┘
           │ 1                            │ 1
           │                              │
           │ *                            │ *
┌──────────▼──────────┐       ┌──────────▼──────────┐
│        Key          │       │ TranslationComment  │
└──────────┬──────────┘       │                     │
           │ 1                │  - id: Long         │
           │                  │  - text: String     │
           │ *                │  - state: Enum      │
┌──────────▼──────────┐       │  - createdAt: Date  │
│    Translation      │◄──────│  - updatedAt: Date  │
│                     │   *   │                     │
│  - id: Long         │       └─────────────────────┘
│  - text: String     │
│  - language: Lang   │
└─────────────────────┘
```

### TranslationComment Entity

```kotlin
@Entity
@ActivityLoggedEntity
class TranslationComment(
    var text: String,                        // max 10,000 chars
    var state: TranslationCommentState,      // RESOLUTION_NOT_NEEDED | NEEDS_RESOLUTION | RESOLVED
    var translation: Translation,            // parent relationship
) : StandardAuditModel() {
    lateinit var author: UserAccount         // who created it
}
```

### State Enum

```
┌─────────────────────────┐
│  RESOLUTION_NOT_NEEDED  │  ← Default for informational comments
└─────────────────────────┘

┌─────────────────────────┐
│    NEEDS_RESOLUTION     │  ← Flags comment for follow-up
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│        RESOLVED         │  ← Issue addressed
└─────────────────────────┘
```

---

## Architecture Diagram

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HTTP Request                                 │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    TranslationCommentController                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Annotations:                                                │    │
│  │    @RestController                                           │    │
│  │    @RequestActivity(ActivityType.TRANSLATION_COMMENT_*)      │    │
│  │    @RequiresProjectPermissions / @UseDefaultPermissions      │    │
│  │    @AllowApiAccess                                           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Responsibilities:                                                   │
│    - Route HTTP requests to appropriate methods                      │
│    - Validate path parameters                                        │
│    - Check ownership (author.id == currentUser.id)                   │
│    - Delegate to SecurityService for scope checks                    │
│    - Delegate to Service for business logic                          │
│    - Use Assembler to convert Entity → Response Model                │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    TranslationCommentService                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Annotations:                                                │    │
│  │    @Service                                                  │    │
│  │    @Transactional (on methods)                               │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Responsibilities:                                                   │
│    - Create, update, delete comments                                 │
│    - State transitions                                               │
│    - Transaction boundaries                                          │
│    - Delegate to Repository for data access                          │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  TranslationCommentRepository                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Extends: JpaRepository<TranslationComment, Long>            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Custom Queries:                                                     │
│    - find(projectId, translationId, commentId)                       │
│    - findWithFetchedAuthor(...)  ← Avoids N+1                        │
│    - getPagedByTranslation(translation, pageable)                    │
│    - deleteByTranslationIdIn(ids)                                    │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          PostgreSQL                                  │
│                                                                      │
│  Table: translation_comment                                          │
│    - id, text, state, translation_id, author_id                      │
│    - created_at, updated_at (from StandardAuditModel)                │
│                                                                      │
│  Indexes: state, translation_id, author_id                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Request Flow Diagrams

### 1. Create Comment (POST /translations/{translationId}/comments)

```
Client                  Controller              SecurityService         Service                  Repository
  │                         │                         │                    │                         │
  │  POST with body         │                         │                    │                         │
  ├────────────────────────►│                         │                    │                         │
  │                         │                         │                    │                         │
  │                         │  @RequiresProjectPermissions                 │                         │
  │                         │  [TRANSLATIONS_COMMENTS_ADD]                 │                         │
  │                         │  (checked by framework)  │                    │                         │
  │                         │                         │                    │                         │
  │                         │  translationService.get(projectId, translationId)                      │
  │                         ├────────────────────────────────────────────►│                         │
  │                         │◄────────────────────────────────────────────┤ Translation             │
  │                         │                         │                    │                         │
  │                         │                         │   create(dto, translation, currentUser)      │
  │                         ├────────────────────────────────────────────►│                         │
  │                         │                         │                    │     save(comment)       │
  │                         │                         │                    ├────────────────────────►│
  │                         │                         │                    │◄────────────────────────┤
  │                         │◄────────────────────────────────────────────┤ TranslationComment      │
  │                         │                         │                    │                         │
  │                         │  assembler.toModel(comment)                  │                         │
  │  201 Created            │                         │                    │                         │
  │◄────────────────────────┤                         │                    │                         │
```

### 2. Update Comment (PUT /translations/{translationId}/comments/{commentId})

```
Client                  Controller              AuthFacade              Service
  │                         │                         │                    │
  │  PUT with body          │                         │                    │
  ├────────────────────────►│                         │                    │
  │                         │                         │                    │
  │                         │  getWithAuthorFetched(projectId, transId, commentId)
  │                         ├────────────────────────────────────────────►│
  │                         │◄────────────────────────────────────────────┤ comment
  │                         │                         │                    │
  │                         │  authenticatedUser.id   │                    │
  │                         ├────────────────────────►│                    │
  │                         │◄────────────────────────┤ userId             │
  │                         │                         │                    │
  │                         │                         │                    │
  │                         │  if (comment.author.id != userId)            │
  │                         │      throw BadRequestException               │
  │                         │      "CAN_EDIT_ONLY_OWN_COMMENT"             │
  │                         │                         │                    │
  │                         │                    update(dto, comment)      │
  │                         ├────────────────────────────────────────────►│
  │                         │◄────────────────────────────────────────────┤
  │  200 OK                 │                         │                    │
  │◄────────────────────────┤                         │                    │
```

### 3. Set State (PUT /translations/{translationId}/comments/{commentId}/set-state/{state})

```
Client                  Controller              SecurityService         Service
  │                         │                         │                    │
  │  PUT .../set-state/RESOLVED                       │                    │
  ├────────────────────────►│                         │                    │
  │                         │                         │                    │
  │                         │  translationService.get(translationId)       │
  │                         ├────────────────────────────────────────────►│
  │                         │◄────────────────────────────────────────────┤ translation
  │                         │                         │                    │
  │                         │  checkScopeOrAssignedToTask(               │
  │                         │    TRANSLATIONS_COMMENTS_SET_STATE,         │
  │                         │    translation.language.id,                 │
  │                         │    translation.key.id)  │                    │
  │                         ├────────────────────────►│                    │
  │                         │◄────────────────────────┤ OK or throw        │
  │                         │                         │                    │
  │                         │  getWithAuthorFetched(...)                   │
  │                         ├────────────────────────────────────────────►│
  │                         │◄────────────────────────────────────────────┤ comment
  │                         │                         │                    │
  │                         │                 setState(comment, RESOLVED)  │
  │                         ├────────────────────────────────────────────►│
  │                         │◄────────────────────────────────────────────┤
  │  200 OK                 │                         │                    │
  │◄────────────────────────┤                         │                    │
```

### 4. Delete Comment (ownership OR permission)

```
Client                  Controller              AuthFacade       SecurityService      Service
  │                         │                         │                 │               │
  │  DELETE                 │                         │                 │               │
  ├────────────────────────►│                         │                 │               │
  │                         │                         │                 │               │
  │                         │  get(projectId, transId, commentId)       │               │
  │                         ├──────────────────────────────────────────────────────────►│
  │                         │◄──────────────────────────────────────────────────────────┤
  │                         │                         │                 │               │
  │                         │  Is author == currentUser?                │               │
  │                         │  ┌─────────────────────────────┐          │               │
  │                         │  │ YES: proceed to delete      │          │               │
  │                         │  │ NO:  check edit permission  │          │               │
  │                         │  └─────────────────────────────┘          │               │
  │                         │                         │                 │               │
  │                         │  (if not author) checkProjectPermission   │               │
  │                         │  (TRANSLATIONS_COMMENTS_EDIT)             │               │
  │                         ├────────────────────────────────────────►  │               │
  │                         │◄────────────────────────────────────────  │               │
  │                         │                         │                 │               │
  │                         │                                    delete(comment)        │
  │                         ├──────────────────────────────────────────────────────────►│
  │                         │◄──────────────────────────────────────────────────────────┤
  │  200 OK                 │                         │                 │               │
  │◄────────────────────────┤                         │                 │               │
```

---

## Permission Model

### Scopes Used

| Scope | Operation | Checked Where |
|-------|-----------|---------------|
| `TRANSLATIONS_COMMENTS_ADD` | Create comment | Controller annotation or SecurityService |
| `TRANSLATIONS_COMMENTS_EDIT` | Delete others' comments | Controller (fallback after ownership check) |
| `TRANSLATIONS_COMMENTS_SET_STATE` | Change resolution state | SecurityService.checkScopeOrAssignedToTask |

### Access Logic Summary

| Operation | Can Current User Do It? |
|-----------|------------------------|
| **Read** | Anyone with project access (`@UseDefaultPermissions`) |
| **Create** | Has `TRANSLATIONS_COMMENTS_ADD` scope |
| **Update** | Is the author (ownership-based, no scope check) |
| **Delete** | Is the author OR has `TRANSLATIONS_COMMENTS_EDIT` scope |
| **Set State** | Has `TRANSLATIONS_COMMENTS_SET_STATE` OR assigned to task for that key/language |

---

## Activity Logging

### How It Works

1. Controller methods are annotated with `@RequestActivity(ActivityType.*)`:
   ```kotlin
   @RequestActivity(ActivityType.TRANSLATION_COMMENT_ADD)
   fun create(...) { ... }
   ```

2. The `ActivityDatabaseInterceptor` (Hibernate interceptor) captures entity changes.

3. Activity events are stored and can trigger automations.

### Activity Types for Comments

| Activity Type | Trigger |
|--------------|---------|
| `TRANSLATION_COMMENT_ADD` | New comment created |
| `TRANSLATION_COMMENT_EDIT` | Comment text or state updated by author |
| `TRANSLATION_COMMENT_SET_STATE` | State changed via set-state endpoint |
| `TRANSLATION_COMMENT_DELETE` | Comment deleted |

### Entity Annotations

```kotlin
@ActivityLoggedEntity                           // Mark entity for tracking
@ActivityEntityDescribingPaths(paths = ["translation"])  // Include related entity info
class TranslationComment(
    @ActivityLoggedProp                         // Track changes to this field
    @ActivityDescribingProp                     // Use in activity description
    var text: String,

    @ActivityLoggedProp
    var state: TranslationCommentState,
    ...
)
```

---

## Observations for Refactoring Discussion

### Patterns Worth Noting

1. **Ownership + Scope Fallback**: Delete operation checks ownership first, then falls back to permission check. This pattern appears in update too (but throws instead of falling back).

2. **Controller-Level Permission Logic**: Permission checks happen in the controller, not the service. The service is "dumb" - it just does CRUD.

3. **Two Create Endpoints**: There are two ways to create a comment:
   - `POST /translations/{translationId}/comments` - standard
   - `POST /translations/create-comment` - with inline key+language IDs

4. **Fetch Strategies**: Repository has both `find()` and `findWithFetchedAuthor()` to control eager loading.

5. **Native SQL for Bulk Delete**: `deleteAllByProject` uses native SQL instead of JPQL for performance.

### Potential Pain Points

1. **Permission Logic in Controller**: Business rules about who can do what are split between:
   - `@RequiresProjectPermissions` annotation
   - `@UseDefaultPermissions` + manual checks in method body
   - `SecurityService.checkScopeOrAssignedToTask()`

   This makes it hard to understand the full permission model at a glance.

2. **Exception Type Inconsistency**:
   - Ownership violation throws `BadRequestException` with message `CAN_EDIT_ONLY_OWN_COMMENT`
   - Permission violation throws `PermissionException`
   - These are different HTTP status codes (400 vs 403)

3. **No Service-Level Authorization**: Services assume the caller has permission. All auth is done at controller layer.

4. **Mixed Annotation Styles**: Some endpoints use `@RequiresProjectPermissions`, others use `@UseDefaultPermissions` with manual checks. Both patterns coexist.

5. **Assembler Dependency**: Controller depends directly on `TranslationCommentModelAssembler` and `TranslationModelAssembler`. No facade layer to orchestrate response building.

---

## File Locations Quick Reference

```
Controllers:    backend/api/src/main/kotlin/io/tolgee/api/v2/controllers/
Services:       backend/data/src/main/kotlin/io/tolgee/service/
Repositories:   backend/data/src/main/kotlin/io/tolgee/repository/
Entities:       backend/data/src/main/kotlin/io/tolgee/model/
DTOs:           backend/data/src/main/kotlin/io/tolgee/dtos/
HATEOAS:        backend/api/src/main/kotlin/io/tolgee/hateoas/
Security:       backend/security/src/main/kotlin/io/tolgee/security/
Activity:       backend/data/src/main/kotlin/io/tolgee/activity/
```
