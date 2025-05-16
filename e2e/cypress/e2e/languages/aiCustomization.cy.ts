import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { createProjectWithThreeLanguages } from '../../common/translations';
import { deleteProject } from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { gcyAdvanced } from '../../common/shared';

describe('Machine translation settings', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    createProjectWithThreeLanguages().then((p) => {
      project = p;
      visit();
    });
  });

  afterEach(() => {
    if (project) {
      deleteProject(project.id);
    }
  });

  it('will update project description', { retries: 5 }, () => {
    const description = 'Tolgee is software localization platform';
    cy.gcy('ai-customization-project-description-add').click();
    cy.gcy('project-ai-prompt-dialog-description-input').type(description);
    cy.gcy('project-ai-prompt-dialog-save').click();
    waitForGlobalLoading();
    cy.gcy('ai-customization-project-description')
      .contains(description)
      .should('be.visible');
  });

  it('will update language description', { retries: 5 }, () => {
    const description = 'Czech is a difficult language';
    gcyAdvanced({
      value: 'ai-languages-description-edit',
      language: 'cs',
    }).click();
    cy.gcy('language-ai-prompt-dialog-description-input').type(description);
    cy.gcy('language-ai-prompt-dialog-save').click();
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'ai-languages-description',
      language: 'cs',
    })
      .contains(description)
      .should('be.visible');
  });

  const visit = () => {
    cy.visit(`${HOST}/projects/${project.id}/ai/context-data`);
  };
});
