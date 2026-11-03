import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import { communityStatsData } from '../../common/apiCalls/testData/testData';
import { gcy, gcyAdvanced } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

const OWNER = 'communityStatsOwner';
const PROJECT_NAME = 'Community Stats Demo';

describe('Community project page', () => {
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
    cy.visit(`${HOST}/projects/${projectId}/community`);
    waitForGlobalLoading();
  };

  const visitSettings = () => {
    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
    waitForGlobalLoading();
  };

  it('shows top contributors with their period and links to all contributors', () => {
    visitPage();
    gcy('community-top-contributor').should('have.length', 3);
    gcy('community-top-contributors-period')
      .invoke('attr', 'data-cy-period')
      .then((period) => {
        expect(['month', 'year']).to.include(period);
        gcy('community-all-contributors')
          .should('have.attr', 'href')
          .and('include', `/community/contributors?period=${period}`);
      });
  });

  it('collapses and expands a sidebar section', () => {
    visitPage();
    gcy('community-top-contributors').within(() => {
      gcy('community-section-toggle').click();
    });
    gcy('community-top-contributor').should('not.exist');
    gcy('community-top-contributors').within(() => {
      gcy('community-section-toggle').click();
    });
    gcy('community-top-contributor').should('have.length', 3);
  });

  it('edits the community page texts in project settings', () => {
    visitSettings();
    gcyAdvanced({
      value: 'project-settings-community-page-item-state',
      done: 'false',
    }).should('have.length', 4);

    gcyAdvanced({
      value: 'project-settings-community-page-edit',
      item: 'description',
    }).click();
    gcy('community-page-dialog-text')
      .find('textarea')
      .first()
      .type('A demo project for community translation.');
    gcy('global-form-save-button').click();
    gcy('community-page-dialog').should('not.exist');

    gcyAdvanced({
      value: 'project-settings-community-page-item-state',
      done: 'true',
    }).should('have.length', 1);

    gcyAdvanced({
      value: 'project-settings-community-page-edit',
      item: 'guidelines',
    }).click();
    gcy('community-page-dialog-text')
      .find('textarea')
      .first()
      .type('Keep product names in English.');
    gcy('global-form-save-button').click();
    gcy('community-page-dialog').should('not.exist');

    visitPage();
    gcy('community-project-description').should(
      'contain',
      'A demo project for community translation.'
    );
    gcy('community-guidelines').should(
      'contain',
      'Keep product names in English.'
    );
  });

  it('collapses the community page box in settings', () => {
    visitSettings();
    gcy('project-settings-community-page-item').should('have.length', 4);
    gcy('project-settings-community-page-toggle').click();
    gcy('project-settings-community-page-item').should('not.be.visible');
  });

  it('keeps a rejected save open with the typed text', () => {
    visitSettings();
    cy.intercept('PUT', '**/community-page', {
      statusCode: 400,
      body: { STANDARD_VALIDATION: { communityDescription: 'invalid' } },
    }).as('rejectedSave');
    gcyAdvanced({
      value: 'project-settings-community-page-edit',
      item: 'description',
    }).click();
    gcy('community-page-dialog-text')
      .find('textarea')
      .first()
      .type('Still here after the server says no.');
    gcy('global-form-save-button').click();
    cy.wait('@rejectedSave');
    gcy('global-form-save-button').should('not.be.disabled');
    gcy('community-page-dialog').should('be.visible');
    gcy('community-page-dialog-text')
      .find('textarea')
      .first()
      .should('have.value', 'Still here after the server says no.');
  });

  it('blocks an invalid app link and drops it when the banner is turned off', () => {
    visitSettings();
    gcyAdvanced({
      value: 'project-settings-community-page-edit',
      item: 'inContext',
    }).click();
    gcy('community-in-context-app-url').should('not.exist');
    gcy('community-in-context-switch').find('input').click();
    gcy('community-in-context-app-url').find('input').type('ftp://orbit');
    gcy('community-in-context-close').should('be.disabled');
    gcy('community-in-context-switch').find('input').click();
    gcy('community-in-context-close').click();
    gcy('community-in-context-dialog').should('not.exist');
    gcyAdvanced({
      value: 'project-settings-community-page-item-state',
      done: 'true',
    }).should('not.exist');

    gcyAdvanced({
      value: 'project-settings-community-page-edit',
      item: 'inContext',
    }).click();
    gcy('community-in-context-switch').find('input').click();
    gcy('community-in-context-app-url').find('input').should('have.value', '');
  });

  it('shows the in-app banner with the app link when turned on', () => {
    visitPage();
    gcy('community-in-app-banner').should('not.exist');

    visitSettings();
    gcyAdvanced({
      value: 'project-settings-community-page-edit',
      item: 'inContext',
    }).click();
    gcy('community-in-context-switch').find('input').click();
    gcy('community-in-context-app-url')
      .find('input')
      .type('orbit-notes.example.com');
    gcy('community-in-context-close').click();
    gcy('community-in-context-dialog').should('not.exist');
    gcyAdvanced({
      value: 'project-settings-community-page-item-state',
      done: 'true',
    }).should('have.length', 1);

    visitPage();
    gcy('community-in-app-banner').should('be.visible');
    gcy('community-in-app-open').should(
      'have.attr',
      'href',
      'https://orbit-notes.example.com'
    );
  });

  it('marks the project page as shared', () => {
    visitSettings();
    gcyAdvanced({
      value: 'project-settings-community-page-edit',
      item: 'share',
    }).click();
    gcy('community-share-link')
      .invoke('val')
      .should('contain', `/projects/${projectId}/community`);
    gcy('community-share-mark-shared').find('input').click();
    gcy('community-share-close').click();
    gcyAdvanced({
      value: 'project-settings-community-page-item-state',
      done: 'true',
    }).should('have.length', 1);
  });
});
