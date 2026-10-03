/// <reference types="cypress" />

import { login } from '../../common/apiCalls/common';
import { assertMessage, gcy } from '../../common/shared';
import { administrationTestData } from '../../common/apiCalls/testData/testData';
import {
  debugUserAccount,
  visitAdministration,
} from '../../common/administration';
import { createProject } from '../../common/projects';

describe('Debug customer account', () => {
  beforeEach(() => {
    administrationTestData.clean();
    administrationTestData.generate();
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

  it("lists projects of user's organization on first request", () => {
    cy.intercept('GET', '**/projects-with-stats*').as('projectsWithStats');
    debugUserAccount();
    cy.wait('@projectsWithStats').its('response.statusCode').should('eq', 200);
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
