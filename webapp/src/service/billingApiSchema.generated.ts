/**
 * Stub — overridden by billing repo when present via path alias.
 * Provides permissive types so platform compiles without billing.
 */

export type paths = Record<string, Record<string, any>>;

export interface components {
  schemas: Record<string, any>;
}
