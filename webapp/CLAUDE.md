# Webapp Translation Guidelines

## Adding New Translation Keys

When adding new translation keys, use the Tolgee MCP tools. The project ID is in `webapp/.tolgeerc.json` (`projectId`
field). The API key is in `.env.development.local` as `VITE_APP_TOLGEE_API_KEY`.

**Important:** Do NOT edit local translation files (`public/i18n/en.json`, etc.). Tolgee serves translations at runtime
via its API/CDN, so local files are only fallbacks shipped with the repo. New keys only need to be created via the
Tolgee MCP tools — the running app will pick them up automatically.

**Important:** Translations MUST always be uploaded to Tolgee — never skip this step. Before making any Tolgee calls,
present a summary of ALL planned keys (names, English translations, tags) for user confirmation. Ask for confirmation
once, then execute all calls together.

**Important:** Screenshots are mandatory for every new key, unless the user explicitly asks to skip them.

**Important:** Never concatenate translated text with other strings. Instead, use ICU message format parameters.

## Code Patterns

```tsx
// T component
<T keyName="my_key" defaultValue="My default text" />

// T component with params
<T keyName="my_key" defaultValue="Hello {name}" params={{ name: userName }} />

// t() function
t('my_key', 'My default text')

// t() function with params
t('my_key', 'Hello {name}', { name: userName })

// WRONG — don't concatenate:
// {t('greeting')} — {name}

// CORRECT — use params:
// {t('greeting', 'Hello — {name}', { name })}
```

### defaultValue Is Temporary

Add `defaultValue` **only to the new keys you are adding** — it makes those keys visible in the UI for screenshots.
Do NOT add `defaultValue` to existing keys. After screenshots are taken, **remove all `defaultValue` props you added**.
The actual translations live in Tolgee, not in the source.

## Workflow

### 1. Add defaultValue to New Keys

Add `defaultValue` to the new `T` components and `t()` calls you are creating, so the UI renders visible text for
screenshots:

```tsx
<T keyName="my_new_key" defaultValue="My default text" />
```

Only add `defaultValue` to the keys you are creating — not to existing ones.

### 2. Take Screenshots (Mandatory)

Screenshots are mandatory unless the user explicitly opts out.

Use the `/doc-screenshot` skill to take polished screenshots. If the skill is not available, follow the basic
instructions below.

#### Start the App

1. Read `webapp/.env.development.local` for port configuration:
   - `VITE_APP_API_URL` — backend URL (extract port, e.g. `http://localhost:8180` → port `8180`)
   - `VITE_PORT` — frontend dev server port
2. Start the backend as a background task:
   ```bash
   ./gradlew server-app:bootRun --args='--spring.profiles.active=dev'
   ```
3. Start the frontend as a background task (suppress auto-opening a browser window):
   ```bash
   cd webapp && BROWSER=none npm start
   ```
4. Wait for the backend to be ready:
   ```bash
   scripts/wait-for-backend.sh <backend-port>
   ```

#### Get Key Positions for Tolgee

After navigating to the page, get the bounding boxes of new translation keys:

```js
const keys = await page.evaluate(() => window.__tolgee.getVisibleKeys());
// Returns: [{ keyName, keyNamespace, position: { x, y, width, height } }, ...]
```

`window.__tolgee` is available in development mode (provided by the `DevTools()` plugin). The app must be fully loaded
and Tolgee initialized before calling this. Filter the results to only include the new keys you added.

#### Upload Screenshots

1. Base64-encode the screenshot file:
   ```bash
   base64 < <screenshot-file>
   ```
2. Call the `upload_image` MCP tool with the base64 string — it returns an `imageId`.

#### Stop the App

Stop both the backend and frontend background tasks when done with screenshots.

### 3. Remove defaultValue from Code

`defaultValue` was only needed for screenshots. Remove it from all `T` components and `t()` calls you added it to.

### 4. Create Keys with Translations and Screenshots

Use the `create_keys` MCP tool. For each key provide:
- `name` — the key name
- `translations` — `{ "en": "English text" }` (base language only)
- `tags` — `["draft: <feature-name>"]` (see branch tagging convention below)
- `description` — optional developer context
- `screenshots` — `[{ "uploadedImageId": <id>, "positions": [{ "x": ..., "y": ..., "width": ..., "height": ... }] }]`

Map `getVisibleKeys()` entries to the `positions` array. If a key appears multiple times on screen (e.g. repeated
buttons), include multiple position entries. Only include positions for the new keys you are creating.

To add screenshots to **existing** keys (without creating new ones), use the `add_key_screenshots` MCP tool instead.

### 5. Upload Context (Related Keys)

Use the `store_big_meta` MCP tool when at least 2 related keys are present. This tells Tolgee which keys appear
together, improving machine translation consistency.

## Branch Tagging Convention

- Extract feature name from branch: `username/feature-name` → `feature-name`
- Tag format: `draft: <feature-name>`
- Example: Branch `jancizmar/batch-job-optimization` → Tag `draft: batch-job-optimization`
