import {
  allScopes,
  assertMessage,
  clickAdd,
  getPopover,
} from '../../common/shared';
import {
  getAnyContainingText,
  getClosestContainingText,
} from '../../common/xPath';
import { HOST } from '../../common/constants';
import {
  cleanProjectsData,
  createApiKey,
  createProject,
  createProjectsData,
  deleteProject,
  login,
} from '../../common/apiCalls';
import { Scope } from '../../common/types';
import { ApiKeyDTO } from '../../../../webapp/src/service/response.types';

describe('Api keys', () => {
  let project;

  describe('As admin', () => {
    beforeEach(() => {
      login();
      createProject({
        name: 'Test',
        languages: [{ tag: 'en', name: 'English', originalName: 'English' }],
      }).then((r) => (project = r.body));
      cy.visit(HOST + '/apiKeys');
    });

    afterEach(() => {
      cy.wrap(null).then(() => deleteProject(project.id));
    });

    it('Will add an api key', () => {
      create('Test', ['translations.view', 'translations.edit']);
      assertMessage('API key successfully created');
      cy.contains('translations.view').should('be.visible');
      cy.contains('translations.edit').should('be.visible');
    });

    it('Will delete an api key', () => {
      createApiKey({
        projectId: project.id,
        scopes: ['keys.edit', 'keys.edit', 'translations.view'],
      }).then((key: ApiKeyDTO) => {
        cy.reload();
        cy.contains('API key:').should('be.visible');
        del(key.key);
        cy.contains('API key successfully deleted!').should('be.visible');
      });
    });

    it('Will edit an api key', () => {
      createApiKey({
        projectId: project.id,
        scopes: ['keys.edit', 'keys.edit', 'translations.view'],
      }).then((key: ApiKeyDTO) => {
        cy.reload();
        cy.contains('API key:').should('be.visible');
        cy.gcy('api-keys-edit-button').eq(0).click();
        cy.gcy('api-keys-create-edit-dialog')
          .contains('translations.edit')
          .click();
        cy.gcy('api-keys-create-edit-dialog').contains('keys.edit').click();
        cy.gcy('global-form-save-button').click();
        assertMessage('API key successfully edited!');
      });
    });
  });

  it('will create API Key for user with lower permissions', () => {
    cleanProjectsData();
    createProjectsData();
    login('cukrberg@facebook.com', 'admin');
    visit();
    clickAdd();
    cy.gcy('global-form-select').click();
    cy.gcy('api-keys-project-select-item')
      .contains("Vaclav's cool project")
      .click();
    cy.gcy('global-form-save-button').click();
    assertMessage('API key successfully created');
  });
});

const visit = () => {
  cy.visit(HOST + '/apiKeys');
};

const create = (project: string, scopes: Scope[]) => {
  clickAdd();
  cy.gcy('global-form-select').click();
  getPopover().contains(project).click();
  const toRemove = new Set(allScopes);
  scopes.forEach((s) => toRemove.delete(s));
  toRemove.forEach((s) =>
    cy.contains('Generate API key').xpath(getClosestContainingText(s)).click()
  );
  cy.xpath(getAnyContainingText('Save', 'button')).click();
};

const del = (key) => {
  cy.wait(500);
  cy.xpath(getAnyContainingText(key))
    .last()
    .xpath("(./ancestor::*//*[@aria-label='delete'])[1]")
    .scrollIntoView({ offset: { top: -500, left: 0 } })
    .click();
  cy.xpath(getAnyContainingText('confirm', 'span')).click();
};
