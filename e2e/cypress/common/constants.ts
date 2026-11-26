export const HOST = Cypress.expose('HOST') || 'http://localhost:8202';
export const PASSWORD = Cypress.expose('DEFAULT_PASSWORD') || 'admin';
export const USERNAME = Cypress.expose('DEFAULT_USERNAME') || 'admin';
export const API_URL = Cypress.expose('API_URL') || 'http://localhost:8201';
export const MAIL_API_URL =
  Cypress.expose('MAIL_API_URL') || 'http://localhost:21080';
