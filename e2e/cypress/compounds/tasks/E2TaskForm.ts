import { dismissMenu } from '../../common/shared';

export class E2TaskForm {
  fill({ name, languages }: { name: string | undefined; languages: string[] }) {
    this.setName(name);
    this.setLanguages(languages);
  }

  setName(name: string | undefined) {
    if (name) {
      cy.gcy('create-task-field-name').type(name);
    }
  }

  clearStateFilters() {
    cy.gcy('translations-state-filter-clear').click();
  }

  setLanguages(languages: string[]) {
    cy.gcy('create-task-field-languages').click();
    languages.forEach((l) => {
      cy.gcy('create-task-field-languages-item').contains(l).click();
    });
    dismissMenu();
  }
}
