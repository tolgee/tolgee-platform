## Install Prerequisites

* [Java 21](https://openjdk.org/install)
* [Docker](https://docs.docker.com/engine/install)
* [Node.js 18](https://nodejs.org/en/download) (or higher)
* [Intellij Idea](https://www.jetbrains.com/help/idea/installation-guide.html) (optional)

## Clone this repo

```shell
git clone --depth 1 git@github.com:tolgee/tolgee-platform.git
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

## Run the stack

1. Run backend
   * With the prepared Idea run configuration `Backend localhost`
   * With command line:
     ```shell
     ./gradlew server-app:bootRun --args='--spring.profiles.active=dev'
     ```
2. Run frontend
   * With the prepared Idea run configuration `Frontend localhost`
   * With command line:
     ```shell
     npm --prefix library ci
     cd webapp && npm ci && npm run start
     ```
3. Open your browser and go to http://localhost:3000.

## Testing

The backend of Tolgee is tested with unit and integration tests.

### Backend testing

To run backend tests, you can run Gradle test task

```shell
./gradlew test
```

Or you can select any integration test the code and run it via Idea CE or Idea Ultimate.
It should just work out of the box.

### E2E testing

Follow the steps in the [E2E readme](e2e/README.md).

## Backend configuration

To configure Tolgee, create an empty file `backend/app/src/main/resources/application-dev.yaml`.
In this file, you can override default configuration properties.

Here are some useful settings for localhost development:

```yaml
spring:
  jpa:
    show-sql: true
tolgee:
  front-end-url: http://localhost:3000
  file-storage-url: http://localhost:8080
  authentication: # to see "Server administration", "Organization settings" etc.
    enabled: true
    initial-username: admin
    initial-password: 123123
```

You can check `application-e2e.yaml` for further inspiration.
To learn more about externalized configuration in Spring boot, read [the docs](https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html).

Since we set the active profile to `dev`, Spring uses the `application-dev.yaml` configuration file.

## Writing emails

Please refer to [email/HACKING.md](email/HACKING.md).

## Updating the database changelog

Tolgee uses Liquibase to handle the database migration. The migrations are run on every app startup. To update the changelog, run:

```shell
./gradlew diffChangeLog
```

### Troubleshooting updating the changelog

If you misspell the command and run diffChangelog, it will find the command, but it would fail, since liquibase changed the command name in the past.
We have enhanced the diffChangeLog (with capital L) command, so you have to run that.

Sometimes, Gradle cannot find a docker command to start the database instance to generate the changelog against.
This happens due to some issue with setting the paths for Gradle daemon.
Running the command without daemon fixes the issue:
```shell
./gradlew diffChangeLog --no-daemon
```

## Static analysis

For the frontend, there are npm tasks `prettier` and `eslint`, which you should run before every commit.
Otherwise, the "Frontend static check" workflow will fail.
You can also use prettier plugins for VS Code, Idea, or WebStorm.

To fix prettier issues and check everything is fine, run these commands:

```shell
cd webapp
npm run prettier
npm run tsc
npm run eslint
```

On the backend, there is Gradle task `ktlintFormat`, which helps you to format Kotlin code.

```shell
./gradlew ktlintFormat
```

On IntelliJ, you can install the Ktlint plugin to get the editor to format the code correctly.

## Using current translations from the Tolgee app

This is an optional step for contributors with access to the Tolgee project at `app.tolgee.io`.

Create a file `webapp/.env.development.local` with following content.
Don't forget to supply it with your API key generated in the Tolgee app:

```properties
VITE_APP_TOLGEE_API_URL=https://app.tolgee.io
VITE_APP_TOLGEE_API_KEY=your-tolgee-api-key
```

## Troubleshooting

### Command not found when executing gradle tasks on MacOS
When running E2e Tests from Idea on Mac, you encounter fails due to command not found.

Apparentrly this happens because IDEA starts the gradle daemon with wrong path.

The only **workaround** I currently found is killing the gradle daemon and running IDEA from terminal

```bash
pkill -f '.*GradleDaemon.*'
open -a 'IntelliJ IDEA Ultimate'
```

This way, IDEA is started with correct environment from zsh or bash and so the Gradle Daemon is started correctly.

If you don't like this solution (I don't like it too), you can start looking for better solution.
This thread is a good starting point: https://discuss.gradle.org/t/exec-execute-in-gradle-doesnt-use-path/25598/3

## Logging business events

To monitor business activities in the Tolgee platform, we use PostHog for event tracking. There are three ways to log business events:

### 1. Automatic logging with ActivityHolder

When an activity is stored with a modifying endpoint on the backend, the event is automatically logged. Developers can optionally provide additional metadata using the `businessEventData` property in `ActivityHolder`.

Usually, you don't need to provide the data, but If you really need to, you can do it this way.
```kotlin
// Example: Adding business event data to an activity
@Component
class YourService(
    private val activityHolder: ActivityHolder
) {
    fun performAction() {
        // Set the activity type
        activityHolder.activity = ActivityType.YOUR_ACTIVITY_TYPE

        // Add business event metadata
        activityHolder.businessEventData["key1"] = "value1"
        activityHolder.businessEventData["key2"] = "value2"

        // Perform your action...
    }
}
```

### 2. Manual logging from backend code

For cases where you need to log events that aren't tied to an activity, you can use the `BusinessEventPublisher` directly:

```kotlin
@Component
class YourService(
    private val businessEventPublisher: BusinessEventPublisher
) {
    fun logCustomEvent() {
        businessEventPublisher.publish(
            OnBusinessEventToCaptureEvent(
                eventName = "YOUR_CUSTOM_EVENT",
                data = mapOf("key1" to "value1", "key2" to "value2"),
                // Optional fields:
                projectId = 123,
                organizationId = 456
            )
        )
    }

    // You can also publish events that should only be sent once in a specific time period
    fun logRareEvent() {
        businessEventPublisher.publishOnceInTime(
            OnBusinessEventToCaptureEvent(
                eventName = "rare_event",
                data = mapOf("key1" to "value1")
            ),
            onceIn = Duration.ofHours(24) // Only log once per day
        )
    }
}
```

### 3. Logging from frontend code

For logging events from the frontend, use the provided React hooks:

```typescript
// Example 1: Using useReportEvent hook for event-triggered reporting
import { useReportEvent } from 'tg.hooks/useReportEvent';

function ExampleComponent() {
  // Get the report function
  const reportEvent = useReportEvent();

  // Use it in an event handler
  function handleButtonClick() {
    reportEvent('button_clicked', { buttonName: 'submit', page: 'settings' });
    // Rest of your click handler logic...
  }
}
```

```typescript
// Example 2: Using useReportOnce hook for reporting on component mount
import { useReportOnce } from 'tg.hooks/useReportEvent';

function AnotherComponent() {
  // This will automatically report the event once when the component mounts
  useReportOnce('page_viewed', { pageName: 'settings' });

  // Rest of your component...
}
```

These frontend hooks send events through the backend API, ensuring they aren't blocked by ad blockers.
