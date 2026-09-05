# E2E tests

UI and its interaction with the backend are tested using E2E (end-to-end) cypress tests.

## Running the E2E tests

To just run it, you can execute the runE2e Gradle task. This command runs a complex task, which installs all dependencies and runs everything it needs.

```shell
./gradlew runE2e
```

To run only selected specs, pass `specs` property with a comma-separated list or glob:

```shell
./gradlew runE2e -Pspecs="**/translations/plurals.cy.ts,**/translations/singleKeyForm.cy.ts"
```

## Step-by-step run

1. Prepare the environment by the [development guide](../DEVELOPMENT.md).
2. Install dependencies:

   ```shell
   npm --prefix e2e ci
   ```

3. Run the tested environment:

   ```shell
   # Run frontend with E2E settings
   VITE_APP_API_URL=http://localhost:8201 npm --prefix webapp run start -- --port 8081 --host --no-open
   # Run the E2E Docker services (like fake SMTP server)
   ./gradlew runDockerE2eDev
   # Run backend with e2e profile
   TOLGEE_E2E_FRONTEND_PORT=8081 ./gradlew server-app:bootRun --args='--spring.profiles.active=e2e'
   # You can also do this by running the application with the E2e profile using Idea CE or Ultimate.
   # Then you will be also able to debug the backend and hotswap classes while running the tests, which can be pretty useful.
   ```

   `TOLGEE_E2E_FRONTEND_PORT` has to name the frontend port you actually started above (8081 here, not the
   8202 the profile defaults to). Specs that follow a backend-issued redirect to `tolgee.frontend-url` —
   `oauth2Consent.cy.ts` is the first — land nowhere without it.

   `oauth2Consent.cy.ts` additionally needs `HOST` and `API_URL` to be the *same* origin, because it visits the
   backend's `/oauth2/authorize` and then follows a redirect back to the SPA; Cypress treats a differing port as a
   different origin. `runE2e` points both at one port, so the spec passes there. This step-by-step flow does not,
   so run it with `CYPRESS_API_URL` set to the frontend origin and `VITE_DEV_PROXY_TARGET` pointing the vite dev
   server at the backend, or skip that spec locally and let CI cover it.

4. Run the tests:

   ```shell
   ./gradlew openE2eDev
   ```

5. Stop the environment when done:

   ```shell
   ./gradlew stopDockerE2e
   ```
