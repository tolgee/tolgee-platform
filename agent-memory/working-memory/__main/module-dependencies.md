# Tolgee Platform - Module Dependency Graph

**Last updated:** After renaming `data` module to `core`

## ASCII Dependency Graph (Current State)

```
                            +-------------------------------------+
                            |         server-app [BOOTJAR]        |
                            |    (backend/app - Final Artifact)   |
                            +-------------------+-----------------+
                                                |
        +---------------+---------------+-------+-------+---------------+---------------+
        |               |               |               |               |               |
        v               v               v               v               v               v
   +---------+    +----------+    +----------+    +-----------+   +----------+    +----------+
   |   api   |    | security |    |   core   |    |development|   |  ee-app  |    |billing-app|
   |         |    |          |    |          |    |           |   | (opt EE) |    |   (opt)  |
   +----+----+    +----+-----+    +----+-----+    +-----+-----+   +----+-----+    +----------+
        |              |               |                |              |
        |              |               |                |              |
        +-------+------+---------------+                |              |
        |       |               |                       |              +---> core
        v       v               v                       v              +---> security
   +---------+    +---------+    +---------+      +---------+         +---> api
   |  core   |    |  core   |    |  misc   |      |  core   |
   +----+----+    +----+----+    +---------+      +---------+
        |              |              ^                |
        |              |              |                |
        v              v              |                v
   +---------+    +---------+        |           +---------+
   |security |    |  misc   |        |           | ee-app? |
   +----+----+    +---------+        |           +---------+
        |                            |
        v                            |
   +---------+                       |
   |  misc   +-----------------------+
   +---------+


## Simplified Layered View

                    +-------------------------------------------------------------+
  APPLICATION       |                    server-app [BOOTJAR]                     |
                    +-------------------------------------------------------------+
                                              |
                    +-------------------------+-------------------------+
                    |                                                   |
                    v                                                   v
                    +---------------+                     +-------------------------+
  OPTIONAL          |  development  |                     |  ee-app    billing-app  |
                    +---------------+                     |      (if present)       |
                                                          +-------------------------+
                                              |
                    +-------------------------+-------------------------+
                    |                                                   |
                    v                                                   v
                    +---------------+                           +---------------+
  API LAYER         |      api      | ------------------------->|   security    |
                    +---------------+                           +---------------+
                            |                                           |
                            +-------------------+-----------------------+
                                                |
                                                v
                                        +---------------+
  CORE LAYER                            |     core      |
                                        +---------------+
                                                |
                                                v
                                        +---------------+
  UTILITIES                             |     misc      |
                                        +---------------+


## Module Summary Table

| Module       | Location              | Type      | Internal Dependencies              |
|--------------|-----------------------|-----------|------------------------------------
| server-app   | backend/app           | BOOTJAR   | api, security, core, misc, development, ee-app?, billing-app? |
| api          | backend/api           | library   | core, security, misc               |
| security     | backend/security      | library   | core, misc                         |
| core         | backend/core          | library   | misc                               |
| misc         | backend/misc          | library   | (none)                             |
| development  | backend/development   | library   | core, ee-app?                      |
| testing      | backend/testing       | test-util | core, misc - used by others as testImplementation |
| ktlint       | backend/ktlint        | build-tool| (none) - linting rules only        |
| ee-app       | ee/backend/app        | optional  | core, security, api                |
| ee-test      | ee/backend/tests      | test      | testing, security, ee-app, server-app, api, core, misc |
| billing-app  | ../billing/app        | optional  | (external repo)                    |
| billing-test | ../billing/tests      | test      | (external repo)                    |

## Key Observations

1. **Leaf module**: `misc` has no internal dependencies - foundational utilities
2. **Core chain**: misc -> core -> security -> api -> server-app
3. **Final artifact**: Only `server-app` produces the deployable bootJar (`tolgee-{version}.jar`)
4. **Optional modules**: ee-app and billing-app are conditionally included based on directory existence
5. **Build command**: `./gradlew packResources` creates final `tolgee.jar` with embedded webapp

## Notes

Current structure is **layered**:
- `misc` = shared utilities
- `core` = entities + repositories (persistence) + services
- `security` = auth/authz
- `api` = REST controllers
- `server-app` = Spring Boot orchestrator

---
