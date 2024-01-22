import {
  addScreenshot,
  createKeyPromise,
  createProject,
  deleteProject,
  login,
} from '../common/apiCalls/common';
import { HOST } from '../common/constants';
import 'cypress-file-upload';
import { getPopover } from '../common/shared';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import { components } from '../../../webapp/src/service/apiSchema.generated';

describe('Screenshots', { retries: 5 }, () => {
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
            originalName: 'Engish',
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

  it('opens popup', () => {
    getCameraButton(1).click();
    cy.contains('No screenshots have been added yet.');
  });

  it('uploads file', () => {
    getCameraButton(1).click();
    cy.contains('No screenshots have been added yet.');
    cy.get('[data-cy=dropzone]').attachFile('screenshots/test_1.png', {
      subjectType: 'drag-n-drop',
    });

    cy.gcy('screenshot-image')
      .should('be.visible')
      .and(($img) => {
        expect(($img[0] as HTMLImageElement).naturalWidth).to.be.greaterThan(0);
      });
  });

  it('uploads with hidden input', () => {
    getCameraButton(1).click();
    cy.contains('No screenshots have been added yet.');
    cy.xpath("//input[@type='file']").attachFile('screenshots/test_1.png');
    cy.gcy('screenshot-image')
      .should('be.visible')
      .and(($img) => {
        expect(($img[0] as HTMLImageElement).naturalWidth).to.be.greaterThan(0);
      });
  });

  it(
    'uploads multiple',
    {
      retries: {
        runMode: 6,
      },
    },
    () => {
      getCameraButton(1).click();
      cy.contains('No screenshots have been added yet.');
      cy.get('[data-cy=dropzone]')
        .attachFile('screenshots/test_1.png', { subjectType: 'drag-n-drop' })
        .attachFile('screenshots/test_1.png', { subjectType: 'drag-n-drop' })
        .attachFile('screenshots/test_1.png', { subjectType: 'drag-n-drop' });
      cy.gcy('screenshot-image', { timeout: 15000 })
        .should('be.visible')
        .and(($img) => {
          expect($img.length).to.be.equal(3);
          for (let i = 0; i < $img.length; i++) {
            expect(
              ($img[i] as HTMLImageElement).naturalWidth
            ).to.be.greaterThan(0);
          }
        });
    }
  );

  it('images and plus button is visible', () => {
    addScreenshot(project.id, keys[3].id, 'screenshots/test_1.png').then(() => {
      getCameraButton(4).click();
      cy.gcy('screenshot-image')
        .should('be.visible')
        .and(($img) => {
          expect($img.length).to.be.equal(1);
          expect(($img[0] as HTMLImageElement).naturalWidth).to.be.greaterThan(
            0
          );
        });
      cy.xpath(
        "//*[text() = 'Screenshots']/parent::*/parent::*/parent::*//div[contains(@data-cy, 'add-box')]"
      ).should('be.visible');
    });
  });

  it('screenshots are visible only for key, where uploaded', () => {
    const promises = [];

    for (let i = 0; i < 10; i++) {
      promises.push(
        addScreenshot(project.id, keys[1].id, 'screenshots/test_1.png')
      );
    }

    Cypress.Promise.all(promises).then(() => {
      getCameraButton(2).click();
      cy.gcy('screenshot-image')
        .should('be.visible')
        .and(($img) => {
          expect($img.length).to.be.equal(10);
        });
      getPopover().xpath('./div[1]').click({ force: true });
      getCameraButton(1).click();
      cy.contains('No screenshots have been added yet.');
    });
  });

  it('deletes screenshot', () => {
    const promises = [];

    for (let i = 0; i < 5; i++) {
      promises.push(
        addScreenshot(project.id, keys[1].id, 'screenshots/test_1.png')
      );
    }

    Cypress.Promise.all(promises).then(() => {
      getCameraButton(2).click();

      for (let i = 5; i >= 1; i--) {
        cy.gcy('screenshot-image')
          .should('be.visible')
          .and(($img) => {
            expect($img.length).to.be.equal(i);
          });
        cy.gcy('screenshot-image')
          .first()
          .trigger('mouseover')
          .xpath(
            "./ancestor::div[contains(@data-cy, 'screenshot-thumbnail')]/button"
          )
          .click();
        cy.contains('Confirm').click();
        if (i > 1) {
          cy.gcy('screenshot-image')
            .should('be.visible')
            .and(($img) => {
              expect($img.length).to.be.equal(i - 1);
            });
        }
      }

      cy.reload();
      getCameraButton(2).click();
      cy.contains('No screenshots have been added yet.');
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

const getCameraButton = (nth: number) => {
  // focus row checkbox, to make sure, row buttons are in place
  cy.waitForDom();
  cy.xpath(`(//*[@data-cy='translations-row'])[${nth}]//input`).focus();
  return cy.xpath(
    `(//*[@data-cy='translations-row'])[${nth}]//*[@data-cy='translations-cell-screenshots-button']`
  );
};
