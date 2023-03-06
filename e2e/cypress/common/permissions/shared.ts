import { components } from '../../../../webapp/src/service/apiSchema.generated';
import { v2apiFetchPromise } from '../apiCalls/common';
import { waitForGlobalLoading } from '../loading';

export type ProjectModel = components['schemas']['ProjectModel'];
export type LanguageModel = components['schemas']['LanguageModel'];

export type ProjectInfo = {
  project: ProjectModel;
  languages: LanguageModel[];
};

export const pageIsPermitted = () => {
  waitForGlobalLoading();
  cy.get('.SnackbarItem-variantError', { timeout: 0 }).should('not.exist');
};

export function getProjectInfo(projectId: number) {
  return new Cypress.Promise<ProjectInfo>((resolve) =>
    Promise.all([
      v2apiFetchPromise(`projects/${projectId}`).then((r) => r.body),
      v2apiFetchPromise(`projects/${projectId}/languages`).then(
        (r_1) => r_1.body
      ),
    ]).then(([pdata, ldata]) =>
      resolve({ project: pdata, languages: ldata._embedded.languages })
    )
  );
}
