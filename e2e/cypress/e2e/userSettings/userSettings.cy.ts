/// <reference types="cypress" />
import { getAnyContainingText } from '../../common/xPath';
import { HOST } from '../../common/constants';
import { createTestProject, login } from '../../common/apiCalls/common';
import { getPopover } from '../../common/shared';

describe('User settings', () => {
  beforeEach(() => {
    login();
    cy.visit(HOST);
  });

  it('Will access api keys', () => {
    cy.xpath("//*[@aria-controls='user-menu']").click();
    cy.wait(50);
    cy.xpath(getAnyContainingText('Api keys')).click();
  });

  it('will open user menu from projects', () => {
    createTestProject().then((r) => {
      cy.visit(`${HOST}/projects/${r.body.id}`);
      cy.xpath("//*[@aria-controls='user-menu']").click();
      getPopover().contains('settings').click();
      cy.contains('User profile').should('be.visible');
    });
  });

  it('will access user settings', () => {
    cy.xpath("//*[@aria-controls='user-menu']").click();
    cy.get('#user-menu').contains('Account settings').click();
  });
});
