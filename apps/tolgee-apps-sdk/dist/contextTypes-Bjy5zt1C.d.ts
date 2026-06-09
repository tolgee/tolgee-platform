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

export type { AppContextClaims as A, TolgeeAppContext as T, TolgeeAppSelection as a };
