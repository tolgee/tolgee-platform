# Webapp Translation Guidelines

## Adding New Translation Keys via API

When adding new translation keys, use the Tolgee REST API at `https://app.tolgee.io`. The API key is stored in
`.env.development.local` as `VITE_APP_TOLGEE_API_KEY`.

**Important:** Do NOT edit local translation files (`public/i18n/en.json`, etc.). Tolgee serves translations at runtime
via its API/CDN, so local files are only fallbacks shipped with the repo. New keys only need to be created via the
Tolgee REST API — the running app will pick them up automatically.

**Important:** Before making any Tolgee API calls, present a summary of ALL planned actions upfront (key creation,
context upload, tagging) with the full details (key names, translations, tags, etc.). Ask the user for confirmation
once, then execute all calls together.

**Important:** Remember to upload the context (BigMeta) and Screenshots for each key.

**Important:** Always provide `defaultValue` when using the `T` component or `t()` function. This ensures the UI
displays meaningful text before translations are uploaded to Tolgee, which is needed for screenshots to show correct
initial values.

**Important:** Never concatenate translated text with other strings. Instead, use ICU message format parameters.

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

### 1. Take Screenshot and Get Key Positions

Before creating keys, take a screenshot of the running app so the screenshot shows the UI with `defaultValue` text.
Use Playwright MCP to navigate to the page, take a screenshot, and call `window.__tolgee.getVisibleKeys()` to get
all visible translation keys with their positions.

**Important:** Resize the Playwright viewport to a fixed size (e.g. 1280x720) **before** navigating to the page.
The positions from `getVisibleKeys()` are in CSS pixels relative to the viewport, so the viewport dimensions must
match the screenshot dimensions. Without this, highlights will be offset in Tolgee.

```js
// Set a fixed viewport size first
await page.setViewportSize({ width: 1280, height: 720 });

// Navigate and get visible keys with positions
const keys = await page.evaluate(() => window.__tolgee.getVisibleKeys());
// Returns: [{ keyName, keyNamespace, position: { x, y, width, height } }, ...]

// Take screenshot
await page.screenshot({ path: 'screenshot.png' });
```

### 2. Upload the Image

**Endpoint:** `POST https://app.tolgee.io/v2/image-upload`

```bash
curl -X POST "https://app.tolgee.io/v2/image-upload" \
  -H "X-API-Key: ${VITE_APP_TOLGEE_API_KEY}" \
  -F "image=@screenshot.png"
```

Response includes `"id": 123456` — this is the `uploadedImageId` for the next step.

### 3. Create Keys with Translations, Tags, and Screenshots

Use `single-step-import-resolvable` to create all keys at once with their translations, tags, and screenshot
references in a single API call. This replaces the need for separate key creation, tagging, and screenshot
association steps.

**Endpoint:** `POST https://app.tolgee.io/v2/projects/single-step-import-resolvable`

```bash
curl -X POST "https://app.tolgee.io/v2/projects/single-step-import-resolvable" \
  -H "X-API-Key: ${VITE_APP_TOLGEE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "keys": [
      {
        "name": "my_key",
        "translations": {
          "en": "English text"
        },
        "tags": ["draft: my-feature-branch"],
        "screenshots": [{
          "uploadedImageId": 123456,
          "positions": [{"x": 100, "y": 200, "width": 150, "height": 40}]
        }]
      }
    ]
  }'
```

Map each entry from `getVisibleKeys()` to a key in the `keys` array, using the `position` values for `positions`.
A key appearing multiple times (e.g. repeated buttons) should have multiple entries in `positions`.
Only provide translations for the base language (English [en]).
Tag each key using the branch tagging convention (see below): `"tags": ["draft: <feature-name>"]`.

### 4. Upload Context (Related Keys)

Store which keys appear together on the same page/component for better MT suggestions. Requires at least 2 keys.

**Endpoint:** `POST https://app.tolgee.io/v2/projects/big-meta`

```bash
curl -X POST "https://app.tolgee.io/v2/projects/big-meta" \
  -H "X-API-Key: ${VITE_APP_TOLGEE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "relatedKeysInOrder": [
      { "keyName": "key_appearing_on_same_page_1" },
      { "keyName": "key_appearing_on_same_page_2" },
      { "keyName": "key_appearing_on_same_page_3" }
    ]
  }'
```

## Branch Tagging Convention

- Extract feature name from branch: `username/feature-name` → `feature-name`
- Tag format: `draft: <feature-name>`
- Example: Branch `jancizmar/batch-job-optimization` → Tag `draft: batch-job-optimization`
