import { E2TaskForm } from './E2TaskForm';

export class E2OrderTranslationDialog {
  selectAgency() {
    cy.gcy('translation-agency-item').contains('Agency 1').click();
    cy.gcy('order-translation-next').click();
  }

  getTaskForm() {
    return new E2TaskForm();
  }

  submit() {
    cy.gcy('order-translation-submit').click();
  }
}
