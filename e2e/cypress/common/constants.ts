export const HOST = Cypress.env('HOST') || 'http://localhost:8202';
export const PASSWORD = Cypress.env('DEFAULT_PASSWORD') || 'admin';
export const USERNAME = Cypress.env('DEFAULT_USERNAME') || 'admin';
export const API_URL = Cypress.env('API_URL') || 'http://localhost:8201';
/**
 * The server as it sees itself — for URLs the server fetches server-side rather than the
 * browser. Not `API_URL`: that is the host-side port, which the e2e container cannot reach
 * once `TOLGEE_E2E_PORT` maps it to something other than the 8201 it listens on inside.
 */
export const SERVER_SELF_URL =
  Cypress.env('SERVER_SELF_URL') || 'http://localhost:8201';
export const MAIL_API_URL =
  Cypress.env('MAIL_API_URL') || 'http://localhost:21080';
