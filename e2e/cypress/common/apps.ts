import { SERVER_SELF_URL } from './constants';
import { gcy } from './shared';

export const APP_MANIFEST_URL = `${SERVER_SELF_URL}/internal/e2e-data/apps/manifest.json`;

const ENABLED_APPS_ALIAS = 'enabledApps';

/**
 * Registers the e2e test app from its manifest URL. Expects the organization Apps
 * settings page to be open, and leaves the registered app listed on it.
 */
export function registerAppFromManifest() {
  gcy('organization-apps-register-button').click();
  gcy('organization-apps-register-manifest-url').type(APP_MANIFEST_URL);
  gcy('organization-apps-register-continue').click();
  gcy('organization-apps-register-submit').click();
  gcy('organization-apps-item').should('exist');
}

/** Call right before visiting a project page whose menu state you are about to assert. */
export function interceptEnabledApps() {
  cy.intercept('GET', '**/v2/projects/*/apps/enabled').as(ENABLED_APPS_ALIAS);
}

/**
 * Waits for the project menu's enabled-apps query. Without it, asserting that no app entry
 * exists passes before the query even resolves, so the assertion can never fail.
 */
export function waitForEnabledApps() {
  return cy.wait(`@${ENABLED_APPS_ALIAS}`);
}
