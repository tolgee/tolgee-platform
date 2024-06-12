import {
  assertMessage,
  clickAdd,
  confirmStandard,
  gcy,
  getPopover,
  selectInSelect,
} from '../../common/shared';
import { getAnyContainingText } from '../../common/xPath';
import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import { apiKeysTestData } from '../../common/apiCalls/testData/testData';
import { setExpiration } from '../../common/apiKeysAndPats';
import { components } from '../../../../webapp/src/service/apiSchema.generated';

type Scope = components['schemas']['ComputedPermissionModel']['scopes'][number];

describe('API keys', () => {
  beforeEach(() => {
    apiKeysTestData.clean();
    apiKeysTestData.generate();
    login('test_username');
    cy.visit(HOST + '/account/apiKeys');
  });

  afterEach(() => {
    apiKeysTestData.clean();
  });

  it('Adds an API key', () => {
    create(
      'test_project',
      ['translations.view', 'translations.edit'],
      'My new api key'
    );
  });

  it('Deletes an api key', () => {
    deleteKey('Oh I am expired');
  });

  it('Edits an api key', () => {
    cy.contains('Oh I am expired')
      .closestDcy('api-key-list-item')
      .findDcy('api-key-list-item-description')
      .click();

    cy.gcy('permissions-advanced-item').contains('Admin').click();

    getPermissionItem('keys.edit').click();
    const newDescription = 'Brand new description';
    cy.gcy('generate-api-key-dialog-description-input')
      .clear()
      .type(newDescription);
    cy.gcy('global-form-save-button').click();
    cy.contains(newDescription)
      .closestDcy('api-key-list-item')
      .should('be.visible')
      .should('not.contain', 'keys.edit');
    assertMessage('API key successfully edited!');
  });

  it('Creates API Key for user with lower permissions', () => {
    login('franta');
    visit();
    clickAdd();
    selectInSelect(cy.gcy('global-form-select'), 'test_project');
    gcy('api-keys-create-edit-dialog')
      .contains('keys.edit')
      .should('not.exist');
    gcy('generate-api-key-dialog-description-input').type('New one');
    cy.gcy('global-form-save-button').click();
    assertMessage('API key successfully created');
  });

  it('Regenerates key', () => {
    const description = 'Oh I am expired';
    cy.contains(description)
      .closestDcy('api-key-list-item')
      .findDcy('api-key-list-item-regenerate-button')
      .click();
    setExpiration('Custom date', '09/20/2050');
    gcy('global-form-save-button').click();
    cy.waitForDom();
    cy.contains(description)
      .closestDcy('api-key-list-item')
      .findDcy('api-key-list-item-new-token-input')
      .find('input')
      .should('contain.value', 'tgpak_');
    cy.contains('Expires on Tuesday, September 20, 2050').should('be.visible');
  });
});

const visit = () => {
  cy.visit(HOST + '/account/apiKeys');
};

const getCheckbox = (scope: Scope) => {
  return cy
    .gcy('permissions-advanced-item')
    .filter(`[permissions-scope="${scope}"]`);
};

const unselectAll = () => {
  getCheckbox('screenshots.delete').click();
  getCheckbox('screenshots.upload').click();
  getCheckbox('screenshots.view').click();
  getCheckbox('translations.state-edit').click();
  getCheckbox('translations.edit').click();
  getCheckbox('translations.view').click();
  getCheckbox('keys.edit').click();
  getCheckbox('keys.create').click();
  getCheckbox('keys.view').click();

  cy.gcy('permissions-advanced-item')
    .find('input[type="checkbox"]')
    .should('not.be.checked');
};

const getPermissionItem = (scope: Scope) => {
  return cy.get(`[aria-label="${scope}"]`);
};

const create = (project: string, scopes: Scope[], description) => {
  clickAdd();
  cy.waitForDom();
  cy.gcy('generate-api-key-dialog-description-input').type(description);
  cy.gcy('global-form-select').click();
  getPopover().contains(project).click();
  unselectAll();
  scopes.forEach((s) => {
    getPermissionItem(s).click();
  });

  cy.xpath(getAnyContainingText('Save', 'button')).click();
  cy.waitForDom();
  cy.contains(description)
    .closestDcy('api-key-list-item')
    .should('be.visible')
    .should('contain.text', 'API key created.');
  cy.contains(description)
    .closestDcy('api-key-list-item')
    .findDcy('api-key-list-item-new-token-input')
    .find('input')
    .should('contain.value', 'tgpak_');
  scopes.forEach((scope) =>
    cy
      .contains(description)
      .closestDcy('api-key-list-item')
      .should('contain', scope)
  );
  assertMessage('API key successfully created');
};

const deleteKey = (description: string) => {
  cy.contains(description)
    .closestDcy('api-key-list-item')
    .findDcy('api-key-list-item-delete-button')
    .click();
  confirmStandard();
  cy.contains(description).should('not.exist');
  assertMessage('API key deleted');
};
