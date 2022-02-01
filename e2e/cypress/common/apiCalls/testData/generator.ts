import { internalFetch } from '../common';

export const cleanTestData = (resource: string) => {
  return internalFetch(`e2e-data/${resource}/clean`);
};

export const generateTestData = (resource: string) => {
  return internalFetch(`e2e-data/${resource}/generate`);
};

export const generateTestDataObject = (resource: string) => ({
  generate: () => generateTestData(resource),
  clean: () => cleanTestData(resource),
});
