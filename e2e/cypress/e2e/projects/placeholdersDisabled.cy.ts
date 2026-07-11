import { login } from '../../common/apiCalls/common';
import { projectTestData } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { createProject } from '../../common/projects';
import { selectInProjectMenu } from '../../common/shared';

describe('disabled placeholders project', () => {
  beforeEach(() => {
    projectTestData.clean();
    projectTestData.generate();
    login('cukrberg@facebook.com', 'admin');
  });

  afterEach(() => {
    projectTestData.clean();
  });

  it('creates project with disabled placeholders', () => {
    const name = 'Disabled placeholders project';
    cy.visit(`${HOST}`);
    createProject(name, 'Facebook');
    selectInProjectMenu('Project settings');
    cy.gcy('project-settings-menu-advanced').click();
    cy.gcy('project-settings-use-tolgee-placeholders-switch').click();
    waitForGlobalLoading();
    cy.reload();
    cy.gcy('project-settings-use-tolgee-placeholders-switch')
      .find('input')
      .should('not.be.checked');
  });
});
