import {
  assertMessage,
  confirmStandard,
  gcyAdvanced,
  visitProjectDeveloperStorage,
} from '../../common/shared';
import { contentDeliveryTestData } from '../../common/apiCalls/testData/testData';
import { login, setContentStorageBypass } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { setFeature } from '../../common/features';

describe('Content storage', () => {
  beforeEach(() => {
    setFeature('PROJECT_LEVEL_CONTENT_STORAGES', true);
    setContentStorageBypass(true);
    contentDeliveryTestData.clean();
    contentDeliveryTestData.generateStandard().then((response) => {
      login('test_username');
      const projectId = response.body.projects[0].id;
      visitProjectDeveloperStorage(projectId);
    });
  });

  afterEach(() => {
    setFeature('PROJECT_LEVEL_CONTENT_STORAGES', true);
  });

  after(() => {
    setContentStorageBypass(false);
  });

  it('creates new azure storage', () => {
    const name = 'New exciting storage';
    cy.gcy('storage-add-item-button').click();
    cy.gcy('storage-form-type-azure').click();
    fillAzure(name);
    cy.gcy('storage-form-test').click();
    assertMessage('Configuration is valid');
    cy.gcy('storage-form-save').click();
    assertMessage('Storage successfully created!');
    gcyAdvanced({ value: 'storage-list-item', name }).should('be.visible');
  });

  it('creates new s3 storage', () => {
    const name = 'New exciting storage';
    cy.gcy('storage-add-item-button').click();
    cy.gcy('storage-form-type-s3').click();

    fillS3(name);

    cy.gcy('storage-form-test').click();
    assertMessage('Configuration is valid');
    cy.gcy('storage-form-save').click();
    assertMessage('Storage successfully created!');
    gcyAdvanced({ value: 'storage-list-item', name }).should('be.visible');
  });

  it('updates existing content storage', () => {
    const name = 'Azure edited';
    gcyAdvanced({ value: 'storage-list-item', name: 'Azure' })
      .findDcy('storage-item-edit')
      .click();

    updateAzure(name);

    cy.gcy('storage-form-test').click();
    assertMessage('Configuration is valid');

    cy.gcy('storage-form-save').click();
    waitForGlobalLoading();
    gcyAdvanced({ value: 'storage-list-item', name }).should('be.visible');
    assertMessage('Content storage successfully updated!');
  });

  it('deletes content storage', () => {
    const name = 'New exciting storage';
    cy.gcy('storage-add-item-button').click();
    cy.gcy('storage-form-type-azure').click();
    fillAzure(name);
    cy.gcy('storage-form-save').click();
    assertMessage('Storage successfully created!');

    gcyAdvanced({ value: 'storage-list-item', name })
      .findDcy('storage-item-edit')
      .click();

    cy.gcy('storage-form-delete').click();

    confirmStandard();
    waitForGlobalLoading();
    assertMessage('Storage successfully deleted!');
    cy.gcy('storage-list-item').contains(name).should('not.exist');
  });

  it('throws an error when deleting a storage in use', () => {
    gcyAdvanced({ value: 'storage-list-item', name: 'Azure' })
      .findDcy('storage-item-edit')
      .click();

    cy.gcy('storage-form-delete').click();

    confirmStandard();
    waitForGlobalLoading();
    assertMessage("Content storage can't be deleted");
  });

  it('fails on incorrect configuration on create', () => {
    setContentStorageBypass(false);
    const name = 'New exciting storage';
    cy.gcy('storage-add-item-button').click();
    cy.gcy('storage-form-type-azure').click();
    fillAzure(name);
    cy.gcy('storage-form-test').click();
    assertMessage('Storage test failed');
  });

  it('fails on incorrect configuration on edit', () => {
    setContentStorageBypass(false);
    const name = 'Azure edited';
    gcyAdvanced({ value: 'storage-list-item', name: 'Azure' })
      .findDcy('storage-item-edit')
      .click();

    updateAzure(name);
    cy.gcy('storage-form-test').click();
    assertMessage('Storage test failed');
  });

  it('shows info if feature not enabled', () => {
    setFeature('PROJECT_LEVEL_CONTENT_STORAGES', false);
    cy.reload();
    cy.contains('You are over limit, modification is restricted').should(
      'be.visible'
    );
    cy.gcy('storage-add-item-button').should('be.disabled');
  });

  function updateAzure(name: string) {
    cy.gcy('storage-form-name').clear().type(name);
    cy.gcy('storage-form-public-url-prefix').clear().type('test');
  }

  function fillAzure(name: string) {
    cy.gcy('storage-form-name').find('input').type(name);
    cy.gcy('storage-form-azure-connection-string').find('input').type('fake');
    cy.gcy('storage-form-azure-container-name').find('input').type('fake');
    cy.gcy('storage-form-public-url-prefix').find('input').type('fake');
  }

  function fillS3(name: string) {
    cy.gcy('storage-form-name').find('input').type(name);

    cy.gcy('storage-form-s3-bucket-name').find('input').type('fake');
    cy.gcy('storage-form-s3-access-key').find('input').type('fake');
    cy.gcy('storage-form-s3-secret-key').find('input').type('fake');
    cy.gcy('storage-form-s3-endpoint').find('input').type('fake');
    cy.gcy('storage-form-s3-signing-region').find('input').type('fake');

    cy.gcy('storage-form-public-url-prefix').find('input').type('fake');
  }
});
