## Install Prerequisites

* [Java 21](https://openjdk.org/install)
* [Docker](https://docs.docker.com/engine/install)
* [Node.js 18](https://nodejs.org/en/download) (or higher)
* [Intellij Idea](https://www.jetbrains.com/help/idea/installation-guide.html) (optional)

## Clone this repo

```shell
git clone --depth 1 git@github.com:tolgee/tolgee-platform.git
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
```

You can check `application-e2e.yaml` for further inspiration.
To learn more about externalized configuration in Spring boot, read [the docs](https://docs.spring.io/spring-boot/docs/2.1.8.RELEASE/reference/html/boot-features-external-config.html).

Since we set the active profile to `dev`, Spring uses the `application-dev.yaml` configuration file.

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
