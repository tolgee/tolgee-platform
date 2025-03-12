/// <reference types="cypress" />

import { login } from '../../common/apiCalls/common';
import { assertMessage, gcy } from '../../common/shared';
import 'cypress-file-upload';
import { administrationTestData } from '../../common/apiCalls/testData/testData';
import {
  getUserListItem,
  visitAdministration,
} from '../../common/administration';
import { createProject } from '../../common/projects';

describe('Debug customer account', () => {
  beforeEach(() => {
    administrationTestData.clean();
    administrationTestData.generate().then((res) => {});
    login('admin@admin.com');
    visitAdministration();
  });

  afterEach(() => {
    administrationTestData.clean();
  });

  it('can login as user and exit', () => {
    debugUserAccount();
    assertDebugFrameVisible();
    gcy('administration-debug-customer-exit-button').click();
    assertDebugFrameNotVisible();
    cy.contains('Server administration').should('be.visible');
  });

  it('can create project in users organization', () => {
    debugUserAccount();
    cy.waitForDom();
    gcy('organization-switch').contains('John User');
    createProject('Test project', 'John User');
    assertMessage('Project created');
  });
});

function assertDebugFrameVisible() {
  gcy('administration-frame').should('exist');
  gcy('administration-debug-customer-account-message').should('be.visible');
}

function assertDebugFrameNotVisible() {
  gcy('administration-frame').should('not.exist');
  gcy('administration-debug-customer-account-message').should('not.exist');
}

function debugUserAccount() {
  visitAdministration();
  gcy('settings-menu-item').contains('Users').click();
  getUserListItem().findDcy('administration-user-debug-account').click();
}
