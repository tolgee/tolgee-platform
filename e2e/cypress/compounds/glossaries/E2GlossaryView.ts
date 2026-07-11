import { HOST } from '../../common/constants';
import { E2GlossaryTermCreateEditDialog } from './E2GlossaryTermCreateEditDialog';
import { E2GlossaryImportDialog } from './E2GlossaryImportDialog';
import { confirmStandard, gcy } from '../../common/shared';
import {
  getGlossaryByNameFromOrganizationData,
  getOrganizationByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

export class E2GlossaryView {
  findAndVisit(
    data: TestDataStandardResponse,
    orgName: string,
    glossaryName: string
  ) {
    const organization = getOrganizationByNameFromTestData(data, orgName);
    const glossary = getGlossaryByNameFromOrganizationData(
      organization,
      glossaryName
    );
    this.visit(organization.slug, glossary.id);
  }

  visit(organizationSlug: string, glossaryId: number) {
    cy.visit(
      `${HOST}/organizations/${organizationSlug}/glossaries/${glossaryId}`
    );
  }

  toggleTermChecked(termDefaultTranslationOrDescription: string) {
    const escaped = Cypress._.escapeRegExp(termDefaultTranslationOrDescription);
    gcy('glossary-term-list-item')
      .filter(`:contains(${escaped})`)
      .find('input[type="checkbox"]')
      .click();
  }

  deleteCheckedTerms() {
    gcy('glossary-batch-delete-button').should('be.visible');
    gcy('glossary-batch-delete-button').should('be.enabled');
    gcy('glossary-batch-delete-button').click();
    confirmStandard();
  }

  setTranslation(current: string, translation: string | undefined) {
    gcy('glossary-translation-cell').filter(`:contains(${current})`).click();
    const chain = gcy('glossary-translation-edit-field')
      .find('textarea')
      .first()
      .clear();
    if (translation !== undefined) {
      chain.type(translation);
    }
    gcy('glossary-translation-save-button').click();
    gcy('glossary-translation-edit-field').should('not.exist');
  }

  checkTranslationExists(translation: string) {
    gcy('glossary-translation-cell').contains(translation).should('be.visible');
  }

  openCreateTermDialog(isFirst = false) {
    if (isFirst) {
      gcy('glossary-empty-add-term-button').click();
    } else {
      gcy('global-plus-button').click();
    }
    gcy('create-glossary-term-dialog').should('be.visible');
    return new E2GlossaryTermCreateEditDialog();
  }

  openEditTermDialog(termDefaultTranslationOrDescription: string) {
    gcy('glossary-term-list-item')
      .filter(`:contains(${termDefaultTranslationOrDescription})`)
      .click();
    gcy('create-glossary-term-dialog').should('be.visible');
    return new E2GlossaryTermCreateEditDialog();
  }

  openImportDialogWhenGlossaryIsEmpty() {
    gcy('glossary-empty-import-terms-button').click();
    gcy('glossary-import-dialog').should('be.visible');
    return new E2GlossaryImportDialog();
  }

  openImportDialog() {
    gcy('glossary-import-button').click();
    gcy('glossary-import-dialog').should('be.visible');
    return new E2GlossaryImportDialog();
  }

  clickExport() {
    gcy('glossary-export-button').click();
  }
}
