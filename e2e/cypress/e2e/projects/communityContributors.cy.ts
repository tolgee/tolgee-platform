import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import { communityStatsData } from '../../common/apiCalls/testData/testData';
import { dismissMenu, gcy, gcyAdvanced } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

const OWNER = 'communityStatsOwner';
const PROJECT_NAME = 'Community Stats Demo';

describe('Community contributors page', () => {
  let projectId: number;

  beforeEach(() => {
    communityStatsData.clean();
    communityStatsData.generateStandard().then((res) => {
      projectId = res.body.projects.find((p) => p.name === PROJECT_NAME).id;
      login(OWNER, 'admin');
    });
  });

  afterEach(() => {
    communityStatsData.clean();
  });

  const visitPage = () => {
    cy.visit(`${HOST}/projects/${projectId}/community/contributors`);
    waitForGlobalLoading();
  };

  it('shows the activity chart and contributor cards', () => {
    visitPage();
    gcy('community-activity-chart').should('be.visible');
    gcy('community-contributor-card').should('have.length.at.least', 2);
  });

  it('switches the chart between suggestions, accepted and contributions', () => {
    visitPage();
    gcy('community-chart-suggested').should(
      'have.attr',
      'aria-pressed',
      'true'
    );
    gcy('community-chart-accepted').click();
    gcy('community-chart-accepted').should('have.attr', 'aria-pressed', 'true');
    gcy('community-chart-contributions').click();
    gcy('community-chart-contributions').should(
      'have.attr',
      'aria-pressed',
      'true'
    );
  });

  it('links to translations with suggestions', () => {
    visitPage();
    gcy('community-chart-suggestions-link')
      .should('have.attr', 'href')
      .and('include', `/projects/${projectId}/translations`)
      .and('include', 'filterHasSuggestions');
  });

  it('filters contributor cards by search', () => {
    visitPage();
    gcy('community-contributors-search').find('input').type('Alice');
    gcy('community-contributor-card').should('have.length', 1);
    gcy('community-contributors-search').find('input').clear().type('zzz');
    gcy('community-contributors-empty').should('be.visible');
  });

  it('changes the period and keeps it in the url', () => {
    visitPage();
    cy.intercept(/community\/activity\?.*granularity=WEEK/).as('activity');
    gcy('community-contributors-period').click();
    gcyAdvanced({
      value: 'community-contributors-period-item',
      period: 'year',
    }).click();
    cy.wait('@activity')
      .its('request.url')
      .should('include', 'granularity=WEEK');
    cy.location('search').should('include', 'period=year');
    gcy('community-activity-chart').should('be.visible');
  });

  it('restores period, sort and languages from the url', () => {
    cy.intercept(/community\/contributors\?.*languageIds=.*sort=accepted/).as(
      'contributors'
    );
    cy.visit(
      `${HOST}/projects/${projectId}/community/contributors?period=year&sort=accepted&languages=cs`
    );
    cy.wait('@contributors');
    gcy('community-contributor-card').should('have.length.at.least', 2);
  });

  it('deselects a language and keeps the filter in the url', () => {
    visitPage();
    cy.intercept(/community\/contributors\?.*languageIds=/).as('contributors');
    gcy('translations-language-select-form-control').click();
    gcy('translations-language-select-item').first().click();
    dismissMenu();
    cy.wait('@contributors')
      .its('request.url')
      .should('include', 'languageIds=');
    cy.location('search').should('include', 'languages=');
    gcy('community-contributor-card').should('have.length.at.least', 2);
  });

  it('changes the sort order and keeps it in the url', () => {
    visitPage();
    cy.intercept(/community\/contributors\?.*sort=contributions/).as(
      'contributors'
    );
    gcy('community-contributors-sort-button').click();
    gcy('translation-controls-sort-item')
      .should('have.length', 4)
      .last()
      .click();
    cy.wait('@contributors');
    cy.location('search').should('include', 'sort=contributions');
    gcy('community-contributor-card').should('have.length.at.least', 2);
  });
});
