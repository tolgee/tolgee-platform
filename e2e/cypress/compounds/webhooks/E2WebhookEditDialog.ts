import { gcy, gcyAdvanced } from '../../common/shared';

export class E2WebhookEditDialog {
  setUrl(url: string) {
    gcy('webhook-form-url').find('input').clear().type(url);
  }

  toggleEventType(type: string) {
    gcy('webhook-form-event-types').click();
    gcyAdvanced({ value: 'webhook-form-event-type-option', type }).click();
    cy.get('body').type('{esc}');
  }

  save() {
    gcy('webhook-form-save').click();
  }

  cancel() {
    gcy('webhook-form-cancel').click();
  }

  delete() {
    gcy('webhook-form-delete').click();
  }
}
