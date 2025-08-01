import {
  createTestProject,
  deleteProject,
  enableNamespaces,
  login,
} from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { visitSingleKey } from '../../common/singleKey';
import { createTranslation } from '../../common/translations';

describe('Project settings namespaces and default namespace', () => {
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

  it('update default namespace properly', () => {
    enableNamespaces(projectId);

    cy.visit(`${HOST}/projects/${projectId}/translations`);
    createTranslation({ namespace: 'test_namespace', key: 'test' });

    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
    setDefaultNamespace('test_namespace');

    cy.visit(`${HOST}/projects/${projectId}/translations`);
    expectDefaultNamespaceInModalCreation('test_namespace');

    cy.visit(`${HOST}/projects/${projectId}/translations/single`);
    cy.gcy('search-select').should('contain', 'test_namespace');
  });

  it('remove default namespace when all keys are removed and selected "none" as a default', () => {
    enableNamespaces(projectId);

    cy.visit(`${HOST}/projects/${projectId}/translations`);
    createTranslation({ namespace: 'test_namespace', key: 'test' });

    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
    setDefaultNamespace('test_namespace');

    deleteNamespaceByDeletingAllKeys('test');

    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
    expectDefaultNamespace('test_namespace');
    setDefaultNamespace('<none>');

    cy.gcy('namespace-value').should('not.contain', 'test_namespace');

    cy.visit(`${HOST}/projects/${projectId}/translations`);
    expectDefaultNamespaceInModalCreation('<none>');
  });

  it('remove default namespace when all keys are removed and selected other as a default', () => {
    enableNamespaces(projectId);

    cy.visit(`${HOST}/projects/${projectId}/translations`);
    createTranslation({ namespace: 'test_namespace1', key: 'test1' });
    createTranslation({ namespace: 'test_namespace2', key: 'test2' });

    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
    setDefaultNamespace('test_namespace1');

    deleteNamespaceByDeletingAllKeys('test1');

    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
    expectDefaultNamespace('test_namespace1');
    setDefaultNamespace('test_namespace2');

    cy.gcy('default-namespace-select').click();
    cy.gcy('namespace-value').should('not.contain', 'test_namespace1');

    cy.visit(`${HOST}/projects/${projectId}/translations`);
    expectDefaultNamespaceInModalCreation('test_namespace2');
  });

  it('default namespace works correctly with single key view', () => {
    enableNamespaces(projectId);

    const key = 'test1';
    const namespace = 'test_namespace1';
    cy.visit(`${HOST}/projects/${projectId}/translations`);
    createTranslation({ namespace, key, translation: 'In namespace' });
    createTranslation({ key, translation: 'Without namespace' });

    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
    setDefaultNamespace(namespace);

    visitSingleKey({ projectId, key, namespace, languages: ['en'] });

    cy.gcy('namespaces-selector').contains(namespace).should('be.visible');
    cy.gcy('translation-text').contains('In namespace').should('be.visible');

    visitSingleKey({ projectId, key: key, languages: ['en'] });

    cy.gcy('namespaces-selector').contains('<none>').should('be.visible');
    cy.gcy('translation-text')
      .contains('Without namespace')
      .should('be.visible');

    visitSingleKey({ projectId, key: 'new_key' });

    cy.gcy('namespaces-selector').contains(namespace).should('be.visible');

    visitSingleKey({ projectId, key: 'new_key', namespace: 'new_namespace' });

    cy.gcy('namespaces-selector')
      .contains('new_namespace')
      .should('be.visible');
  });

  const setDefaultNamespace = (namespace: string) => {
    cy.gcy('default-namespace-select').click();
    cy.gcy('namespace-value').filter(`:contains("${namespace}")`).click();
    expectDefaultNamespace(namespace);
  };

  const deleteNamespaceByDeletingAllKeys = (key: string) => {
    visitSingleKey({ projectId, key });
    cy.gcy('translation-edit-delete-button').click();
    cy.gcy('global-confirmation-confirm').click();
  };

  const expectDefaultNamespace = (namespace: string) => {
    cy.gcy('default-namespace-select').should('contain', namespace);
  };

  const expectDefaultNamespaceInModalCreation = (namespace: string) => {
    cy.gcy('translations-add-button').click();
    cy.gcy('search-select').should('contain', namespace);
  };
});
