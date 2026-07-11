import {
  addScreenshot,
  createKeyPromise,
  createProject,
  deleteProject,
  login,
} from '../common/apiCalls/common';
import { HOST } from '../common/constants';
import 'cypress-file-upload';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import { components } from '../../../webapp/src/service/apiSchema.generated';
import { waitForGlobalLoading } from '../common/loading';

describe('Screenshots', () => {
  let project: ProjectDTO = null;
  let keys: components['schemas']['KeyModel'][];

  beforeEach(() => {
    login().then(() => {
      createProject({
        name: 'Test',
        languages: [
          {
            tag: 'en',
            name: 'English',
            originalName: 'English',
          },
          {
            tag: 'cs',
            name: 'Czech',
            originalName: 'čeština',
          },
        ],
      }).then((r) => {
        project = r.body as ProjectDTO;
        window.localStorage.setItem(
          'selectedLanguages',
          `{"${project.id}":["en"]}`
        );
        const promises = [];
        for (let i = 1; i < 5; i++) {
          promises.push(
            createKeyPromise(
              project.id,
              `Cool key ${i.toString().padStart(2, '0')}`,
              {
                en: 'Cool',
              }
            )
          );
        }
        return Cypress.Promise.all(promises).then((keyResponse) => {
          keys = keyResponse as any;
          visit(project.id);
        });
      });
    });
  });

  it('uploads with hidden input', () => {
    getAndFocusRow(0)
      .findDcy('cell-key-screenshot-file-input')
      .attachFile('screenshots/test_1.png', {
        subjectType: 'input',
      });

    cy.waitForDom();
    getAndFocusRow(0).findDcy('screenshot-thumbnail').should('have.length', 1);
  });

  it('dropzone appears on dragover', () => {
    cy.gcy('translations-row').eq(1).trigger('dragover');
    cy.gcy('translations-row')
      .eq(1)
      .findDcy('cell-key-screenshot-dropzone')
      .should('be.visible');
    cy.gcy('translations-row').eq(1).trigger('dragleave');
    cy.gcy('cell-key-screenshot-dropzone').should('not.exist');
  });

  it('uploads multiple', () => {
    getAndFocusRow(0).findDcy('screenshot-thumbnail').should('have.length', 0);
    getAndFocusRow(0)
      .findDcy('cell-key-screenshot-file-input')
      .attachFile('screenshots/test_1.png', { subjectType: 'input' })
      .attachFile('screenshots/test_2.png', { subjectType: 'input' })
      .attachFile('screenshots/test_3.png', { subjectType: 'input' });
    getAndFocusRow(0).findDcy('screenshot-thumbnail').should('have.length', 3);
  });

  it('images and plus button is visible', () => {
    addScreenshot(project.id, keys[3].id, 'screenshots/test_1.png').then(() => {
      getAndFocusRow(3)
        .findDcy('screenshot-thumbnail')
        .should('have.length', 1)
        .should('be.visible');
      getAndFocusRow(3)
        .findDcy('translations-cell-screenshots-button')
        .should('be.visible');
    });
  });

  it('screenshots are visible only for key, where uploaded', () => {
    const promises = [];

    for (let i = 0; i < 3; i++) {
      promises.push(
        addScreenshot(project.id, keys[1].id, `screenshots/test_${i + 1}.png`)
      );
    }

    Cypress.Promise.all(promises).then(() => {
      cy.reload();
      getAndFocusRow(0)
        .findDcy('screenshot-thumbnail')
        .should('have.length', 0);
      getAndFocusRow(1)
        .findDcy('screenshot-thumbnail')
        .should('have.length', 3);
    });
  });

  it('deletes screenshot', () => {
    const promises = [];
    for (let i = 0; i < 3; i++) {
      promises.push(
        addScreenshot(project.id, keys[0].id, `screenshots/test_${i + 1}.png`)
      );
    }

    Cypress.Promise.all(promises).then(() => {
      cy.reload();
      for (let i = 0; i < 3; i++) {
        getAndFocusRow(0)
          .findDcy('screenshot-thumbnail')
          .first()
          .siblingDcy('screenshot-thumbnail-delete')
          .click({ force: true });

        cy.contains('Confirm').click();
        waitForGlobalLoading();
        cy.waitForDom();
        getAndFocusRow(0)
          .findDcy('screenshot-thumbnail')
          .should('have.length', 2 - i);
      }

      cy.reload();
      getAndFocusRow(0)
        .findDcy('screenshot-thumbnail')
        .should('have.length', 0);
    });
  });

  afterEach(() => {
    if (project) {
      deleteProject(project.id);
    }
  });

  const visit = (projectId) => {
    cy.visit(`${HOST}/projects/${projectId}/translations`);
  };
});

const getAndFocusRow = (nth: number) => {
  cy.waitForDom();
  cy.gcy('translations-row').eq(nth).find('input').first().focus();
  cy.waitForDom();
  return cy.gcy('translations-row').eq(nth);
};
