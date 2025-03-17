import {
  assertMessage,
  confirmStandard,
  dismissMenu,
  gcy,
  gcyAdvanced,
  visitProjectDeveloperContentDelivery,
} from '../../common/shared';
import { contentDeliveryTestData } from '../../common/apiCalls/testData/testData';
import { login, setContentStorageBypass } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { setFeature } from '../../common/features';
import { FormatTest, testExportFormats } from '../../common/export';

describe('Content delivery', () => {
  let projectId: number;
  beforeEach(() => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
    setContentStorageBypass(true);
    contentDeliveryTestData.clean();
    contentDeliveryTestData.generateStandard().then((response) => {
      login('test_username');
      projectId = response.body.projects[0].id;
      visitProjectDeveloperContentDelivery(projectId);
    });
  });

  afterEach(() => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
    setContentStorageBypass(false);
    contentDeliveryTestData.clean();
  });

  it('publishes content manually and shows files', () => {
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-publish')
      .click();
    waitForGlobalLoading();
    assertMessage('Content published successfully!');
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-files-button')
      .click();
    gcy('content-delivery-published-file').should('contain', 'en.json');
  });

  it('creates content delivery', () => {
    const name = 'Crazy content delivery';
    createAzureContentDeliveryConfig(name);
    waitForGlobalLoading();
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' }).should(
      'be.visible'
    );
    assertMessage('Content delivery successfully created!');
  });

  it('show custom slug only for custom storage', () => {
    cy.gcy('content-delivery-add-button').click();
    fillContentDeliveryConfigForm('Custom slug');
    waitForGlobalLoading();
    gcy('content-delivery-form-custom-slug').should('be.visible');
    selectContentStorage('Default');
    gcy('content-delivery-form-custom-slug').should('not.exist');
    selectContentStorage('Azure');
    gcy('content-delivery-form-custom-slug').find('input').type('my-slug');
    saveForm();
    waitForGlobalLoading();
    openEditDialog('Custom slug');
    gcy('content-delivery-form-custom-slug')
      .find('input')
      .should('have.value', 'my-slug');
    selectContentStorage('Default');
    saveForm();
    openEditDialog('Custom slug');
    gcy('content-delivery-form-custom-slug').should('not.exist');
    selectContentStorage('Azure');
    gcy('content-delivery-form-custom-slug')
      .find('input')
      .invoke('val')
      .should('have.length', 32);
  });

  it('stores prune before publishing', () => {
    cy.gcy('content-delivery-add-button').click();
    fillContentDeliveryConfigForm('Pruning');
    cy.gcy('content-delivery-prune-before-publish-checkbox')
      .find('input')
      .should('be.checked')
      .click();
    saveForm();
    waitForGlobalLoading();
    openEditDialog('Pruning');
    cy.gcy('content-delivery-prune-before-publish-checkbox')
      .find('input')
      .should('not.be.checked')
      .click();
    saveForm();
    openEditDialog('Pruning');
    cy.gcy('content-delivery-prune-before-publish-checkbox')
      .find('input')
      .should('be.checked');
  });

  it('creates content delivery config with proper export params ', () => {
    testExportFormats(
      () => {
        cy.gcy('content-delivery-add-button').click();
        fillContentDeliveryConfigForm('CD');
        return cy.intercept('POST', '/v2/projects/**/content-delivery-configs');
      },
      () => {
        cy.gcy('content-delivery-form-save').click();
      },
      false,
      () => {}
    );
  });

  it('updates content delivery config with proper export params ', () => {
    testExportFormats(
      () => {
        openEditDialog();
        fillContentDeliveryConfigForm('Azure');
        return cy.intercept(
          'PUT',
          '/v2/projects/**/content-delivery-configs/**'
        );
      },
      () => {
        cy.gcy('content-delivery-form-save').click();
      },
      false,
      (test) => {
        // we need to also test that the saved props are correctly dsplayed, since the logic is not
        // super simple
        verifyPublishType();
        openEditDialog();
        if (test.expectedParams.supportArrays) {
          cy.gcy('export-support_arrays-selector')
            .find('input')
            .should('be.checked');
        }
        gcy('export-format-selector').should('contain', test.format);
        testMessageFormatPersists(test);
        dismissMenu();
      }
    );
  });

  it('stores content delivery configuration for XLIFF format with HTML escaping', () => {
    cy.gcy('content-delivery-add-button').click();
    fillContentDeliveryConfigForm('XLIFF Test');

    // Select XLIFF format
    cy.gcy('export-format-selector').click();
    cy.gcy('export-format-selector-item').contains('XLIFF').click();

    // Enable HTML escaping
    cy.gcy('export-escape_html-selector')
      .find('input')
      .should('not.be.checked')
      .click();

    // Save the configuration
    saveForm();
    waitForGlobalLoading();

    // Verify the settings persist
    openEditDialog('XLIFF Test');
    cy.gcy('export-escape_html-selector').find('input').should('be.checked');

    // Verify format is still XLIFF
    gcy('export-format-selector').should('contain', 'XLIFF');
  });

  it('updates existing content delivery', () => {
    const name = 'Azure edited';
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-type')
      .contains('Manual');
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-edit')
      .click();
    cy.gcy('content-delivery-form-name').find('input').clear().type(name);
    cy.gcy('content-delivery-auto-publish-checkbox').click();
    cy.gcy('content-delivery-form-save').click();
    waitForGlobalLoading();
    gcyAdvanced({ value: 'content-delivery-list-item', name }).should(
      'be.visible'
    );
    assertMessage('Content delivery successfully updated!');
    gcyAdvanced({ value: 'content-delivery-list-item', name })
      .findDcy('content-delivery-item-type')
      .contains('Auto');
  });

  it('deletes content delivery', () => {
    deleteContentDeliveryConfig('Azure');
  });

  it('shows info if feature not enabled', () => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', false);
    cy.reload();
    cy.contains('You are over limit, modification is restricted').should(
      'be.visible'
    );

    deleteContentDeliveryConfig('Azure');
    deleteContentDeliveryConfig('S3');
    deleteContentDeliveryConfig('Custom Slug');

    cy.contains('Only single content delivery configuration enabled');
    cy.gcy('content-delivery-add-button').should('be.disabled');
  });

  function deleteContentDeliveryConfig(name: string) {
    gcyAdvanced({ value: 'content-delivery-list-item', name })
      .findDcy('content-delivery-item-edit')
      .click();
    cy.gcy('content-delivery-delete-button').click();
    confirmStandard();
    waitForGlobalLoading();
    assertMessage('Content delivery successfully deleted!');
    cy.gcy('content-delivery-list-item').contains(name).should('not.exist');
  }
});

function selectContentStorage(storage = 'Azure') {
  cy.gcy('content-delivery-storage-selector').click();
  cy.gcy('content-delivery-storage-selector-item').contains(storage).click();
}

function fillContentDeliveryConfigForm(name: string) {
  cy.gcy('content-delivery-form-name').find('input').clear().type(name);
  selectContentStorage();
}

function createAzureContentDeliveryConfig(name: string) {
  cy.gcy('content-delivery-add-button').click();
  fillContentDeliveryConfigForm(name);
  saveForm();
}

function saveForm() {
  cy.gcy('content-delivery-form-save').click();
}

function testMessageFormatPersists(test: FormatTest) {
  if (test.messageFormat) {
    gcy('export-message-format-selector').should('contain', test.messageFormat);
    return;
  }
  gcy('export-message-format-selector').should('not.exist');
}

function verifyPublishType(name = 'Azure', content: any = 'Manual') {
  gcyAdvanced({ value: 'content-delivery-list-item', name: name })
    .findDcy('content-delivery-item-type')
    .contains(content);
}

function openEditDialog(name = 'Azure') {
  gcyAdvanced({ value: 'content-delivery-list-item', name })
    .findDcy('content-delivery-item-edit')
    .click();
}
