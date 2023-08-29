import { components } from '../../../../webapp/src/service/apiSchema.generated';
import { v2apiFetchPromise } from '../apiCalls/common';
import { waitForGlobalLoading } from '../loading';

export type ProjectModel = components['schemas']['ProjectModel'];
export type LanguageModel = components['schemas']['LanguageModel'];

export type ProjectInfo = {
  project: ProjectModel;
  languages: LanguageModel[];
};

export const pageAcessibleWithoutErrors = () => {
  waitForGlobalLoading();
  cy.get('.SnackbarItem-variantError', { timeout: 0 }).should('not.exist');
};

export function getProjectInfo(projectId: number) {
  return new Cypress.Promise<ProjectInfo>((resolve) =>
    Promise.all([
      v2apiFetchPromise(`projects/${projectId}`).then((r) => r.body),
      v2apiFetchPromise(`projects/${projectId}/languages`).then((r) => r.body),
    ]).then(([pdata, ldata]) =>
      resolve({ project: pdata, languages: ldata._embedded.languages })
    )
  );
}

export function getLanguageName(languages: LanguageModel[], id: number) {
  return languages.find((l) => l.id === id).name;
}

export function getLanguageId(languages: LanguageModel[], tag: string) {
  return languages.find((l) => l.tag === tag)?.id;
}

export function getLanguages() {
  return [
    ['en', `English`],
    ['de', `German`],
    ['cs', `Czech`],
  ];
}
