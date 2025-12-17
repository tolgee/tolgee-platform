import {
  GLOBAL_KEY,
  PARAM_KEY,
  PARAM_DISABLE_KEY,
  PARAM_LANGUAGE_KEY,
} from './constants';

export type Locales = Record<string, { name?: string; flag?: string }>;

export interface TolgeeAddonState {
  language?: string;
  locales: Locales;
}

export interface TolgeeAddonParameters {
  /** Tolgee addon configuration. Can be applied on stories meta or a single story. */
  [PARAM_KEY]?: {
    /** Remove the addon from UI and disable its behavior. */
    [PARAM_DISABLE_KEY]?: boolean;
    /** Override language. */
    [PARAM_LANGUAGE_KEY]?: string;
  };
}

export interface TolgeeAddonGlobals {
  /** Tolgee language set globally. The variable is shared across stories and the addon switcher. */
  [GLOBAL_KEY]?: string;
}

export interface TolgeeAddonTypes {
  parameters: TolgeeAddonParameters;
  globals: TolgeeAddonGlobals;
}
