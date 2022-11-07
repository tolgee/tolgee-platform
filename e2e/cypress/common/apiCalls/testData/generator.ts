import { internalFetch } from '../common';

export const cleanTestData = (resource: string) => {
  return internalFetch(`e2e-data/${resource}/clean`);
};

export const generateTestData = (resource: string) => {
  return internalFetch(`e2e-data/${resource}/generate`);
};

export const generateStandardTestData = (resource: string) => {
  return internalFetch(
    `e2e-data/${resource}/generate-standard`
  ) as Cypress.Chainable<Cypress.Response<TestDataStandardResponse>>;
};

export type TestDataStandardResponse = {
  projects: { name: string; id: number }[];
  users: { username: string; name: string; id: number };
};

export const generateTestDataObject = (resource: string) => ({
  generate: () => generateTestData(resource),
  generateStandard: () => generateStandardTestData(resource),
  clean: () => cleanTestData(resource),
});
