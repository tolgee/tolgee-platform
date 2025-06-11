import { login } from '../../common/apiCalls/common';
import { prompt } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import {
  assertMessage,
  confirmStandard,
  gcy,
  gcyAdvanced,
} from '../../common/shared';

describe('basic prompt', () => {
  beforeEach(() => {
    prompt.clean();
    prompt
      .generateStandard()
      .then((r) => r.body)
      .then((data) => {
        const organization = data.organizations.find(
          (org) => org.name === 'my organization'
        );
        const user = data.users.find((u) =>
          [u.username, u.name].includes('owner@organization.com')
        );
        login(user.username);
        visitLlmProviders(organization.slug);
      });
  });

  it('can create llm provider', () => {
    gcy('global-plus-button').click();
    gcyAdvanced({ value: 'llm-provider-form-text-field', name: 'name' }).type(
      'new-provider'
    );
    gcyAdvanced({ value: 'llm-provider-form-text-field', name: 'apiUrl' }).type(
      'https://test.com'
    );
    gcyAdvanced({ value: 'llm-provider-form-text-field', name: 'apiKey' }).type(
      'secret key'
    );
    gcyAdvanced({
      value: 'llm-provider-form-text-field',
      name: 'model',
    }).type('some model');
    gcy('llm-provider-form-priority-select').click();
    gcy('llm-provider-form-priority-select-item').contains('High').click();

    gcy('llm-provider-create-dialog-submit').click();
    gcy('llm-provider-item-name').should('contain', 'new-provider');
  });

  it('can edit llm provider', () => {
    gcy('llm-provider-item-name').should('contain', 'organization-provider');
    gcyAdvanced({
      value: 'llm-provider-item-type',
      name: 'organization-provider',
    }).should('contain', 'OpenAI');
    gcy('llm-provider-item-name')
      .should('contain', 'organization-provider')
      .click();
    gcy('llm-provider-form-type-select').click();
    gcy('llm-provider-form-type-select-item').contains('Azure OpenAI').click();
    gcyAdvanced({ value: 'llm-provider-form-text-field', name: 'name' }).type(
      'custom-provider'
    );
    gcyAdvanced({ value: 'llm-provider-form-text-field', name: 'apiUrl' }).type(
      'https://test.com'
    );
    gcyAdvanced({ value: 'llm-provider-form-text-field', name: 'apiKey' }).type(
      'secret key'
    );
    gcyAdvanced({
      value: 'llm-provider-form-text-field',
      name: 'deployment',
    }).type('mock');
    gcy('llm-provider-create-dialog-update').click();
    gcy('llm-provider-item-name').should('contain', 'custom-provider');
    gcyAdvanced({
      value: 'llm-provider-item-type',
      name: 'custom-provider',
    }).should('contain', 'Azure OpenAI');
  });

  it('can delete llm provider', () => {
    gcyAdvanced({
      value: 'llm-provider-item-menu',
      name: 'organization-provider',
    }).click();
    gcy('llm-provider-menu-item-delete').click();
    confirmStandard();
    assertMessage('Provider deleted');
    gcy('llm-provider-item-name').should('not.exist');
  });

  it('server providers visible', () => {
    gcy('organization-llm-providers-tab').contains('Server').click();
    gcy('llm-provider-item-name').should('contain', 'server-provider');
    gcyAdvanced({
      value: 'llm-provider-item-type',
      name: 'server-provider',
    }).should('have.text', 'OpenAI');
  });

  function visitLlmProviders(organizationSlug: string) {
    cy.visit(`${HOST}/organizations/${organizationSlug}/llm-providers`);
  }
});
