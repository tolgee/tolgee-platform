import {
  createTestProject,
  deleteProject,
  login,
} from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';

describe('Projects Basics', () => {
  let projectId: number;
  beforeEach(() => {
    login().then(() =>
      createTestProject().then((r) => {
        projectId = r.body.id;
      })
    );
  });

  afterEach(() => {
    deleteProject(projectId);
  });

  it('updates project settings', () => {
    cy.visit(`${HOST}/projects/${projectId}/manage/edit`);
    cy.gcy('project-settings-name').find('input').clear().type('New name');

    cy.gcy('project-settings-description')
      .find('textarea')
      .first()
      .type('Test description');

    cy.gcy('default-namespace-select').should('not.exist');

    cy.gcy('global-form-save-button').click();
    cy.reload();
    cy.gcy('project-settings-name')
      .find('input')
      .should('have.value', 'New name');

    cy.gcy('project-settings-description')
      .contains('Test description')
      .should('be.visible');

    // shows description on dashboard page
    cy.visit(`${HOST}/projects/${projectId}`);
    cy.gcy('project-dashboard-description')
      .contains('Test description')
      .should('be.visible');
  });
});
