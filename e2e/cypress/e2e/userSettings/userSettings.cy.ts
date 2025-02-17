/// <reference types="cypress" />
import { HOST } from '../../common/constants';
import { createTestProject, login } from '../../common/apiCalls/common';
import { gcy, getPopover } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

describe('User settings', () => {
  beforeEach(() => {
    login();
    cy.visit(HOST);
  });

  it('Accesses api keys', () => {
    clickOnMenuItem('Project API keys');
  });

  it('Opens user menu from projects', () => {
    createTestProject().then((r) => {
      cy.visit(`${HOST}/projects/${r.body.id}`);
      waitForGlobalLoading();
      cy.xpath("//*[@aria-controls='user-menu']").click();
      getPopover().contains('settings').click();
      cy.contains('User profile').should('be.visible');
    });
  });

  it('Accesses user settings', () => {
    clickOnMenuItem('Account settings');
  });

  it('Accesses personal access tokens', () => {
    clickOnMenuItem('Personal Access Tokens');
    cy.get('h6').contains('Personal Access Tokens').should('be.visible');
    gcy('global-empty-state').should('be.visible');
  });

  function clickOnMenuItem(itemLabel: string) {
    cy.xpath("//*[@aria-controls='user-menu']").click();
    cy.get('#user-menu').contains(itemLabel).click();
  }
});
