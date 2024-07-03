/* eslint-disable no-prototype-builtins */
// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands';
import 'cypress-promise/register';
import { setFeature } from '../common/features';

require('cypress-xpath');

// Alternatively you can use CommonJS syntax:
// require('./commands')

Cypress.on('uncaught:exception', (err, runnable) => {
  if (
    err.hasOwnProperty('CUSTOM_VALIDATION') ||
    err.hasOwnProperty('STANDARD_VALIDATION')
  ) {
    return false;
  }
  // @ts-ignore
  if (err.hasOwnProperty('code') && typeof err.code == 'string') {
    return false;
  }
});

Cypress.on('window:before:load', (win) => {
  win.localStorage.setItem('__tolgee_currentLanguage', 'en');
});

before(() => {
  // turn on all features
  setFeature('GRANULAR_PERMISSIONS', true);
  setFeature('WEBHOOKS', true);
  setFeature('PROJECT_LEVEL_CONTENT_STORAGES', true);
  setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
  setFeature('AI_PROMPT_CUSTOMIZATION', true);
  setFeature('TASKS', true);
});
