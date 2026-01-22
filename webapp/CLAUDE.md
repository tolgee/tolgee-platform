# Webapp Translation Guidelines

## Adding New Translation Keys via API

When adding new translation keys, use the Tolgee REST API at `https://app.tolgee.io`. The API key is stored in `.env.development.local` as `VITE_APP_TOLGEE_API_KEY`.

**Important:** Always test API calls on a demo project first before applying to the main project.

### 1. Create Keys with Translations

**Endpoint:** `POST https://app.tolgee.io/v2/projects/single-step-import-resolvable`

```bash
curl -X POST "https://app.tolgee.io/v2/projects/single-step-import-resolvable" \
  -H "X-API-Key: ${VITE_APP_TOLGEE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "keys": [
      {
        "name": "my_translation_key",
        "translations": {
          "en": { "text": "English text", "resolution": "EXPECT_NO_CONFLICT" }
        }
      }
    ]
  }'
```

**Note:** Using `EXPECT_NO_CONFLICT` ensures the import fails if the key already exists, preventing accidental overwrites.

### 2. Upload Context (Related Keys)

Store which keys appear together on the same page/component for better MT suggestions. Do this BEFORE tagging so MT can use the context immediately. Requires at least 2 keys.

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

### 3. Tag Keys as Draft

Tag keys with current branch name (without username prefix). Format: `draft: <feature-name>`

**Endpoint:** `PUT https://app.tolgee.io/v2/projects/tag-complex`

```bash
curl -X PUT "https://app.tolgee.io/v2/projects/tag-complex" \
  -H "X-API-Key: ${VITE_APP_TOLGEE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "filterKeys": [
      { "name": "my_translation_key_1" },
      { "name": "my_translation_key_2" }
    ],
    "tagFiltered": ["draft: my-feature-branch"]
  }'
```

## Branch Tagging Convention

- Extract feature name from branch: `username/feature-name` → `feature-name`
- Tag format: `draft: <feature-name>`
- Example: Branch `jancizmar/batch-job-optimization` → Tag `draft: batch-job-optimization`
