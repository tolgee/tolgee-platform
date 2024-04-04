/// <reference types="cypress" />
import { getAnyContainingText } from '../../common/xPath';
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
    cy.xpath("//*[@aria-controls='user-menu']").click();
    cy.wait(50);
    cy.xpath(getAnyContainingText('Api keys')).click();
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
    cy.xpath("//*[@aria-controls='user-menu']").click();
    cy.get('#user-menu').contains('Account settings').click();
  });

  it('Accesses personal access tokens', () => {
    cy.xpath("//*[@aria-controls='user-menu']").click();
    cy.get('#user-menu').contains('Personal Access Tokens').click();
    cy.get('h6').contains('Personal Access Tokens').should('be.visible');
    gcy('global-empty-list').should('be.visible');
  });
});
