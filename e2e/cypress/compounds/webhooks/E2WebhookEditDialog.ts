import { gcy } from '../../common/shared';

export class E2WebhookEditDialog {
  setUrl(url: string) {
    gcy('webhook-form-url').find('input').clear().type(url);
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
