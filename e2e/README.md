# E2E tests

UI and its interaction with the backend are tested using E2E (end-to-end) cypress tests.

## Running the E2E tests

To just run it, you can execute the runE2e Gradle task. This command runs a complex task, which installs all dependencies and runs everything it needs.

```shell
./gradlew runE2e
```

## Step-by-step run

1. Prepare the environment by the [development guide](DEVELOPMENT.md)
2. Install dependencies
   ```shell
   cd e2e && npm i
   ```

3. Run the tested environment
   ```shell
   # Run frontend with E2E settings
   ./gradlew runWebAppNpmStartE2eDev
   # Run the E2E Docker services (like fake SMTP server)
   ./gradlew runDockerE2eDev
   # Run backend with e2e profile
   ./gradlew server-app:bootRun --args='--spring.profiles.active=e2e'
   # You can also do this by running the application with the E2e profile using Idea CE or Ultimate.
   # Then you will be also able to debug the backend and hotswap classes while running the tests, which can be pretty useful.
   ```

4. Run the tests
   ```shell
   ./gradlew openE2eDev
   ```

5. Stop the environment when done
   ```shell
   ./gradlew stopDockerE2e
   ```
