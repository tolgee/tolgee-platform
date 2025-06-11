import { gcy } from '../../../common/shared';
import { E2LabelModal } from './E2LabelModal';
import { HOST } from '../../../common/constants';

export class E2ProjectLabelsSection {
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/manage/edit/labels`);
  }

  openFromProjectSettings(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/manage/edit`);
    gcy('project-settings-menu-labels').should('be.visible').click();
  }

  openCreateLabelModal() {
    gcy('project-settings-labels-add-button').click();
    gcy('label-modal').should('be.visible');
    return new E2LabelModal();
  }

  openEditLabelModal(labelName: string) {
    gcy('project-settings-label-item')
      .filter(`:contains(${labelName})`)
      .first()
      .within(() => {
        gcy('project-settings-labels-edit-button').click();
      });
    gcy('label-modal').should('be.visible');
    return new E2LabelModal();
  }

  deleteLabel(labelName: string) {
    gcy('project-settings-label-item')
      .filter(`:contains(${labelName})`)
      .first()
      .within(() => {
        gcy('project-settings-labels-remove-button').click();
      });
    gcy('global-confirmation-confirm').click();
  }

  assertLabelExists(labelName: string, description?: string, color?: string) {
    gcy('project-settings-label-item')
      .filter(`:contains(${labelName})`)
      .first()
      .within(() => {
        if (description) {
          gcy('project-settings-label-item-description')
            .should('be.visible')
            .contains(description);
        }
        if (color) {
          gcy('project-settings-label-item-label').should(
            'have.css',
            'background-color',
            color
          );
        }
      });
  }

  assertLabelsCount(count: number) {
    gcy('project-settings-label-item').should('have.length', count);
  }
}
