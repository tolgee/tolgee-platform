import {
  createProject,
  deleteProject,
  login,
} from '../../common/apiCalls/common';
import { visitProjectSettingsAdvanced } from '../../common/shared';

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
    cy.reload();
    publicSwitchInput().should('be.checked');

    cy.gcy('project-settings-public-switch').click();
    cy.gcy('global-confirmation-confirm').click();
    publicSwitchInput().should('not.be.checked');
    cy.reload();
    publicSwitchInput().should('not.be.checked');
  });
});
