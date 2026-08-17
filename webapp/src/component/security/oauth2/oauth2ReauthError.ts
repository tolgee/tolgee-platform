// A present-but-stale webapp JWT (e.g. invalidated by a password change) makes session-bootstrap fail with one of
// these instead of the unauthenticated redirect PrivateRoute already handles. Dropping the token routes the user to
// log in and resume the OAuth flow.
const REAUTH_ERROR_CODES = [
  'unauthenticated',
  'expired_jwt_token',
  'invalid_jwt_token',
  'general_jwt_error',
];

export const isReauthError = (code: string | undefined): boolean =>
  code !== undefined && REAUTH_ERROR_CODES.includes(code);
