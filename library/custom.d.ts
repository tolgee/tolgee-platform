/// <reference types="storybook/test" />

// ^ Load Storybook types (play, queries...)

// Enable type checking for import.meta.env.VITE_APP_TOLGEE_API_URL/VITE_APP_TOLGEE_API_KEY

interface ViteTypeOptions {
  strictImportMetaEnv: unknown;
}

interface ImportMetaEnv {
  readonly VITE_APP_TOLGEE_API_URL: string;
  readonly VITE_APP_TOLGEE_API_KEY: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
