// shortcut: prototype-only preferences kept in this browser; each key needs backend support before shipping
export const STORAGE_KEYS = {
  projectLanguages: 'tolgee-notification-project-languages',
  globalLanguages: 'tolgee-notification-global-languages',
  myTranslations: 'tolgee-notification-my-translations',
};

export const OVERRIDES_PREFIX = 'tolgee-notification-project-overrides-';

export const projectOverridesKey = (projectId: number) =>
  `${OVERRIDES_PREFIX}${projectId}`;

const isPrototypeKey = (key: string) =>
  Object.values(STORAGE_KEYS).includes(key) || key.startsWith(OVERRIDES_PREFIX);

export const readJson = <T>(key: string, fallback: T): T => {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
};

export const writeJson = (key: string, value: unknown) => {
  try {
    if (value === null || value === undefined) {
      localStorage.removeItem(key);
      return;
    }
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // storage unavailable, the change just isn't kept
  }
};

export const clearPrototypeStorage = () => {
  try {
    Object.keys(localStorage)
      .filter((k) => k.startsWith('tolgee-notification-'))
      .forEach((k) => localStorage.removeItem(k));
  } catch {
    // storage unavailable
  }
};

export const hasPrototypeOverrides = () => {
  try {
    return Object.keys(localStorage).some(isPrototypeKey);
  } catch {
    return false;
  }
};
