import { describe, expect, it } from 'vitest';
import { isReauthError } from 'tg.component/security/oauth2/oauth2ReauthError';

describe('isReauthError', () => {
  it('is true for each stale-token error code', () => {
    expect(isReauthError('unauthenticated')).toBe(true);
    expect(isReauthError('expired_jwt_token')).toBe(true);
    expect(isReauthError('invalid_jwt_token')).toBe(true);
    expect(isReauthError('general_jwt_error')).toBe(true);
  });

  it('is false for an unrelated or missing code', () => {
    expect(isReauthError('some_other_error')).toBe(false);
    expect(isReauthError(undefined)).toBe(false);
    expect(isReauthError('')).toBe(false);
  });
});
