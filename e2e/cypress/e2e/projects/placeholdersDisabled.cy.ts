import { login } from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { createProject } from '../../common/projects';
import { selectInProjectMenu } from '../../common/shared';

describe('disabled placeholders project', () => {
  it('creates project with disabled placeholders', () => {
    login();
    const name = 'Disabled placeholders project';
    cy.visit(`${HOST}`);
    createProject(name, 'admin');
    selectInProjectMenu('Project settings');
    cy.gcy('project-settings-menu-advanced').click();
    cy.gcy('project-settings-use-tolgee-placeholders-checkbox').click();
    waitForGlobalLoading();
    cy.reload();
    cy.gcy('project-settings-use-tolgee-placeholders-checkbox')
      .find('input')
      .should('not.be.checked');
  });
});
