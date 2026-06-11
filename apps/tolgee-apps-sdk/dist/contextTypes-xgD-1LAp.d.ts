/**
 * Selection passed to iframes that render in a context where the user
 * has focused a specific key, language, or translation cell. All
 * fields are optional — modules launched from the project sidebar see
 * an empty selection.
 */
type TolgeeAppSelection = {
    keyId?: number;
    languageId?: number;
    languageTag?: string;
    translationId?: number;
    /**
     * Tags of the languages currently shown in the translations view. Provided
     * to the `translation-tools-panel-empty` module; updates as the user changes
     * the language selector. Undefined for surfaces that don't supply it.
     */
    selectedLanguages?: string[];
};
/**
 * The host's current theme. Delivered at init and again whenever the user
 * toggles light/dark, so the plugin can match Tolgee's look. `colors` are
 * resolved CSS color strings from Tolgee's palette; feed them to
 * {@link applyTolgeeTheme} to expose them as `--tg-color-*` CSS variables.
 */
type TolgeeAppTheme = {
    mode: 'light' | 'dark';
    colors: {
        background: string;
        backgroundPaper: string;
        text: string;
        textSecondary: string;
        primary: string;
        primaryContrast: string;
        divider: string;
        error: string;
    };
};
/**
 * Context delivered to the iframe via the `tolgee-app:init` postMessage.
 * `token` is the install-context JWT — pass it as a bearer token on
 * every REST call back to Tolgee.
 */
type TolgeeAppContext = {
    token: string;
    apiUrl: string;
    organizationId: number;
    projectId: number;
    selection: TolgeeAppSelection;
    /** Host theme at init; subscribe to changes via `onThemeChanged`. */
    theme: TolgeeAppTheme;
    /**
     * Trigger-specific fields merged into the init payload (e.g.
     * `selectedKeyIds` for bulk actions, `keyId` for key-edit-footer
     * modals). Untyped because the set varies per trigger surface; cast
     * as needed.
     */
    extra: Record<string, unknown>;
};
/**
 * Claims extracted from the Tolgee-issued JWT carried in
 * `TolgeeAppContext.token`. Backend plugins typically don't need to
 * verify the signature themselves — they pass the token as a bearer
 * token on REST calls back to Tolgee, which verifies it server-side.
 */
type AppContextClaims = {
    installId: number;
    projectId: number;
    userId: number;
    audience: string;
    expiresAt: number;
};

export type { AppContextClaims as A, TolgeeAppContext as T, TolgeeAppSelection as a, TolgeeAppTheme as b };
