import { HOST } from './constants';

export const visitSingleKey = (
  projectId: number,
  keyName?: string,
  languages?: string[]
) => {
  cy.visit(
    `${HOST}/projects/${projectId}/translations/single?` +
      (keyName ? `key=${keyName}&` : '') +
      (languages ? languages.map((l) => `languages=${l}&`).join('') : '')
  );
  return cy
    .get('[data-cy="global-base-view-content"', {
      timeout: 50000,
    })
    .should('be.visible');
};
