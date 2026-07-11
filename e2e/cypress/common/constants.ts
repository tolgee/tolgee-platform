export const HOST = Cypress.env('HOST') || 'http://localhost:8202';
export const PASSWORD = Cypress.env('DEFAULT_PASSWORD') || 'admin';
export const USERNAME = Cypress.env('DEFAULT_USERNAME') || 'admin';
export const API_URL = Cypress.env('API_URL') || 'http://localhost:8201';
export const MAIL_API_URL =
  Cypress.env('MAIL_API_URL') || 'http://localhost:21080';
