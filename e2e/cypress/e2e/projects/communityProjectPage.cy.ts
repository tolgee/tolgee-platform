import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import { communityStatsData } from '../../common/apiCalls/testData/testData';
import { gcy } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';
import { E2CommunityPageSettings } from '../../compounds/projectSettings/E2CommunityPageSettings';

const settings = new E2CommunityPageSettings();

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

  const stubRecentTopTotal = (total: number) => {
    const counts = { year: 0 };
    cy.intercept(
      { method: 'GET', url: /\/community\/contributors\?/ },
      (req) => {
        const params = new URL(req.url).searchParams;
        if (params.get('size') !== '3') return;
        const daysAgo =
          (Date.now() - Date.parse(params.get('from')!)) / 86_400_000;
        if (daysAgo < 60) {
          req.continue((res) => {
            res.body.page.totalElements = total;
          });
          return;
        }
        counts.year++;
      }
    );
    return counts;
  };

  it('shows the last 30 days when at least 3 people contributed in them', () => {
    const counts = stubRecentTopTotal(3);
    visitPage();
    gcy('community-top-contributors-period').should(
      'have.attr',
      'data-cy-period',
      'month'
    );
    cy.wrap(counts).its('year').should('eq', 0);
  });

  it('falls back to the last 12 months when fewer than 3 contributed in the last 30 days', () => {
    const counts = stubRecentTopTotal(2);
    visitPage();
    gcy('community-top-contributors-period').should(
      'have.attr',
      'data-cy-period',
      'year'
    );
    cy.wrap(counts).its('year').should('eq', 1);
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
    settings.getItemStates(false).should('have.length', 4);

    settings.openEditor('description');
    settings.getDialogText().type('A demo project for community translation.');
    gcy('global-form-save-button').click();
    gcy('community-page-dialog').should('not.exist');

    settings.getItemStates(true).should('have.length', 1);

    settings.openEditor('guidelines');
    settings.getDialogText().type('Keep product names in English.');
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
    settings.openEditor('description');
    settings.getDialogText().type('Still here after the server says no.');
    gcy('global-form-save-button').click();
    cy.wait('@rejectedSave');
    gcy('global-form-save-button').should('not.be.disabled');
    gcy('community-page-dialog').should('be.visible');
    settings
      .getDialogText()
      .should('have.value', 'Still here after the server says no.');
  });

  it('blocks an invalid app link and drops it when the banner is turned off', () => {
    visitSettings();
    settings.openEditor('inContext');
    gcy('community-in-context-app-url').should('not.exist');
    settings.getInContextSwitch().click();
    settings.getInContextAppUrl().type('ftp://orbit');
    gcy('community-in-context-close').should('be.disabled');
    settings.getInContextSwitch().click();
    gcy('community-in-context-close').click();
    gcy('community-in-context-dialog').should('not.exist');
    settings.getItemStates(true).should('not.exist');

    settings.openEditor('inContext');
    settings.getInContextSwitch().click();
    settings.getInContextAppUrl().should('have.value', '');
  });

  it('shows the in-app banner with the app link when turned on', () => {
    visitPage();
    gcy('community-in-app-banner').should('not.exist');

    visitSettings();
    settings.openEditor('inContext');
    settings.getInContextSwitch().click();
    settings.getInContextAppUrl().type('orbit-notes.example.com');
    gcy('community-in-context-close').click();
    gcy('community-in-context-dialog').should('not.exist');
    settings.getItemStates(true).should('have.length', 1);

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
    settings.openEditor('share');
    gcy('community-share-link')
      .invoke('val')
      .should('contain', `/projects/${projectId}/community`);
    gcy('community-share-mark-shared').find('input').click();
    gcy('community-share-close').click();
    settings.getItemStates(true).should('have.length', 1);
  });
});
