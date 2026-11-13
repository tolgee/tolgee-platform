import {
  createProject,
  deleteProject,
  login,
} from '../../common/apiCalls/common';
import { gcyAdvanced, visitProjectSettingsAdvanced } from '../../common/shared';
import { E2CommunityPageSettings } from '../../compounds/projectSettings/E2CommunityPageSettings';

const settings = new E2CommunityPageSettings();

const PROJECT_NAME = 'Public Toggle E2E';

describe('Project public toggle', () => {
  let projectId: number;

  beforeEach(() => {
    login().then(() =>
      createProject({
        name: PROJECT_NAME,
        languages: [
          {
            tag: 'en',
            name: 'English',
            originalName: 'English',
            flagEmoji: '🇬🇧',
          },
        ],
      }).then((r) => {
        projectId = r.body.id;
      })
    );
  });

  afterEach(() => {
    deleteProject(projectId);
  });

  const visitAdvanced = () => visitProjectSettingsAdvanced(projectId);

  const publicSwitchInput = () =>
    cy.gcy('project-settings-public-switch').find('input');

  it('starts private', () => {
    visitAdvanced();
    publicSwitchInput().should('not.be.checked');
  });

  it('cancelling the confirmation leaves the project private', () => {
    visitAdvanced();
    cy.gcy('project-settings-public-switch').click();
    cy.gcy('global-confirmation-cancel').click();
    publicSwitchInput().should('not.be.checked');
    cy.reload();
    publicSwitchInput().should('not.be.checked');
  });

  it('publishes then unpublishes', () => {
    visitAdvanced();

    cy.gcy('project-settings-public-switch').click();
    cy.gcy('global-confirmation-confirm').click();
    publicSwitchInput().should('be.checked');
    cy.gcy('community-setup-dialog').should('be.visible');
    cy.gcy('community-setup-later').click();
    cy.gcy('community-setup-dialog').should('not.exist');
    cy.reload();
    publicSwitchInput().should('be.checked');

    cy.gcy('project-settings-public-switch').click();
    cy.gcy('global-confirmation-confirm').click();
    publicSwitchInput().should('not.be.checked');
    cy.reload();
    publicSwitchInput().should('not.be.checked');
  });

  it('keeps the setup text typed before "Finish later"', () => {
    visitAdvanced();

    cy.gcy('project-settings-public-switch').click();
    cy.gcy('global-confirmation-confirm').click();
    settings.getSetupDescription().type('Typed before finishing later.');
    cy.gcy('community-setup-later').click();
    cy.gcy('community-setup-dialog').should('not.exist');
    settings.getItemStates(true).should('have.length', 1);
  });

  it('keeps the setup open on its step when the save is rejected', () => {
    visitAdvanced();

    cy.gcy('project-settings-public-switch').click();
    cy.gcy('global-confirmation-confirm').click();
    cy.intercept('PUT', '**/community-page', {
      statusCode: 400,
      body: { STANDARD_VALIDATION: { communityDescription: 'invalid' } },
    }).as('rejectedSave');
    settings.getSetupDescription().type('Kept after a rejected save.');
    cy.gcy('community-setup-next').click();
    cy.wait('@rejectedSave');
    cy.gcy('community-setup-next').should('not.be.disabled');
    cy.gcy('community-setup-dialog').should('be.visible');
    settings
      .getSetupDescription()
      .should('be.visible')
      .and('have.value', 'Kept after a rejected save.');
  });

  it('blocks leaving the setup on an invalid app link and saves a bare host with https://', () => {
    visitAdvanced();

    cy.gcy('project-settings-public-switch').click();
    cy.gcy('global-confirmation-confirm').click();
    gcyAdvanced({ value: 'community-setup-step', step: '3' }).click();
    settings.getInContextSwitch().click();
    settings.getInContextAppUrl().type('ftp://orbit');
    cy.gcy('community-setup-next').should('be.disabled');
    cy.gcy('community-setup-later').should('be.disabled');
    settings.getInContextAppUrl().clear().type('localhost:3000');
    cy.gcy('community-setup-next').click();
    cy.gcy('community-setup-dialog').should('not.exist');

    cy.reload();
    settings.openEditor('inContext');
    settings
      .getInContextAppUrl()
      .should('have.value', 'https://localhost:3000');
  });
});
