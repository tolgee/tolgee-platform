import { HOST } from './constants';

type Props = {
  projectId: number;
  key?: string;
  languages?: string[];
  namespace?: string;
};

export const visitSingleKey = ({
  projectId,
  key,
  languages,
  namespace,
}: Props) => {
  cy.visit(
    `${HOST}/projects/${projectId}/translations/single?` +
      (key ? `key=${key}&` : '') +
      (namespace ? `ns=${namespace}&` : '') +
      (languages ? languages.map((l) => `languages=${l}&`).join('') : '')
  );
  return cy
    .get('[data-cy="global-base-view-content"', {
      timeout: 50000,
    })
    .should('be.visible');
};
