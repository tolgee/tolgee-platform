import { internalFetch } from '../common';

export const cleanTestData = (resource: string) => {
  return internalFetch(`e2e-data/${resource}/clean`, { timeout: 20000 });
};

export const generateTestData = (resource: string) => {
  return internalFetch(`e2e-data/${resource}/generate`);
};

export const generateTestDataObject = (resource: string) => ({
  generate: () => generateTestData(resource),
  clean: () => cleanTestData(resource),
});
