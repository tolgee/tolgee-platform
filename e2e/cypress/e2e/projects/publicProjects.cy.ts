import { HOST } from '../../common/constants';
import { gcy } from '../../common/shared';
import { login } from '../../common/apiCalls/common';
import { loginViaForm } from '../../common/login';
import { publicProjectsData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';

describe('Public projects view', () => {
  const visit = () => {
    cy.visit(`${HOST}/public-projects`);
    waitForGlobalLoading();
  };

  beforeEach(() => {
    publicProjectsData.clean();
  });

  afterEach(() => {
    publicProjectsData.clean();
  });

  it('shows the community banner and login/sign-up for a logged-out visitor', () => {
    publicProjectsData.generateStandard();
    visit();
    gcy('community-translation-banner').should('be.visible');
    gcy('public-projects-login-button').should('be.visible');
    gcy('public-projects-sign-up-button').should('be.visible');
    gcy('organization-switch').should('not.exist');
    gcy('global-plus-button').should('not.exist');
  });

  it('lists public projects with the public badge + org and hides private ones', () => {
    publicProjectsData.generateStandard();
    visit();
    gcy('dashboard-projects-list-item').should('have.length', 6);
    gcy('project-list-public-badge').should('have.length', 6);
    gcy('global-search-field').should('exist');
    cy.contains('Community Alpha').should('be.visible');
    cy.contains('Community Zeta').should('be.visible');
    cy.contains('Private project').should('not.exist');
    gcy('project-list-more-button').should('not.exist');
    gcy('project-list-translations-button').should('not.exist');
    gcy('project-list-qa-badge-button').should('not.exist');
  });

  it('narrows the list with search', () => {
    publicProjectsData.generateStandard();
    visit();
    gcy('global-search-field').find('input').type('Alpha');
    waitForGlobalLoading();
    gcy('dashboard-projects-list-item').should('have.length', 1);
    cy.contains('Community Alpha').should('be.visible');
    gcy('global-search-field').should('exist');

    gcy('global-search-field').find('input').clear();
    gcy('dashboard-projects-list-item').should('have.length', 6);
    gcy('global-search-field').should('exist');
  });

  it('hides the search field when the project count is at the threshold', () => {
    publicProjectsData.generateFew();
    visit();
    gcy('dashboard-projects-list-item').should('have.length', 5);
    gcy('global-search-field').should('not.exist');
  });

  it('routes a public row click to the login page', () => {
    publicProjectsData.generateStandard();
    visit();
    gcy('dashboard-projects-list-item').first().click();
    cy.url().should('include', '/login');
  });

  it('returns a logged-out visitor to the clicked project after login', () => {
    publicProjectsData.generateStandard();
    visit();
    gcy('dashboard-projects-list-item').first().click();
    cy.url().should('include', '/login');
    loginViaForm('admin', 'admin');
    cy.url().should('match', /\/projects\/[0-9]+/);
  });

  it('opens the project instead of login for a logged-in visitor', () => {
    publicProjectsData.generateStandard();
    login('admin', 'admin');
    visit();
    gcy('community-translation-banner').should('be.visible');
    gcy('dashboard-projects-list-item').first().click();
    cy.url().should('match', /\/projects\/[0-9]+/);
  });
});
