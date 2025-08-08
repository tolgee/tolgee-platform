import { ProjectDTO } from '../../../../../webapp/src/service/response.types';
import { internalFetch } from '../common';
import {
  cleanTestData,
  generateTestDataObject,
  TestDataStandardResponse,
} from './generator';
import { components } from '../../../../../webapp/src/service/apiSchema.generated';

export type PermissionModelScopes =
  components['schemas']['PermissionModel']['scopes'];

export type SuggestionMode =
  components['schemas']['ProjectModel']['suggestionsMode'];
export type TranslationProtection =
  components['schemas']['ProjectModel']['translationProtection'];

export const ssoOrganizationsLoginTestData = generateTestDataObject(
  'sso-organizations-login'
);

export const organizationTestData = generateTestDataObject('organizations');

export const organizationNewTestData =
  generateTestDataObject('organization-new');

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

export const importNamespacesTestData =
  generateTestDataObject('import-namespaces');

export const projectListData = generateTestDataObject(
  'projects-list-dashboard'
);

export const projectTestData = generateTestDataObject('projects');

export const apiKeysTestData = generateTestDataObject('api-keys');

export const patsTestData = generateTestDataObject('pat');

export const languagePermissionsData = generateTestDataObject(
  'language-permissions'
);

export const contentDeliveryTestData =
  generateTestDataObject('content-delivery');

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
    ).then((r) => r.body as ProjectDTO),

  cleanupForFilters: () =>
    internalFetch('e2e-data/translations/cleanup-for-filters'),

  generateForFilters: () =>
    internalFetch('e2e-data/translations/generate-for-filters').then(
      (r) => r.body as ProjectDTO
    ),
};

export const translationsDisabled = generateTestDataObject(
  'translation-disabled'
);

export const emptyProjectTestData = generateTestDataObject('empty-project');

export const selfHostedLimitsTestData =
  generateTestDataObject('self-hosted-limits');

export const translationsNsAndTagsTestData =
  generateTestDataObject('ns-and-tags');

export const projectLeavingTestData = generateTestDataObject('project-leaving');

export const projectTransferringTestData = generateTestDataObject(
  'project-transferring'
);

export const avatarTestData = generateTestDataObject('avatars');

export const administrationTestData = generateTestDataObject('administration');

export const translationWebsocketsTestData = generateTestDataObject(
  'websocket-translations'
);

export const userDeletionTestData = generateTestDataObject('user-deletion');

export const formerUserTestData = generateTestDataObject('former-user');

export const namespaces = generateTestDataObject('namespaces');

export const tasks = generateTestDataObject('task');

export const prompt = generateTestDataObject('prompt');

export const batchJobs = generateTestDataObject('batch-jobs');

export const glossaryTestData = generateTestDataObject('glossary');

export const notificationTestData = generateTestDataObject('notification');

export const authProviderChange = generateTestDataObject(
  'auth-provider-change'
);

export const labelsTestData = generateTestDataObject('label');

export const suggestionsTestData = {
  ...generateTestDataObject('suggestions'),
  generate: (props?: {
    suggestionsMode?: SuggestionMode;
    translationProtection?: TranslationProtection;
    disableTranslation?: boolean;
  }) =>
    internalFetch(
      `e2e-data/suggestions/generate?suggestionsMode=${
        props?.suggestionsMode ?? 'DISABLED'
      }&translationProtection=${
        props?.translationProtection ?? 'NONE'
      }&disableTranslation=${props?.disableTranslation ?? false}`
    ) as Cypress.Chainable<Cypress.Response<TestDataStandardResponse>>,
};

export const sensitiveOperationProtectionTestData = {
  ...generateTestDataObject('sensitive-operation-protection'),
  getOtp: () =>
    internalFetch(`e2e-data/sensitive-operation-protection/get-totp`),
};

export type PermissionsOptions = {
  scopes: PermissionModelScopes;
  translateLanguageTags?: string[];
  viewLanguageTags?: string[];
  stateChangeLanguageTags?: string[];
};

export const generatePermissionsData = {
  generate: (options: Partial<PermissionsOptions>, useNamespaces = false) => {
    const params = new URLSearchParams();
    Object.entries(options).forEach(([key, values]) => {
      values.forEach((value) => {
        params.append(key, value);
      });
    });
    params.append('useNamespaces', String(useNamespaces));
    return internalFetch(
      `e2e-data/permissions/generate-with-user?${params.toString()}`
    );
  },
  clean: () => {
    return internalFetch('e2e-data/permissions/clean');
  },
};

export function getUserByUsernameFromTestData(
  data: TestDataStandardResponse,
  username: string
) {
  return data.users.find((user) => user.username === username);
}

export function getOrganizationByNameFromTestData(
  data: TestDataStandardResponse,
  name: string
) {
  return data.organizations.find((organization) => organization.name === name);
}

export function getGlossaryByNameFromOrganizationData(
  data: TestDataStandardResponse['organizations'][number],
  name: string
) {
  return data.glossaries.find((glossary) => glossary.name === name);
}

export function getGlossaryByNameFromTestData(
  data: TestDataStandardResponse,
  orgName: string,
  name: string
) {
  const organization = getOrganizationByNameFromTestData(data, orgName);
  return getGlossaryByNameFromOrganizationData(organization, name);
}

export function getProjectByNameFromTestData(
  data: TestDataStandardResponse,
  name: string
) {
  return data.projects.find((project) => project.name === name);
}

export function getInvitationsByProjectIdFromTestData(
  data: TestDataStandardResponse,
  projectId: number
) {
  return data.invitations.filter((i) => i.projectId === projectId);
}
