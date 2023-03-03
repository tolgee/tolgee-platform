import { components } from '../../../../webapp/src/service/apiSchema.generated';
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
