import { gcy } from './shared';
import { HOST } from './constants';
import { waitForGlobalLoading } from './loading';

export const getFileIssuesDialog = () => {
  return gcy('import-file-issues-dialog');
};

export const getShowDataDialog = () => {
  return gcy('import-show-data-dialog');
};

export const getResolutionDialog = () => {
  return gcy('import-conflict-resolution-dialog');
};

export const assertInResultDialog = (text: string) => {
  getShowDataDialog().contains(text).scrollIntoView().should('be.visible');
};

export const assertInResolutionDialog = (text: string) => {
  getResolutionDialog()
    .contains(text, { timeout: 20000 })
    .scrollIntoView()
    .should('be.visible');
};

export const findResolutionRow = (key: string) => {
  return gcy('import-resolution-dialog-data-row')
    .findDcy('import-resolution-dialog-key-name')
    .contains(key)
    .closestDcy('import-resolution-dialog-data-row');
};

export const visitImport = (projectId: number) => {
  cy.visit(`${HOST}/projects/${projectId}/import`);
  waitForGlobalLoading(undefined, 10000);
};

export const getLanguageRow = (filename: string) => {
  return cy
    .xpath(`//*[@data-cy='import-result-row']//*[. = '${filename}']`)
    .closestDcy('import-result-row');
};

export const getLanguageSelect = (filename: string) => {
  return cy.xpath(
    `//*[@data-cy='import-result-row']//*[. = '${filename}']` +
      `/ancestor::*[@data-cy='import-result-row']` +
      `//*[@data-cy='import-row-language-select-form-control']`
  );
};
