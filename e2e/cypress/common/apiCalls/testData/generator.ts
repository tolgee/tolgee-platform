import { ArgumentTypes } from '../../types';
import { internalFetch } from '../common';

export const cleanTestData = (
  resource: string,
  options?: ArgumentTypes<typeof cy.request>[0]
) => {
  return internalFetch(`e2e-data/${resource}/clean`, {
    timeout: 60000,
    ...options,
  });
};

export const generateTestData = (resource: string) => {
  return internalFetch(`e2e-data/${resource}/generate`, { timeout: 60000 });
};

export const generateStandardTestData = (resource: string) => {
  return internalFetch(`e2e-data/${resource}/generate-standard`, {
    timeout: 60000,
  }) as Cypress.Chainable<Cypress.Response<TestDataStandardResponse>>;
};

export type TestDataStandardResponse = {
  projects: { name: string; id: number }[];
  users: { username: string; name: string; id: number }[];
  organizations: {
    id: number;
    name: string;
    slug: string;
    glossaries: { id: number; name: string }[];
  }[];
  invitations: { code: string; projectId: number; organizationId: number }[];
};

export const generateTestDataObject = (resource: string) => ({
  generate: () => generateTestData(resource),
  generateStandard: () => generateStandardTestData(resource),
  clean: (options?: ArgumentTypes<typeof cy.request>[0]) =>
    cleanTestData(resource, options),
});
