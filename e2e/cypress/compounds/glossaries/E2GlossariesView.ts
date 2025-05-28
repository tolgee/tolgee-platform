import { HOST } from '../../common/constants';
import { confirmHardMode, gcy } from '../../common/shared';
import { E2GlossaryCreateEditDialog } from './E2GlossaryCreateEditDialog';
import { E2GlossaryView } from './E2GlossaryView';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { getOrganizationByNameFromTestData } from '../../common/apiCalls/testData/testData';

export class E2GlossariesView {
  findAndVisit(data: TestDataStandardResponse, orgName: string) {
    const organization = getOrganizationByNameFromTestData(data, orgName);
    this.visit(organization.slug);
  }

  visit(organizationSlug: string) {
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);
  }

  openGlossary(name: string) {
    gcy('glossary-list-item').filter(`:contains(${name})`).click();
    return new E2GlossaryView();
  }

  openGlossaryMenu(name: string) {
    gcy('glossary-list-item')
      .filter(`:contains(${name})`)
      .findDcy('glossaries-list-more-button')
      .click();
  }

  deleteGlossary(name: string) {
    this.openGlossaryMenu(name);
    gcy('glossary-delete-button').click();
    confirmHardMode();
  }

  openCreateGlossaryDialog(isFirst = false): E2GlossaryCreateEditDialog {
    if (isFirst) {
      gcy('glossaries-empty-add-button').click();
    } else {
      gcy('global-plus-button').click();
    }
    gcy('create-edit-glossary-dialog').should('be.visible');
    return new E2GlossaryCreateEditDialog();
  }

  openEditGlossaryDialog(name: string): E2GlossaryCreateEditDialog {
    this.openGlossaryMenu(name);
    gcy('glossary-edit-button').click();

    gcy('create-edit-glossary-dialog').should('be.visible');
    return new E2GlossaryCreateEditDialog();
  }
}
