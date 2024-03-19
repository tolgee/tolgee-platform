/// <reference types="cypress" />
import { HOST } from '../common/constants';
import {
  createUser,
  forceDate,
  login,
  releaseForcedDate,
} from '../common/apiCalls/common';

describe('Feature announcement', () => {
  let initalUser: string;
  const INITIAL_PASSWORD = 'honzaaaaaaaa';

  beforeEach(() => {
    initalUser = `honza${Math.random()}@test.com`;
    createUser(initalUser, INITIAL_PASSWORD);
    login(initalUser, INITIAL_PASSWORD);
  });

  afterEach(() => {
    releaseForcedDate();
  });

  it('Announcement is here', () => {
    forceDate(new Date('2023-08-28').getTime());
    visit();
    cy.gcy('top-banner-content').should('be.visible');
  });

  it('Announcement will disappear', () => {
    forceDate(new Date('2100-01-01').getTime());
    login(initalUser, INITIAL_PASSWORD); // relog necessary: JWT is expired!
    visit();
    cy.gcy('global-base-view-content').should('exist');
    cy.gcy('top-banner-content').should('not.exist');
  });

  it('Announcement can be dismissed', () => {
    forceDate(new Date('2023-08-28').getTime());
    visit();
    cy.gcy('top-banner-content').should('be.visible');
    cy.gcy('top-banner-dismiss-button').click();
    cy.gcy('top-banner-content').should('not.exist');
    visit();
    cy.gcy('global-base-view-content').should('exist');
    cy.gcy('top-banner-content').should('not.exist');
  });

  function visit() {
    cy.visit(HOST);
  }
});
