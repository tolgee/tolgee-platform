import { ProjectDTO } from '../../../../../webapp/src/service/response.types';
import { internalFetch } from '../common';
import { cleanTestData, generateTestDataObject } from './generator';

export const organizationTestData = generateTestDataObject('organizations');

export const languagesTestData = generateTestDataObject('languages');

export const commentsTestData = generateTestDataObject('translation-comments');

export const translationSingleTestData =
  generateTestDataObject('translation-single');

export const importTestData = {
  clean: () => cleanTestData('import'),
  generateBasic: () => internalFetch('e2e-data/import/generate'),
  generateApplicable: () =>
    internalFetch('e2e-data/import/generate-applicable'),
  generateAllSelected: () =>
    internalFetch('e2e-data/import/generate-all-selected'),
  generateLotOfData: () =>
    internalFetch('e2e-data/import/generate-lot-of-data'),
  generateBase: () => internalFetch('e2e-data/import/generate-base'),
  generateWithManyLanguages: () =>
    internalFetch('e2e-data/import/generate-many-languages'),
  generateWithLongText: () =>
    internalFetch('e2e-data/import/generate-with-long-text'),
};

export const projectsDashboardData = generateTestDataObject(
  'projects-list-dashboard'
);

export const projectTestData = generateTestDataObject('projects');

export const languagePermissionsData = generateTestDataObject(
  'language-permissions'
);

export const generateExampleKeys = (
  projectId: number,
  numberOfExamples: number
) =>
  internalFetch(
    `e2e-data/translations/generate/${projectId}/${numberOfExamples}`
  );

export const translationsTestData = {
  generateExampleKeys: (projectId: number, numberOfExamples: number) =>
    internalFetch(
      `e2e-data/translations/generate/${projectId}/${numberOfExamples}`
    ),

  cleanupForFilters: () =>
    internalFetch('e2e-data/translations/cleanup-for-filters'),

  generateForFilters: () =>
    internalFetch('e2e-data/translations/generate-for-filters').then(
      (r) => r.body as ProjectDTO
    ),
};

export const projectLeavingTestData = generateTestDataObject('project-leaving');

export const projectTransferringTestData = generateTestDataObject(
  'project-transferring'
);

export const avatarTestData = generateTestDataObject('avatars');
