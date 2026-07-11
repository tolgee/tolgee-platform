import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { E2GlossariesView } from '../../compounds/glossaries/E2GlossariesView';
import { E2GlossaryCreateEditDialog } from '../../compounds/glossaries/E2GlossaryCreateEditDialog';

describe('Glossary editing', () => {
  let view: E2GlossariesView;
  let dialog: E2GlossaryCreateEditDialog;

  beforeEach(() => {
    glossaryTestData.clean();
    glossaryTestData.generateStandard().then((res) => {
      login('Owner');
      view = new E2GlossariesView();
      view.findAndVisit(res.body, 'Owner');

      dialog = view.openEditGlossaryDialog('Empty Glossary');
    });
  });

  afterEach(() => {
    glossaryTestData.clean();
  });

  it('Edits glossary name', () => {
    dialog.setName('Edited Glossary Name');
    dialog.submit();

    gcy('glossary-list-item')
      .filter(':contains("Edited Glossary Name")')
      .should('be.visible');
  });

  it('Edits glossary base language', () => {
    dialog.setBaseLanguage('English');
    dialog.submit();

    const dialogAfterEdit = view.openEditGlossaryDialog('Empty Glossary');
    dialogAfterEdit.checkBaseLanguage('English');
    dialogAfterEdit.cancel();
  });

  it('Edits glossary assigned projects', () => {
    dialog.toggleAssignedProject('TheProject');
    dialog.submit();

    const dialogAfterEdit = view.openEditGlossaryDialog('Empty Glossary');
    cy.should('not.contain', 'TheProject');
    dialogAfterEdit.cancel();
  });
});
