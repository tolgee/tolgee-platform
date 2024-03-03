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
import { testExportFormats } from './export/exportFormats.cy';

describe('Content delivery', () => {
  let projectId: number;
  beforeEach(() => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
    setContentStorageBypass(true);
    contentDeliveryTestData.clean();
    contentDeliveryTestData.generateStandard().then((response) => {
      login();
      projectId = response.body.projects[0].id;
      visitProjectDeveloperContentDelivery(projectId);
    });
  });

  afterEach(() => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
    setContentStorageBypass(false);
  });

  it('publishes content manually', () => {
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-publish')
      .click();
    waitForGlobalLoading();
    assertMessage('Content published sucessfuly!');
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

  it.only('updates content delivery config with proper export params ', () => {
    function openEditDialog() {
      gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
        .findDcy('content-delivery-item-type')
        .contains('Manual');
      gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
        .findDcy('content-delivery-item-edit')
        .click();
    }

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
        openEditDialog();
        if (test.expectedParams.supportArrays) {
          cy.gcy('export-support_arrays-selector')
            .find('input')
            .should('be.checked');
        }
        gcy('export-format-selector').should('contain', test.format);
        dismissMenu();
      }
    );
  });

  it('updates existing content delivery', () => {
    const name = 'Azure edited';
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-type')
      .contains('Manual');
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-edit')
      .click();
    cy.gcy('content-delivery-form-name').clear().type(name);
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

function fillContentDeliveryConfigForm(name: string) {
  cy.gcy('content-delivery-form-name').clear().type(name);
  cy.gcy('content-delivery-storage-selector').click();
  cy.gcy('content-delivery-storage-selector-item').contains('Azure').click();
}

function createAzureContentDeliveryConfig(name: string) {
  cy.gcy('content-delivery-add-button').click();
  fillContentDeliveryConfigForm(name);
  cy.gcy('content-delivery-form-save').click();
}
