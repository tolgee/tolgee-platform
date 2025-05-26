import { login } from '../../common/apiCalls/common';
import { labelsTestData } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import { gcy } from '../../common/shared';

let testLabelsAccess = true;

describe('Projects Settings - Labels', () => {
  beforeEach(() => {
    labelsTestData.clean();
    labelsTestData.generate().then((data) => {
      login('test_username');
      const projectId = data.body.projects[0].id;
      if (testLabelsAccess) {
        cy.visit(`${HOST}/projects/${projectId}/manage/edit`);
      } else {
        cy.visit(`${HOST}/projects/${projectId}/manage/edit/labels`);
      }
    });
  });

  it('list project labels', () => {
    gcy('project-settings-menu-labels').should('be.visible').click();
    gcy('project-settings-label-item')
      .first()
      .within(() => {
        gcy('project-settings-label-item-name')
          .should('be.visible')
          .contains('First label');
        gcy('project-settings-label-item-color')
          .should('be.visible')
          .should('have.css', 'color', 'rgb(255, 0, 0)')
          .contains('#FF0000');
        gcy('project-settings-label-item-description')
          .should('be.visible')
          .contains('This is a description');
      });
    testLabelsAccess = false;
  });

  it('create project label', () => {
    gcy('project-settings-labels-add-button')
      .click()
      .then(() => {
        gcy('label-modal')
          .should('be.visible')
          .within(() => {
            cy.get('input[name="name"]').type('test-label');
            cy.get('input[name="color"]').then(($input) => {
              cy.wrap($input)
                .invoke('val')
                .should('match', new RegExp('^#[A-Fa-f0-9]{6}$'));
              cy.wrap($input).clear().type('#FF0055');
            });
            cy.get('textarea[name="description"]').type(
              'New label description'
            );
            gcy('global-form-save-button').click();
          });
      });
    gcy('project-settings-label-item')
      .should('have.length', 2)
      .last()
      .within(() => {
        gcy('project-settings-label-item-name')
          .should('be.visible')
          .contains('test-label');
      });
  });

  it('edit project label', () => {
    gcy('project-settings-label-item')
      .first()
      .within(() => {
        gcy('project-settings-labels-edit-button').click();
      });
    gcy('label-modal')
      .should('be.visible')
      .within(() => {
        cy.get('input[name="name"]').clear().type('Edited label');
        cy.get('input[name="color"]').clear().type('#00FF00');
        cy.get('textarea[name="description"]')
          .clear()
          .type('Edited label description');
        gcy('global-form-save-button').click();
      });
    gcy('project-settings-label-item')
      .first()
      .within(() => {
        gcy('project-settings-label-item-name')
          .should('be.visible')
          .contains('Edited label');
        gcy('project-settings-label-item-color')
          .should('have.css', 'color', 'rgb(0, 255, 0)')
          .contains('#00FF00');
        gcy('project-settings-label-item-description')
          .should('be.visible')
          .contains('Edited label description');
      });
  });

  it('remove project label', () => {
    gcy('project-settings-label-item')
      .first()
      .within(() => {
        gcy('project-settings-labels-remove-button').click();
      });
    gcy('global-confirmation-dialog').within(() => {
      gcy('global-confirmation-confirm').click();
    });
    gcy('project-settings-label-item').should('have.length', 0);
  });
});
