import { HOST } from './constants';

type Props = {
  projectId: number;
  key?: string;
  languages?: string[];
  namespace?: string;
  branch?: string;
};

export const visitSingleKey = ({
  projectId,
  branch,
  key,
  languages,
  namespace,
}: Props) => {
  cy.visit(
    `${HOST}/projects/${projectId}/translations/single` +
      (branch ? `/tree/${encodeURIComponent(branch)}?` : '?') +
      (key ? `key=${encodeURIComponent(key)}&` : '') +
      (namespace ? `ns=${encodeURIComponent(namespace)}&` : '') +
      (languages
        ? languages.map((l) => `languages=${encodeURIComponent(l)}&`).join('')
        : '')
  );
  return cy
    .get('[data-cy="global-base-view-content"', {
      timeout: 50000,
    })
    .should('be.visible');
};
