import { HOST } from '../../common/constants';
import { gcy } from '../../common/shared';
import { publicProjectsData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';

describe('Public projects view', () => {
  beforeEach(() => {
    publicProjectsData.clean();
    publicProjectsData.generate();
    cy.visit(`${HOST}/public-projects`);
    waitForGlobalLoading();
  });

  afterEach(() => {
    publicProjectsData.clean();
  });

  it('shows the community banner and login/sign-up for a logged-out visitor', () => {
    gcy('community-translation-banner').should('be.visible');
    gcy('public-projects-login-button').should('be.visible');
    gcy('public-projects-sign-up-button').should('be.visible');
    gcy('organization-switch').should('not.exist');
    gcy('global-plus-button').should('not.exist');
  });

  it('lists public projects with the public badge + org and hides private ones', () => {
    gcy('dashboard-projects-list-item').should('have.length', 2);
    gcy('project-list-public-info').should('have.length', 2);
    cy.contains('Community Alpha').should('be.visible');
    cy.contains('Community Beta').should('be.visible');
    cy.contains('Private project').should('not.exist');
  });

  it('narrows the list with search', () => {
    gcy('global-list-search').find('input').type('Alpha');
    waitForGlobalLoading();
    gcy('dashboard-projects-list-item').should('have.length', 1);
    cy.contains('Community Alpha').should('be.visible');
  });

  it('routes a public row click to the login page', () => {
    gcy('dashboard-projects-list-item').first().click();
    cy.url().should('include', '/login');
  });
});
