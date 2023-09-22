import { createKey, createProject, login } from './apiCalls/common';
import { HOST } from './constants';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import Chainable = Cypress.Chainable;
import { dismissMenu } from './shared';

export function createExportableProject(): Chainable<ProjectDTO> {
  return login().then(() => {
    return createProject({
      name: 'Test project',
      languages: [
        {
          tag: 'en',
          name: 'English',
          originalName: 'English',
        },
        {
          tag: 'cs',
          name: 'Česky',
          originalName: 'česky',
        },
      ],
    }).then((r) => {
      return r.body as ProjectDTO;
    });
  });
}

export const visitExport = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/export`);
};

export const create4Translations = (projectId: number) => {
  const promises = [];
  for (let i = 1; i < 5; i++) {
    promises.push(
      createKey(projectId, `Cool key ${i.toString().padStart(2, '0')}`, {
        en: `Cool translated text ${i}`,
        cs: `Studený přeložený text ${i}`,
      })
    );
  }
};

export const exportToggleLanguage = (lang: string) => {
  cy.gcy('export-language-selector').click();
  cy.gcy('export-language-selector-item').contains(lang).click();
  dismissMenu();
};

export const exportSelectFormat = (format: string) => {
  cy.gcy('export-format-selector').click();
  cy.gcy('export-format-selector-item').contains(format).click();
  dismissMenu();
};
