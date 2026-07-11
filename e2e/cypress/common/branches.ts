import { HOST } from './constants';

export const visitBranches = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/branches`);
};
